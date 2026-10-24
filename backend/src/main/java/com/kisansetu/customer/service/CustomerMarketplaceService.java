package com.kisansetu.customer.service;

import com.kisansetu.common.PageResponse;
import com.kisansetu.common.util.GeoUtil;
import com.kisansetu.customer.dto.FarmerSummaryResponse;
import com.kisansetu.customer.dto.PriceCompareGroup;
import com.kisansetu.customer.dto.PriceCompareRow;
import com.kisansetu.farmer.dto.ListingResponse;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.customer.repository.FarmerRatingRepository;
import com.kisansetu.customer.entity.FarmerRating;
import com.kisansetu.user.entity.Profile;
import com.kisansetu.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Customer-facing marketplace: farmer discovery with distance/ratings,
 * produce browsing, price comparison across farmers.
 */
@Service
@RequiredArgsConstructor
public class CustomerMarketplaceService {

    private final MarketplaceListingRepository listingRepository;
    private final ProfileRepository profileRepository;
    private final FarmerRatingRepository ratingRepository;
    private final com.kisansetu.customer.repository.CustomerOrderRepository customerOrderRepository;

    @Transactional(readOnly = true)
    public List<FarmerSummaryResponse> getFarmers(Double userLat, Double userLng, Double radiusKm, String search,
                                                  String category, String sort) {
        List<MarketplaceListing> listings = listingRepository.findAll().stream()
                .filter(MarketplaceListing::isAvailable)
                .filter(l -> search == null || search.isBlank()
                        || l.getTitle().toLowerCase().contains(search.toLowerCase())
                        || l.getCategory().toLowerCase().contains(search.toLowerCase()))
                .filter(l -> category == null || category.isBlank() || l.getCategory().equals(category))
                .toList();

        Map<UUID, List<MarketplaceListing>> byFarmer = listings.stream()
                .collect(Collectors.groupingBy(MarketplaceListing::getFarmerId));
        if (byFarmer.isEmpty()) {
            return List.of();
        }
        Map<UUID, Profile> profiles = profileRepository.findByUserIds(new ArrayList<>(byFarmer.keySet()))
                .stream().collect(Collectors.toMap(Profile::getUserId, p -> p));
        Map<UUID, Object[]> ratingAgg = ratingRepository.aggregateByFarmerIds(new ArrayList<>(byFarmer.keySet()))
                .stream().collect(Collectors.toMap(a -> (UUID) a[0], a -> a));

        List<FarmerSummaryResponse> result = new ArrayList<>();
        for (var entry : byFarmer.entrySet()) {
            Profile profile = profiles.get(entry.getKey());
            if (profile == null) {
                continue;
            }
            List<MarketplaceListing> items = entry.getValue();
            BigDecimal avgPrice = items.stream().map(MarketplaceListing::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
            BigDecimal minPrice = items.stream().map(MarketplaceListing::getPrice)
                    .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxPrice = items.stream().map(MarketplaceListing::getPrice)
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            List<String> categories = items.stream().map(MarketplaceListing::getCategory)
                    .filter(Objects::nonNull).distinct().toList();

            Object[] agg = ratingAgg.get(entry.getKey());
            double avgRating = agg != null ? ((Number) agg[2]).doubleValue() : 0;
            long reviewCount = agg != null ? ((Number) agg[1]).longValue() : 0;

            Double distance = null;
            double lat = profile.getLatitude() != null ? profile.getLatitude().doubleValue() : Double.NaN;
            double lng = profile.getLongitude() != null ? profile.getLongitude().doubleValue() : Double.NaN;
            if (!Double.isNaN(lat) && !Double.isNaN(lng) && userLat != null && userLng != null) {
                distance = GeoUtil.distanceKm(userLat, userLng, lat, lng);
            }

            // Apply radius filter
            if (radiusKm != null && distance != null && distance > radiusKm) {
                continue;
            }

            result.add(new FarmerSummaryResponse(
                    profile.getUserId(), profile.getFullName(), profile.getCity(), profile.getState(),
                    profile.getAvatarUrl(), distance, items.size(), avgPrice, minPrice, maxPrice,
                    avgRating, reviewCount, categories));
        }

        if (sort != null) {
            switch (sort) {
                case "nearest" -> result.sort(Comparator.comparing(FarmerSummaryResponse::distanceKm,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                case "farthest" -> result.sort(Comparator.comparing(FarmerSummaryResponse::distanceKm,
                        Comparator.nullsFirst(Comparator.reverseOrder())));
                case "lowest" -> result.sort(Comparator.comparing(FarmerSummaryResponse::avgPrice));
                case "highest" -> result.sort(Comparator.comparing(FarmerSummaryResponse::avgPrice).reversed());
                case "top-rated" -> result.sort(Comparator.comparing(FarmerSummaryResponse::avgRating).reversed());
                default -> { }
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> browseProduce(String search, String category,
                                                       BigDecimal minPrice, BigDecimal maxPrice,
                                                       Pageable pageable) {
        return PageResponse.from(
                listingRepository.search(search, category, minPrice, maxPrice, null, pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getFarmerActiveListings(UUID farmerId) {
        return listingRepository.findByFarmerIdAndStatus(farmerId, MarketplaceListing.ListingStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<FarmerSummaryResponse> getFarmerProfile(UUID farmerId, Double userLat, Double userLng) {
        java.util.Optional<Profile> profileOpt = profileRepository.findByUserId(farmerId);
        if (profileOpt.isEmpty()) {
            return java.util.Optional.empty();
        }
        Profile profile = profileOpt.get();

        List<MarketplaceListing> listings = listingRepository.findByFarmerIdAndStatus(farmerId, MarketplaceListing.ListingStatus.ACTIVE);

        if (listings.isEmpty()) {
            return java.util.Optional.empty();
        }

        BigDecimal avgPrice = listings.stream().map(MarketplaceListing::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(listings.size()), 2, RoundingMode.HALF_UP);
        BigDecimal minPrice = listings.stream().map(MarketplaceListing::getPrice)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = listings.stream().map(MarketplaceListing::getPrice)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<String> categories = listings.stream().map(MarketplaceListing::getCategory)
                .filter(Objects::nonNull).distinct().toList();

        Object[] agg = ratingRepository.aggregateByFarmerIds(java.util.List.of(farmerId))
                .stream().findFirst().orElse(null);
        double avgRating = agg != null ? ((Number) agg[2]).doubleValue() : 0;
        long reviewCount = agg != null ? ((Number) agg[1]).longValue() : 0;

        Double distance = null;
        double lat = profile.getLatitude() != null ? profile.getLatitude().doubleValue() : Double.NaN;
        double lng = profile.getLongitude() != null ? profile.getLongitude().doubleValue() : Double.NaN;
        if (!Double.isNaN(lat) && !Double.isNaN(lng) && userLat != null && userLng != null) {
            distance = GeoUtil.distanceKm(userLat, userLng, lat, lng);
        }

        return java.util.Optional.of(new FarmerSummaryResponse(
                profile.getUserId(), profile.getFullName(), profile.getCity(), profile.getState(),
                profile.getAvatarUrl(), distance, listings.size(), avgPrice, minPrice, maxPrice,
                avgRating, reviewCount, categories));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return listingRepository.findActiveCategories();
    }

    /**
     * Group active listings by normalized title; show every group with
     * per-farmer price rows (used by /customer/compare).
     */
    @Transactional(readOnly = true)
    public List<PriceCompareGroup> compareProduce(String search, String category) {
        Map<UUID, Profile> profiles = profileRepository.findAll().stream()
                .collect(Collectors.toMap(Profile::getUserId, p -> p));
        Map<UUID, Double> avgRatings = ratingRepository.aggregateByFarmerIds(
                        profileRepository.findAll().stream().map(Profile::getUserId).toList())
                .stream().collect(Collectors.toMap(a -> (UUID) a[0], a -> ((Number) a[2]).doubleValue()));

        List<MarketplaceListing> listings = listingRepository.findAll().stream()
                .filter(MarketplaceListing::isAvailable)
                .filter(l -> search == null || search.isBlank()
                        || l.getTitle().toLowerCase().contains(search.toLowerCase()))
                .filter(l -> category == null || category.isBlank() || l.getCategory().equals(category))
                .toList();

        Map<String, List<MarketplaceListing>> byTitle = listings.stream()
                .collect(Collectors.groupingBy(l -> l.getTitle().trim().toLowerCase()));

        return byTitle.entrySet().stream()
                .filter(e -> e.getValue().stream().map(MarketplaceListing::getFarmerId).distinct().count() > 1)
                .map(e -> {
                    List<MarketplaceListing> group = e.getValue();
                    String display = group.get(0).getTitle();
                    return new PriceCompareGroup(
                            display.substring(0, 1).toUpperCase() + display.substring(1),
                            e.getKey(),
                            group.stream()
                                    .sorted(Comparator.comparing(MarketplaceListing::getPrice))
                                    .map(l -> {
                                        Profile p = profiles.get(l.getFarmerId());
                                        return new PriceCompareRow(
                                                l.getId(), l.getFarmerId(),
                                                p != null ? p.getFullName() : "Farmer",
                                                l.getPrice(), l.getQuantity(), l.getUnit(),
                                                l.getLocation(), l.getFarmingMethod(),
                                                avgRatings.getOrDefault(l.getFarmerId(), 0.0));
                                    })
                                    .toList());
                })
                .sorted(Comparator.comparing(PriceCompareGroup::displayName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FarmerRating> getFarmerRatings(UUID farmerId) {
        return ratingRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId);
    }

    @Transactional(readOnly = true)
    public long countDeliveredSales(UUID farmerId) {
        return customerOrderRepository.countByFarmerIdAndStatus(farmerId,
                com.kisansetu.order.OrderState.CustomerOrderStatus.DELIVERED);
    }

    private ListingResponse toResponse(MarketplaceListing l) {
        return ListingResponse.from(l, null);
    }
}
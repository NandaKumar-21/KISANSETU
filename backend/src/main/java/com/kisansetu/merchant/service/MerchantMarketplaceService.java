package com.kisansetu.merchant.service;

import com.kisansetu.common.PageResponse;
import com.kisansetu.common.util.GeoUtil;
import com.kisansetu.merchant.dto.MerchantSummaryResponse;
import com.kisansetu.merchant.dto.ProductResponse;
import com.kisansetu.merchant.entity.Product;
import com.kisansetu.merchant.repository.ProductRepository;
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
import java.util.Optional;

/**
 * Merchant marketplace for farmers: browse merchants by product availability,
 * aggregate pricing, distance, and price comparison across merchants.
 */
@Service
@RequiredArgsConstructor
public class MerchantMarketplaceService {

    private final ProductRepository productRepository;
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<MerchantSummaryResponse> getMerchants(Double userLat, Double userLng, Double radiusKm, String search) {
        List<Product> products = productRepository.findAllInStock();
        Map<UUID, List<Product>> byMerchant = products.stream()
                .collect(Collectors.groupingBy(Product::getMerchantId));
        if (byMerchant.isEmpty()) {
            return List.of();
        }
        Map<UUID, Profile> profiles = profileRepository.findByUserIds(new ArrayList<>(byMerchant.keySet()))
                .stream().collect(Collectors.toMap(Profile::getUserId, p -> p));

        List<MerchantSummaryResponse> result = new ArrayList<>();
        for (var entry : byMerchant.entrySet()) {
            Profile profile = profiles.get(entry.getKey());
            if (profile == null) {
                continue;
            }

            // Apply search filter
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.trim().toLowerCase();
                boolean matchesName = profile.getFullName() != null && 
                    profile.getFullName().toLowerCase().contains(searchLower);
                boolean matchesCity = profile.getCity() != null && 
                    profile.getCity().toLowerCase().contains(searchLower);
                boolean matchesState = profile.getState() != null && 
                    profile.getState().toLowerCase().contains(searchLower);
                if (!matchesName && !matchesCity && !matchesState) {
                    continue;
                }
            }

            List<Product> items = entry.getValue();
            BigDecimal avgPrice = items.stream().map(Product::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
            BigDecimal minPrice = items.stream().map(Product::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxPrice = items.stream().map(Product::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            List<String> categories = items.stream()
                    .map(Product::getCategory)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            Double distance = null;
            if (userLat != null && userLng != null && profile.getLatitude() != null && profile.getLongitude() != null) {
                distance = GeoUtil.distanceKm(userLat, userLng,
                        profile.getLatitude().doubleValue(), profile.getLongitude().doubleValue());
            }

            // Apply radius filter
            if (radiusKm != null && distance != null && distance > radiusKm) {
                continue;
            }

            result.add(new MerchantSummaryResponse(
                    profile.getUserId(), profile.getFullName(), profile.getCity(), profile.getState(),
                    profile.getAvatarUrl(),
                    GeoUtil.asDouble(profile.getLatitude()), GeoUtil.asDouble(profile.getLongitude()),
                    distance, items.size(), avgPrice, minPrice, maxPrice, categories));
        }

        // Sort by distance (nearest first), putting merchants without distance at the end
        result.sort(Comparator.comparing(MerchantSummaryResponse::distanceKm, 
            Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMerchantProducts(UUID merchantId) {
        return productRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(p -> p.getQuantity() > 0)
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MerchantSummaryResponse> getMerchantProfile(UUID merchantId, Double userLat, Double userLng) {
        Optional<Profile> profileOpt = profileRepository.findByUserId(merchantId);
        if (profileOpt.isEmpty()) {
            return Optional.empty();
        }
        Profile profile = profileOpt.get();

        List<Product> products = productRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .filter(p -> p.getQuantity() > 0)
                .toList();

        if (products.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal avgPrice = products.stream().map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(products.size()), 2, RoundingMode.HALF_UP);
        BigDecimal minPrice = products.stream().map(Product::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = products.stream().map(Product::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<String> categories = products.stream()
                .map(Product::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Double distance = null;
        if (userLat != null && userLng != null && profile.getLatitude() != null && profile.getLongitude() != null) {
            distance = GeoUtil.distanceKm(userLat, userLng,
                    profile.getLatitude().doubleValue(), profile.getLongitude().doubleValue());
        }

        return Optional.of(new MerchantSummaryResponse(
                profile.getUserId(), profile.getFullName(), profile.getCity(), profile.getState(),
                profile.getAvatarUrl(),
                GeoUtil.asDouble(profile.getLatitude()), GeoUtil.asDouble(profile.getLongitude()),
                distance, products.size(), avgPrice, minPrice, maxPrice, categories));
    }

    @Transactional(readOnly = true)
    public ProductResponse getMerchantProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> com.kisansetu.common.exception.ApiException.notFound("Product not found"));
        return ProductResponse.from(product);
    }

    /**
     * Comparison data: products grouped by normalized name, only groups
     * sold by more than one merchant.
     */
    @Transactional(readOnly = true)
    public List<PriceCompareGroup> compareProducts() {
        Map<UUID, Profile> merchantNames = profileRepository.findAll().stream()
                .collect(Collectors.toMap(Profile::getUserId, p -> p));
        Map<String, List<Product>> byName = productRepository.findAllInStock().stream()
                .collect(Collectors.groupingBy(p -> p.getName().trim().toLowerCase()));

        return byName.entrySet().stream()
                .filter(e -> e.getValue().stream().map(Product::getMerchantId).distinct().count() > 1)
                .map(e -> new PriceCompareGroup(
                        displayName(e.getValue().get(0).getName()),
                        e.getKey(),
                        e.getValue().stream()
                                .sorted(Comparator.comparing(Product::getPrice))
                                .map(p -> new CompareRow(
                                        p.getId(), p.getMerchantId(),
                                        merchantNames.containsKey(p.getMerchantId())
                                                ? merchantNames.get(p.getMerchantId()).getFullName() : "Merchant",
                                        p.getPrice(), p.getQuantity(), p.getUnit()))
                                .toList()))
                .sorted(Comparator.comparing(PriceCompareGroup::displayName))
                .toList();
    }

    private String displayName(String name) {
        if (name == null || name.isBlank()) {
            return "Product";
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public record PriceCompareGroup(String displayName, String key, List<CompareRow> rows) {
    }

    public record CompareRow(UUID productId, UUID merchantId, String merchantName,
                             BigDecimal price, Integer quantity, String unit) {
    }

    /**
     * Browse all merchant products with pagination, search, and filtering.
     * This is the main endpoint for the farmer marketplace.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> browseProducts(String search, String category,
                                                        BigDecimal minPrice, BigDecimal maxPrice,
                                                        Pageable pageable) {
        var page = productRepository.searchInStockPaginated(search, category, minPrice, maxPrice, pageable);
        
        // Fetch merchant profiles for all products on this page
        Set<UUID> merchantIds = page.getContent().stream()
                .map(Product::getMerchantId)
                .collect(Collectors.toSet());
        Map<UUID, Profile> profiles = profileRepository.findByUserIds(new ArrayList<>(merchantIds))
                .stream()
                .collect(Collectors.toMap(Profile::getUserId, p -> p));
        
        // Convert to responses with merchant names using mapper function
        return PageResponse.from(page, p -> {
            Profile profile = profiles.get(p.getMerchantId());
            String merchantName = profile != null ? profile.getFullName() : null;
            return ProductResponse.withMerchantName(p, merchantName);
        });
    }

    /**
     * Get distinct categories from merchant products for filtering.
     */
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }
}
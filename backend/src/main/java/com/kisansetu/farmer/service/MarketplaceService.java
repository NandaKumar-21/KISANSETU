package com.kisansetu.farmer.service;

import com.kisansetu.common.PageResponse;
import com.kisansetu.common.exception.ApiException;
import com.kisansetu.farmer.dto.ListingRequest;
import com.kisansetu.farmer.dto.ListingResponse;
import com.kisansetu.farmer.dto.MarketplaceOrderResponse;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.entity.MarketplaceOrder;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.farmer.repository.MarketplaceOrderRepository;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState;
import com.kisansetu.order.OrderState.MarketplaceOrderStatus;
import com.kisansetu.order.entity.OrderStatusHistory;
import com.kisansetu.order.repository.OrderStatusHistoryRepository;
import com.kisansetu.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Farmer produce marketplace: listing management (sell) and
 * farmer-to-farmer marketplace orders (buy).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceOrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final ProfileService profileService;

    // ---------------- Listings (sell) ----------------

    @Transactional
    public ListingResponse createListing(UUID farmerId, ListingRequest request) {
        MarketplaceListing listing = new MarketplaceListing();
        applyRequest(listing, request);
        listing.setFarmerId(farmerId);
        listingRepository.save(listing);
        return toResponse(listing);
    }

    @Transactional
    public ListingResponse updateListing(UUID farmerId, UUID listingId, ListingRequest request) {
        MarketplaceListing listing = getOwnedListing(farmerId, listingId);
        applyRequest(listing, request);
        listingRepository.save(listing);
        return toResponse(listing);
    }

    @Transactional
    public void deleteListing(UUID farmerId, UUID listingId) {
        MarketplaceListing listing = getOwnedListing(farmerId, listingId);
        listingRepository.delete(listing);
    }

    @Transactional
    public ListingResponse changeStatus(UUID farmerId, UUID listingId, String status) {
        MarketplaceListing listing = getOwnedListing(farmerId, listingId);
        MarketplaceListing.ListingStatus next;
        try {
            next = MarketplaceListing.ListingStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid listing status: " + status);
        }
        listing.setStatus(next);
        listingRepository.save(listing);
        return toResponse(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getMyListings(UUID farmerId) {
        return listingRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> browseListings(String search, String category,
                                                        BigDecimal minPrice, BigDecimal maxPrice,
                                                        Pageable pageable) {
        return PageResponse.from(
                listingRepository.search(search, category, minPrice, maxPrice, null, pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public long activeListingCount(UUID farmerId) {
        return listingRepository.countByFarmerIdAndStatus(farmerId, MarketplaceListing.ListingStatus.ACTIVE);
    }

    // ---------------- Marketplace orders (buy/sell between farmers) ----------------

    @Transactional
    public MarketplaceOrderResponse createMarketplaceOrder(UUID buyerId, UUID listingId, BigDecimal quantity, String notes) {
        MarketplaceListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Listing not found"));
        if (!listing.isAvailable()) {
            throw ApiException.conflict("This listing is no longer available");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Quantity must be greater than zero");
        }
        if (quantity.compareTo(listing.getQuantity()) > 0) {
            throw ApiException.conflict("Only " + listing.getQuantity() + " " + listing.getUnit()
                    + " available");
        }
        if (listing.getFarmerId().equals(buyerId)) {
            throw ApiException.badRequest("You cannot order your own listing");
        }

        MarketplaceOrder order = new MarketplaceOrder();
        order.setListingId(listing.getId());
        order.setBuyerId(buyerId);
        order.setFarmerId(listing.getFarmerId());
        order.setQuantity(quantity);
        order.setUnitPrice(listing.getPrice());
        order.setTotalPrice(listing.getPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP));
        order.setStatus(MarketplaceOrderStatus.PENDING);
        order.setNotes(notes);
        orderRepository.save(order);
        recordHistory("marketplace", order.getId(), null, "pending", buyerId, null);

        notificationService.notify(listing.getFarmerId(), "marketplace_order",
                "New order received!",
                "You have a new order for " + quantity + " " + listing.getUnit()
                        + " of " + listing.getTitle() + ".");
        return toOrderResponse(order, listing.getTitle(), listing.getUnit(), null);
    }

    @Transactional
    public MarketplaceOrderResponse updateMarketplaceOrderStatus(UUID farmerId, UUID orderId, String nextStatus) {
        MarketplaceOrder order = getFarmerOrder(farmerId, orderId);
        MarketplaceOrderStatus next;
        try {
            next = MarketplaceOrderStatus.valueOf(nextStatus.toUpperCase());
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid status: " + nextStatus);
        }
        MarketplaceOrderStatus[] allowed = MarketplaceOrderStatus.allowedNext(order.getStatus());
        if (!OrderState.isAllowed(order.getStatus(), next, allowed)) {
            throw ApiException.conflict("Invalid transition from " + order.getStatus().name().toLowerCase()
                    + " to " + next.name().toLowerCase());
        }
        String from = order.getStatus().name().toLowerCase();
        order.setStatus(next);
        orderRepository.save(order);
        recordHistory("marketplace", order.getId(), from, next.name().toLowerCase(), farmerId, null);
        return toOrderResponse(order, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceOrderResponse> getMyMarketplaceOrders(UUID buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(o -> toOrderResponse(o, null, null, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MarketplaceOrderResponse> getOrdersForMyListings(UUID farmerId) {
        return orderRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream()
                .map(o -> toOrderResponse(o, null, null, null))
                .toList();
    }

    private MarketplaceOrder getFarmerOrder(UUID farmerId, UUID orderId) {
        MarketplaceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getFarmerId().equals(farmerId)) {
            throw ApiException.forbidden("This order does not belong to you");
        }
        return order;
    }

    private MarketplaceListing getOwnedListing(UUID farmerId, UUID listingId) {
        MarketplaceListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Listing not found"));
        if (!listing.getFarmerId().equals(farmerId)) {
            throw ApiException.forbidden("This listing does not belong to you");
        }
        return listing;
    }

    private void applyRequest(MarketplaceListing listing, ListingRequest request) {
        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setCategory(request.category());
        listing.setPrice(request.price());
        listing.setQuantity(request.quantity());
        listing.setUnit(request.unit());
        if (request.imageUrl() != null) {
            listing.setImageUrl(request.imageUrl());
        }
        listing.setLocation(request.location());
        if (request.variety() != null) {
            listing.setVariety(request.variety());
        }
        if (request.farmingMethod() != null) {
            listing.setFarmingMethod(request.farmingMethod());
        }
        listing.setHarvestDate(request.harvestDate());
        if (request.latitude() != null) {
            listing.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            listing.setLongitude(request.longitude());
        }
    }

    private void recordHistory(String type, UUID orderId, String from, String to, UUID actorId, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderType(type);
        history.setOrderId(orderId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actorId);
        history.setNote(note);
        historyRepository.save(history);
    }

    private ListingResponse toResponse(MarketplaceListing listing) {
        String farmerName = null;
        try {
            farmerName = profileService.getProfileEntityByUserId(listing.getFarmerId()).getFullName();
        } catch (Exception ignored) {
        }
        return ListingResponse.from(listing, farmerName);
    }

    private MarketplaceOrderResponse toOrderResponse(MarketplaceOrder order, String listingTitle, String listingUnit, String buyerName) {
        if (listingTitle == null) {
            MarketplaceListing listing = listingRepository.findById(order.getListingId()).orElse(null);
            if (listing != null) {
                listingTitle = listing.getTitle();
                listingUnit = listing.getUnit();
            }
        }
        if (buyerName == null) {
            try {
                buyerName = profileService.getProfileEntityByUserId(order.getBuyerId()).getFullName();
            } catch (Exception ignored) {
            }
        }
        return MarketplaceOrderResponse.from(order, listingTitle, listingUnit, buyerName);
    }
}
package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.dto.CheckoutResponse;
import com.kisansetu.customer.entity.CartItem;
import com.kisansetu.customer.entity.CustomerAddress;
import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.customer.repository.CartItemRepository;
import com.kisansetu.customer.repository.CustomerAddressRepository;
import com.kisansetu.customer.repository.CustomerOrderRepository;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.logistics.service.LogisticsAssignmentService;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState.CustomerOrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Checkout: validates the cart against live stock and prices, creates one
 * CustomerOrder per seller, clears the cart, notifies farmers and triggers
 * logistics assignment. All pricing is computed server-side in one
 * transaction — frontend-submitted totals are never trusted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartItemRepository cartRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerAddressRepository addressRepository;
    private final MarketplaceListingRepository listingRepository;
    private final NotificationService notificationService;
    private final LogisticsAssignmentService logisticsAssignmentService;

    @Transactional
    public CheckoutResponse checkout(UUID customerId, UUID deliveryAddressId,
                                     String deliveryPreference, String notes) {
        List<CartItem> cart = cartRepository.findByCustomerIdOrderByCreatedAtAsc(customerId);
        if (cart.isEmpty()) {
            throw ApiException.badRequest("Your cart is empty");
        }
        CustomerAddress address = addressRepository.findById(deliveryAddressId)
                .orElseThrow(() -> ApiException.notFound("Delivery address not found"));
        if (!address.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("This address does not belong to you");
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        Map<UUID, List<CartItem>> byFarmer = cart.stream()
                .collect(Collectors.groupingBy(item -> {
                    MarketplaceListing listing = listingRepository.findById(item.getListingId())
                            .orElseThrow(() -> ApiException.conflict("Some items in your cart are no longer available"));
                    return listing.getFarmerId();
                }));

        // Fetch listings once
        Map<UUID, MarketplaceListing> listings = listingRepository.findAllById(cart.stream()
                        .map(CartItem::getListingId).toList())
                .stream().collect(Collectors.toMap(MarketplaceListing::getId, l -> l));

        int createdCount = 0;
        for (CartItem item : cart) {
            MarketplaceListing listing = listings.get(item.getListingId());
            if (listing == null || !listing.isAvailable()) {
                throw ApiException.conflict(item != null && listings.containsKey(item.getListingId())
                        ? "One of your items is no longer available"
                        : "Produce no longer available");
            }
            if (item.getQuantity().compareTo(listing.getQuantity()) > 0) {
                throw ApiException.conflict("Only " + listing.getQuantity() + " " + listing.getUnit()
                        + " of " + listing.getTitle() + " available");
            }
            BigDecimal total = listing.getPrice().multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            grandTotal = grandTotal.add(total);

            CustomerOrder order = new CustomerOrder();
            order.setCustomerId(customerId);
            order.setFarmerId(listing.getFarmerId());
            order.setListingId(listing.getId());
            order.setQuantity(item.getQuantity());
            order.setUnitPrice(listing.getPrice());
            order.setTotalPrice(total);
            order.setStatus(CustomerOrderStatus.PENDING);
            order.setDeliveryAddressId(address.getId());
            order.setDeliveryPreference(deliveryPreference == null ? "any" : deliveryPreference);
            order.setEstimatedDelivery(Instant.now().plus(3, ChronoUnit.DAYS));
            order.setNotes(notes);
            customerOrderRepository.save(order);
            createdCount++;
        }

        // Notify each farmer once
        for (Map.Entry<UUID, List<CartItem>> entry : byFarmer.entrySet()) {
            int items = entry.getValue().size();
            notificationService.notify(entry.getKey(), "customer_order",
                    "New customer order!",
                    "You have " + items + " new item(s) ordered from your marketplace.");
        }

        // Auto-assign logistics for new orders (best-effort; failures are logged, not fatal)
        customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .filter(o -> o.getStatus() == CustomerOrderStatus.PENDING)
                .forEach(o -> {
                    try {
                        logisticsAssignmentService.tryAssign(o.getId());
                    } catch (Exception e) {
                        log.warn("Logistics assignment failed for order {}: {}", o.getId(), e.getMessage());
                    }
                });

        cartRepository.deleteByCustomerId(customerId);
        return new CheckoutResponse(createdCount, grandTotal, "Order placed successfully");
    }
}
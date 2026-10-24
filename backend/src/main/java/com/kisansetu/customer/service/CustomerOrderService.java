package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.dto.CustomerOrderResponse;
import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.customer.repository.CustomerOrderRepository;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.logistics.service.LogisticsAssignmentService;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState;
import com.kisansetu.order.OrderState.CustomerOrderStatus;
import com.kisansetu.order.entity.OrderStatusHistory;
import com.kisansetu.order.repository.OrderStatusHistoryRepository;
import com.kisansetu.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Customer <-> Farmer order lifecycle with backend-enforced transitions:
 *
 * PENDING -> CONFIRMED -> PACKED -> DISPATCHED -> DELIVERED
 * PENDING -> CANCELLED (farmer rejects)
 * CONFIRMED -> CANCELLED
 *
 * Farmer contact becomes visible once the order is confirmed and is
 * disabled after delivery, matching the original behavior.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final MarketplaceListingRepository listingRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final ProfileService profileService;
    private final LogisticsAssignmentService logisticsAssignmentService;

    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getCustomerOrders(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getFarmerOrders(UUID farmerId) {
        return orderRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerOrderResponse getCustomerOrder(UUID customerId, UUID orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("This order does not belong to you");
        }
        return toResponse(order);
    }

    /**
     * Farmer accepts (PENDING -> CONFIRMED), with optional farmer notes.
     */
    @Transactional
    public CustomerOrderResponse acceptOrder(UUID farmerId, UUID orderId, String farmerNotes) {
        CustomerOrder order = getFarmerOrder(farmerId, orderId);
        transition(order, CustomerOrderStatus.CONFIRMED, farmerId);
        if (farmerNotes != null && !farmerNotes.isBlank()) {
            order.setFarmerNotes(farmerNotes);
        }
        order.setFarmerContactVisible(true);
        orderRepository.save(order);
        recordHistory(order.getId(), "pending", "confirmed", farmerId, null);
        notificationService.notify(order.getCustomerId(), "order_status",
                "Order confirmed",
                "Your order has been confirmed by the farmer.");
        return toResponse(order);
    }

    @Transactional
    public CustomerOrderResponse rejectOrder(UUID farmerId, UUID orderId, String reason) {
        CustomerOrder order = getFarmerOrder(farmerId, orderId);
        String from = order.getStatus().name().toLowerCase();
        transition(order, CustomerOrderStatus.CANCELLED, farmerId);
        orderRepository.save(order);
        recordHistory(order.getId(), from, "cancelled", farmerId, reason);
        notificationService.notify(order.getCustomerId(), "order_status",
                "Order cancelled",
                reason == null || reason.isBlank()
                        ? "The farmer could not accept your order."
                        : "Order cancelled: " + reason);
        return toResponse(order);
    }

    /**
     * Farmer advances the order one step: CONFIRMED -> PACKED -> DISPATCHED -> DELIVERED.
     * Dispatching triggers logistics assignment.
     */
    @Transactional
    public CustomerOrderResponse advanceStatus(UUID farmerId, UUID orderId) {
        CustomerOrder order = getFarmerOrder(farmerId, orderId);
        CustomerOrderStatus next = switch (order.getStatus()) {
            case CONFIRMED -> CustomerOrderStatus.PACKED;
            case PACKED -> CustomerOrderStatus.DISPATCHED;
            case DISPATCHED -> CustomerOrderStatus.DELIVERED;
            default -> throw ApiException.conflict("Order cannot be advanced from "
                    + order.getStatus().name().toLowerCase());
        };
        String from = order.getStatus().name().toLowerCase();
        transition(order, next, farmerId);
        switch (next) {
            case PACKED -> order.setPackedAt(Instant.now());
            case DISPATCHED -> {
                order.setDispatchedAt(Instant.now());
                try {
                    logisticsAssignmentService.tryAssign(order.getId());
                } catch (Exception e) {
                    log.warn("Logistics assignment failed after dispatch for {}: {}", order.getId(), e.getMessage());
                }
            }
            case DELIVERED -> {
                order.setDeliveredAt(Instant.now());
                order.setContactDisabledAt(Instant.now());
                order.setDeliveryStatus(com.kisansetu.order.OrderState.DeliveryStatus.COMPLETED);
            }
            default -> { }
        }
        orderRepository.save(order);
        recordHistory(order.getId(), from, next.name().toLowerCase(), farmerId, null);
        notificationService.notify(order.getCustomerId(), "order_status",
                "Order " + next.name().toLowerCase(),
                "Your order status changed to " + next.name().toLowerCase() + ".");
        return toResponse(order);
    }

    /**
     * Customer cancels a pending order.
     */
    @Transactional
    public CustomerOrderResponse cancelByCustomer(UUID customerId, UUID orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("This order does not belong to you");
        }
        String from = order.getStatus().name().toLowerCase();
        transition(order, CustomerOrderStatus.CANCELLED, customerId);
        orderRepository.save(order);
        recordHistory(order.getId(), from, "cancelled", customerId, null);
        notificationService.notify(order.getFarmerId(), "order_status",
                "Order cancelled by customer",
                "The customer cancelled their order.");
        return toResponse(order);
    }

    /**
     * Package-visible helper used by the logistics module.
     */
    public CustomerOrderResponse getResponseFor(CustomerOrder order) {
        return toResponse(order);
    }

    private CustomerOrder getFarmerOrder(UUID farmerId, UUID orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getFarmerId().equals(farmerId)) {
            throw ApiException.forbidden("This order does not belong to you");
        }
        return order;
    }

    private void transition(CustomerOrder order, CustomerOrderStatus next, UUID actorId) {
        CustomerOrderStatus current = order.getStatus();
        CustomerOrderStatus[] allowed = CustomerOrderStatus.allowedNext(current);
        if (!OrderState.isAllowed(current, next, allowed)) {
            throw ApiException.conflict("Invalid order transition from "
                    + current.name().toLowerCase() + " to " + next.name().toLowerCase());
        }
        order.setStatus(next);
    }

    private void recordHistory(UUID orderId, String from, String to, UUID actorId, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderType("customer");
        history.setOrderId(orderId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actorId);
        history.setNote(note);
        historyRepository.save(history);
    }

    private CustomerOrderResponse toResponse(CustomerOrder order) {
        MarketplaceListing listing = listingRepository.findById(order.getListingId()).orElse(null);
        String customerName = null;
        String farmerName = null;
        String addressText = null;
        try {
            customerName = profileService.getProfileEntityByUserId(order.getCustomerId()).getFullName();
        } catch (Exception ignored) {
        }
        try {
            farmerName = profileService.getProfileEntityByUserId(order.getFarmerId()).getFullName();
        } catch (Exception ignored) {
        }
        return CustomerOrderResponse.from(order, listing, customerName, farmerName);
    }
}
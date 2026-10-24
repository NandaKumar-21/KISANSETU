package com.kisansetu.order.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.merchant.entity.Product;
import com.kisansetu.merchant.repository.ProductRepository;
import com.kisansetu.merchant.service.MerchantProductService;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState;
import com.kisansetu.order.OrderState.MerchantOrderStatus;
import com.kisansetu.order.dto.OrderRequest;
import com.kisansetu.order.dto.OrderResponse;
import com.kisansetu.order.entity.Order;
import com.kisansetu.order.entity.OrderStatusHistory;
import com.kisansetu.order.repository.OrderRepository;
import com.kisansetu.order.repository.OrderStatusHistoryRepository;
import com.kisansetu.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Farmer -> Merchant orders with a strict backend-enforced state machine.
 *
 * PENDING -> ACCEPTED -> PROCESSING -> COMPLETED
 * PENDING -> REJECTED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final MerchantProductService merchantProductService;
    private final NotificationService notificationService;
    private final ProfileService profileService;

    @Transactional
    public OrderResponse createOrder(UUID farmerId, OrderRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (product.getQuantity() < request.quantity()) {
            throw ApiException.conflict("Insufficient stock. Only " + product.getQuantity()
                    + " " + product.getUnit() + " available.");
        }

        Order order = new Order();
        order.setFarmerId(farmerId);
        order.setMerchantId(product.getMerchantId());
        order.setProductId(product.getId());
        order.setQuantity(request.quantity());
        order.setUnitPrice(product.getPrice());
        order.setTotalPrice(calculateTotal(product.getPrice(), request.quantity()));
        order.setStatus(MerchantOrderStatus.PENDING);
        order.setNotes(request.notes());
        orderRepository.save(order);
        recordHistory("merchant", order.getId(), null, "pending", farmerId, null);

        notificationService.notify(product.getMerchantId(), "order_received",
                "New order received",
                "A farmer ordered " + request.quantity() + " " + product.getUnit()
                        + " of " + product.getName() + " (₹" + order.getTotalPrice() + ").");
        return toResponse(order, product);
    }

    @Transactional
    public OrderResponse acceptOrder(UUID merchantId, UUID orderId) {
        Order order = getMerchantOrder(merchantId, orderId);
        transition(order, MerchantOrderStatus.ACCEPTED, merchantId);
        // Stock deduction happens atomically in the same transaction.
        merchantProductService.deductStock(order.getProductId(), order.getQuantity(), order.getId(), merchantId);
        orderRepository.save(order);
        recordHistory("merchant", order.getId(), "pending", "accepted", merchantId, null);
        notificationService.notify(order.getFarmerId(), "order_accepted",
                "Order accepted",
                "Your order for " + order.getQuantity() + " items has been accepted by the merchant.");
        return toResponse(order, null);
    }

    @Transactional
    public OrderResponse advanceStatus(UUID merchantId, UUID orderId, String nextStatus) {
        Order order = getMerchantOrder(merchantId, orderId);
        MerchantOrderStatus next = parseStatus(nextStatus);
        String from = order.getStatus().name().toLowerCase();
        transition(order, next, merchantId);
        orderRepository.save(order);
        recordHistory("merchant", order.getId(), from, next.name().toLowerCase(), merchantId, null);
        return toResponse(order, null);
    }

    @Transactional
    public OrderResponse rejectOrder(UUID merchantId, UUID orderId, String reason) {
        Order order = getMerchantOrder(merchantId, orderId);
        String from = order.getStatus().name().toLowerCase();
        transition(order, MerchantOrderStatus.REJECTED, merchantId);
        orderRepository.save(order);
        recordHistory("merchant", order.getId(), from, "rejected", merchantId, reason);
        notificationService.notify(order.getFarmerId(), "order_rejected",
                "Order rejected",
                reason == null || reason.isBlank()
                        ? "Your order was rejected by the merchant."
                        : "Your order was rejected: " + reason);
        return toResponse(order, null);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getFarmerOrders(UUID farmerId) {
        return orderRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream()
                .map(o -> toResponse(o, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMerchantOrders(UUID merchantId) {
        Map<UUID, Product> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        return orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(o -> toResponse(o, products.get(o.getProductId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getMerchantOrderCounts(UUID merchantId) {
        Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("pending", orderRepository.countByMerchantIdAndStatus(merchantId, MerchantOrderStatus.PENDING));
        counts.put("accepted", orderRepository.countByMerchantIdAndStatus(merchantId, MerchantOrderStatus.ACCEPTED));
        counts.put("processing", orderRepository.countByMerchantIdAndStatus(merchantId, MerchantOrderStatus.PROCESSING));
        counts.put("completed", orderRepository.countByMerchantIdAndStatus(merchantId, MerchantOrderStatus.COMPLETED));
        return counts;
    }

    private void transition(Order order, MerchantOrderStatus next, UUID actorId) {
        MerchantOrderStatus current = order.getStatus();
        MerchantOrderStatus[] allowed = MerchantOrderStatus.allowedNext(current);
        if (!OrderState.isAllowed(current, next, allowed)) {
            throw ApiException.conflict("Invalid order transition from "
                    + current.name().toLowerCase() + " to " + next.name().toLowerCase());
        }
        order.setStatus(next);
    }

    private Order getMerchantOrder(UUID merchantId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getMerchantId().equals(merchantId)) {
            throw ApiException.forbidden("This order does not belong to you");
        }
        return order;
    }

    private MerchantOrderStatus parseStatus(String value) {
        try {
            return MerchantOrderStatus.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid status: " + value);
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

    private BigDecimal calculateTotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private OrderResponse toResponse(Order order, Product product) {
        String farmerName = null;
        String merchantName = null;
        try {
            farmerName = profileService.getProfileEntityByUserId(order.getFarmerId()).getFullName();
        } catch (Exception ignored) {
        }
        try {
            merchantName = profileService.getProfileEntityByUserId(order.getMerchantId()).getFullName();
        } catch (Exception ignored) {
        }
        return OrderResponse.from(order, product, farmerName, merchantName);
    }
}
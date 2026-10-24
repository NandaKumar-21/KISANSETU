package com.kisansetu.logistics.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.common.util.GeoUtil;
import com.kisansetu.config.KisanSetuProperties;
import com.kisansetu.customer.dto.CustomerOrderResponse;
import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.customer.repository.CustomerOrderRepository;
import com.kisansetu.customer.service.CustomerOrderService;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.logistics.entity.Delivery;
import com.kisansetu.logistics.entity.DeliveryPartnerStatus;
import com.kisansetu.logistics.repository.DeliveryPartnerStatusRepository;
import com.kisansetu.logistics.repository.DeliveryRepository;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState;
import com.kisansetu.order.OrderState.DeliveryStatus;
import com.kisansetu.order.entity.OrderStatusHistory;
import com.kisansetu.order.repository.OrderStatusHistoryRepository;
import com.kisansetu.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Delivery partner workflows: list deliveries, accept/reject, advance the
 * delivery state machine, availability management and earnings calculation.
 *
 * Delivery pipeline: ASSIGNED -> ACCEPTED -> PICKUP_SCHEDULED -> PICKED_UP
 *   -> IN_TRANSIT -> DELIVERED -> COMPLETED
 * ASSIGNED -> REJECTED (triggers reassignment)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private static final List<DeliveryStatus> ACTIVE_STATES = List.of(
            DeliveryStatus.ASSIGNED, DeliveryStatus.ACCEPTED, DeliveryStatus.PICKUP_SCHEDULED,
            DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT);

    private final CustomerOrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryPartnerStatusRepository partnerStatusRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final MarketplaceListingRepository listingRepository;
    private final NotificationService notificationService;
    private final ProfileService profileService;
    private final KisanSetuProperties props;
    private final LogisticsAssignmentService assignmentService;
    private final CustomerOrderService customerOrderService;

    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getPartnerOrders(UUID partnerId) {
        return orderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .map(customerOrderService::getResponseFor)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getActiveDeliveries(UUID partnerId) {
        return orderRepository.findPartnerDeliveries(partnerId,
                        List.of(DeliveryStatus.DELIVERED, DeliveryStatus.COMPLETED, DeliveryStatus.REJECTED))
                .stream().map(customerOrderService::getResponseFor).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getCompletedDeliveries(UUID partnerId) {
        return orderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .filter(o -> o.getDeliveryStatus() == DeliveryStatus.DELIVERED
                        || o.getDeliveryStatus() == DeliveryStatus.COMPLETED)
                .map(customerOrderService::getResponseFor)
                .toList();
    }

    @Transactional
    public CustomerOrderResponse acceptDelivery(UUID partnerId, UUID orderId) {
        CustomerOrder order = getAssignedOrder(partnerId, orderId);
        transitionDelivery(order, DeliveryStatus.ACCEPTED, partnerId);
        orderRepository.save(order);
        deliveryRepository.findByOrderId(orderId).ifPresent(d -> {
            d.setStatus(DeliveryStatus.ACCEPTED);
            d.setAcceptedAt(Instant.now());
            deliveryRepository.save(d);
        });
        recordHistory(orderId, "assigned", "accepted", partnerId);
        return customerOrderService.getResponseFor(order);
    }

    @Transactional
    public CustomerOrderResponse rejectDelivery(UUID partnerId, UUID orderId) {
        CustomerOrder order = getAssignedOrder(partnerId, orderId);
        String from = order.getDeliveryStatus() != null
                ? order.getDeliveryStatus().name().toLowerCase() : "assigned";
        recordHistory(orderId, from, "rejected", partnerId);
        notificationService.notify(order.getCustomerId(), "order_status",
                "Delivery reassigned",
                "Your delivery partner changed; a new partner will be assigned shortly.");
        assignmentService.reassign(orderId, partnerId);
        return customerOrderService.getResponseFor(orderRepository.findById(orderId).orElseThrow());
    }

    /**
     * Advance the delivery pipeline one step.
     */
    @Transactional
    public CustomerOrderResponse advanceDelivery(UUID partnerId, UUID orderId) {
        CustomerOrder order = getAssignedOrder(partnerId, orderId);
        DeliveryStatus current = order.getDeliveryStatus() != null
                ? order.getDeliveryStatus() : DeliveryStatus.ASSIGNED;
        DeliveryStatus next = switch (current) {
            case ASSIGNED -> DeliveryStatus.ACCEPTED;
            case ACCEPTED -> DeliveryStatus.PICKUP_SCHEDULED;
            case PICKUP_SCHEDULED -> DeliveryStatus.PICKED_UP;
            case PICKED_UP -> DeliveryStatus.IN_TRANSIT;
            case IN_TRANSIT -> DeliveryStatus.DELIVERED;
            case DELIVERED -> DeliveryStatus.COMPLETED;
            default -> throw ApiException.conflict("Delivery cannot be advanced from "
                    + current.name().toLowerCase());
        };
        return transitionDelivery(order, next, partnerId);
    }

    @Transactional
    public CustomerOrderResponse transitionDelivery(CustomerOrder order, DeliveryStatus next, UUID partnerId) {
        DeliveryStatus current = order.getDeliveryStatus() != null
                ? order.getDeliveryStatus() : DeliveryStatus.ASSIGNED;
        DeliveryStatus[] allowed = DeliveryStatus.allowedNext(current);
        if (!OrderState.isAllowed(current, next, allowed)) {
            throw ApiException.conflict("Invalid delivery transition from "
                    + current.name().toLowerCase() + " to " + next.name().toLowerCase());
        }
        String from = current.name().toLowerCase();
        order.setDeliveryStatus(next);
        switch (next) {
            case ACCEPTED -> { /* nothing */ }
            case PICKUP_SCHEDULED -> order.setPickupTime(Instant.now());
            case DELIVERED, COMPLETED -> {
                order.setDeliveredAt(Instant.now());
                order.setStatus(com.kisansetu.order.OrderState.CustomerOrderStatus.DELIVERED);
                order.setContactDisabledAt(Instant.now());
            }
            default -> { }
        }
        orderRepository.save(order);

        Delivery delivery = deliveryRepository.findByOrderId(order.getId()).orElseGet(() -> {
            Delivery d = new Delivery();
            d.setOrderId(order.getId());
            return d;
        });
        delivery.setPartnerId(partnerId);
        delivery.setStatus(next);
        delivery.setEarning(computeEarning(order.getTotalPrice()));
        switch (next) {
            case ACCEPTED -> delivery.setAcceptedAt(Instant.now());
            case PICKED_UP -> delivery.setPickedUpAt(Instant.now());
            case IN_TRANSIT -> delivery.setInTransitAt(Instant.now());
            case DELIVERED, COMPLETED -> delivery.setDeliveredAt(Instant.now());
            default -> { }
        }
        deliveryRepository.save(delivery);

        recordHistory(order.getId(), from, next.name().toLowerCase(), partnerId);

        if (next == DeliveryStatus.DELIVERED || next == DeliveryStatus.COMPLETED) {
            notificationService.notify(order.getCustomerId(), "order_status",
                    "Order delivered",
                    "Your order has been delivered. Please rate your experience.");
            notificationService.notify(order.getFarmerId(), "order_status",
                    "Order delivered",
                    "Your order was delivered to the customer.");
        } else if (next == DeliveryStatus.ACCEPTED) {
            notificationService.notify(order.getCustomerId(), "order_status",
                    "Delivery accepted",
                    "Your delivery partner accepted your order.");
        }
        return customerOrderService.getResponseFor(order);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(UUID partnerId) {
        List<CustomerOrder> all = orderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(partnerId);
        long assigned = all.stream()
                .filter(o -> o.getDeliveryStatus() == DeliveryStatus.ASSIGNED).count();
        long active = all.stream()
                .filter(o -> o.getDeliveryStatus() != null && ACTIVE_STATES.contains(o.getDeliveryStatus())).count();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long completedToday = all.stream()
                .filter(o -> o.getDeliveredAt() != null
                        && o.getDeliveredAt().atZone(ZoneOffset.UTC).toLocalDate().equals(today))
                .count();
        BigDecimal deliveredValueToday = all.stream()
                .filter(o -> o.getDeliveredAt() != null
                        && o.getDeliveredAt().atZone(ZoneOffset.UTC).toLocalDate().equals(today))
                .map(CustomerOrder::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal earningsToday = deliveredValueToday
                .multiply(BigDecimal.valueOf(props.logistics().earningPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        long completed = all.stream()
                .filter(o -> o.getDeliveryStatus() == DeliveryStatus.DELIVERED
                        || o.getDeliveryStatus() == DeliveryStatus.COMPLETED)
                .count();
        long total = all.size();
        double completionRate = total == 0 ? 0 : Math.round((completed * 100.0) / total);

        long inTransit = all.stream()
                .filter(o -> o.getDeliveryStatus() == DeliveryStatus.IN_TRANSIT).count();

        return Map.of(
                "assignedDeliveries", assigned,
                "activeDeliveries", active,
                "completedToday", completedToday,
                "earningsToday", earningsToday,
                "completionRate", completionRate,
                "totalDeliveries", total,
                "completed", completed,
                "inProgress", active,
                "inTransit", inTransit
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEarnings(UUID partnerId) {
        Instant now = Instant.now();
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekStart = now.minusSeconds(7 * 86400L);
        Instant monthStart = now.minusSeconds(30 * 86400L);

        BigDecimal pct = BigDecimal.valueOf(props.logistics().earningPercentage());

        BigDecimal todayValue = orderRepository.sumDeliveredValueSince(partnerId, todayStart);
        BigDecimal weekValue = orderRepository.sumDeliveredValueSince(partnerId, weekStart);
        BigDecimal monthValue = orderRepository.sumDeliveredValueSince(partnerId, monthStart);
        long todayCount = orderRepository.countDeliveredSince(partnerId, todayStart);
        long weekCount = orderRepository.countDeliveredSince(partnerId, weekStart);
        long monthCount = orderRepository.countDeliveredSince(partnerId, monthStart);

        BigDecimal rate = pct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal todayEarnings = todayValue.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal weekEarnings = weekValue.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthEarnings = monthValue.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        List<Map<String, Object>> history = orderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(partnerId)
                .stream()
                .filter(o -> o.getDeliveryStatus() == DeliveryStatus.DELIVERED
                        || o.getDeliveryStatus() == DeliveryStatus.COMPLETED)
                .map(o -> Map.<String, Object>of(
                        "orderId", o.getId().toString(),
                        "amount", o.getTotalPrice().multiply(rate).setScale(2, RoundingMode.HALF_UP),
                        "deliveredAt", o.getDeliveredAt(),
                        "orderValue", o.getTotalPrice()))
                .toList();

        return Map.of(
                "today", todayEarnings,
                "week", weekEarnings,
                "month", monthEarnings,
                "todayDeliveries", todayCount,
                "weekDeliveries", weekCount,
                "monthDeliveries", monthCount,
                "earningPerDelivery", "5% of order value",
                "history", history
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRoutes(UUID partnerId) {
        List<CustomerOrder> active = orderRepository.findActiveDeliveriesAsc(partnerId,
                List.of(DeliveryStatus.DELIVERED, DeliveryStatus.COMPLETED, DeliveryStatus.REJECTED));
        List<Map<String, Object>> stops = new java.util.ArrayList<>();
        for (int i = 0; i < active.size(); i++) {
            CustomerOrder order = active.get(i);
            MarketplaceListing listing = listingRepository.findById(order.getListingId()).orElse(null);
            stops.add(Map.of(
                    "sequence", i + 1,
                    "orderId", order.getId().toString(),
                    "title", listing != null ? listing.getTitle() : "Produce",
                    "quantity", order.getQuantity().toString(),
                    "unit", listing != null ? listing.getUnit() : "kg",
                    "status", order.getDeliveryStatus() != null
                            ? order.getDeliveryStatus().name().toLowerCase() : "assigned",
                    "createdAt", order.getCreatedAt().toString()
            ));
        }
        return Map.of("stops", stops, "totalStops", stops.size());
    }

    @Transactional
    public Map<String, Object> updateAvailability(UUID partnerId, String status) {
        DeliveryPartnerStatus.PartnerAvailability next;
        try {
            next = DeliveryPartnerStatus.PartnerAvailability.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid availability: " + status);
        }
        DeliveryPartnerStatus record = partnerStatusRepository.findByPartnerId(partnerId)
                .orElseGet(() -> {
                    DeliveryPartnerStatus s = new DeliveryPartnerStatus();
                    s.setPartnerId(partnerId);
                    return s;
                });
        record.setStatus(next);
        partnerStatusRepository.save(record);
        return Map.of("status", record.getStatus().name().toLowerCase());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailability(UUID partnerId) {
        Map<String, Object> available = new java.util.HashMap<>();
        available.put("status", "available");
        Map<String, Object> result = new java.util.HashMap<>(available);
        return partnerStatusRepository.findByPartnerId(partnerId)
                .map(s -> {
                    Map<String, Object> status = new java.util.HashMap<>();
                    status.put("status", s.getStatus().name().toLowerCase());
                    return status;
                })
                .orElse(result);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatsForToday(UUID partnerId) {
        return getDashboard(partnerId);
    }

    public BigDecimal computeEarning(BigDecimal orderValue) {
        return orderValue.multiply(BigDecimal.valueOf(props.logistics().earningPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private CustomerOrder getAssignedOrder(UUID partnerId, UUID orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Delivery not found"));
        if (!partnerId.equals(order.getDeliveryPartnerId())) {
            throw ApiException.forbidden("This delivery is not assigned to you");
        }
        return order;
    }

    private void recordHistory(UUID orderId, String from, String to, UUID actorId) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderType("delivery");
        history.setOrderId(orderId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actorId);
        historyRepository.save(history);
    }
}
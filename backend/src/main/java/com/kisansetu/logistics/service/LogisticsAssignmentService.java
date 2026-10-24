package com.kisansetu.logistics.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.entity.CustomerAddress;
import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.customer.repository.CustomerAddressRepository;
import com.kisansetu.customer.repository.CustomerOrderRepository;
import com.kisansetu.logistics.entity.Delivery;
import com.kisansetu.logistics.entity.DeliveryPartnerStatus;
import com.kisansetu.logistics.repository.DeliveryPartnerStatusRepository;
import com.kisansetu.logistics.repository.DeliveryRepository;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState.DeliveryStatus;
import com.kisansetu.user.entity.Profile;
import com.kisansetu.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Logistics delivery assignment engine (ported from the original SQL/edge
 * function logic):
 *   ranking: same city > same state > other, then fewest active deliveries,
 *   then earliest last_assigned_at.
 * An order with no available partner is marked PENDING_ASSIGNMENT and can
 * be retried later.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsAssignmentService {

    private static final List<DeliveryStatus> ACTIVE_DELIVERY_STATES =
            List.of(DeliveryStatus.PENDING_ASSIGNMENT, DeliveryStatus.ASSIGNED, DeliveryStatus.ACCEPTED,
                    DeliveryStatus.PICKUP_SCHEDULED, DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT);

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerAddressRepository addressRepository;
    private final DeliveryPartnerStatusRepository partnerStatusRepository;
    private final DeliveryRepository deliveryRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    /**
     * Attempt to assign a partner to an order. No-op if already assigned.
     * Throws when the order does not exist; otherwise best-effort.
     */
    @Transactional
    public AssignmentResult tryAssign(UUID orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (order.getDeliveryPartnerId() != null) {
            return new AssignmentResult(order.getDeliveryPartnerId(), "already_assigned");
        }
        if (order.getDeliveryAddressId() == null) {
            return new AssignmentResult(null, "no_address");
        }
        CustomerAddress address = addressRepository.findById(order.getDeliveryAddressId()).orElse(null);

        List<DeliveryPartnerStatus> available = partnerStatusRepository
                .findByStatus(DeliveryPartnerStatus.PartnerAvailability.AVAILABLE);
        if (available.isEmpty()) {
            order.setDeliveryStatus(DeliveryStatus.PENDING_ASSIGNMENT);
            customerOrderRepository.save(order);
            return new AssignmentResult(null, "no_available_partners");
        }

        Map<UUID, Long> activeCounts = new HashMap<>();
        for (DeliveryPartnerStatus partner : available) {
            activeCounts.put(partner.getPartnerId(),
                    customerOrderRepository.countActiveDeliveries(partner.getPartnerId(),
                            List.of(DeliveryStatus.DELIVERED, DeliveryStatus.COMPLETED, DeliveryStatus.REJECTED)));
        }

        Map<UUID, Profile> profiles = profileRepository.findByUserIds(
                        available.stream().map(DeliveryPartnerStatus::getPartnerId).toList())
                .stream().collect(Collectors.toMap(Profile::getUserId, p -> p));

        List<ScoredPartner> scored = new ArrayList<>();
        for (DeliveryPartnerStatus partner : available) {
            Profile profile = profiles.get(partner.getPartnerId());
            int rankGroup = 3;
            if (address != null && profile != null) {
                if (address.getCity() != null && profile.getCity() != null
                        && address.getCity().equalsIgnoreCase(profile.getCity())) {
                    rankGroup = 1;
                } else if (address.getState() != null && profile.getState() != null
                        && address.getState().equalsIgnoreCase(profile.getState())) {
                    rankGroup = 2;
                }
            }
            scored.add(new ScoredPartner(partner.getPartnerId(), rankGroup,
                    activeCounts.getOrDefault(partner.getPartnerId(), 0L),
                    partner.getLastAssignedAt() == null ? Instant.EPOCH : partner.getLastAssignedAt()));
        }

        scored.sort(Comparator.comparingInt(ScoredPartner::rankGroup)
                .thenComparingLong(ScoredPartner::activeCount)
                .thenComparing(ScoredPartner::lastAssignedAt));

        UUID chosen = scored.get(0).partnerId();
        order.setDeliveryPartnerId(chosen);
        order.setDeliveryStatus(DeliveryStatus.ASSIGNED);
        customerOrderRepository.save(order);

        partnerStatusRepository.findByPartnerId(chosen).ifPresent(p -> {
            p.setLastAssignedAt(Instant.now());
            partnerStatusRepository.save(p);
        });

        upsertDelivery(order, chosen, DeliveryStatus.ASSIGNED);
        notificationService.notify(chosen, "delivery_assigned",
                "New delivery assigned",
                "A new delivery has been assigned to you. Check your deliveries page.");
        return new AssignmentResult(chosen, "assigned");
    }

    /**
     * Called when a partner rejects: clear the partner, mark rejected, and
     * retry assignment.
     */
    @Transactional
    public AssignmentResult reassign(UUID orderId, UUID rejectingPartnerId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!Objects.equals(order.getDeliveryPartnerId(), rejectingPartnerId)) {
            throw ApiException.forbidden("This delivery is not assigned to you");
        }
        order.setDeliveryPartnerId(null);
        order.setDeliveryStatus(DeliveryStatus.REJECTED);
        customerOrderRepository.save(order);
        deliveryRepository.findByOrderId(orderId).ifPresent(d -> {
            d.setPartnerId(null);
            d.setStatus(DeliveryStatus.REJECTED);
            deliveryRepository.save(d);
        });
        return tryAssign(orderId);
    }

    /**
     * Best-effort sweep for orders stranded in PENDING_ASSIGNMENT.
     */
    @Transactional
    public int assignPending() {
        int processed = 0;
        List<CustomerOrder> pending = customerOrderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(null)
                .stream()
                .filter(o -> o.getDeliveryStatus() == DeliveryStatus.PENDING_ASSIGNMENT)
                .toList();
        for (CustomerOrder order : pending) {
            try {
                AssignmentResult result = tryAssign(order.getId());
                if ("assigned".equals(result.status())) {
                    processed++;
                }
            } catch (Exception e) {
                log.warn("assignPending failed for {}: {}", order.getId(), e.getMessage());
            }
        }
        return processed;
    }

    private void upsertDelivery(CustomerOrder order, UUID partnerId, DeliveryStatus status) {
        Delivery delivery = deliveryRepository.findByOrderId(order.getId()).orElse(new Delivery());
        delivery.setOrderId(order.getId());
        delivery.setPartnerId(partnerId);
        delivery.setStatus(status);
        deliveryRepository.save(delivery);
    }

    public record AssignmentResult(UUID partnerId, String status) {
    }

    private record ScoredPartner(UUID partnerId, int rankGroup, long activeCount, Instant lastAssignedAt) {
    }
}
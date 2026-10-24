package com.kisansetu.logistics.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.config.KisanSetuProperties;
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
import com.kisansetu.order.OrderState.CustomerOrderStatus;
import com.kisansetu.order.OrderState.DeliveryStatus;
import com.kisansetu.order.repository.OrderStatusHistoryRepository;
import com.kisansetu.user.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogisticsServiceTest {

    private static final UUID PARTNER = UUID.fromString("a0000000-0000-4000-8000-000000000031");
    private static final UUID CUSTOMER = UUID.fromString("a0000000-0000-4000-8000-000000000021");
    private static final UUID FARMER = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID LISTING_ID = UUID.randomUUID();

    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private DeliveryPartnerStatusRepository partnerStatusRepository;
    @Mock
    private OrderStatusHistoryRepository historyRepository;
    @Mock
    private MarketplaceListingRepository listingRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ProfileService profileService;
    @Mock
    private LogisticsAssignmentService assignmentService;
    @Mock
    private CustomerOrderService customerOrderService;

    private LogisticsService service;

    private CustomerOrder assignedOrder() {
        CustomerOrder order = new CustomerOrder();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER);
        order.setFarmerId(FARMER);
        order.setListingId(LISTING_ID);
        order.setQuantity(new BigDecimal("5"));
        order.setTotalPrice(new BigDecimal("200"));
        order.setStatus(CustomerOrderStatus.DISPATCHED);
        order.setDeliveryPartnerId(PARTNER);
        order.setDeliveryStatus(DeliveryStatus.ASSIGNED);
        order.setCreatedAt(Instant.now());
        return order;
    }

    private void stubDeliveryPersistence() {
        when(deliveryRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(new Delivery()));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderService.getResponseFor(any(CustomerOrder.class))).thenAnswer(inv -> {
            CustomerOrder o = inv.getArgument(0);
            return com.kisansetu.customer.dto.CustomerOrderResponse.from(o, null, null, null);
        });
    }

    @BeforeEach
    void setUp() {
        KisanSetuProperties props = new KisanSetuProperties(null, null, null, null, null,
                new KisanSetuProperties.Logistics(5, null), null);
        service = new LogisticsService(orderRepository, deliveryRepository, partnerStatusRepository,
                historyRepository, listingRepository, notificationService, profileService,
                props, assignmentService, customerOrderService);
    }

    @Test
    void acceptDelivery_transitionsAssignedToAccepted() {
        CustomerOrder order = assignedOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        stubDeliveryPersistence();

        service.acceptDelivery(PARTNER, ORDER_ID);

        assertEquals(DeliveryStatus.ACCEPTED, order.getDeliveryStatus());
        verify(orderRepository, times(2)).save(order);
        verify(historyRepository, times(2)).save(any());
    }

    @Test
    void acceptDelivery_forbiddenForOtherPartner() {
        CustomerOrder order = assignedOrder();
        order.setDeliveryPartnerId(UUID.randomUUID());
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.acceptDelivery(PARTNER, ORDER_ID));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void acceptDelivery_orderNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.acceptDelivery(PARTNER, ORDER_ID));
        assertEquals(404, ex.getStatus());
    }

    @Test
    void advanceDelivery_runsFullPipeline() {
        List<DeliveryStatus> sequence = List.of(
                DeliveryStatus.ACCEPTED, DeliveryStatus.PICKUP_SCHEDULED,
                DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT,
                DeliveryStatus.DELIVERED, DeliveryStatus.COMPLETED);
        for (DeliveryStatus expected : sequence) {
            CustomerOrder order = assignedOrder();
            if (expected != DeliveryStatus.ACCEPTED) {
                order.setDeliveryStatus(sequence.get(sequence.indexOf(expected) - 1));
            }
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            stubDeliveryPersistence();

            service.advanceDelivery(PARTNER, ORDER_ID);

            assertEquals(expected, order.getDeliveryStatus());
            verify(orderRepository).save(order);
            if (expected == DeliveryStatus.DELIVERED) {
                assertEquals(CustomerOrderStatus.DELIVERED, order.getStatus());
                assertNotNull(order.getDeliveredAt());
                assertNotNull(order.getContactDisabledAt());
                verify(notificationService, atLeastOnce()).notify(eq(CUSTOMER), eq("order_status"), any(), any());
                verify(notificationService, atLeastOnce()).notify(eq(FARMER), eq("order_status"), any(), any());
            }
        }
    }

    @Test
    void advanceDelivery_rejectsAfterCompleted() {
        CustomerOrder order = assignedOrder();
        order.setDeliveryStatus(DeliveryStatus.COMPLETED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.advanceDelivery(PARTNER, ORDER_ID));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void rejectDelivery_reassignsAndNotifiesCustomer() {
        CustomerOrder order = assignedOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(customerOrderService.getResponseFor(order))
                .thenReturn(com.kisansetu.customer.dto.CustomerOrderResponse.from(order, null, null, null));

        service.rejectDelivery(PARTNER, ORDER_ID);

        verify(assignmentService).reassign(ORDER_ID, PARTNER);
        verify(notificationService).notify(eq(CUSTOMER), eq("order_status"), any(), any());
    }

    @Test
    void updateAvailability_roundTrips() {
        Map<String, Object> updated = service.updateAvailability(PARTNER, "BUSY");
        assertEquals("busy", updated.get("status"));
        verify(partnerStatusRepository).save(any(DeliveryPartnerStatus.class));

        DeliveryPartnerStatus busy = new DeliveryPartnerStatus();
        busy.setStatus(DeliveryPartnerStatus.PartnerAvailability.BUSY);
        when(partnerStatusRepository.findByPartnerId(PARTNER)).thenReturn(Optional.of(busy));
        assertEquals("busy", service.getAvailability(PARTNER).get("status"));

        when(partnerStatusRepository.findByPartnerId(PARTNER)).thenReturn(Optional.empty());
        assertEquals("available", service.getAvailability(PARTNER).get("status"));
    }

    @Test
    void updateAvailability_invalidValueBadRequest() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateAvailability(PARTNER, "maybe"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void computeEarning_appliesPercentage() {
        assertEquals(0, new BigDecimal("10.00").compareTo(service.computeEarning(new BigDecimal("200"))));
    }

    @Test
    void getRoutes_sortsActiveDeliveriesIntoStops() {
        CustomerOrder order = assignedOrder();
        order.setDeliveryStatus(DeliveryStatus.ACCEPTED);
        when(orderRepository.findActiveDeliveriesAsc(PARTNER,
                List.of(DeliveryStatus.DELIVERED, DeliveryStatus.COMPLETED, DeliveryStatus.REJECTED)))
                .thenReturn(List.of(order));
        MarketplaceListing listing = new MarketplaceListing();
        listing.setTitle("Fresh Tomatoes");
        listing.setUnit("kg");
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));

        Map<String, Object> routes = service.getRoutes(PARTNER);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stops = (List<Map<String, Object>>) routes.get("stops");
        assertEquals(1, stops.size());
        assertEquals("Fresh Tomatoes", stops.get(0).get("title"));
        assertEquals("accepted", stops.get(0).get("status"));
        assertEquals(1, routes.get("totalStops"));
    }

    @Test
    void getEarnings_computesPercentages() {
        when(orderRepository.sumDeliveredValueSince(eq(PARTNER), any())).thenReturn(new BigDecimal("1000"));
        when(orderRepository.countDeliveredSince(eq(PARTNER), any())).thenReturn(2L);
        CustomerOrder order = assignedOrder();
        order.setDeliveryStatus(DeliveryStatus.COMPLETED);
        order.setDeliveredAt(Instant.now());
        when(orderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(PARTNER)).thenReturn(List.of(order));

        Map<String, Object> earnings = service.getEarnings(PARTNER);

        assertEquals(0, new BigDecimal("50.00").compareTo((BigDecimal) earnings.get("today")));
        assertEquals(2L, earnings.get("todayDeliveries"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) earnings.get("history");
        assertEquals(1, history.size());
        assertEquals(0, new BigDecimal("10.00").compareTo((BigDecimal) history.get(0).get("amount")));
    }
}
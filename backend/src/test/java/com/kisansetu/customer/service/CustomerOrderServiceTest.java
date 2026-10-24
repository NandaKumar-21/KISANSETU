package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.dto.CustomerOrderResponse;
import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.customer.repository.CustomerOrderRepository;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.logistics.service.LogisticsAssignmentService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTest {

    private static final UUID CUSTOMER = UUID.fromString("a0000000-0000-4000-8000-000000000021");
    private static final UUID FARMER = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID LISTING = UUID.randomUUID();

    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private MarketplaceListingRepository listingRepository;
    @Mock
    private OrderStatusHistoryRepository historyRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ProfileService profileService;
    @Mock
    private LogisticsAssignmentService logisticsAssignmentService;

    private CustomerOrderService service;

    private CustomerOrder order(CustomerOrderStatus status) {
        CustomerOrder order = new CustomerOrder();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER);
        order.setFarmerId(FARMER);
        order.setListingId(LISTING);
        order.setQuantity(new BigDecimal("5"));
        order.setUnitPrice(new BigDecimal("35"));
        order.setTotalPrice(new BigDecimal("175"));
        order.setStatus(status);
        return order;
    }

    @BeforeEach
    void setUp() {
        service = new CustomerOrderService(orderRepository, listingRepository, historyRepository,
                notificationService, profileService, logisticsAssignmentService);
    }

    @Test
    void acceptOrder_confirmsAndMakesContactVisible() {
        CustomerOrder order = order(CustomerOrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(new MarketplaceListing()));

        CustomerOrderResponse response = service.acceptOrder(FARMER, ORDER_ID, "Fresh batch");

        assertEquals("confirmed", response.status());
        assertTrue(response.farmerContactVisible());
        assertEquals("Fresh batch", order.getFarmerNotes());
        verify(historyRepository).save(any());
        verify(notificationService).notify(eq(CUSTOMER), any(), any(), any());
    }

    @Test
    void acceptOrder_forbiddenForWrongFarmer() {
        CustomerOrder order = order(CustomerOrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.acceptOrder(UUID.randomUUID(), ORDER_ID, null));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void rejectOrder_cancelsAndNotifies() {
        CustomerOrder order = order(CustomerOrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        CustomerOrderResponse response = service.rejectOrder(FARMER, ORDER_ID, "No stock");

        assertEquals("cancelled", response.status());
        verify(notificationService).notify(eq(CUSTOMER), any(), any(), contains("No stock"));
    }

    @Test
    void advanceStatus_movesThroughPipeline() {
        // input status -> expected status after advancing
        Map<CustomerOrderStatus, CustomerOrderStatus> steps = Map.of(
                CustomerOrderStatus.CONFIRMED, CustomerOrderStatus.PACKED,
                CustomerOrderStatus.PACKED, CustomerOrderStatus.DISPATCHED,
                CustomerOrderStatus.DISPATCHED, CustomerOrderStatus.DELIVERED);
        for (Map.Entry<CustomerOrderStatus, CustomerOrderStatus> step : steps.entrySet()) {
            CustomerOrder order = order(step.getKey());
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(listingRepository.findById(LISTING)).thenReturn(Optional.of(new MarketplaceListing()));

            CustomerOrderResponse response = service.advanceStatus(FARMER, ORDER_ID);

            assertEquals(step.getValue().name().toLowerCase(), response.status());
            verify(historyRepository, atLeastOnce()).save(any());
        }
        // dispatch triggers logistics assignment exactly once
        verify(logisticsAssignmentService, times(1)).tryAssign(ORDER_ID);
    }

    @Test
    void advanceStatus_rejectsAdvanceFromPending() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(CustomerOrderStatus.PENDING)));

        ApiException ex = assertThrows(ApiException.class, () -> service.advanceStatus(FARMER, ORDER_ID));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void advanceStatus_deliveryCompletedWhenFarmerDeliversDirectly() {
        CustomerOrder order = order(CustomerOrderStatus.DISPATCHED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(new MarketplaceListing()));

        CustomerOrderResponse response = service.advanceStatus(FARMER, ORDER_ID);

        assertEquals("delivered", response.status());
        assertEquals(DeliveryStatus.COMPLETED, order.getDeliveryStatus());
        assertNotNull(order.getDeliveredAt());
    }

    @Test
    void cancelByCustomer_cancelsPendingOrder() {
        CustomerOrder order = order(CustomerOrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        CustomerOrderResponse response = service.cancelByCustomer(CUSTOMER, ORDER_ID);

        assertEquals("cancelled", response.status());
        verify(notificationService).notify(eq(FARMER), any(), any(), any());
    }

    @Test
    void cancelByCustomer_forbiddenForOtherCustomer() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(CustomerOrderStatus.PENDING)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancelByCustomer(UUID.randomUUID(), ORDER_ID));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void getCustomerOrder_ownershipEnforced() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(CustomerOrderStatus.PENDING)));

        assertThrows(ApiException.class,
                () -> service.getCustomerOrder(UUID.randomUUID(), ORDER_ID));
        // own order resolves fine
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(new MarketplaceListing()));
        assertDoesNotThrow(() -> service.getCustomerOrder(CUSTOMER, ORDER_ID));
    }
}
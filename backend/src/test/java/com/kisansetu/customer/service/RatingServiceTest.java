package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.dto.RatingRequest;
import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.customer.entity.FarmerRating;
import com.kisansetu.customer.repository.CustomerOrderRepository;
import com.kisansetu.customer.repository.FarmerRatingRepository;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState.CustomerOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    private static final UUID CUSTOMER = UUID.fromString("a0000000-0000-4000-8000-000000000021");
    private static final UUID FARMER = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID ORDER = UUID.randomUUID();

    @Mock
    private FarmerRatingRepository ratingRepository;
    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private MarketplaceListingRepository listingRepository;
    @Mock
    private NotificationService notificationService;

    private RatingService service;

    private CustomerOrder deliveredOrder() {
        CustomerOrder order = new CustomerOrder();
        order.setId(ORDER);
        order.setCustomerId(CUSTOMER);
        order.setFarmerId(FARMER);
        order.setStatus(CustomerOrderStatus.DELIVERED);
        return order;
    }

    @BeforeEach
    void setUp() {
        service = new RatingService(ratingRepository, orderRepository, listingRepository, notificationService);
    }

    @Test
    void rateOrder_successOnDeliveredOrder() {
        when(orderRepository.findById(ORDER)).thenReturn(Optional.of(deliveredOrder()));
        when(ratingRepository.existsByCustomerIdAndOrderId(CUSTOMER, ORDER)).thenReturn(false);
        when(ratingRepository.save(any(FarmerRating.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmerRating rating = service.rateOrder(CUSTOMER, ORDER, new RatingRequest(5, "Great produce"));

        assertEquals(5, rating.getRating());
        assertEquals(FARMER, rating.getFarmerId());
        assertEquals(CUSTOMER, rating.getCustomerId());
        verify(notificationService).notify(eq(FARMER), eq("rating"), any(), any());
    }

    @Test
    void rateOrder_forbiddenWhenNotYourOrder() {
        CustomerOrder order = deliveredOrder();
        order.setCustomerId(UUID.randomUUID());
        when(orderRepository.findById(ORDER)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.rateOrder(CUSTOMER, ORDER, new RatingRequest(5, null)));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void rateOrder_conflictWhenNotDelivered() {
        CustomerOrder order = deliveredOrder();
        order.setStatus(CustomerOrderStatus.PENDING);
        when(orderRepository.findById(ORDER)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.rateOrder(CUSTOMER, ORDER, new RatingRequest(5, null)));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void rateOrder_conflictWhenAlreadyRated() {
        when(orderRepository.findById(ORDER)).thenReturn(Optional.of(deliveredOrder()));
        when(ratingRepository.existsByCustomerIdAndOrderId(CUSTOMER, ORDER)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.rateOrder(CUSTOMER, ORDER, new RatingRequest(5, null)));
        assertEquals(409, ex.getStatus());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void rateOrder_notFound() {
        when(orderRepository.findById(ORDER)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class,
                () -> service.rateOrder(CUSTOMER, ORDER, new RatingRequest(5, null)));
        assertEquals(404, ex.getStatus());
    }

    @Test
    void canRate_requiresDeliveredOrderWithoutExistingRating() {
        when(orderRepository.findById(ORDER)).thenReturn(Optional.of(deliveredOrder()));
        when(ratingRepository.existsByCustomerIdAndOrderId(CUSTOMER, ORDER)).thenReturn(false);
        assertTrue(service.canRate(CUSTOMER, ORDER));
    }

    @Test
    void canRate_falseWhenAlreadyRated() {
        when(orderRepository.findById(ORDER)).thenReturn(Optional.of(deliveredOrder()));
        when(ratingRepository.existsByCustomerIdAndOrderId(CUSTOMER, ORDER)).thenReturn(true);
        assertFalse(service.canRate(CUSTOMER, ORDER));
    }

    @Test
    void canRate_falseForNonDelivered() {
        CustomerOrder order = deliveredOrder();
        order.setStatus(CustomerOrderStatus.CONFIRMED);
        when(orderRepository.findById(ORDER)).thenReturn(Optional.of(order));
        assertFalse(service.canRate(CUSTOMER, ORDER));
    }
}
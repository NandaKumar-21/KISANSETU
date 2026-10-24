package com.kisansetu.farmer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.farmer.dto.ListingRequest;
import com.kisansetu.farmer.dto.ListingResponse;
import com.kisansetu.farmer.dto.MarketplaceOrderResponse;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.entity.MarketplaceOrder;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.farmer.repository.MarketplaceOrderRepository;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.repository.OrderStatusHistoryRepository;
import com.kisansetu.user.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    private static final UUID FARMER1 = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID FARMER2 = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    private static final UUID LISTING_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private MarketplaceListingRepository listingRepository;
    @Mock
    private MarketplaceOrderRepository orderRepository;
    @Mock
    private OrderStatusHistoryRepository historyRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ProfileService profileService;

    private MarketplaceService service;

    private ListingRequest listingRequest() {
        return new ListingRequest("Fresh Tomatoes", "Farm fresh", "Vegetables",
                new BigDecimal("35"), new BigDecimal("100"), "kg", null,
                "Kolhapur", "Desi", "Organic", LocalDate.now().plusDays(2), null, null);
    }

    private MarketplaceListing ownedListing() {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(LISTING_ID);
        listing.setFarmerId(FARMER1);
        listing.setTitle("Fresh Tomatoes");
        listing.setPrice(new BigDecimal("35"));
        listing.setQuantity(new BigDecimal("100"));
        listing.setUnit("kg");
        listing.setStatus(MarketplaceListing.ListingStatus.ACTIVE);
        return listing;
    }

    @BeforeEach
    void setUp() {
        service = new MarketplaceService(listingRepository, orderRepository, historyRepository,
                notificationService, profileService);
    }

    @Test
    void createListing_defaultsToActive() {
        when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(inv -> {
            MarketplaceListing saved = inv.getArgument(0);
            if (saved.getStatus() == null) {
                saved.setStatus(MarketplaceListing.ListingStatus.ACTIVE);
            }
            return saved;
        });

        ListingResponse response = service.createListing(FARMER1, listingRequest());

        assertEquals("Fresh Tomatoes", response.title());
        assertEquals("active", response.status());
        assertTrue(response.available());
    }

    @Test
    void updateListing_requiresOwnership() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(ownedListing()));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateListing(FARMER2, LISTING_ID, listingRequest()));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void changeStatus_updatesOwnListing() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(ownedListing()));
        when(listingRepository.save(any(MarketplaceListing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse response = service.changeStatus(FARMER1, LISTING_ID, "SOLD");

        assertEquals("sold", response.status());
        assertFalse(response.available());
    }

    @Test
    void changeStatus_invalidStatusBadRequest() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(ownedListing()));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changeStatus(FARMER1, LISTING_ID, "not-a-status"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void createMarketplaceOrder_createsPendingOrder() {
        MarketplaceListing listing = ownedListing();
        listing.setFarmerId(FARMER2);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));
        when(orderRepository.save(any(MarketplaceOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileService.getProfileEntityByUserId(any())).thenThrow(new RuntimeException("no profile"));

        MarketplaceOrderResponse response = service.createMarketplaceOrder(FARMER1, LISTING_ID, new BigDecimal("10"), "notes");

        assertEquals("pending", response.status());
        assertEquals(0, new BigDecimal("350.00").compareTo(response.totalPrice()));
        verify(notificationService).notify(eq(FARMER2), eq("marketplace_order"), any(), any());
    }

    @Test
    void createMarketplaceOrder_cannotOrderOwnListing() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(ownedListing()));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createMarketplaceOrder(FARMER1, LISTING_ID, BigDecimal.ONE, null));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void createMarketplaceOrder_rejectsQuantityAboveStock() {
        MarketplaceListing listing = ownedListing();
        listing.setFarmerId(FARMER2);
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createMarketplaceOrder(FARMER1, LISTING_ID, new BigDecimal("500"), null));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void updateMarketplaceOrderStatus_confirmsPendingOrder() {
        MarketplaceOrder order = new MarketplaceOrder();
        order.setId(ORDER_ID);
        order.setFarmerId(FARMER2);
        order.setBuyerId(FARMER1);
        order.setListingId(LISTING_ID);
        order.setStatus(com.kisansetu.order.OrderState.MarketplaceOrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(MarketplaceOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.of(ownedListing()));
        when(profileService.getProfileEntityByUserId(any())).thenThrow(new RuntimeException("no profile"));

        MarketplaceOrderResponse response = service.updateMarketplaceOrderStatus(FARMER2, ORDER_ID, "CONFIRMED");

        assertEquals("confirmed", response.status());
    }

    @Test
    void updateMarketplaceOrderStatus_rejectsInvalidTransition() {
        MarketplaceOrder order = new MarketplaceOrder();
        order.setId(ORDER_ID);
        order.setFarmerId(FARMER2);
        order.setStatus(com.kisansetu.order.OrderState.MarketplaceOrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateMarketplaceOrderStatus(FARMER2, ORDER_ID, "SHIPPED"));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void updateMarketplaceOrderStatus_forbiddenForOtherFarmer() {
        MarketplaceOrder order = new MarketplaceOrder();
        order.setId(ORDER_ID);
        order.setFarmerId(FARMER2);
        order.setStatus(com.kisansetu.order.OrderState.MarketplaceOrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateMarketplaceOrderStatus(FARMER1, ORDER_ID, "CONFIRMED"));
        assertEquals(403, ex.getStatus());
    }
}
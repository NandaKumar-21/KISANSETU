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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    private static final UUID CUSTOMER = UUID.fromString("a0000000-0000-4000-8000-000000000021");
    private static final UUID FARMER1 = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID FARMER2 = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    private static final UUID ADDRESS = UUID.randomUUID();
    private static final UUID LISTING1 = UUID.randomUUID();
    private static final UUID LISTING2 = UUID.randomUUID();
    private static final UUID LISTING3 = UUID.randomUUID();

    @Mock
    private CartItemRepository cartRepository;
    @Mock
    private CustomerOrderRepository customerOrderRepository;
    @Mock
    private CustomerAddressRepository addressRepository;
    @Mock
    private MarketplaceListingRepository listingRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LogisticsAssignmentService logisticsAssignmentService;

    private CheckoutService service;

    private final List<CustomerOrder> savedOrders = new ArrayList<>();

    private CustomerAddress ownedAddress() {
        CustomerAddress address = new CustomerAddress();
        address.setId(ADDRESS);
        address.setCustomerId(CUSTOMER);
        return address;
    }

    private MarketplaceListing listing(UUID id, UUID farmer, BigDecimal price, BigDecimal stock) {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(id);
        listing.setFarmerId(farmer);
        listing.setPrice(price);
        listing.setQuantity(stock);
        listing.setTitle("Produce " + id);
        listing.setUnit("kg");
        listing.setStatus(MarketplaceListing.ListingStatus.ACTIVE);
        return listing;
    }

    private CartItem cartItem(UUID id, UUID listingId, BigDecimal qty) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setCustomerId(CUSTOMER);
        item.setListingId(listingId);
        item.setQuantity(qty);
        return item;
    }

    @BeforeEach
    void setUp() {
        service = new CheckoutService(cartRepository, customerOrderRepository, addressRepository,
                listingRepository, notificationService, logisticsAssignmentService);
    }

    @Test
    void checkout_emptyCartRejected() {
        when(cartRepository.findByCustomerIdOrderByCreatedAtAsc(CUSTOMER)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> service.checkout(CUSTOMER, ADDRESS, "any", null));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void checkout_addressMustExistAndBelongToCustomer() {
        when(cartRepository.findByCustomerIdOrderByCreatedAtAsc(CUSTOMER))
                .thenReturn(List.of(cartItem(UUID.randomUUID(), LISTING1, BigDecimal.ONE)));
        when(addressRepository.findById(ADDRESS)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.checkout(CUSTOMER, ADDRESS, "any", null));

        CustomerAddress other = ownedAddress();
        other.setCustomerId(UUID.randomUUID());
        when(addressRepository.findById(ADDRESS)).thenReturn(Optional.of(other));

        ApiException ex = assertThrows(ApiException.class, () -> service.checkout(CUSTOMER, ADDRESS, "any", null));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void checkout_splitsOrdersPerFarmerAndClearsCart() {
        when(cartRepository.findByCustomerIdOrderByCreatedAtAsc(CUSTOMER)).thenReturn(List.of(
                cartItem(UUID.randomUUID(), LISTING1, new BigDecimal("2")),
                cartItem(UUID.randomUUID(), LISTING2, new BigDecimal("3")),
                cartItem(UUID.randomUUID(), LISTING3, new BigDecimal("5"))));
        when(addressRepository.findById(ADDRESS)).thenReturn(Optional.of(ownedAddress()));
        when(listingRepository.findById(LISTING1)).thenReturn(Optional.of(listing(LISTING1, FARMER1, new BigDecimal("35"), new BigDecimal("100"))));
        when(listingRepository.findById(LISTING2)).thenReturn(Optional.of(listing(LISTING2, FARMER1, new BigDecimal("30"), new BigDecimal("100"))));
        when(listingRepository.findById(LISTING3)).thenReturn(Optional.of(listing(LISTING3, FARMER2, new BigDecimal("68"), new BigDecimal("100"))));
        when(listingRepository.findAllById(any())).thenReturn(List.of(
                listing(LISTING1, FARMER1, new BigDecimal("35"), new BigDecimal("100")),
                listing(LISTING2, FARMER1, new BigDecimal("30"), new BigDecimal("100")),
                listing(LISTING3, FARMER2, new BigDecimal("68"), new BigDecimal("100"))));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(inv -> {
            CustomerOrder saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            savedOrders.add(saved);
            return saved;
        });
        when(customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER))
                .thenAnswer(inv -> new ArrayList<>(savedOrders));

        CheckoutResponse response = service.checkout(CUSTOMER, ADDRESS, "morning", "notes");

        assertEquals(3, response.ordersCreated());
        // 2*35 + 3*30 + 5*68 = 70 + 90 + 340 = 500
        assertEquals(0, new BigDecimal("500.00").compareTo(response.grandTotal()));
        verify(cartRepository).deleteByCustomerId(CUSTOMER);
        // one notification per farmer (2 distinct farmers)
        verify(notificationService, times(2)).notify(any(), any(), any(), any());
        // logistics assignment retried for each pending order
        verify(logisticsAssignmentService, times(3)).tryAssign(any());
    }

    @Test
    void checkout_quantityAboveStockRejected() {
        when(cartRepository.findByCustomerIdOrderByCreatedAtAsc(CUSTOMER))
                .thenReturn(List.of(cartItem(UUID.randomUUID(), LISTING1, new BigDecimal("50"))));
        when(addressRepository.findById(ADDRESS)).thenReturn(Optional.of(ownedAddress()));
        when(listingRepository.findById(LISTING1)).thenReturn(Optional.of(listing(LISTING1, FARMER1, BigDecimal.ONE, new BigDecimal("10"))));
        when(listingRepository.findAllById(any())).thenReturn(List.of(listing(LISTING1, FARMER1, BigDecimal.ONE, new BigDecimal("10"))));

        ApiException ex = assertThrows(ApiException.class, () -> service.checkout(CUSTOMER, ADDRESS, "any", null));
        assertEquals(409, ex.getStatus());
        verify(cartRepository, never()).deleteByCustomerId(any());
    }

    @Test
    void checkout_unavailableListingRejected() {
        MarketplaceListing listing = listing(LISTING1, FARMER1, BigDecimal.ONE, new BigDecimal("10"));
        listing.setStatus(MarketplaceListing.ListingStatus.SOLD);
        when(cartRepository.findByCustomerIdOrderByCreatedAtAsc(CUSTOMER))
                .thenReturn(List.of(cartItem(UUID.randomUUID(), LISTING1, BigDecimal.ONE)));
        when(addressRepository.findById(ADDRESS)).thenReturn(Optional.of(ownedAddress()));
        when(listingRepository.findById(LISTING1)).thenReturn(Optional.of(listing));
        when(listingRepository.findAllById(any())).thenReturn(List.of(listing));

        assertThrows(ApiException.class, () -> service.checkout(CUSTOMER, ADDRESS, "any", null));
        verify(cartRepository, never()).deleteByCustomerId(any());
    }
}
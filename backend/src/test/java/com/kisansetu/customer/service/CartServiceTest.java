package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.entity.CartItem;
import com.kisansetu.customer.repository.CartItemRepository;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final UUID CUSTOMER = UUID.fromString("a0000000-0000-4000-8000-000000000021");
    private static final UUID LISTING = UUID.randomUUID();
    private static final UUID ITEM = UUID.randomUUID();

    @Mock
    private CartItemRepository cartRepository;
    @Mock
    private MarketplaceListingRepository listingRepository;

    private CartService service;

    private MarketplaceListing availableListing(BigDecimal stock) {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(LISTING);
        listing.setQuantity(stock);
        listing.setStatus(MarketplaceListing.ListingStatus.ACTIVE);
        return listing;
    }

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepository, listingRepository);
    }

    @Test
    void addToCart_createsNewItem() {
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(availableListing(BigDecimal.TEN)));
        when(cartRepository.findByCustomerIdAndListingId(CUSTOMER, LISTING)).thenReturn(Optional.empty());
        when(cartRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem item = service.addToCart(CUSTOMER, LISTING, new BigDecimal("2.5"));

        assertEquals(new BigDecimal("2.5"), item.getQuantity());
        assertEquals(CUSTOMER, item.getCustomerId());
        assertEquals(LISTING, item.getListingId());
    }

    @Test
    void addToCart_mergesIntoExistingItem() {
        CartItem existing = new CartItem();
        existing.setQuantity(new BigDecimal("1.5"));
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(availableListing(BigDecimal.TEN)));
        when(cartRepository.findByCustomerIdAndListingId(CUSTOMER, LISTING)).thenReturn(Optional.of(existing));
        when(cartRepository.save(existing)).thenReturn(existing);

        CartItem item = service.addToCart(CUSTOMER, LISTING, new BigDecimal("2"));

        assertEquals(new BigDecimal("3.5"), item.getQuantity());
        verify(cartRepository).save(existing);
    }

    @Test
    void addToCart_defaultsQuantityToOne() {
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(availableListing(BigDecimal.TEN)));
        when(cartRepository.findByCustomerIdAndListingId(CUSTOMER, LISTING)).thenReturn(Optional.empty());
        when(cartRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem item = service.addToCart(CUSTOMER, LISTING, null);
        assertEquals(BigDecimal.ONE, item.getQuantity());
    }

    @Test
    void addToCart_rejectsUnavailableListing() {
        MarketplaceListing listing = availableListing(BigDecimal.TEN);
        listing.setStatus(MarketplaceListing.ListingStatus.SOLD);
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(listing));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.addToCart(CUSTOMER, LISTING, BigDecimal.ONE));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void addToCart_rejectsQuantityAboveStock() {
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(availableListing(new BigDecimal("3"))));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.addToCart(CUSTOMER, LISTING, new BigDecimal("4")));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void addToCart_rejectsMergeAboveStock() {
        CartItem existing = new CartItem();
        existing.setQuantity(new BigDecimal("2.5"));
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(availableListing(new BigDecimal("3"))));
        when(cartRepository.findByCustomerIdAndListingId(CUSTOMER, LISTING)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.addToCart(CUSTOMER, LISTING, BigDecimal.ONE));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void updateQuantity_deletesItemWhenQuantityInvalid() {
        CartItem item = new CartItem();
        item.setId(ITEM);
        item.setCustomerId(CUSTOMER);
        when(cartRepository.findById(ITEM)).thenReturn(Optional.of(item));

        CartItem result = service.updateQuantity(CUSTOMER, ITEM, BigDecimal.ZERO);

        assertNull(result);
        verify(cartRepository).delete(item);
    }

    @Test
    void updateQuantity_updatesWhenValid() {
        CartItem item = new CartItem();
        item.setId(ITEM);
        item.setCustomerId(CUSTOMER);
        item.setListingId(LISTING);
        when(cartRepository.findById(ITEM)).thenReturn(Optional.of(item));
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(availableListing(BigDecimal.TEN)));
        when(cartRepository.save(item)).thenReturn(item);

        CartItem result = service.updateQuantity(CUSTOMER, ITEM, new BigDecimal("7"));

        assertEquals(new BigDecimal("7"), result.getQuantity());
    }

    @Test
    void updateQuantity_rejectsAboveStock() {
        CartItem item = new CartItem();
        item.setId(ITEM);
        item.setCustomerId(CUSTOMER);
        item.setListingId(LISTING);
        when(cartRepository.findById(ITEM)).thenReturn(Optional.of(item));
        when(listingRepository.findById(LISTING)).thenReturn(Optional.of(availableListing(new BigDecimal("5"))));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateQuantity(CUSTOMER, ITEM, new BigDecimal("6")));
        assertEquals(409, ex.getStatus());
    }

    @Test
    void updateQuantity_forbiddenForOtherCustomer() {
        CartItem item = new CartItem();
        item.setId(ITEM);
        item.setCustomerId(UUID.randomUUID());
        when(cartRepository.findById(ITEM)).thenReturn(Optional.of(item));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateQuantity(CUSTOMER, ITEM, BigDecimal.ONE));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void removeFromCart_throwsForOtherCustomer() {
        CartItem item = new CartItem();
        item.setId(ITEM);
        item.setCustomerId(UUID.randomUUID());
        when(cartRepository.findById(ITEM)).thenReturn(Optional.of(item));

        assertThrows(ApiException.class, () -> service.removeFromCart(CUSTOMER, ITEM));
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void clearCart_delegates() {
        service.clearCart(CUSTOMER);
        verify(cartRepository).deleteByCustomerId(CUSTOMER);
    }
}
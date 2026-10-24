package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.entity.CartItem;
import com.kisansetu.customer.repository.CartItemRepository;
import com.kisansetu.farmer.entity.MarketplaceListing;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Customer cart. Prices are always read from the database server-side;
 * the frontend never supplies prices.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartRepository;
    private final MarketplaceListingRepository listingRepository;

    @Transactional(readOnly = true)
    public List<CartItem> getCart(UUID customerId) {
        return cartRepository.findByCustomerIdOrderByCreatedAtAsc(customerId);
    }

    @Transactional(readOnly = true)
    public long cartCount(UUID customerId) {
        return cartRepository.countByCustomerId(customerId);
    }

    @Transactional
    public CartItem addToCart(UUID customerId, UUID listingId, BigDecimal quantity) {
        MarketplaceListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Produce not found"));
        if (!listing.isAvailable()) {
            throw ApiException.conflict("This produce is no longer available");
        }
        BigDecimal qty = quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : quantity;
        if (qty.compareTo(listing.getQuantity()) > 0) {
            throw ApiException.conflict("Only " + listing.getQuantity() + " " + listing.getUnit()
                    + " available");
        }

        CartItem existing = cartRepository.findByCustomerIdAndListingId(customerId, listingId).orElse(null);
        if (existing != null) {
            BigDecimal combined = existing.getQuantity().add(qty);
            if (combined.compareTo(listing.getQuantity()) > 0) {
                throw ApiException.conflict("Cart quantity exceeds available stock");
            }
            existing.setQuantity(combined);
            return cartRepository.save(existing);
        }
        CartItem item = new CartItem();
        item.setCustomerId(customerId);
        item.setListingId(listingId);
        item.setQuantity(qty);
        return cartRepository.save(item);
    }

    @Transactional
    public CartItem updateQuantity(UUID customerId, UUID itemId, BigDecimal quantity) {
        CartItem item = getOwnedItem(customerId, itemId);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            cartRepository.delete(item);
            return null;
        }
        MarketplaceListing listing = listingRepository.findById(item.getListingId())
                .orElseThrow(() -> ApiException.notFound("Produce not found"));
        if (quantity.compareTo(listing.getQuantity()) > 0) {
            throw ApiException.conflict("Only " + listing.getQuantity() + " " + listing.getUnit()
                    + " available");
        }
        item.setQuantity(quantity);
        return cartRepository.save(item);
    }

    @Transactional
    public void removeFromCart(UUID customerId, UUID itemId) {
        cartRepository.delete(getOwnedItem(customerId, itemId));
    }

    @Transactional
    public void clearCart(UUID customerId) {
        cartRepository.deleteByCustomerId(customerId);
    }

    private CartItem getOwnedItem(UUID customerId, UUID itemId) {
        CartItem item = cartRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Cart item not found"));
        if (!item.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("This cart item does not belong to you");
        }
        return item;
    }
}
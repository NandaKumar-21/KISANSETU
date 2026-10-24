package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.entity.WishlistItem;
import com.kisansetu.customer.repository.WishlistItemRepository;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Customer wishlist supporting both produce listings and farmer favorites
 * (dual-use, matching the original behavior).
 */
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final MarketplaceListingRepository listingRepository;
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<WishlistItem> getWishlist(UUID customerId) {
        return wishlistRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public long wishlistCount(UUID customerId) {
        return wishlistRepository.countByCustomerId(customerId);
    }

    @Transactional
    public WishlistItem toggleListing(UUID customerId, UUID listingId) {
        var existing = wishlistRepository.findByCustomerIdAndListingId(customerId, listingId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return null;
        }
        listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Produce not found"));
        WishlistItem item = new WishlistItem();
        item.setCustomerId(customerId);
        item.setListingId(listingId);
        return wishlistRepository.save(item);
    }

    @Transactional
    public WishlistItem toggleFarmer(UUID customerId, UUID farmerId) {
        var existing = wishlistRepository.findByCustomerIdAndFarmerId(customerId, farmerId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return null;
        }
        profileRepository.findByUserId(farmerId)
                .orElseThrow(() -> ApiException.notFound("Farmer not found"));
        WishlistItem item = new WishlistItem();
        item.setCustomerId(customerId);
        item.setFarmerId(farmerId);
        return wishlistRepository.save(item);
    }

    @Transactional
    public boolean isFavoriteFarmer(UUID customerId, UUID farmerId) {
        return wishlistRepository.findByCustomerIdAndFarmerId(customerId, farmerId).isPresent();
    }

    @Transactional
    public void remove(UUID customerId, UUID itemId) {
        WishlistItem item = wishlistRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Wishlist item not found"));
        if (!item.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("This wishlist item does not belong to you");
        }
        wishlistRepository.delete(item);
    }
}
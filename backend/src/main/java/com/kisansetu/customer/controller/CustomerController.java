package com.kisansetu.customer.controller;

import com.kisansetu.ai.service.CustomerRecommendationService;
import com.kisansetu.common.ApiResponse;
import com.kisansetu.common.PageResponse;
import com.kisansetu.common.util.GeoUtil;
import com.kisansetu.customer.dto.*;
import com.kisansetu.customer.entity.CartItem;
import com.kisansetu.customer.entity.CustomerAddress;
import com.kisansetu.customer.entity.FarmerRating;
import com.kisansetu.customer.entity.WishlistItem;
import com.kisansetu.customer.service.*;
import com.kisansetu.farmer.dto.ListingResponse;
import com.kisansetu.security.CurrentUser;
import com.kisansetu.user.entity.Profile;
import com.kisansetu.user.repository.ProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer marketplace, cart, orders and profile actions")
public class CustomerController {

    private final CustomerMarketplaceService marketplaceService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final AddressService addressService;
    private final CheckoutService checkoutService;
    private final CustomerOrderService customerOrderService;
    private final RatingService ratingService;
    private final CustomerRecommendationService recommendationService;
    private final com.kisansetu.user.repository.ProfileRepository profileRepository;

    // ---------------- Marketplace ----------------

    @GetMapping("/farmers")
    @Operation(summary = "Discover farmers with distance and ratings")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<FarmerSummaryResponse>> farmers(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort) {
        // If no location provided, try to get from customer's profile
        if (lat == null || lng == null) {
            UUID customerId = CurrentUser.get().userId();
            var customerProfile = profileRepository.findByUserId(customerId);
            lat = customerProfile.map(p -> GeoUtil.asDouble(p.getLatitude())).orElse(null);
            lng = customerProfile.map(p -> GeoUtil.asDouble(p.getLongitude())).orElse(null);
        }
        return ApiResponse.ok(marketplaceService.getFarmers(lat, lng, radiusKm, search, category, sort));
    }

    @GetMapping("/farmers/{farmerId}")
    @Operation(summary = "Active listings of a single farmer")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<ListingResponse>> farmerListings(@PathVariable UUID farmerId) {
        return ApiResponse.ok(marketplaceService.getFarmerActiveListings(farmerId));
    }

    @GetMapping("/farmers/{farmerId}/profile")
    @Operation(summary = "Profile details of a single farmer")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<FarmerSummaryResponse> farmerProfile(
            @PathVariable UUID farmerId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        // If no location provided, try to get from customer's profile
        if (lat == null || lng == null) {
            UUID customerId = CurrentUser.get().userId();
            var customerProfile = profileRepository.findByUserId(customerId);
            lat = customerProfile.map(p -> GeoUtil.asDouble(p.getLatitude())).orElse(null);
            lng = customerProfile.map(p -> GeoUtil.asDouble(p.getLongitude())).orElse(null);
        }
        return ApiResponse.ok(marketplaceService.getFarmerProfile(farmerId, lat, lng)
                .orElseThrow(() -> com.kisansetu.common.exception.ApiException.notFound("Farmer not found")));
    }

    @GetMapping("/farmers/{farmerId}/ratings")
    @Operation(summary = "Ratings of a single farmer")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<FarmerRating>> farmerRatings(@PathVariable UUID farmerId) {
        return ApiResponse.ok(marketplaceService.getFarmerRatings(farmerId));
    }

    @GetMapping("/farmers/{farmerId}/sales-count")
    @Operation(summary = "Delivered sales count of a farmer")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<Map<String, Long>> farmerSalesCount(@PathVariable UUID farmerId) {
        return ApiResponse.ok(Map.of("deliveredSales", marketplaceService.countDeliveredSales(farmerId)));
    }

    @GetMapping("/produce")
    @Operation(summary = "Browse produce listings")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<PageResponse<ListingResponse>> produce(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ApiResponse.ok(marketplaceService.browseProduce(search, category, minPrice, maxPrice, pageable));
    }

    @GetMapping("/categories")
    @Operation(summary = "Distinct produce categories")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(marketplaceService.categories());
    }

    @GetMapping("/compare")
    @Operation(summary = "Price comparison across farmers")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<PriceCompareGroup>> compare(@RequestParam(required = false) String search,
                                                        @RequestParam(required = false) String category) {
        return ApiResponse.ok(marketplaceService.compareProduce(search, category));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Personalized produce recommendations")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CustomerRecommendationService.Result> recommendations() {
        return ApiResponse.ok(recommendationService.generate(CurrentUser.get().userId()));
    }

    // ---------------- Cart ----------------

    @GetMapping("/cart")
    @Operation(summary = "View cart")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<CartItem>> cart() {
        return ApiResponse.ok(cartService.getCart(CurrentUser.get().userId()));
    }

    @GetMapping("/cart/count")
    @Operation(summary = "Cart item count")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<Map<String, Long>> cartCount() {
        return ApiResponse.ok(Map.of("count", cartService.cartCount(CurrentUser.get().userId())));
    }

    @PostMapping("/cart")
    @Operation(summary = "Add produce to cart")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CartItem> addToCart(@RequestParam UUID listingId,
                                           @RequestParam(required = false) BigDecimal quantity) {
        return ApiResponse.created(cartService.addToCart(CurrentUser.get().userId(), listingId, quantity));
    }

    @PutMapping("/cart/{itemId}")
    @Operation(summary = "Update cart item quantity")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CartItem> updateCart(@PathVariable UUID itemId, @RequestParam BigDecimal quantity) {
        CartItem updated = cartService.updateQuantity(CurrentUser.get().userId(), itemId, quantity);
        if (updated == null) {
            return ApiResponse.deleted("Item removed from cart");
        }
        return ApiResponse.ok(updated);
    }

    @DeleteMapping("/cart/{itemId}")
    @Operation(summary = "Remove cart item")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<Void> removeCartItem(@PathVariable UUID itemId) {
        cartService.removeFromCart(CurrentUser.get().userId(), itemId);
        return ApiResponse.deleted("Item removed from cart");
    }

    @DeleteMapping("/cart")
    @Operation(summary = "Clear cart")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<Void> clearCart() {
        cartService.clearCart(CurrentUser.get().userId());
        return ApiResponse.deleted("Cart cleared");
    }

    // ---------------- Wishlist ----------------

    @GetMapping("/wishlist")
    @Operation(summary = "View wishlist")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<WishlistItem>> wishlist() {
        return ApiResponse.ok(wishlistService.getWishlist(CurrentUser.get().userId()));
    }

    @PostMapping("/wishlist/listing/{listingId}")
    @Operation(summary = "Toggle listing in wishlist")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<WishlistItem> toggleListing(@PathVariable UUID listingId) {
        WishlistItem item = wishlistService.toggleListing(CurrentUser.get().userId(), listingId);
        return item == null ? ApiResponse.deleted("Removed from wishlist") : ApiResponse.ok(item);
    }

    @PostMapping("/wishlist/farmer/{farmerId}")
    @Operation(summary = "Toggle farmer favorite")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<WishlistItem> toggleFarmer(@PathVariable UUID farmerId) {
        WishlistItem item = wishlistService.toggleFarmer(CurrentUser.get().userId(), farmerId);
        return item == null ? ApiResponse.deleted("Removed from favorites") : ApiResponse.ok(item);
    }

    @DeleteMapping("/wishlist/{itemId}")
    @Operation(summary = "Remove wishlist item")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<Void> removeWishlistItem(@PathVariable UUID itemId) {
        wishlistService.remove(CurrentUser.get().userId(), itemId);
        return ApiResponse.deleted("Removed from wishlist");
    }

    // ---------------- Addresses ----------------

    @GetMapping("/addresses")
    @Operation(summary = "Customer address book")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<CustomerAddress>> addresses() {
        return ApiResponse.ok(addressService.getAddresses(CurrentUser.get().userId()));
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add an address")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CustomerAddress> addAddress(@Valid @RequestBody AddressRequest request) {
        return ApiResponse.created(addressService.addAddress(CurrentUser.get().userId(), request));
    }

    @PutMapping("/addresses/{addressId}")
    @Operation(summary = "Update an address")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CustomerAddress> updateAddress(@PathVariable UUID addressId,
                                                      @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.updateAddress(CurrentUser.get().userId(), addressId, request));
    }

    @PostMapping("/addresses/{addressId}/default")
    @Operation(summary = "Set default address")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CustomerAddress> setDefaultAddress(@PathVariable UUID addressId) {
        return ApiResponse.ok(addressService.setDefault(CurrentUser.get().userId(), addressId));
    }

    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "Delete an address")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<Void> deleteAddress(@PathVariable UUID addressId) {
        addressService.deleteAddress(CurrentUser.get().userId(), addressId);
        return ApiResponse.deleted("Address deleted");
    }

    // ---------------- Orders & checkout ----------------

    @PostMapping("/checkout")
    @Operation(summary = "Checkout the cart (orders split per farmer)")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.ok(checkoutService.checkout(
                CurrentUser.get().userId(),
                request.deliveryAddressId(),
                request.deliveryPreference(),
                request.notes()));
    }

    @GetMapping("/orders")
    @Operation(summary = "My customer orders")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<List<CustomerOrderResponse>> myOrders() {
        return ApiResponse.ok(customerOrderService.getCustomerOrders(CurrentUser.get().userId()));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Order detail")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CustomerOrderResponse> orderDetail(@PathVariable UUID orderId) {
        return ApiResponse.ok(customerOrderService.getCustomerOrder(CurrentUser.get().userId(), orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancel a pending customer order")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CustomerOrderResponse> cancelOrder(@PathVariable UUID orderId) {
        return ApiResponse.ok(customerOrderService.cancelByCustomer(CurrentUser.get().userId(), orderId));
    }

    // ---------------- Ratings ----------------

    @PostMapping("/orders/{orderId}/rate")
    @Operation(summary = "Rate a delivered order (one rating per order)")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<FarmerRating> rateOrder(@PathVariable UUID orderId,
                                               @Valid @RequestBody RatingRequest request) {
        return ApiResponse.created(ratingService.rateOrder(CurrentUser.get().userId(), orderId, request));
    }

    @GetMapping("/orders/{orderId}/can-rate")
    @Operation(summary = "Whether the customer can rate an order")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<Map<String, Boolean>> canRate(@PathVariable UUID orderId) {
        return ApiResponse.ok(Map.of("canRate",
                ratingService.canRate(CurrentUser.get().userId(), orderId)));
    }

    // ---------------- Dashboard ----------------

    @GetMapping("/dashboard")
    @Operation(summary = "Customer dashboard aggregates")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<Map<String, Object>> dashboard() {
        UUID customerId = CurrentUser.get().userId();
        List<CustomerOrderResponse> orders = customerOrderService.getCustomerOrders(customerId);
        return ApiResponse.ok(Map.of(
                "cartCount", cartService.cartCount(customerId),
                "wishlistCount", wishlistService.wishlistCount(customerId),
                "activeOrders", orders.stream()
                        .filter(o -> List.of("pending", "confirmed", "packed", "dispatched").contains(o.status()))
                        .count(),
                "deliveredOrders", orders.stream().filter(o -> "delivered".equals(o.status())).count(),
                "recentOrders", orders.stream().limit(5).toList()
        ));
    }
}
package com.kisansetu.merchant.controller;

import com.kisansetu.common.ApiResponse;
import com.kisansetu.common.PageResponse;
import com.kisansetu.common.util.GeoUtil;
import com.kisansetu.merchant.dto.MerchantSummaryResponse;
import com.kisansetu.merchant.dto.ProductRequest;
import com.kisansetu.merchant.dto.ProductResponse;
import com.kisansetu.merchant.service.MerchantMarketplaceService;
import com.kisansetu.merchant.service.MerchantProductService;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.dto.OrderRequest;
import com.kisansetu.order.dto.OrderResponse;
import com.kisansetu.order.service.OrderService;
import com.kisansetu.security.CurrentUser;
import com.kisansetu.security.Role;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@Tag(name = "Merchant", description = "Merchant inventory, marketplace and orders")
public class MerchantController {

    private final MerchantProductService productService;
    private final MerchantMarketplaceService marketplaceService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final ProfileRepository profileRepository;

    // ---------------- Merchant marketplace (farmer browsing) ----------------

    @GetMapping("/marketplace/compare")
    @Operation(summary = "Price comparison across merchants")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<MerchantMarketplaceService.PriceCompareGroup>> compare() {
        return ApiResponse.ok(marketplaceService.compareProducts());
    }

    @GetMapping("/marketplace/categories")
    @Operation(summary = "Get distinct product categories")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.ok(marketplaceService.getCategories());
    }

    @GetMapping("/marketplace/products")
    @Operation(summary = "Browse all merchant products (farmer marketplace)")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<PageResponse<ProductResponse>> browseProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ApiResponse.ok(marketplaceService.browseProducts(search, category, minPrice, maxPrice, pageable));
    }

    @GetMapping("/marketplace/nearby")
    @Operation(summary = "Browse nearby merchants using authenticated farmer's location")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<MerchantSummaryResponse>> getNearbyMerchants(
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String search) {
        UUID farmerId = CurrentUser.get().userId();
        var farmerProfile = profileRepository.findByUserId(farmerId);
        Double lat = farmerProfile.map(p -> GeoUtil.asDouble(p.getLatitude())).orElse(null);
        Double lng = farmerProfile.map(p -> GeoUtil.asDouble(p.getLongitude())).orElse(null);
        return ApiResponse.ok(marketplaceService.getMerchants(lat, lng, radiusKm, search));
    }

    @GetMapping("/marketplace/{merchantId}")
    @Operation(summary = "Get a merchant's in-stock products")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<ProductResponse>> getMerchantProducts(@PathVariable UUID merchantId) {
        return ApiResponse.ok(marketplaceService.getMerchantProducts(merchantId));
    }

    @GetMapping("/marketplace")
    @Operation(summary = "Browse merchants (aggregated pricing + distance)")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<MerchantSummaryResponse>> getMerchants(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String search) {
        // If no location provided, try to get from farmer's profile
        if (lat == null || lng == null) {
            UUID farmerId = CurrentUser.get().userId();
            Optional<Profile> farmerProfile = profileRepository.findByUserId(farmerId);
            lat = farmerProfile.map(p -> GeoUtil.asDouble(p.getLatitude())).orElse(null);
            lng = farmerProfile.map(p -> GeoUtil.asDouble(p.getLongitude())).orElse(null);
        }
        return ApiResponse.ok(marketplaceService.getMerchants(lat, lng, radiusKm, search));
    }

    @GetMapping("/marketplace/{merchantId}/profile")
    @Operation(summary = "Get merchant profile details")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<MerchantSummaryResponse> getMerchantProfile(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return ApiResponse.ok(marketplaceService.getMerchantProfile(merchantId, lat, lng)
                .orElseThrow(() -> com.kisansetu.common.exception.ApiException.notFound("Merchant not found")));
    }

    // ---------------- Farmer -> Merchant orders ----------------

    @PostMapping("/orders")
    @Operation(summary = "Place an order from a farmer to a merchant")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.created(orderService.createOrder(CurrentUser.get().userId(), request));
    }

    @GetMapping("/orders/mine")
    @Operation(summary = "Farmer's own orders")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<OrderResponse>> myFarmerOrders() {
        return ApiResponse.ok(orderService.getFarmerOrders(CurrentUser.get().userId()));
    }

    @GetMapping("/orders")
    @Operation(summary = "Orders received by the merchant")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<List<OrderResponse>> receivedOrders() {
        return ApiResponse.ok(orderService.getMerchantOrders(CurrentUser.get().userId()));
    }

    @GetMapping("/orders/counts")
    @Operation(summary = "Merchant order counts by status")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<Map<String, Long>> orderCounts() {
        return ApiResponse.ok(orderService.getMerchantOrderCounts(CurrentUser.get().userId()));
    }

    @PostMapping("/orders/{orderId}/accept")
    @Operation(summary = "Merchant accepts an order (deducts stock)")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<OrderResponse> acceptOrder(@PathVariable UUID orderId) {
        return ApiResponse.ok(orderService.acceptOrder(CurrentUser.get().userId(), orderId));
    }

    @PostMapping("/orders/{orderId}/advance")
    @Operation(summary = "Advance merchant order status")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<OrderResponse> advanceOrder(@PathVariable UUID orderId,
                                                   @RequestParam String status) {
        return ApiResponse.ok(orderService.advanceStatus(CurrentUser.get().userId(), orderId, status));
    }

    @PostMapping("/orders/{orderId}/reject")
    @Operation(summary = "Merchant rejects an order")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<OrderResponse> rejectOrder(@PathVariable UUID orderId,
                                                  @RequestParam(required = false) String reason) {
        return ApiResponse.ok(orderService.rejectOrder(CurrentUser.get().userId(), orderId, reason));
    }

    // ---------------- Products (inventory) ----------------

    @GetMapping("/products")
    @Operation(summary = "Merchant's own products")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<List<ProductResponse>> myProducts() {
        return ApiResponse.ok(productService.getMyProducts(CurrentUser.get().userId()));
    }

    @PostMapping("/products")
    @Operation(summary = "Create a product")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.created(productService.createProduct(CurrentUser.get().userId(), request));
    }

    @PutMapping("/products/{productId}")
    @Operation(summary = "Update a product")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable UUID productId,
                                                      @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.updateProduct(CurrentUser.get().userId(), productId, request));
    }

    @DeleteMapping("/products/{productId}")
    @Operation(summary = "Delete a product")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<Void> deleteProduct(@PathVariable UUID productId) {
        productService.deleteProduct(CurrentUser.get().userId(), productId);
        return ApiResponse.deleted("Product deleted");
    }

    // ---------------- Dashboard ----------------

    @GetMapping("/dashboard")
    @Operation(summary = "Merchant dashboard: product and order aggregates")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<Map<String, Object>> dashboard() {
        UUID merchantId = CurrentUser.get().userId();
        List<ProductResponse> products = productService.getMyProducts(merchantId);
        Map<String, Long> counts = orderService.getMerchantOrderCounts(merchantId);
        int totalStock = products.stream().mapToInt(ProductResponse::quantity).sum();
        long lowStock = products.stream().filter(p -> p.quantity() <= p.stockThreshold()).count();
        BigDecimal stockValue = products.stream()
                .map(p -> p.price().multiply(BigDecimal.valueOf(p.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ApiResponse.ok(Map.of(
                "totalProducts", products.size(),
                "totalStock", totalStock,
                "lowStockItems", lowStock,
                "stockValue", stockValue,
                "orderCounts", counts,
                "unreadNotifications", notificationService.unreadCount(merchantId)
        ));
    }

    @GetMapping("/products/paginated")
    @Operation(summary = "Paginated product list for the merchant")
    @PreAuthorize("hasAnyRole('MERCHANT')")
    public ApiResponse<PageResponse<ProductResponse>> paginatedProducts(Pageable pageable) {
        List<ProductResponse> all = productService.getMyProducts(CurrentUser.get().userId());
        int page = pageable.getPageNumber();
        int size = Math.max(1, pageable.getPageSize());
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        var sub = all.subList(from, to);
        PageResponse<ProductResponse> response = new PageResponse<>(
                sub, page, size, all.size(),
                (int) Math.ceil(all.size() / (double) size),
                page == 0, to == all.size());
        return ApiResponse.ok(response);
    }
}
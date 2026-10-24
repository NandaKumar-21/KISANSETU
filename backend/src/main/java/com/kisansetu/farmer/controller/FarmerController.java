package com.kisansetu.farmer.controller;

import com.kisansetu.ai.service.CropRecommendationService;
import com.kisansetu.common.ApiResponse;
import com.kisansetu.common.PageResponse;
import com.kisansetu.customer.dto.CustomerOrderResponse;
import com.kisansetu.customer.service.CustomerOrderService;
import com.kisansetu.farmer.dto.CalendarEventRequest;
import com.kisansetu.farmer.dto.CalendarEventResponse;
import com.kisansetu.farmer.dto.ListingRequest;
import com.kisansetu.farmer.dto.ListingResponse;
import com.kisansetu.farmer.dto.MarketplaceOrderResponse;
import com.kisansetu.farmer.service.CalendarService;
import com.kisansetu.farmer.service.MarketplaceService;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.security.CurrentUser;
import com.kisansetu.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/farmer")
@RequiredArgsConstructor
@Tag(name = "Farmer", description = "Farmer marketplace, calendar and dashboard")
public class FarmerController {

    private final MarketplaceService marketplaceService;
    private final CustomerOrderService customerOrderService;
    private final CalendarService calendarService;
    private final WeatherService weatherService;
    private final NotificationService notificationService;
    private final CropRecommendationService cropRecommendationService;

    // ---------------- Listings (sell produce) ----------------

    @GetMapping("/listings")
    @Operation(summary = "Farmer's own marketplace listings")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<ListingResponse>> myListings() {
        return ApiResponse.ok(marketplaceService.getMyListings(CurrentUser.get().userId()));
    }

    @PostMapping("/listings")
    @Operation(summary = "Create a marketplace listing")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<ListingResponse> createListing(@Valid @RequestBody ListingRequest request) {
        return ApiResponse.created(marketplaceService.createListing(CurrentUser.get().userId(), request));
    }

    @PutMapping("/listings/{listingId}")
    @Operation(summary = "Update a listing")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<ListingResponse> updateListing(@PathVariable UUID listingId,
                                                      @Valid @RequestBody ListingRequest request) {
        return ApiResponse.ok(marketplaceService.updateListing(CurrentUser.get().userId(), listingId, request));
    }

    @PatchMapping("/listings/{listingId}/status")
    @Operation(summary = "Change listing status (active/sold/archived)")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<ListingResponse> changeListingStatus(@PathVariable UUID listingId,
                                                            @RequestParam String status) {
        return ApiResponse.ok(marketplaceService.changeStatus(CurrentUser.get().userId(), listingId, status));
    }

    @DeleteMapping("/listings/{listingId}")
    @Operation(summary = "Delete a listing")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<Void> deleteListing(@PathVariable UUID listingId) {
        marketplaceService.deleteListing(CurrentUser.get().userId(), listingId);
        return ApiResponse.deleted("Listing deleted");
    }

    // ---------------- Marketplace orders (buy from other farmers) ----------------

    @GetMapping("/marketplace")
    @Operation(summary = "Browse other farmers' listings")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<PageResponse<ListingResponse>> browse(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ApiResponse.ok(marketplaceService.browseListings(search, category, minPrice, maxPrice, pageable));
    }

    @PostMapping("/marketplace/{listingId}/orders")
    @Operation(summary = "Place a marketplace order with another farmer")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<MarketplaceOrderResponse> placeMarketplaceOrder(
            @PathVariable UUID listingId,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) String notes) {
        return ApiResponse.created(marketplaceService.createMarketplaceOrder(
                CurrentUser.get().userId(), listingId, quantity, notes));
    }

    @GetMapping("/marketplace/orders/bought")
    @Operation(summary = "Marketplace orders I bought")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<MarketplaceOrderResponse>> boughtOrders() {
        return ApiResponse.ok(marketplaceService.getMyMarketplaceOrders(CurrentUser.get().userId()));
    }

    @GetMapping("/marketplace/orders/sold")
    @Operation(summary = "Marketplace orders on my listings")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<MarketplaceOrderResponse>> soldOrders() {
        return ApiResponse.ok(marketplaceService.getOrdersForMyListings(CurrentUser.get().userId()));
    }

    @PostMapping("/marketplace/orders/{orderId}/status")
    @Operation(summary = "Update marketplace order status (confirm/ship/deliver/cancel)")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<MarketplaceOrderResponse> updateMarketplaceOrderStatus(
            @PathVariable UUID orderId, @RequestParam String status) {
        return ApiResponse.ok(marketplaceService.updateMarketplaceOrderStatus(
                CurrentUser.get().userId(), orderId, status));
    }

    // ---------------- Customer orders (received from customers) ----------------

    @GetMapping("/customer-orders")
    @Operation(summary = "Customer orders received by the farmer")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<CustomerOrderResponse>> customerOrders() {
        return ApiResponse.ok(customerOrderService.getFarmerOrders(CurrentUser.get().userId()));
    }

    @PostMapping("/customer-orders/{orderId}/accept")
    @Operation(summary = "Accept a customer order")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<CustomerOrderResponse> acceptCustomerOrder(@PathVariable UUID orderId,
                                                                  @RequestParam(required = false) String notes) {
        return ApiResponse.ok(customerOrderService.acceptOrder(CurrentUser.get().userId(), orderId, notes));
    }

    @PostMapping("/customer-orders/{orderId}/reject")
    @Operation(summary = "Reject a customer order")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<CustomerOrderResponse> rejectCustomerOrder(@PathVariable UUID orderId,
                                                                  @RequestParam(required = false) String reason) {
        return ApiResponse.ok(customerOrderService.rejectOrder(CurrentUser.get().userId(), orderId, reason));
    }

    @PostMapping("/customer-orders/{orderId}/advance")
    @Operation(summary = "Advance customer order status (packed/dispatched/delivered)")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<CustomerOrderResponse> advanceCustomerOrder(@PathVariable UUID orderId) {
        return ApiResponse.ok(customerOrderService.advanceStatus(CurrentUser.get().userId(), orderId));
    }

    // ---------------- Calendar ----------------

    @GetMapping("/calendar")
    @Operation(summary = "Farmer calendar events")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<CalendarEventResponse>> calendar(@RequestParam(required = false) LocalDate from,
                                                             @RequestParam(required = false) LocalDate to) {
        return ApiResponse.ok(calendarService.getEvents(CurrentUser.get().userId(), from, to));
    }

    @GetMapping("/calendar/upcoming")
    @Operation(summary = "Upcoming calendar tasks")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<CalendarEventResponse>> upcomingCalendar() {
        return ApiResponse.ok(calendarService.getUpcoming(CurrentUser.get().userId()));
    }

    @PostMapping("/calendar")
    @Operation(summary = "Add a calendar event")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<CalendarEventResponse> addCalendarEvent(@Valid @RequestBody CalendarEventRequest request) {
        return ApiResponse.created(calendarService.createEvent(CurrentUser.get().userId(), request));
    }

    @PutMapping("/calendar/{eventId}")
    @Operation(summary = "Update a calendar event")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<CalendarEventResponse> updateCalendarEvent(@PathVariable UUID eventId,
                                                                  @Valid @RequestBody CalendarEventRequest request) {
        return ApiResponse.ok(calendarService.updateEvent(CurrentUser.get().userId(), eventId, request));
    }

    @PatchMapping("/calendar/{eventId}/toggle")
    @Operation(summary = "Toggle calendar event completion")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<CalendarEventResponse> toggleCalendarEvent(@PathVariable UUID eventId) {
        return ApiResponse.ok(calendarService.toggleCompleted(CurrentUser.get().userId(), eventId));
    }

    @DeleteMapping("/calendar/{eventId}")
    @Operation(summary = "Delete a calendar event")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<Void> deleteCalendarEvent(@PathVariable UUID eventId) {
        calendarService.deleteEvent(CurrentUser.get().userId(), eventId);
        return ApiResponse.deleted("Event deleted");
    }

    // ---------------- Dashboard ----------------

    @GetMapping("/dashboard")
    @Operation(summary = "Farmer dashboard aggregates")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam(required = false) Double lat,
                                                      @RequestParam(required = false) Double lng) {
        UUID farmerId = CurrentUser.get().userId();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("activeListings", marketplaceService.activeListingCount(farmerId));
        data.put("myListings", marketplaceService.getMyListings(farmerId).size());
        data.put("pendingCustomerOrders", customerOrderService.getFarmerOrders(farmerId).stream()
                .filter(o -> "pending".equals(o.status())).count());
        data.put("totalCustomerOrders", customerOrderService.getFarmerOrders(farmerId).size());
        data.put("upcomingCalendar", calendarService.getUpcoming(farmerId));
        data.put("unreadNotifications", notificationService.unreadCount(farmerId));
        try {
            data.put("weather", weatherService.getWeather(lat, lng));
        } catch (Exception ignored) {
        }
        return ApiResponse.ok(data);
    }
}
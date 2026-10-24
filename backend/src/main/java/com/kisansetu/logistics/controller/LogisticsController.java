package com.kisansetu.logistics.controller;

import com.kisansetu.common.ApiResponse;
import com.kisansetu.customer.dto.CustomerOrderResponse;
import com.kisansetu.logistics.service.LogisticsService;
import com.kisansetu.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
@Tag(name = "Logistics", description = "Delivery partner orders, deliveries, routes and earnings")
public class LogisticsController {

    private final LogisticsService logisticsService;

    @GetMapping("/orders")
    @Operation(summary = "Orders assigned to the partner (all statuses)")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<List<CustomerOrderResponse>> orders() {
        return ApiResponse.ok(logisticsService.getPartnerOrders(CurrentUser.get().userId()));
    }

    @GetMapping("/deliveries/active")
    @Operation(summary = "Active deliveries for the partner")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<List<CustomerOrderResponse>> activeDeliveries() {
        return ApiResponse.ok(logisticsService.getActiveDeliveries(CurrentUser.get().userId()));
    }

    @GetMapping("/deliveries/completed")
    @Operation(summary = "Completed deliveries for the partner")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<List<CustomerOrderResponse>> completedDeliveries() {
        return ApiResponse.ok(logisticsService.getCompletedDeliveries(CurrentUser.get().userId()));
    }

    @PostMapping("/deliveries/{orderId}/accept")
    @Operation(summary = "Accept an assigned delivery")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<CustomerOrderResponse> accept(@PathVariable UUID orderId) {
        return ApiResponse.ok(logisticsService.acceptDelivery(CurrentUser.get().userId(), orderId));
    }

    @PostMapping("/deliveries/{orderId}/reject")
    @Operation(summary = "Reject a delivery (triggers reassignment)")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<CustomerOrderResponse> reject(@PathVariable UUID orderId) {
        return ApiResponse.ok(logisticsService.rejectDelivery(CurrentUser.get().userId(), orderId));
    }

    @PostMapping("/deliveries/{orderId}/advance")
    @Operation(summary = "Advance the delivery pipeline one step")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<CustomerOrderResponse> advance(@PathVariable UUID orderId) {
        return ApiResponse.ok(logisticsService.advanceDelivery(CurrentUser.get().userId(), orderId));
    }

    @GetMapping("/routes")
    @Operation(summary = "Optimized route stops for active deliveries")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<Map<String, Object>> routes() {
        return ApiResponse.ok(logisticsService.getRoutes(CurrentUser.get().userId()));
    }

    @GetMapping("/earnings")
    @Operation(summary = "Earnings breakdown (5% of order value)")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<Map<String, Object>> earnings() {
        return ApiResponse.ok(logisticsService.getEarnings(CurrentUser.get().userId()));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Logistics dashboard aggregates")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(logisticsService.getDashboard(CurrentUser.get().userId()));
    }

    @PutMapping("/availability")
    @Operation(summary = "Set partner availability (available/busy/offline)")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<Map<String, Object>> updateAvailability(@RequestParam String status) {
        return ApiResponse.ok(logisticsService.updateAvailability(CurrentUser.get().userId(), status));
    }

    @GetMapping("/availability")
    @Operation(summary = "Current partner availability")
    @PreAuthorize("hasAnyRole('LOGISTICS')")
    public ApiResponse<Map<String, Object>> availability() {
        return ApiResponse.ok(logisticsService.getAvailability(CurrentUser.get().userId()));
    }
}
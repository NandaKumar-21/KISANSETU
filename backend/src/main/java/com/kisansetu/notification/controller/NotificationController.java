package com.kisansetu.notification.controller;

import com.kisansetu.common.ApiResponse;
import com.kisansetu.notification.entity.Notification;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "My notifications (latest 50)")
    public ApiResponse<List<Notification>> myNotifications() {
        return ApiResponse.ok(notificationService.getMyNotifications(CurrentUser.get().userId()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count",
                notificationService.unreadCount(CurrentUser.get().userId())));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark one notification as read")
    public ApiResponse<Void> markRead(@PathVariable UUID notificationId) {
        notificationService.markRead(CurrentUser.get().userId(), notificationId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ApiResponse<Map<String, Integer>> markAllRead() {
        return ApiResponse.ok(Map.of("updated",
                notificationService.markAllRead(CurrentUser.get().userId())));
    }
}
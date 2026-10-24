package com.kisansetu.notification.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.notification.entity.Notification;
import com.kisansetu.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID USER_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    @Mock
    private NotificationRepository repository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
    }

    @Test
    void notify_createsNotificationWithDefaults() {
        Notification saved = new Notification();
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = service.notify(USER_ID, "order_status", "Title", "Message");

        assertFalse(result.isRead());
        assertEquals(USER_ID, result.getUserId());
        assertEquals("order_status", result.getType());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertEquals("Title", captor.getValue().getTitle());
    }

    @Test
    void getMyNotifications_limitsToFifty() {
        when(repository.findTop50ByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());
        assertTrue(service.getMyNotifications(USER_ID).isEmpty());
        verify(repository).findTop50ByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    void unreadCount_delegates() {
        when(repository.countByUserIdAndReadFalse(USER_ID)).thenReturn(3L);
        assertEquals(3L, service.unreadCount(USER_ID));
    }

    @Test
    void markRead_marksOwnNotification() {
        Notification n = new Notification();
        n.setUserId(USER_ID);
        when(repository.findById(any())).thenReturn(Optional.of(n));

        service.markRead(USER_ID, UUID.randomUUID());

        assertTrue(n.isRead());
        verify(repository).save(n);
    }

    @Test
    void markRead_throwsForOtherUsersNotification() {
        Notification n = new Notification();
        n.setUserId(UUID.randomUUID());
        when(repository.findById(any())).thenReturn(Optional.of(n));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.markRead(USER_ID, UUID.randomUUID()));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void markRead_throwsWhenMissing() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class,
                () -> service.markRead(USER_ID, UUID.randomUUID()));
        assertEquals(404, ex.getStatus());
    }

    @Test
    void markAllRead_delegates() {
        when(repository.markAllRead(USER_ID)).thenReturn(2);
        assertEquals(2, service.markAllRead(USER_ID));
    }
}
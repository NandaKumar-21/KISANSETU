package com.kisansetu.notification.service;

import com.kisansetu.notification.entity.Notification;
import com.kisansetu.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification notify(UUID userId, String type, String title, String message) {
        return notify(userId, type, title, message, null);
    }

    @Transactional
    public Notification notify(UUID userId, String type, String title, String message, String dataJson) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setData(dataJson);
        return notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(UUID userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> com.kisansetu.common.exception.ApiException.notFound("Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw com.kisansetu.common.exception.ApiException.forbidden("Not your notification");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId);
    }
}
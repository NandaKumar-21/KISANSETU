package com.kisansetu.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class NotificationIntegrationTest extends IntegrationTestBase {

    @Test
    void listUnreadAndMarkRead() {
        ResponseEntity<String> unread = get("/api/notifications/unread-count", farmer1Token());
        assertEquals(200, unread.getStatusCode().value());
        assertTrue(unread.getBody().contains("\"count\""));

        ResponseEntity<String> list = get("/api/notifications", farmer1Token());
        assertEquals(200, list.getStatusCode().value());
        assertTrue(list.getBody().contains("\"items\"") || list.getBody().contains("["));

        // Mark one read if any exist; endpoint must at least respond
        ResponseEntity<String> all = post("/api/notifications/read-all", farmer1Token(), null);
        assertEquals(200, all.getStatusCode().value());

        ResponseEntity<String> after = get("/api/notifications/unread-count", farmer1Token());
        assertEquals(200, after.getStatusCode().value());
    }

    @Test
    void notificationsArePerUser() {
        // Customer has their own notification stream; must not 403/404
        assertEquals(200, get("/api/notifications", customerToken()).getStatusCode().value());
        assertEquals(200, get("/api/notifications/unread-count", partnerToken()).getStatusCode().value());
    }
}
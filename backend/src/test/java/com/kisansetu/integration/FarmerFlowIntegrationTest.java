package com.kisansetu.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FarmerFlowIntegrationTest extends IntegrationTestBase {

    private Map<String, Object> listingBody(String title) {
        return Map.of(
                "title", title,
                "description", "Integration test listing",
                "category", "Vegetables",
                "price", 40.00,
                "quantity", 50,
                "unit", "kg",
                "location", "Kolhapur",
                "variety", "Desi",
                "farmingMethod", "Organic",
                "harvestDate", "2026-09-01");
    }

    @Test
    void createListingsAndUpdateStatus() {
        String title = "IT Tomatoes " + System.nanoTime();
        ResponseEntity<String> created = post("/api/farmer/listings", farmer1Token(), listingBody(title));
        assertEquals(200, created.getStatusCode().value());
        assertTrue(created.getBody().contains(title));
        assertTrue(created.getBody().contains("\"status\":\"active\""));

        String listingId = created.getBody().replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1");
        assertTrue(listingId.matches("[0-9a-f-]{36}"));

        ResponseEntity<String> status = patch(
                "/api/farmer/listings/" + listingId + "/status?status=SOLD", farmer1Token(), null);
        assertEquals(200, status.getStatusCode().value());
        assertTrue(status.getBody().contains("\"status\":\"sold\""));

        ResponseEntity<String> mine = get("/api/farmer/listings", farmer1Token());
        assertEquals(200, mine.getStatusCode().value());
        assertTrue(mine.getBody().contains(title));
    }

    @Test
    void createListingAndValidateOwnershipRules() {
        String title = "IT Chillies " + System.nanoTime();
        ResponseEntity<String> created = post("/api/farmer/listings", farmer1Token(), listingBody(title));
        assertEquals(200, created.getStatusCode().value());
        String listingId = created.getBody().replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1");

        // Farmer2 cannot update Farmer1's listing
        ResponseEntity<String> forbidden = put("/api/farmer/listings/" + listingId,
                farmer2Token(), listingBody("Hijack"));
        assertEquals(403, forbidden.getStatusCode().value());
    }

    @Test
    void calendarEventLifecycle() {
        ResponseEntity<String> created = post("/api/farmer/calendar", farmer1Token(),
                Map.of("title", "Spray day", "eventType", "task",
                        "eventDate", "2026-09-10", "cropType", "tomato"));
        assertEquals(200, created.getStatusCode().value());
        String eventId = created.getBody().replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1");

        ResponseEntity<String> toggled = patch("/api/farmer/calendar/" + eventId + "/toggle",
                farmer1Token(), null);
        assertEquals(200, toggled.getStatusCode().value());
        assertTrue(toggled.getBody().contains("\"completed\":true"));

        ResponseEntity<String> upcoming = get("/api/farmer/calendar/upcoming", farmer1Token());
        assertEquals(200, upcoming.getStatusCode().value());
    }

    @Test
    void dashboard_returnsExpectedAggregates() {
        ResponseEntity<String> response = get("/api/farmer/dashboard?lat=16.7&lng=74.24", farmer1Token());
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("activeListings"));
        assertTrue(response.getBody().contains("pendingCustomerOrders"));
        assertTrue(response.getBody().contains("upcomingCalendar"));
        assertTrue(response.getBody().contains("unreadNotifications"));
    }

    @Test
    void browseMarketplace_returnsPageOfListings() {
        ResponseEntity<String> response = get("/api/farmer/marketplace?page=0&size=5", farmer1Token());
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"content\""));
    }
}

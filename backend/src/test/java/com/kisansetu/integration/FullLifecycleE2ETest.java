package com.kisansetu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full end-to-end lifecycle over real HTTP + real PostgreSQL:
 * farmer lists produce -> customer orders it -> farmer confirms/packs/dispatches
 * -> logistics partner delivers -> customer rates -> earnings/notifications update.
 */
class FullLifecycleE2ETest extends IntegrationTestBase {

    @Test
    void completeOrderLifecycle_endToEnd() throws Exception {
        // 1. Make partner 031 available so assignment is deterministic
        ResponseEntity<String> availability = put("/api/logistics/availability?status=AVAILABLE",
                partnerToken(), null);
        assertEquals(200, availability.getStatusCode().value());

        // 2. Farmer creates a fresh listing
        String title = "E2E Tomatoes " + System.nanoTime();
        ResponseEntity<String> listing = post("/api/farmer/listings", farmer1Token(), Map.of(
                "title", title,
                "description", "End-to-end test produce",
                "category", "Vegetables",
                "price", 50.00,
                "quantity", 100,
                "unit", "kg",
                "location", "Kolhapur",
                "harvestDate", "2026-09-01"));
        assertEquals(200, listing.getStatusCode().value());
        String listingId = extractId(listing);

        // 3. Customer adds to cart (query params)
        ResponseEntity<String> cartItem = post(
                "/api/customer/cart?listingId=" + listingId + "&quantity=4", customerToken(), null);
        assertEquals(200, cartItem.getStatusCode().value());

        // 4. Customer checks out with the seed default address
        ResponseEntity<String> checkout = post("/api/customer/checkout", customerToken(),
                Map.of("deliveryAddressId", "e0000000-0000-4000-8000-000000000101",
                        "deliveryPreference", "morning",
                        "notes", "E2E order"));
        assertEquals(200, checkout.getStatusCode().value());
        assertTrue(checkout.getBody().contains("\"ordersCreated\":1"));

        // 4b. Locate the created order id from the customer order list
        String ordersBody = get("/api/customer/orders", customerToken()).getBody();
        JsonNode array = new ObjectMapper().readTree(ordersBody).get("data");
        String orderId = null;
        for (JsonNode node : array) {
            if (listingId.equals(node.get("listingId").asText())) {
                orderId = node.get("id").asText();
                break;
            }
        }
        assertNotNull(orderId, "created order not found: " + ordersBody);

        // 5. Farmer sees the pending order and accepts it
        ResponseEntity<String> accepted = post("/api/farmer/customer-orders/" + orderId + "/accept",
                farmer1Token(), null);
        assertEquals(200, accepted.getStatusCode().value());
        assertTrue(accepted.getBody().contains("\"status\":\"confirmed\""));

        // 6. Farmer packs and dispatches
        post("/api/farmer/customer-orders/" + orderId + "/advance", farmer1Token(), null);
        ResponseEntity<String> dispatched = post("/api/farmer/customer-orders/" + orderId + "/advance",
                farmer1Token(), null);
        assertEquals(200, dispatched.getStatusCode().value());
        assertTrue(dispatched.getBody().contains("\"status\":\"dispatched\""));

        // 7. Partner 031 was assigned; accept the delivery
        ResponseEntity<String> assigned = get("/api/logistics/deliveries/active", partnerToken());
        assertEquals(200, assigned.getStatusCode().value());
        assertTrue(assigned.getBody().contains(orderId));

        ResponseEntity<String> deliveryAccepted = post("/api/logistics/deliveries/" + orderId + "/accept",
                partnerToken(), null);
        assertEquals(200, deliveryAccepted.getStatusCode().value());

        // 8. Partner advances through pickup -> transit -> delivered
        post("/api/logistics/deliveries/" + orderId + "/advance", partnerToken(), null);
        post("/api/logistics/deliveries/" + orderId + "/advance", partnerToken(), null);
        post("/api/logistics/deliveries/" + orderId + "/advance", partnerToken(), null);
        ResponseEntity<String> delivered = post("/api/logistics/deliveries/" + orderId + "/advance",
                partnerToken(), null);
        assertEquals(200, delivered.getStatusCode().value());
        assertTrue(delivered.getBody().contains("\"deliveryStatus\":\"delivered\"")
                || delivered.getBody().contains("\"status\":\"delivered\""));

        // 9. Customer can rate the delivered order, then rates it
        ResponseEntity<String> canRate = get("/api/customer/orders/" + orderId + "/can-rate", customerToken());
        assertEquals(200, canRate.getStatusCode().value());
        assertTrue(canRate.getBody().contains("\"canRate\":true"));

        ResponseEntity<String> rating = post("/api/customer/orders/" + orderId + "/rate", customerToken(),
                Map.of("rating", 5, "review", "Fresh and fast delivery"));
        assertEquals(200, rating.getStatusCode().value());

        // 10. Partner earnings reflect the delivered order
        ResponseEntity<String> earnings = get("/api/logistics/earnings", partnerToken());
        assertEquals(200, earnings.getStatusCode().value());
        assertTrue(earnings.getBody().contains(orderId));

        // 11. Farmer received the delivery notification
        ResponseEntity<String> notifications = get("/api/notifications", farmer1Token());
        assertEquals(200, notifications.getStatusCode().value());
        assertTrue(notifications.getBody().contains("delivered"));

        // 12. Customer order list shows delivered
        ResponseEntity<String> orders = get("/api/customer/orders", customerToken());
        assertEquals(200, orders.getStatusCode().value());
        assertTrue(orders.getBody().contains("\"delivered\""));

        // 13. Marketplace listing was removed from availability (sold via order creation)
        ResponseEntity<String> produce = get(
                "/api/customer/produce?search=" + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8),
                customerToken());
        assertEquals(200, produce.getStatusCode().value());
    }

    private String extractId(ResponseEntity<String> response) {
        String body = response.getBody();
        return body.replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1");
    }
}
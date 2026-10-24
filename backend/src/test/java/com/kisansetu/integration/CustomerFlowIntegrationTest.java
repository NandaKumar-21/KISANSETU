package com.kisansetu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomerFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void cartLifecycle_addUpdateRemove() {
        ResponseEntity<String> added = post(
                "/api/customer/cart?listingId=c0000000-0000-4000-8000-000000000101&quantity=3",
                customerToken(), null);
        assertEquals(200, added.getStatusCode().value());
        String itemId = added.getBody().replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1");

        ResponseEntity<String> cart = get("/api/customer/cart", customerToken());
        assertEquals(200, cart.getStatusCode().value());
        assertTrue(cart.getBody().contains("c0000000-0000-4000-8000-000000000101"));

        ResponseEntity<String> updated = put("/api/customer/cart/" + itemId + "?quantity=2",
                customerToken(), null);
        assertEquals(200, updated.getStatusCode().value());

        ResponseEntity<String> deleted = delete("/api/customer/cart/" + itemId, customerToken());
        assertEquals(200, deleted.getStatusCode().value());

        ResponseEntity<String> count = get("/api/customer/cart/count", customerToken());
        assertEquals(200, count.getStatusCode().value());
        assertTrue(count.getBody().contains("\"count\""));
    }

    @Test
    void wishlistLifecycle() {
        ResponseEntity<String> added = post("/api/customer/wishlist/listing/c0000000-0000-4000-8000-000000000102",
                customerToken(), null);
        assertEquals(200, added.getStatusCode().value());
        String itemId = added.getBody().replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1");

        ResponseEntity<String> wishlist = get("/api/customer/wishlist", customerToken());
        assertEquals(200, wishlist.getStatusCode().value());
        assertTrue(wishlist.getBody().contains("c0000000-0000-4000-8000-000000000102"));

        assertEquals(200, delete("/api/customer/wishlist/" + itemId, customerToken()).getStatusCode().value());
    }

    @Test
    void browseProduceAndFarmers() {
        ResponseEntity<String> produce = get("/api/customer/produce?page=0&size=5", customerToken());
        assertEquals(200, produce.getStatusCode().value());
        assertTrue(produce.getBody().contains("\"content\""));

        ResponseEntity<String> farmers = get("/api/customer/farmers", customerToken());
        assertEquals(200, farmers.getStatusCode().value());
        assertTrue(farmers.getBody().contains("Ramesh Patil"));

        ResponseEntity<String> categories = get("/api/customer/categories", customerToken());
        assertEquals(200, categories.getStatusCode().value());
    }

    @Test
    void addressLifecycle() {
        ResponseEntity<String> created = post("/api/customer/addresses", customerToken(),
                Map.of("addressLine1", "Flat 9, Green Heights",
                        "city", "Hyderabad", "state", "Telangana", "pincode", "500034",
                        "phone", "9876500000"));
        assertEquals(200, created.getStatusCode().value());
        String addressId = created.getBody().replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1");

        assertEquals(200, delete("/api/customer/addresses/" + addressId, customerToken()).getStatusCode().value());
    }

    @Test
    void dashboard_returnsAllAggregates() {
        ResponseEntity<String> response = get("/api/customer/dashboard", customerToken());
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("cartCount"));
        assertTrue(response.getBody().contains("wishlistCount"));
        assertTrue(response.getBody().contains("activeOrders"));
        assertTrue(response.getBody().contains("deliveredOrders"));
    }

    @Test
    void cancelOrder_transitionsPendingToCancelled() throws Exception {
        // No available partner -> order stays PENDING and is cancellable
        assertEquals(200, put("/api/logistics/availability?status=BUSY", partnerToken(), null)
                .getStatusCode().value());
        assertEquals(200, post("/api/customer/cart?listingId=c0000000-0000-4000-8000-000000000102&quantity=1",
                customerToken(), null).getStatusCode().value());
        assertEquals(200, post("/api/customer/checkout", customerToken(),
                Map.of("deliveryAddressId", "e0000000-0000-4000-8000-000000000101"))
                .getStatusCode().value());

        String ordersBody = get("/api/customer/orders", customerToken()).getBody();
        JsonNode array = new ObjectMapper().readTree(ordersBody).get("data");
        String orderId = null;
        for (JsonNode node : array) {
            if ("pending".equals(node.get("status").asText())) {
                orderId = node.get("id").asText();
                break;
            }
        }
        assertNotNull(orderId, "expected a pending order to cancel");

        ResponseEntity<String> cancelled = post("/api/customer/orders/" + orderId + "/cancel",
                customerToken(), null);
        assertEquals(200, cancelled.getStatusCode().value());
        assertTrue(cancelled.getBody().contains("\"status\":\"cancelled\""));
    }
}
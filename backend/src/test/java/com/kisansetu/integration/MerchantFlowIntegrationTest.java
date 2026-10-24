package com.kisansetu.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MerchantFlowIntegrationTest extends IntegrationTestBase {

    private Map<String, Object> productBody(String name, int quantity) {
        return Map.of(
                "name", name,
                "description", "IT product",
                "category", "Fertilizers",
                "price", 450.00,
                "quantity", quantity,
                "unit", "kg",
                "stockThreshold", 10);
    }

    @Test
    void productCrud_worksForMerchant() {
        String name = "IT Urea " + System.nanoTime();
        ResponseEntity<String> created = post("/api/merchant/products", merchantToken(), productBody(name, 25));
        assertEquals(200, created.getStatusCode().value());
        assertTrue(created.getBody().contains(name));

        String productId = created.getBody().replaceAll(".*\"id\":\"([0-9a-f-]{36})\".*", "$1");

        ResponseEntity<String> updated = put("/api/merchant/products/" + productId,
                merchantToken(), productBody(name, 30));
        assertEquals(200, updated.getStatusCode().value());
        assertTrue(updated.getBody().contains("\"quantity\":30"));

        ResponseEntity<String> deleted = delete("/api/merchant/products/" + productId, merchantToken());
        assertEquals(200, deleted.getStatusCode().value());
    }

    @Test
    void productListAndDashboard_accessible() {
        ResponseEntity<String> products = get("/api/merchant/products", merchantToken());
        assertEquals(200, products.getStatusCode().value());

        ResponseEntity<String> dashboard = get("/api/merchant/dashboard", merchantToken());
        assertEquals(200, dashboard.getStatusCode().value());
        assertTrue(dashboard.getBody().contains("orderCounts"));
    }

    @Test
    void farmerCanCreateOrderAgainstMerchantProduct() {
        ResponseEntity<String> products = get("/api/merchant/marketplace", farmer1Token());
        assertEquals(200, products.getStatusCode().value());

        ResponseEntity<String> created = post("/api/merchant/orders", farmer1Token(),
                Map.of("productId", "b0000000-0000-4000-8000-000000000101",
                        "quantity", 2, "notes", "IT order"));
        assertEquals(200, created.getStatusCode().value());
        assertTrue(created.getBody().contains("\"status\":\"pending\""));
    }

    @Test
    void merchantSeesAndProcessesOrders() {
        ResponseEntity<String> orders = get("/api/merchant/orders", merchantToken());
        assertEquals(200, orders.getStatusCode().value());

        ResponseEntity<String> counts = get("/api/merchant/orders/counts", merchantToken());
        assertEquals(200, counts.getStatusCode().value());
        assertTrue(counts.getBody().contains("\"pending\""));
    }
}

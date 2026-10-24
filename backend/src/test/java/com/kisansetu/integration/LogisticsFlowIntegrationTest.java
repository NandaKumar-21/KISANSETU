package com.kisansetu.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class LogisticsFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void dashboard_returnsExpectedAggregates() {
        ResponseEntity<String> response = get("/api/logistics/dashboard", partnerToken());
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("assignedDeliveries"));
        assertTrue(response.getBody().contains("activeDeliveries"));
        assertTrue(response.getBody().contains("completedToday"));
        assertTrue(response.getBody().contains("earningsToday"));
        assertTrue(response.getBody().contains("completionRate"));
    }

    @Test
    void ordersAndDeliveriesListings() {
        assertEquals(200, get("/api/logistics/orders", partnerToken()).getStatusCode().value());
        assertEquals(200, get("/api/logistics/deliveries/active", partnerToken()).getStatusCode().value());
        assertEquals(200, get("/api/logistics/deliveries/completed", partnerToken()).getStatusCode().value());
    }

    @Test
    void availability_roundTrips() {
        ResponseEntity<String> updated = put("/api/logistics/availability?status=BUSY",
                partnerToken(), null);
        assertEquals(200, updated.getStatusCode().value());
        assertTrue(updated.getBody().contains("\"status\":\"busy\""));

        ResponseEntity<String> current = get("/api/logistics/availability", partnerToken());
        assertTrue(current.getBody().contains("\"status\":\"busy\""));

        assertEquals(200, put("/api/logistics/availability?status=AVAILABLE",
                partnerToken(), null).getStatusCode().value());
    }

    @Test
    void earnings_andRoutes_endpoints() {
        ResponseEntity<String> earnings = get("/api/logistics/earnings", partnerToken());
        assertEquals(200, earnings.getStatusCode().value());
        assertTrue(earnings.getBody().contains("\"today\""));
        assertTrue(earnings.getBody().contains("earningPerDelivery"));

        ResponseEntity<String> routes = get("/api/logistics/routes", partnerToken());
        assertEquals(200, routes.getStatusCode().value());
        assertTrue(routes.getBody().contains("stops"));
    }

    @Test
    void advanceUnassignedOrder_rejected() {
        // f...104 is PENDING (no partner assigned) - partner cannot advance it
        ResponseEntity<String> response = post(
                "/api/logistics/deliveries/f0000000-0000-4000-8000-000000000104/advance",
                partnerToken(), null);
        assertEquals(403, response.getStatusCode().value());
    }
}
package com.kisansetu.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void health_isPublic() {
        ResponseEntity<String> response = get("/api/health", null);
        assertNotEquals(401, response.getStatusCode().value());
    }

    @Test
    void protectedEndpoint_returns401WithoutToken() {
        ResponseEntity<String> response = get("/api/auth/me", null);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void invalidToken_returns401() {
        ResponseEntity<String> response = get("/api/auth/me", "not-a-real-jwt");
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void validToken_returnsProfileWithRoles() {
        ResponseEntity<String> response = get("/api/auth/me", farmer1Token());
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("a0000000-0000-4000-8000-000000000001"));
        assertTrue(response.getBody().contains("FARMER"));
    }

    @Test
    void farmerToken_cannotAccessLogisticsEndpoints() {
        ResponseEntity<String> response = get("/api/logistics/dashboard", farmer1Token());
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void merchantToken_cannotAccessCustomerEndpoints() {
        ResponseEntity<String> response = get("/api/customer/cart", merchantToken());
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void customerToken_cannotAccessFarmerEndpoints() {
        ResponseEntity<String> response = get("/api/farmer/listings", customerToken());
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void profileEndpoints_workForEveryRole() {
        assertEquals(200, get("/api/profiles/me", farmer1Token()).getStatusCode().value());
        assertEquals(200, get("/api/profiles/me", merchantToken()).getStatusCode().value());
        assertEquals(200, get("/api/profiles/me", customerToken()).getStatusCode().value());
        assertEquals(200, get("/api/profiles/me", partnerToken()).getStatusCode().value());
        assertEquals(200, get("/api/profiles/roles", customerToken()).getStatusCode().value());
    }
}
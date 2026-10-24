package com.kisansetu.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies graceful AI fallback behavior when no AI_API_KEY is configured
 * (test profile keeps api-key empty -> OfflineAiProvider is active).
 */
class AiDiseaseIntegrationTest extends IntegrationTestBase {

    @Test
    void chat_returnsOfflineFallbackInsteadOfErroring() {
        ResponseEntity<String> response = post("/api/ai/chat", farmer1Token(),
                Map.of("message", "What should I plant in July?"));
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("offline mode"));
    }

    @Test
    void advisory_returnsStructuredFallback() {
        ResponseEntity<String> response = post("/api/ai/advisory", farmer1Token(),
                Map.of("message", "Leaves are yellowing"));
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("diseaseName")
                || response.getBody().contains("General Farm Advisory"));
    }

    @Test
    void cropPlanner_stillWorksWithoutAi() {
        ResponseEntity<String> response = post("/api/ai/crop-planner", farmer1Token(),
                Map.of("soilType", "loamy", "region", "Kolhapur", "season", "kharif",
                        "waterAvailability", "medium", "budget", 50000));
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void diseaseDetect_gracefullyRejectsNonPlantImageWithoutAi() {
        ResponseEntity<String> response = post("/api/disease/detect", farmer1Token(),
                Map.of("method", "image", "image", "aGVsbG8="));
        // No AI configured -> stage-1 validation cannot confirm a plant -> 400, not 500
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().contains("plant"));
    }

    @Test
    void diseaseHistory_accessible() {
        assertEquals(200, get("/api/disease/history", farmer1Token()).getStatusCode().value());
    }
}
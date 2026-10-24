package com.kisansetu.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.ai.provider.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * General agricultural advisory (voice-assistant style text queries).
 * Returns the same structured disease-style result used across the app.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisoryService {

    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record AdvisoryResult(String diseaseName, String confidence, String severity,
                                 String description, List<String> symptoms,
                                 List<AdvisoryTreatment> treatments,
                                 List<AdvisoryStep> applicationGuide,
                                 List<String> preventionTips) {
    }

    public record AdvisoryTreatment(String name, String dosagePerAcre, String description) {
    }

    public record AdvisoryStep(String step, String timing) {
    }

    public AdvisoryResult generate(String query) {
        String prompt = """
                You are an expert agricultural advisor helping Indian farmers. A farmer has asked: "%s"

                Provide helpful, practical advice tailored for Indian farming conditions. If the query seems
                to be about crop disease or symptoms, analyze it as a symptom-based disease detection.

                Return ONLY a JSON object in the following format:
                {
                  "disease_name": "Most likely disease name (if applicable)",
                  "confidence": "medium",
                  "severity": "low" | "medium" | "high",
                  "description": "Brief description of the probable disease or issue",
                  "symptoms": ["symptom1", "symptom2"],
                  "treatments": [
                    { "name": "Treatment product name", "dosagePerAcre": "2.5 kg per acre", "description": "How to apply" }
                  ],
                  "applicationGuide": [
                    { "step": "Step description", "timing": "When to do it" }
                  ],
                  "preventionTips": ["tip1", "tip2"]
                }

                The response must be valid JSON with no extra commentary.
                Be practical and specific for Indian farmers.
                """.formatted(query);

        try {
            String text = aiProvider.chat(List.of(ChatMessage.user(prompt)));
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalStateException("No JSON in AI response");
            }
            JsonNode parsed = objectMapper.readTree(text.substring(start, end + 1));
            List<AdvisoryTreatment> treatments = new ArrayList<>();
            parsed.path("treatments").forEach(t -> treatments.add(new AdvisoryTreatment(
                    t.path("name").asText(""),
                    t.path("dosagePerAcre").asText(""),
                    t.path("description").asText(""))));
            List<AdvisoryStep> steps = new ArrayList<>();
            parsed.path("applicationGuide").forEach(s -> steps.add(new AdvisoryStep(
                    s.path("step").asText(""), s.path("timing").asText(""))));
            List<String> symptoms = new ArrayList<>();
            parsed.path("symptoms").forEach(s -> symptoms.add(s.asText()));
            List<String> prevention = new ArrayList<>();
            parsed.path("preventionTips").forEach(p -> prevention.add(p.asText()));
            return new AdvisoryResult(
                    parsed.path("disease_name").asText("Crop Issue"),
                    parsed.path("confidence").asText("medium"),
                    parsed.path("severity").asText("medium"),
                    parsed.path("description").asText("No description available."),
                    symptoms, treatments, steps, prevention);
        } catch (Exception e) {
            log.warn("Advisory AI failed: {}", e.getMessage());
            return fallback(query);
        }
    }

    private AdvisoryResult fallback(String query) {
        return new AdvisoryResult(
                "General Farm Advisory",
                "medium",
                "low",
                "Our AI advisory service is temporarily unavailable. Based on your query \"" + query
                        + "\", we recommend: inspect your crop regularly in the early morning, maintain "
                        + "proper irrigation and drainage, use certified seeds, and consult your local "
                        + "agriculture officer (Kisan Call Centre 1800-180-1551) for site-specific advice.",
                List.of("Regular field scouting twice a week"),
                List.of(new AdvisoryTreatment("Neem-based organic spray", "2 ml per litre",
                        "Apply as a preventive measure for common pests and fungal issues")),
                List.of(new AdvisoryStep("Monitor soil moisture", "Daily"),
                        new AdvisoryStep("Apply balanced fertilization", "After soil testing")),
                List.of("Rotate crops every season", "Remove and destroy infected plant debris",
                        "Maintain field hygiene and clean equipment"));
    }
}
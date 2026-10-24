package com.kisansetu.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.ai.provider.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Smart crop planner. Combines a deterministic rule-based baseline
 * (Indian crop database with suitability scoring) with an AI enrichment
 * step; falls back gracefully to the rule-based result when AI fails.
 */
@Slf4j
@Service
public class CropRecommendationService {

    private static final Map<String, Integer> WATER_RANK = Map.of(
            "Low", 1, "Medium", 2, "High", 3);

    private record CropEntry(String crop, List<String> soilTypes, List<String> seasons,
                             String waterNeed, int baseYieldPerAcreKg, int inputCostPerAcre,
                             int[] priceRangePerKg, List<String> fertilizers) {
    }

    private static final List<CropEntry> CROP_DATABASE = List.of(
            new CropEntry("Tomato", List.of("loamy", "red", "black"), List.of("Kharif", "Rabi"),
                    "Medium", 2500, 28000, new int[]{12, 18}, List.of("NPK 19:19:19", "Organic compost")),
            new CropEntry("Wheat", List.of("loamy", "clay", "black"), List.of("Rabi"),
                    "Medium", 2200, 24000, new int[]{18, 24}, List.of("DAP", "Urea", "Zinc sulphate")),
            new CropEntry("Paddy (Rice)", List.of("clay", "loamy", "black"), List.of("Kharif"),
                    "High", 2600, 32000, new int[]{18, 22}, List.of("NPK 10:26:26", "Organic manure")),
            new CropEntry("Cotton", List.of("black"), List.of("Kharif"),
                    "Medium", 800, 30000, new int[]{65, 85}, List.of("NPK 20:20:0", "Potash")),
            new CropEntry("Groundnut", List.of("sandy", "red", "loamy"), List.of("Kharif"),
                    "Low", 900, 22000, new int[]{60, 75}, List.of("Gypsum", "Single super phosphate")),
            new CropEntry("Onion", List.of("red", "loamy"), List.of("Rabi", "Summer"),
                    "Medium", 1500, 26000, new int[]{10, 18}, List.of("NPK 12:32:16", "Farmyard manure")),
            new CropEntry("Chillies", List.of("black", "red", "loamy"), List.of("Kharif", "Rabi"),
                    "Medium", 800, 25000, new int[]{80, 120}, List.of("NPK 19:19:19", "Micronutrient mix")),
            new CropEntry("Maize", List.of("loamy", "sandy", "red"), List.of("Kharif"),
                    "Medium", 1800, 21000, new int[]{16, 22}, List.of("Urea", "DAP", "Zinc sulphate")),
            new CropEntry("Banana", List.of("loamy", "black"), List.of("Kharif", "Rabi", "Summer"),
                    "High", 3000, 35000, new int[]{22, 30}, List.of("Potash (MOP)", "Organic manure"))
    );

    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CropRecommendationService(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public record Input(String soilType, String region, String season, String waterAvailability,
                        double budget, double farmSize, String previousCrop, String preferredCrop) {
    }

    public record Recommendation(String crop, String expectedProfit, double expectedProfitValue,
                                 String expectedPriceRange, List<String> fertilizers,
                                 String waterNeed, String whyRecommended) {
    }

    public record Result(List<Recommendation> recommendations) {
    }

    /**
     * Generate crop recommendations (AI with rule-based fallback).
     */
    public Result recommend(Input input) {
        List<Recommendation> baseline = computeRuleBasedBaseline(input);
        try {
            String prompt = buildPrompt(input, baseline);
            String text = aiProvider.chat(List.of(ChatMessage.user(prompt)));
            JsonNode parsed = parseJson(text);
            List<Recommendation> aiResult = new ArrayList<>();
            parsed.path("recommendations").forEach(r -> aiResult.add(new Recommendation(
                    r.path("crop").asText(""),
                    r.path("expectedProfit").asText(""),
                    r.path("expectedProfitValue").asDouble(0),
                    r.path("expectedPriceRange").asText(""),
                    toStringList(r.path("fertilizers")),
                    r.path("waterNeed").asText("Medium"),
                    r.path("whyRecommended").asText("Recommended based on your soil, season, water and budget."))));
            if (!aiResult.isEmpty()) {
                return new Result(aiResult);
            }
        } catch (Exception e) {
            log.warn("AI crop recommendation failed, using rule-based fallback: {}", e.getMessage());
        }
        return new Result(baseline);
    }

    private List<Recommendation> computeRuleBasedBaseline(Input input) {
        return CROP_DATABASE.stream().map(entry -> {
                    boolean matchesSoil = entry.soilTypes().contains(input.soilType());
                    boolean matchesSeason = entry.seasons().contains(input.season());
                    boolean waterOk = WATER_RANK.getOrDefault(input.waterAvailability(), 2)
                            >= WATER_RANK.getOrDefault(entry.waterNeed(), 2);
                    double avgPrice = (entry.priceRangePerKg()[0] + entry.priceRangePerKg()[1]) / 2.0;
                    double grossPerAcre = entry.baseYieldPerAcreKg() * avgPrice;
                    double profitPerAcre = grossPerAcre - entry.inputCostPerAcre();
                    double totalCost = entry.inputCostPerAcre() * input.farmSize();
                    boolean withinBudget = totalCost <= input.budget();

                    int score = (matchesSoil ? 3 : 0) + (matchesSeason ? 3 : 0)
                            + (waterOk ? 2 : 0) + (withinBudget ? 2 : 0);

                    return new Scored(entry, matchesSoil, matchesSeason, waterOk, withinBudget,
                            profitPerAcre, score, avgPrice);
                })
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing(Comparator.comparingDouble(Scored::profitPerAcre).reversed()))
                .limit(3)
                .map(scored -> {
                    CropEntry entry = scored.entry();
                    double profit = Math.max(0, scored.profitPerAcre());
                    return new Recommendation(
                            entry.crop(),
                            "₹" + formatRupees((long) profit) + " / acre",
                            profit,
                            "₹" + entry.priceRangePerKg()[0] + "-" + entry.priceRangePerKg()[1] + "/kg",
                            entry.fertilizers(),
                            entry.waterNeed(),
                            "Based on your " + input.soilType() + " soil, " + entry.crop()
                                    + " fits the " + input.season() + " season, works with "
                                    + input.waterAvailability().toLowerCase() + " water availability, "
                                    + "and matches your budget for " + formatRupees((long) input.farmSize()) + " acre(s).");
                })
                .toList();
    }

    private record Scored(CropEntry entry, boolean matchesSoil, boolean matchesSeason, boolean waterOk,
                          boolean withinBudget, double profitPerAcre, int score, double avgPrice) {
    }

    private String buildPrompt(Input input, List<Recommendation> baseline) {
        StringBuilder baselineText = new StringBuilder();
        for (int i = 0; i < baseline.size(); i++) {
            Recommendation rec = baseline.get(i);
            baselineText.append(i + 1).append(". ").append(rec.crop())
                    .append(" – Profit ~").append(rec.expectedProfit())
                    .append(", Water: ").append(rec.waterNeed())
                    .append(", Price: ").append(rec.expectedPriceRange())
                    .append(", Fertilizers: ").append(String.join(", ", rec.fertilizers()))
                    .append("\n");
        }

        return """
                You are an AI agricultural planning assistant helping Indian farmers plan crops for the next season.

                Farmer context:
                - Soil type: %s
                - Region: %s
                - Season: %s
                - Water availability: %s
                - Budget: ₹%s total
                - Farm size: %s acres
                - Previous crop: %s
                - Preferred crop: %s

                You also have rule-based baseline suggestions and mandi-style price estimates:
                %s

                Using agronomy knowledge plus market understanding, suggest the TOP 3 most profitable yet realistic crops to grow next season.

                Very important:
                - Make suggestions practical for small and medium Indian farmers.
                - Respect water constraints and budget.
                - Prefer crops that match the soil and season; only deviate if no suitable option exists.
                - Consider risk diversification.
                - These are advisory estimates, not guaranteed outcomes.

                Return ONLY a JSON object in the following format:
                {
                  "recommendations": [
                    {
                      "crop": "Tomato",
                      "expectedProfit": "₹45,000 / acre",
                      "expectedProfitValue": 45000,
                      "expectedPriceRange": "₹14-18/kg",
                      "fertilizers": ["NPK 19:19:19", "Organic compost"],
                      "waterNeed": "Medium",
                      "whyRecommended": "Short-season cash crop suitable for loamy soil with medium water and strong urban demand."
                    }
                  ]
                }

                Rules:
                - expectedProfitValue must be a NUMBER representing profit per acre in rupees.
                - expectedProfit must be a human-readable string using Indian format.
                - fertilizers must be a short array of 2-4 input recommendations.
                - whyRecommended must be a 1-2 sentence explanation tailored to the given inputs.
                - Do NOT include any extra commentary outside JSON.
                """.formatted(
                input.soilType(),
                input.region() == null || input.region().isBlank() ? "Not specified" : input.region(),
                input.season(),
                input.waterAvailability(),
                formatRupees((long) input.budget()),
                input.farmSize(),
                input.previousCrop() == null || input.previousCrop().isBlank() ? "Not specified" : input.previousCrop(),
                input.preferredCrop() == null || input.preferredCrop().isBlank() ? "No preference" : input.preferredCrop(),
                baselineText);
    }

    private JsonNode parseJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("No JSON object found in AI response");
        }
        try {
            return objectMapper.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON in AI response");
        }
    }

    private List<String> toStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    private String formatRupees(long value) {
        return String.format("%,d", value);
    }
}
package com.kisansetu.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.ai.dto.DiseaseAnalysisResult;
import com.kisansetu.ai.provider.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AiVisionService {

    private static final int CONFIDENCE_THRESHOLD = 65;

    private static final List<String> BANNED_WORDS = List.of(
            "human",
            "skin",
            "face",
            "person",
            "animal"
    );

    /**
     * Single-stage vision analysis.
     *
     * The AI performs:
     *
     * 1. Plant/crop validation
     * 2. Disease detection
     *
     * in ONE vision request.
     *
     * This avoids two expensive vision requests for every image.
     */
    private static final String DISEASE_DETECTION_PROMPT = """
            You are an agricultural plant disease expert AI.

            Analyze the uploaded image.

            FIRST determine whether the image clearly contains a plant leaf
            or crop suitable for agricultural disease analysis.

            If the image does NOT contain a plant leaf or crop, return ONLY:

            {
              "isPlant": false
            }

            If the image DOES contain a plant leaf or crop, analyze it for
            possible disease and return ONLY valid JSON in exactly this
            structure:

            {
              "isPlant": true,
              "disease": "Disease name or Unknown",
              "confidence": 0,
              "disease_name": "Disease name or Unknown",
              "severity": "low",
              "description": "Brief description of the disease",
              "symptoms": [
                "symptom1",
                "symptom2"
              ],
              "recommendedChemicals": [
                {
                  "name": "Chemical name",
                  "dosagePerAcre": "e.g., 500 ml per acre"
                }
              ],
              "applicationGuide": [
                {
                  "step": "Step description",
                  "timing": "When to do it"
                }
              ],
              "preventionTips": [
                "prevention tip 1",
                "prevention tip 2"
              ]
            }

            STRICT RULES:

            - Return valid JSON only.
            - Do not return Markdown.
            - Do not return ```json.
            - Do not return explanations outside the JSON object.
            - If the image is not a plant leaf or crop, return only:
              { "isPlant": false }
            - Do NOT guess.
            - If you are unsure about the disease, use "Unknown".
            - confidence must be a number from 0 to 100.
            - severity must be exactly one of:
              "low", "medium", "high".
            - Tailor recommendations for Indian farmers.
            - Do NOT generate price, cost, savings or financial values.
            - Do not identify humans, animals or human skin as plant disease.
            - Only recommend treatments when reasonably appropriate.
            - Keep descriptions and recommendations concise.
            """;

    /**
     * Symptom-based analysis remains a normal text request.
     */
    private static final String SYMPTOM_DETECTION_PROMPT_TEMPLATE = """
            You are an expert agricultural plant pathologist.

            Based on the following symptoms observed by a farmer,
            identify the most likely plant disease and provide practical
            agricultural guidance.

            Observed symptoms:
            %s

            Return ONLY a JSON object in this format:

            {
              "disease_name": "Most likely disease name",
              "confidence": 70,
              "severity": "medium",
              "description": "Brief description of the probable disease",
              "symptoms": [
                "symptom1",
                "symptom2"
              ],
              "recommendedChemicals": [
                {
                  "name": "Chemical name",
                  "dosagePerAcre": "e.g., 2.5 kg per acre"
                }
              ],
              "applicationGuide": [
                {
                  "step": "Step description",
                  "timing": "When to do it"
                }
              ],
              "preventionTips": [
                "prevention tip 1",
                "prevention tip 2"
              ],
              "note": "This is preliminary guidance based on symptoms only."
            }

            STRICT RULES:

            - Return valid JSON only.
            - Do not return Markdown.
            - Do not return ```json.
            - Do not return explanations outside the JSON object.
            - Do NOT generate price, cost, savings or financial values.
            - If unsure, use "Unknown".
            - Tailor recommendations for Indian farmers.
            """;

    private final AiProvider aiProvider;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiVisionService(
            AiProvider aiProvider,
            PricingService pricingService
    ) {
        this.aiProvider = aiProvider;
        this.pricingService = pricingService;
    }

    /**
     * Analyze a plant/crop image.
     *
     * IMPORTANT:
     * Only ONE vision request is made.
     */
    public DiseaseAnalysisResult analyzeImage(String imageBase64) {

        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new com.kisansetu.common.exception.ApiException(
                    400,
                    "Please upload a plant leaf image."
            );
        }

        try {

            log.info(
                    "Starting single-stage plant disease vision analysis"
            );

            /*
             * ONE Qwen vision request.
             *
             * The model performs both:
             *
             * - isPlant validation
             * - disease detection
             */
            String text = aiProvider.chatWithImage(
                    DISEASE_DETECTION_PROMPT,
                    imageBase64
            );

            log.debug(
                    "Vision model response received"
            );

            JsonNode parsed = parseJson(text);

            /*
             * Validate that the model actually returned isPlant.
             */
            if (!parsed.has("isPlant")) {
                throw new IllegalStateException(
                        "Missing field 'isPlant' in model response"
                );
            }

            boolean isPlant =
                    parsed.path("isPlant").asBoolean(false);

            /*
             * Reject non-plant images.
             */
            if (!isPlant) {
                throw new com.kisansetu.common.exception.ApiException(
                        400,
                        "Please upload a plant leaf image only."
                );
            }

            /*
             * Apply safety checks.
             */
            applySafetyFilters(parsed);

            /*
             * Convert AI response to application DTO.
             */
            return buildResult(parsed);

        } catch (com.kisansetu.common.exception.ApiException e) {

            throw e;

        } catch (Exception e) {

            log.error(
                    "Plant disease analysis failed: {}",
                    e.getMessage(),
                    e
            );

            throw new com.kisansetu.common.exception.ApiException(
                    502,
                    "Failed to analyze the plant image. Please try again."
            );
        }
    }

    /**
     * Symptom-based preliminary analysis.
     *
     * This does not use vision.
     */
    public DiseaseAnalysisResult analyzeSymptoms(
            List<String> symptoms
    ) {

        if (symptoms == null || symptoms.isEmpty()) {
            throw new com.kisansetu.common.exception.ApiException(
                    400,
                    "Please provide at least one symptom."
            );
        }

        String prompt =
                SYMPTOM_DETECTION_PROMPT_TEMPLATE.formatted(
                        String.join(", ", symptoms)
                );

        try {

            String text =
                    aiProvider.chat(
                            List.of(
                                    ChatMessage.user(prompt)
                            )
                    );

            JsonNode parsed =
                    parseJson(text);

            return buildResult(parsed);

        } catch (com.kisansetu.common.exception.ApiException e) {

            throw e;

        } catch (Exception e) {

            log.error(
                    "Symptom analysis failed: {}",
                    e.getMessage(),
                    e
            );

            throw new com.kisansetu.common.exception.ApiException(
                    502,
                    "Failed to analyze symptoms. Please try again."
            );
        }
    }

    /**
     * Parse JSON returned by the model.
     *
     * Handles:
     *
     * 1. Pure JSON
     * 2. JSON surrounded by accidental text
     * 3. Markdown code fences
     */
    private JsonNode parseJson(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalStateException(
                    "Empty AI response"
            );
        }

        try {

            String cleaned = text.trim();

            /*
             * Remove ```json and ``` if the model
             * accidentally returns Markdown.
             */
            if (cleaned.startsWith("```")) {

                int firstNewline =
                        cleaned.indexOf('\n');

                if (firstNewline >= 0) {
                    cleaned =
                            cleaned.substring(
                                    firstNewline + 1
                            );
                }

                int closingFence =
                        cleaned.lastIndexOf("```");

                if (closingFence >= 0) {
                    cleaned =
                            cleaned.substring(
                                    0,
                                    closingFence
                            );
                }

                cleaned = cleaned.trim();
            }

            /*
             * Locate the JSON object.
             */
            int start =
                    cleaned.indexOf('{');

            int end =
                    cleaned.lastIndexOf('}');

            if (start < 0 || end <= start) {

                throw new IllegalStateException(
                        "No JSON object found in model response"
                );
            }

            String json =
                    cleaned.substring(
                            start,
                            end + 1
                    );

            JsonNode node =
                    objectMapper.readTree(json);

            if (node == null || !node.isObject()) {

                throw new IllegalStateException(
                        "AI response is not a JSON object"
                );
            }

            return node;

        } catch (Exception e) {

            log.error(
                    "Failed to parse AI response: {}",
                    e.getMessage()
            );

            throw new com.kisansetu.common.exception.ApiException(
                    502,
                    "Failed to parse AI response. Please try again."
            );
        }
    }

    /**
     * Safety checks for AI disease output.
     */
    private void applySafetyFilters(JsonNode parsed) {

        int confidence =
                parsed.path("confidence").asInt(0);

        /*
         * Only reject when the model actually supplied
         * a confidence value.
         */
        if (
                confidence > 0 &&
                        confidence < CONFIDENCE_THRESHOLD
        ) {

            throw new com.kisansetu.common.exception.ApiException(
                    400,
                    "Low confidence detection. Please upload a clearer plant leaf image."
            );
        }

        /*
         * Support both:
         *
         * disease_name
         * disease
         */
        String disease =
                parsed.path("disease_name")
                        .asText("");

        if (disease.isBlank()) {

            disease =
                    parsed.path("disease")
                            .asText("");
        }

        String lower =
                disease.toLowerCase();

        for (String word : BANNED_WORDS) {

            if (lower.contains(word)) {

                throw new com.kisansetu.common.exception.ApiException(
                        400,
                        "Please upload a plant leaf image only."
                );
            }
        }
    }

    /**
     * Convert AI JSON into the application's
     * DiseaseAnalysisResult DTO.
     */
    private DiseaseAnalysisResult buildResult(
            JsonNode parsed
    ) {

        /*
         * Prefer disease_name.
         * Fall back to disease.
         */
        String diseaseName =
                parsed.path("disease_name")
                        .asText(
                                parsed.path("disease")
                                        .asText(
                                                "Analysis Pending"
                                        )
                        );

        /*
         * Convert numeric confidence into
         * the application's high/medium/low representation.
         */
        String confidenceText =
                parsed.path("confidence")
                        .asText();

        if (confidenceText.matches("\\d+")) {

            int value =
                    Integer.parseInt(
                            confidenceText
                    );

            confidenceText =
                    value >= 75
                            ? "high"
                            : value >= 50
                              ? "medium"
                              : "low";
        }

        /*
         * Treatments / chemicals.
         */
        List<JsonNode> chemicals =
                new ArrayList<>();

        parsed.path(
                "recommendedChemicals"
        ).forEach(
                chemicals::add
        );

        /*
         * Backward compatibility with
         * an older "treatments" field.
         */
        if (chemicals.isEmpty()) {

            parsed.path(
                    "treatments"
            ).forEach(
                    chemicals::add
            );
        }

        List<DiseaseAnalysisResult.Treatment> treatments =
                chemicals.stream()
                        .map(c -> {

                            String name =
                                    c.path("name")
                                            .asText(
                                                    "Treatment"
                                            );

                            String dosage =
                                    c.path(
                                            "dosagePerAcre"
                                    ).asText(
                                            c.path(
                                                    "dosage"
                                            ).asText("")
                                    );

                            var cost =
                                    pricingService
                                            .calculateCost(
                                                    name,
                                                    dosage
                                            );

                            return new DiseaseAnalysisResult.Treatment(
                                    name,
                                    dosage,
                                    c.path(
                                            "description"
                                    ).asText(""),
                                    cost != null
                                            ? cost.unitPrice()
                                            : null,
                                    cost != null
                                            ? cost.totalCost()
                                            : null,
                                    cost != null
                                            ? cost.requiredQuantity()
                                            : dosage,
                                    cost != null
                            );
                        })
                        .toList();

        /*
         * Application instructions.
         */
        List<DiseaseAnalysisResult.ApplicationStep> steps =
                new ArrayList<>();

        parsed.path(
                "applicationGuide"
        ).forEach(
                s ->
                        steps.add(
                                new DiseaseAnalysisResult.ApplicationStep(
                                        s.path("step")
                                                .asText(""),
                                        s.path("timing")
                                                .asText("")
                                )
                        )
        );

        /*
         * Symptoms.
         */
        List<String> symptoms =
                new ArrayList<>();

        parsed.path(
                "symptoms"
        ).forEach(
                s ->
                        symptoms.add(
                                s.asText()
                        )
        );

        /*
         * Prevention tips.
         */
        List<String> prevention =
                new ArrayList<>();

        parsed.path(
                "preventionTips"
        ).forEach(
                p ->
                        prevention.add(
                                p.asText()
                        )
        );

        return new DiseaseAnalysisResult(
                diseaseName,
                confidenceText,
                parsed.path("severity")
                        .asText("medium"),
                parsed.path("description")
                        .asText(
                                "No description available."
                        ),
                symptoms,
                treatments,
                steps,
                prevention
        );
    }
}
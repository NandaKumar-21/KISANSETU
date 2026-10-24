package com.kisansetu.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.common.exception.ApiException;
import com.kisansetu.config.KisanSetuProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class GroqAiProvider implements AiProvider {

    private static final String DEFAULT_TEXT_MODEL =
            "openai/gpt-oss-120b";

    private static final String DEFAULT_VISION_MODEL =
            "qwen/qwen3.6-27b";

    private final KisanSetuProperties props;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    public GroqAiProvider(KisanSetuProperties props) {
        this.props = props;
    }

    /**
     * Normal text/chat request.
     */
    @Override
    public String chat(List<ChatMessage> messages) {
        return callText(messages);
    }

    /**
     * Vision request.
     *
     * Uses qwen/qwen3.6-27b.
     */
    @Override
    public String chatWithImage(
            String prompt,
            String imageBase64
    ) {

        if (imageBase64 == null ||
                imageBase64.isBlank()) {

            throw ApiException.badRequest(
                    "Image is required for AI vision analysis"
            );
        }

        String cleanBase64 = imageBase64;

        /*
         * Remove data URL prefix.
         *
         * Example:
         * data:image/jpeg;base64,AAAA...
         */
        if (cleanBase64.contains(",")) {

            cleanBase64 =
                    cleanBase64.substring(
                            cleanBase64.indexOf(",") + 1
                    );
        }

        return callVision(
                prompt,
                cleanBase64
        );
    }

    @Override
    public String providerName() {
        return "groq";
    }

    /**
     * Normal text AI.
     */
    private String callText(
            List<ChatMessage> messages
    ) {

        String apiKey =
                props.ai().apiKey();

        validateApiKey(apiKey);

        try {

            var messagesArray =
                    objectMapper.createArrayNode();

            for (ChatMessage message : messages) {

                var messageNode =
                        objectMapper.createObjectNode();

                messageNode.put(
                        "role",
                        message.role()
                );

                messageNode.put(
                        "content",
                        message.content()
                );

                messagesArray.add(
                        messageNode
                );
            }

            var requestBody =
                    objectMapper.createObjectNode();

            requestBody.put(
                    "model",
                    getTextModel()
            );

            requestBody.put(
                    "temperature",
                    0.7
            );

            requestBody.put(
                    "max_completion_tokens",
                    props.ai().maxTokens()
            );

            requestBody.set(
                    "messages",
                    messagesArray
            );

            return sendRequest(
                    requestBody,
                    apiKey
            );

        } catch (ApiException e) {

            throw e;

        } catch (Exception e) {

            log.error(
                    "AI text provider call failed",
                    e
            );

            throw ApiException.badRequest(
                    "AI service temporarily unavailable"
            );
        }
    }

    /**
     * Vision AI.
     *
     * IMPORTANT:
     *
     * 1. Uses Qwen 3.6 27B.
     * 2. Uses non-thinking mode.
     * 3. Hides reasoning.
     * 4. Uses one vision request.
     * 5. Does not use response_format.
     *
     * This prevents Qwen from spending the 1024
     * output tokens on <think> content.
     */
    private String callVision(
            String prompt,
            String base64
    ) {

        String apiKey =
                props.ai().apiKey();

        validateApiKey(apiKey);

        try {

            String visionModel =
                    getVisionModel();

            log.info(
                    "Sending AI vision request using model: {}",
                    visionModel
            );

            var requestBody =
                    objectMapper.createObjectNode();

            /*
             * Vision model.
             */
            requestBody.put(
                    "model",
                    visionModel
            );

            /*
             * Qwen non-thinking mode.
             *
             * This is important because the previous
             * response was consuming the output budget
             * with <think> reasoning.
             */
            requestBody.put(
                    "reasoning_effort",
                    "none"
            );

            /*
             * Hide reasoning from returned content.
             */
            requestBody.put(
                    "reasoning_format",
                    "hidden"
            );

            /*
             * Low temperature for deterministic
             * disease classification.
             */
            requestBody.put(
                    "temperature",
                    0.7
            );

            /*
             * Keep output compact.
             */
            requestBody.put(
                    "max_completion_tokens",
                    1024
            );

            /*
             * DO NOT use response_format here.
             *
             * We previously received:
             *
             * json_validate_failed
             *
             * The prompt itself requests JSON and
             * AiVisionService parses it.
             */

            requestBody.set(
                    "messages",
                    buildVisionMessages(
                            prompt,
                            base64
                    )
            );

            return sendRequest(
                    requestBody,
                    apiKey
            );

        } catch (ApiException e) {

            throw e;

        } catch (Exception e) {

            log.error(
                    "AI vision provider call failed",
                    e
            );

            throw ApiException.badRequest(
                    "AI vision service temporarily unavailable"
            );
        }
    }

    /**
     * Get normal text model.
     */
    private String getTextModel() {

        String configuredModel =
                System.getenv("AI_MODEL");

        if (configuredModel != null &&
                !configuredModel.isBlank()) {

            return configuredModel.trim();
        }

        String propertyModel =
                props.ai().model();

        if (propertyModel != null &&
                !propertyModel.isBlank()) {

            return propertyModel;
        }

        return DEFAULT_TEXT_MODEL;
    }

    /**
     * Get vision model.
     */
    private String getVisionModel() {

        String configuredModel =
                System.getenv("AI_VISION_MODEL");

        if (configuredModel != null &&
                !configuredModel.isBlank()) {

            return configuredModel.trim();
        }

        return DEFAULT_VISION_MODEL;
    }

    /**
     * Build multimodal OpenAI-compatible message.
     */
    private JsonNode buildVisionMessages(
            String prompt,
            String base64
    ) {

        /*
         * Text portion.
         */
        var textContent =
                objectMapper.createObjectNode();

        textContent.put(
                "type",
                "text"
        );

        textContent.put(
                "text",
                prompt
        );

        /*
         * Image URL.
         */
        var imageUrl =
                objectMapper.createObjectNode();

        imageUrl.put(
                "url",
                "data:image/jpeg;base64," + base64
        );

        /*
         * Image content.
         */
        var imageContent =
                objectMapper.createObjectNode();

        imageContent.put(
                "type",
                "image_url"
        );

        imageContent.set(
                "image_url",
                imageUrl
        );

        /*
         * Content array.
         */
        var content =
                objectMapper.createArrayNode();

        content.add(
                textContent
        );

        content.add(
                imageContent
        );

        /*
         * User message.
         */
        var userMessage =
                objectMapper.createObjectNode();

        userMessage.put(
                "role",
                "user"
        );

        userMessage.set(
                "content",
                content
        );

        /*
         * Messages array.
         */
        var messages =
                objectMapper.createArrayNode();

        messages.add(
                userMessage
        );

        return messages;
    }

    /**
     * Execute HTTP request against Groq.
     */
    private String sendRequest(
            JsonNode requestBody,
            String apiKey
    ) {

        try {

            String requestJson =
                    objectMapper.writeValueAsString(
                            requestBody
                    );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            props.ai().baseUrl()
                                                    + "/chat/completions"
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(
                                            props.ai()
                                                    .timeoutSeconds()
                                    )
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(
                                                    requestJson
                                            )
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );

            /*
             * Handle API errors.
             */
            if (response.statusCode() != 200) {

                log.warn(
                        "AI provider returned {}: {}",
                        response.statusCode(),
                        response.body()
                );

                if (response.statusCode() == 429) {

                    throw ApiException.badRequest(
                            "AI service rate limit reached. Please try again shortly."
                    );
                }

                throw ApiException.badRequest(
                        "AI service temporarily unavailable"
                );
            }

            /*
             * Parse response.
             */
            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            JsonNode choices =
                    root.path("choices");

            if (!choices.isArray() ||
                    choices.isEmpty()) {

                log.error(
                        "AI provider returned no choices: {}",
                        response.body()
                );

                throw ApiException.badRequest(
                        "AI service returned an empty response"
                );
            }

            JsonNode message =
                    choices
                            .path(0)
                            .path("message");

            JsonNode content =
                    message.path("content");

            /*
             * If content is missing, log the complete
             * message so the problem is diagnosable.
             */
            if (content.isMissingNode() ||
                    content.isNull()) {

                log.error(
                        "AI provider response missing content: {}",
                        message
                );

                throw ApiException.badRequest(
                        "AI service returned an invalid response"
                );
            }

            String result =
                    content.asText();

            /*
             * IMPORTANT:
             *
             * Log the raw response temporarily so we can
             * diagnose any unexpected model output.
             */
            log.info(
                    "AI provider response: {}",
                    result
            );

            if (result == null ||
                    result.isBlank()) {

                throw ApiException.badRequest(
                        "AI service returned an empty response"
                );
            }

            return result;

        } catch (ApiException e) {

            throw e;

        } catch (Exception e) {

            log.error(
                    "AI provider HTTP request failed",
                    e
            );

            throw ApiException.badRequest(
                    "AI service temporarily unavailable"
            );
        }
    }

    /**
     * Validate API key.
     */
    private void validateApiKey(
            String apiKey
    ) {

        if (apiKey == null ||
                apiKey.isBlank()) {

            throw ApiException.badRequest(
                    "AI service is not configured (AI_API_KEY missing)"
            );
        }
    }
}
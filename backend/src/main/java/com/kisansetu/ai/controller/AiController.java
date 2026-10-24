package com.kisansetu.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.ai.entity.AiConversation;
import com.kisansetu.ai.entity.AiMessage;
import com.kisansetu.ai.entity.CropPlan;
import com.kisansetu.ai.repository.CropPlanRepository;
import com.kisansetu.ai.service.AdvisoryService;
import com.kisansetu.ai.service.AiConversationService;
import com.kisansetu.ai.service.CropRecommendationService;
import com.kisansetu.ai.service.CustomerRecommendationService;
import com.kisansetu.common.ApiResponse;
import com.kisansetu.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI endpoints: persistent chatbot conversations, crop planner,
 * advisory and customer recommendations.
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "Agricultural AI services")
public class AiController {

    private final AiConversationService conversationService;
    private final CropRecommendationService cropRecommendationService;
    private final AdvisoryService advisoryService;
    private final CustomerRecommendationService customerRecommendationService;
    private final CropPlanRepository cropPlanRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------- Chatbot conversations ----------------

    @GetMapping("/conversations")
    @Operation(summary = "My AI conversations")
    public ApiResponse<List<AiConversation>> conversations() {
        return ApiResponse.ok(conversationService.getConversations(CurrentUser.get().userId()));
    }

    @PostMapping("/conversations")
    @Operation(summary = "Start a new conversation")
    public ApiResponse<AiConversation> createConversation(@RequestParam(required = false) String title) {
        return ApiResponse.created(conversationService.createConversation(CurrentUser.get().userId(), title));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Delete a conversation")
    public ApiResponse<Void> deleteConversation(@PathVariable UUID conversationId) {
        conversationService.deleteConversation(CurrentUser.get().userId(), conversationId);
        return ApiResponse.deleted("Conversation deleted");
    }

    @DeleteMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Clear conversation history")
    public ApiResponse<Void> clearConversation(@PathVariable UUID conversationId) {
        conversationService.clearConversation(CurrentUser.get().userId(), conversationId);
        return ApiResponse.deleted("Conversation cleared");
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Message history of a conversation")
    public ApiResponse<List<AiMessage>> messages(@PathVariable UUID conversationId) {
        return ApiResponse.ok(conversationService.getMessages(CurrentUser.get().userId(), conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Send a message and get the AI reply")
    public ApiResponse<AiConversationService.SendResult> sendMessage(@PathVariable UUID conversationId,
                                                                     @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(conversationService.sendMessage(
                CurrentUser.get().userId(), conversationId, request.message()));
    }

    @PostMapping("/chat")
    @Operation(summary = "One-off chat (creates a conversation automatically)")
    public ApiResponse<Map<String, Object>> chat(@RequestBody SendMessageRequest request) {
        UUID userId = CurrentUser.get().userId();
        AiConversation conversation = conversationService.createConversation(userId, null);
        AiConversationService.SendResult result = conversationService.sendMessage(
                userId, conversation.getId(), request.message());
        return ApiResponse.ok(Map.of(
                "conversationId", conversation.getId().toString(),
                "reply", result.reply(),
                "userMessage", result.userMessage(),
                "assistantMessage", result.assistantMessage()
        ));
    }

    // ---------------- Crop planner ----------------

    @PostMapping("/crop-planner")
    @Operation(summary = "Generate crop recommendations (persisted)")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<CropRecommendationService.Result> cropPlanner(
            @RequestBody CropPlannerRequest request) {
        CropRecommendationService.Input input = new CropRecommendationService.Input(
                request.soilType(),
                request.region(),
                request.season(),
                request.waterAvailability(),
                request.budget() == null ? 100000 : request.budget(),
                request.farmSize() == null ? 1 : request.farmSize(),
                request.previousCrop(),
                request.preferredCrop());
        CropRecommendationService.Result result = cropRecommendationService.recommend(input);

        CropPlan plan = new CropPlan();
        plan.setFarmerId(CurrentUser.get().userId());
        try {
            plan.setInputJson(objectMapper.writeValueAsString(input));
            plan.setResultJson(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("Could not serialize crop plan: {}", e.getMessage());
        }
        cropPlanRepository.save(plan);
        return ApiResponse.ok(result);
    }

    @GetMapping("/crop-plans")
    @Operation(summary = "Recent crop plans")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<CropPlan>> cropPlans() {
        return ApiResponse.ok(cropPlanRepository
                .findTop10ByFarmerIdOrderByCreatedAtDesc(CurrentUser.get().userId()));
    }

    // ---------------- Advisory ----------------

    @PostMapping("/advisory")
    @Operation(summary = "Free-form agricultural advisory query")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<AdvisoryService.AdvisoryResult> advisory(@RequestBody SendMessageRequest request) {
        return ApiResponse.ok(advisoryService.generate(request.message()));
    }

    // ---------------- Customer recommendations ----------------

    @GetMapping("/customer-recommendations")
    @Operation(summary = "Personalized recommendations for customers")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ApiResponse<CustomerRecommendationService.Result> customerRecommendations() {
        return ApiResponse.ok(customerRecommendationService.generate(CurrentUser.get().userId()));
    }

    public record SendMessageRequest(
            @NotBlank(message = "Message is required")
            String message
    ) {
    }

    public record CropPlannerRequest(
            @NotBlank(message = "Soil type is required")
            String soilType,
            String region,
            @NotBlank(message = "Season is required")
            String season,
            @NotBlank(message = "Water availability is required")
            String waterAvailability,
            Double budget,
            Double farmSize,
            String previousCrop,
            String preferredCrop
    ) {
    }
}
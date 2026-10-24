package com.kisansetu.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.ai.provider.AiProvider;
import com.kisansetu.customer.repository.CustomerOrderRepository;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Personalized produce recommendations for customers, based on purchase
 * history + currently available listings (ported from the original service).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRecommendationService {

    private final AiProvider aiProvider;
    private final CustomerOrderRepository customerOrderRepository;
    private final MarketplaceListingRepository listingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record CustomerRecommendation(String title, String category, String reason,
                                         String listingId, String priority) {
    }

    public record Result(List<CustomerRecommendation> recommendations, String seasonalTip) {
    }

    public Result generate(UUID customerId) {
        var orderHistory = customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .filter(o -> o.getStatus().name().equals("DELIVERED"))
                .limit(20)
                .toList();

        var listings = listingRepository.findActiveCategories().isEmpty()
                ? listingRepository.findAll()
                : listingRepository.findAll();

        LinkedHashSet<String> purchasedCategories = new LinkedHashSet<>();
        LinkedHashSet<String> purchasedItems = new LinkedHashSet<>();
        for (var order : orderHistory) {
            listingRepository.findById(order.getListingId()).ifPresent(l -> {
                purchasedCategories.add(l.getCategory());
                purchasedItems.add(l.getTitle());
            });
        }

        String currentMonth = DateTimeFormatter.ofPattern("MMMM").withZone(ZoneId.systemDefault()).format(Instant.now());

        StringBuilder listingText = new StringBuilder();
        listings.stream().filter(l -> l.isAvailable()).limit(50).forEach(l ->
                listingText.append("- ").append(l.getTitle()).append(" (")
                        .append(l.getCategory()).append(", ₹").append(l.getPrice())
                        .append("/").append(l.getUnit()).append(", ")
                        .append(l.getFarmingMethod()).append(")\n"));

        String prompt = """
                Based on the following customer purchase history and current available produce,
                suggest 5 personalized crop recommendations.

                Customer Purchase History:
                - Categories bought: %s
                - Items purchased: %s
                - Current month: %s (consider seasonal availability)

                Available listings:
                %s

                Provide recommendations that:
                1. Match customer preferences based on history
                2. Consider seasonal availability for %s
                3. Include a mix of their favorites and new discoveries
                4. Prioritize organic/sustainable options when available

                Return ONLY a JSON object in the following format:
                {
                  "recommendations": [
                    {
                      "title": "Product or crop name",
                      "category": "Category",
                      "reason": "Why this is recommended",
                      "listing_id": "optional listing id from the list above",
                      "priority": "high" | "medium" | "low"
                    }
                  ],
                  "seasonal_tip": "One paragraph seasonal tip"
                }

                The response must be valid JSON with no extra commentary.
                """.formatted(
                purchasedCategories.isEmpty() ? "None yet" : String.join(", ", purchasedCategories),
                purchasedItems.isEmpty() ? "None yet" : String.join(", ",
                        purchasedItems.stream().limit(10).toList()),
                currentMonth,
                listingText,
                currentMonth);

        try {
            String text = aiProvider.chat(List.of(ChatMessage.user(prompt)));
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalStateException("No JSON in AI response");
            }
            JsonNode parsed = objectMapper.readTree(text.substring(start, end + 1));
            List<CustomerRecommendation> recs = new ArrayList<>();
            parsed.path("recommendations").forEach(r -> recs.add(new CustomerRecommendation(
                    r.path("title").asText(""),
                    r.path("category").asText(""),
                    r.path("reason").asText(""),
                    r.path("listing_id").asText(null),
                    r.path("priority").asText("medium"))));
            return new Result(recs, parsed.path("seasonal_tip").asText(""));
        } catch (Exception e) {
            log.warn("AI customer recommendations failed: {}", e.getMessage());
            return new Result(List.of(), "");
        }
    }
}
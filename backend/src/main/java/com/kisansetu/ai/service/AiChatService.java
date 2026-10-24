package com.kisansetu.ai.service;

import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.ai.provider.AiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Text-based agricultural AI assistant (the "Agri Chat").
 * Enforces an agriculture-only guardrail in the system prompt.
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final String SYSTEM_PROMPT = """
            You are an agricultural expert assistant for Indian farmers on the KisanSetu platform.
            Answer only agriculture-related questions including crops, soil, fertilizers, irrigation,
            pests, diseases, weather farming guidance, crop planning, harvesting, and market selling tips.
            If the user asks anything unrelated to agriculture, politely respond:
            "I can assist only with agriculture-related questions."
            Provide concise, farmer-friendly responses in simple language.
            """;

    private final AiProvider aiProvider;

    public String reply(String userMessage) {
        return aiProvider.chat(List.of(
                ChatMessage.system(SYSTEM_PROMPT),
                ChatMessage.user(userMessage)
        )).trim();
    }

    public String replyWithHistory(String userMessage, List<ChatMessage> history) {
        var messages = new java.util.ArrayList<ChatMessage>();
        messages.add(ChatMessage.system(SYSTEM_PROMPT));
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(ChatMessage.user(userMessage));
        return aiProvider.chat(messages).trim();
    }
}
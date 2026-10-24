package com.kisansetu.ai.provider;

import com.kisansetu.ai.dto.ChatMessage;

import java.util.List;

/**
 * Pluggable AI provider for text and vision generation.
 * Implementations are configured via AI_PROVIDER / AI_API_KEY / AI_MODEL.
 */
public interface AiProvider {

    /**
     * Generate a text completion.
     */
    String chat(List<ChatMessage> messages);

    /**
     * Generate a text completion with an image attached (base64, with or
     * without data-URI prefix).
     */
    String chatWithImage(String prompt, String imageBase64);

    String providerName();
}
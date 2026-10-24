package com.kisansetu.ai.provider;

import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.config.KisanSetuProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfflineAiProviderTest {

    private KisanSetuProperties props() {
        return new KisanSetuProperties(null, null, null, null, null, null, null);
    }

    @Test
    void chat_returnsLocalGuidance() {
        OfflineAiProvider provider = new OfflineAiProvider(props());

        String response = provider.chat(List.of(
                new ChatMessage("system", "You are an assistant"),
                new ChatMessage("user", "What should I plant in July?")));

        assertTrue(response.contains("offline mode"));
        assertTrue(response.contains("AI_API_KEY"));
    }

    @Test
    void chat_withNoUserMessagesStillResponds() {
        OfflineAiProvider provider = new OfflineAiProvider(props());
        assertFalse(provider.chat(List.of(new ChatMessage("system", "hi"))).isBlank());
    }

    @Test
    void chatWithImage_returnsVisionOfflineMessage() {
        OfflineAiProvider provider = new OfflineAiProvider(props());
        String response = provider.chatWithImage("Analyze this leaf", "aGVsbG8=");
        assertTrue(response.contains("offline mode"));
    }

    @Test
    void providerName_isOffline() {
        assertEquals("offline", new OfflineAiProvider(props()).providerName());
    }
}
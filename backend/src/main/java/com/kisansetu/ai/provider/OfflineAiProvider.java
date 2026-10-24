package com.kisansetu.ai.provider;

import com.kisansetu.ai.dto.ChatMessage;
import com.kisansetu.config.KisanSetuProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Graceful fallback provider used when AI credentials are not configured.
 * Keeps the application functional in development while clearly flagging
 * that the response is advisory-only and locally generated.
 */
@Slf4j
@Component
public class OfflineAiProvider implements AiProvider {

    private final KisanSetuProperties props;

    public OfflineAiProvider(KisanSetuProperties props) {
        this.props = props;
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        String lastUser = messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(ChatMessage::content)
                .orElse("");
        log.info("OfflineAiProvider used (AI_API_KEY not configured). User asked: {}", abbreviate(lastUser));
        return """
                I am currently running in offline mode because the AI service is not configured yet.

                Please add your AI_API_KEY to the backend environment (see .env.example) and restart the
                server to enable the full agricultural assistant experience.

                Meanwhile, here are a few general farming pointers:
                - Water young crops early in the morning or late evening to reduce evaporation.
                - Inspect leaves weekly for early signs of pests or disease.
                - Test your soil before applying fertilizer.
                """.trim();
    }

    @Override
    public String chatWithImage(String prompt, String imageBase64) {
        log.info("OfflineAiProvider image analysis requested but not configured.");
        return """
                I am currently running in offline mode because the AI vision service is not configured.

                Please add your AI_API_KEY to the backend environment and restart the server to analyze crop images.
                """.trim();
    }

    @Override
    public String providerName() {
        return "offline";
    }

    private String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }
}
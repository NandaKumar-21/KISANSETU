package com.kisansetu.ai.config;

import com.kisansetu.ai.provider.AiProvider;
import com.kisansetu.ai.provider.GroqAiProvider;
import com.kisansetu.ai.provider.OfflineAiProvider;
import com.kisansetu.config.KisanSetuProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the active AI provider from AI_PROVIDER. Falls back to the
 * offline provider when no API key is configured so the app never crashes.
 */
@Configuration
public class AiProviderConfig {

    @Bean
    public AiProvider aiProvider(KisanSetuProperties props, GroqAiProvider groqProvider, OfflineAiProvider offlineProvider) {
        String provider = props.ai().provider();
        boolean hasKey = props.ai().apiKey() != null && !props.ai().apiKey().isBlank();
        if (!hasKey) {
            return offlineProvider;
        }
        if (provider == null || provider.isBlank() || "groq".equalsIgnoreCase(provider)
                || "openai-compatible".equalsIgnoreCase(provider)) {
            return groqProvider;
        }
        return groqProvider;
    }
}
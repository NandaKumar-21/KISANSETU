package com.kisansetu.ai.config;

import com.kisansetu.ai.provider.AiProvider;
import com.kisansetu.ai.provider.GroqAiProvider;
import com.kisansetu.ai.provider.OfflineAiProvider;
import com.kisansetu.config.KisanSetuProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiProviderConfigTest {

    private KisanSetuProperties props(String provider, String apiKey) {
        return new KisanSetuProperties(null, null,
                new KisanSetuProperties.Ai(provider, apiKey, "llama-3.1-8b-instant",
                        "https://api.groq.com/openai/v1", 30, 1024),
                null, null, null, null);
    }

    @Test
    void noApiKey_returnsOfflineProvider() {
        OfflineAiProvider offline = new OfflineAiProvider(props("groq", null));

        AiProvider selected = new AiProviderConfig().aiProvider(props("groq", null),
                new GroqAiProvider(props("groq", null)), offline);

        assertSame(offline, selected);
    }

    @Test
    void blankApiKey_returnsOfflineProvider() {
        OfflineAiProvider offline = new OfflineAiProvider(props("groq", " "));

        AiProvider selected = new AiProviderConfig().aiProvider(props("groq", " "),
                new GroqAiProvider(props("groq", null)), offline);

        assertSame(offline, selected);
    }

    @Test
    void groqWithKey_returnsGroqProvider() {
        GroqAiProvider groq = new GroqAiProvider(props("groq", "sk-test"));
        OfflineAiProvider offline = new OfflineAiProvider(props("groq", "sk-test"));

        AiProvider selected = new AiProviderConfig().aiProvider(props("groq", "sk-test"), groq, offline);

        assertSame(groq, selected);
    }

    @Test
    void unknownProviderWithKey_fallsBackToGroq() {
        GroqAiProvider groq = new GroqAiProvider(props("watsonx", "sk-test"));
        OfflineAiProvider offline = new OfflineAiProvider(props("watsonx", "sk-test"));

        AiProvider selected = new AiProviderConfig().aiProvider(props("watsonx", "sk-test"), groq, offline);

        assertSame(groq, selected);
    }
}
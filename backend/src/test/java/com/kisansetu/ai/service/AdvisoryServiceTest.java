package com.kisansetu.ai.service;

import com.kisansetu.ai.provider.AiProvider;
import com.kisansetu.ai.service.AdvisoryService.AdvisoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvisoryServiceTest {

    @Mock
    private AiProvider aiProvider;

    private AdvisoryService service;

    @BeforeEach
    void setUp() {
        service = new AdvisoryService(aiProvider);
    }

    @Test
    void generate_parsesProviderJson() {
        when(aiProvider.chat(any())).thenReturn("""
                {
                  "disease_name": "Powdery Mildew",
                  "confidence": "high",
                  "severity": "medium",
                  "description": "White powdery patches on leaves",
                  "symptoms": ["White powder", "Stunted growth"],
                  "treatments": [
                    { "name": "Sulfur spray", "dosagePerAcre": "2 g per litre", "description": "Spray weekly" }
                  ],
                  "applicationGuide": [
                    { "step": "Mix and spray", "timing": "Early morning" }
                  ],
                  "preventionTips": ["Improve airflow"]
                }
                """);

        AdvisoryResult result = service.generate("Leaves look white and powdery");

        assertEquals("Powdery Mildew", result.diseaseName());
        assertEquals("high", result.confidence());
        assertEquals(1, result.treatments().size());
        assertEquals("Sulfur spray", result.treatments().get(0).name());
        assertEquals(1, result.applicationGuide().size());
        assertEquals(1, result.preventionTips().size());
    }

    @Test
    void generate_fallsBackWhenProviderReturnsNonJson() {
        when(aiProvider.chat(any())).thenReturn("I am sorry, I cannot help with that.");

        AdvisoryResult result = service.generate("How do I grow tomatoes?");

        assertEquals("General Farm Advisory", result.diseaseName());
        assertTrue(result.treatments().get(0).name().contains("Neem"));
    }

    @Test
    void generate_fallsBackWhenProviderThrows() {
        when(aiProvider.chat(any())).thenThrow(new RuntimeException("provider down"));

        AdvisoryResult result = service.generate("Irrigation tips");

        assertEquals("General Farm Advisory", result.diseaseName());
        assertEquals(2, result.applicationGuide().size());
    }
}
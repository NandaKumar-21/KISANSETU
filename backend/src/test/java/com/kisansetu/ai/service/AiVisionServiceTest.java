package com.kisansetu.ai.service;

import com.kisansetu.ai.dto.DiseaseAnalysisResult;
import com.kisansetu.ai.provider.AiProvider;
import com.kisansetu.common.exception.ApiException;
import com.kisansetu.ai.service.PricingService.TreatmentCost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiVisionServiceTest {

    @Mock
    private AiProvider aiProvider;
    @Mock
    private PricingService pricingService;

    private AiVisionService service;

    @BeforeEach
    void setUp() {
        service = new AiVisionService(aiProvider, pricingService);
    }

    @Test
    void analyzeImage_rejectsNonPlantImage() {
        when(aiProvider.chatWithImage(any(), any())).thenReturn("{\"isPlant\": false}");

        ApiException ex = assertThrows(ApiException.class, () -> service.analyzeImage("aGVsbG8="));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void analyzeImage_buildsResultFromValidResponse() {
        when(aiProvider.chatWithImage(any(), any()))
                .thenReturn("{\"isPlant\": true}")
                .thenReturn("""
                        {
                          "disease": "Late Blight",
                          "disease_name": "Late Blight",
                          "confidence": 82,
                          "severity": "high",
                          "description": "Dark spots on leaves",
                          "symptoms": ["Dark spots", "Wilting"],
                          "recommendedChemicals": [
                            { "name": "Mancozeb", "dosagePerAcre": "500 g per acre" }
                          ],
                          "applicationGuide": [
                            { "step": "Spray on affected area", "timing": "Morning" }
                          ],
                          "preventionTips": ["Rotate crops"]
                        }
                        """);
        when(pricingService.calculateCost("Mancozeb", "500 g per acre"))
                .thenReturn(new TreatmentCost(150.0, 150.0, "1 kg"));

        DiseaseAnalysisResult result = service.analyzeImage("aGVsbG8=");

        assertEquals("Late Blight", result.diseaseName());
        assertEquals("high", result.confidence());
        assertEquals(1, result.treatments().size());
        assertEquals(150.0, result.treatments().get(0).totalCost());
        assertEquals("Rotate crops", result.preventionTips().get(0));
    }

    @Test
    void analyzeImage_lowConfidenceRejected() {
        when(aiProvider.chatWithImage(any(), any()))
                .thenReturn("{\"isPlant\": true}")
                .thenReturn("{\"disease\": \"Unknown\", \"disease_name\": \"Unknown\", \"confidence\": 40}");

        ApiException ex = assertThrows(ApiException.class, () -> service.analyzeImage("aGVsbG8="));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void analyzeImage_bannedWordRejected() {
        when(aiProvider.chatWithImage(any(), any()))
                .thenReturn("{\"isPlant\": true}")
                .thenReturn("{\"disease\": \"Human skin irritation\", \"disease_name\": \"Human skin irritation\", \"confidence\": 90}");

        ApiException ex = assertThrows(ApiException.class, () -> service.analyzeImage("aGVsbG8="));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void analyzeSymptoms_unparseableResponseBadGateway() {
        when(aiProvider.chat(any())).thenReturn("No JSON here");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.analyzeSymptoms(List.of("yellow leaves")));
        assertEquals(502, ex.getStatus());
    }

    @Test
    void analyzeSymptoms_parsesSymptomResponse() {
        when(aiProvider.chat(any())).thenReturn("""
                {
                  "disease": "Yellow Vein Mosaic",
                  "disease_name": "Yellow Vein Mosaic",
                  "confidence": 70,
                  "severity": "medium",
                  "description": "Yellow veins on leaves",
                  "symptoms": ["Yellow veins"],
                  "recommendedChemicals": [],
                  "applicationGuide": [],
                  "preventionTips": ["Remove infected plants"]
                }
                """);

        DiseaseAnalysisResult result = service.analyzeSymptoms(List.of("yellow leaves"));

        assertEquals("Yellow Vein Mosaic", result.diseaseName());
        assertEquals("medium", result.confidence());
        assertEquals(0, result.treatments().size());
    }
}
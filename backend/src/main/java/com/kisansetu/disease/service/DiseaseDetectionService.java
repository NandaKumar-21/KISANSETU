package com.kisansetu.disease.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.ai.dto.DiseaseAnalysisResult;
import com.kisansetu.ai.service.AiVisionService;
import com.kisansetu.common.exception.ApiException;
import com.kisansetu.disease.entity.DiseaseRecord;
import com.kisansetu.disease.repository.DiseaseRecordRepository;
import com.kisansetu.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Farmer disease detection with history persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseDetectionService {

    private final AiVisionService visionService;
    private final DiseaseRecordRepository recordRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Map<String, Object> detect(UUID farmerId, String method, String imageBase64,
                                      String imageUrl, List<String> symptoms) {
        DiseaseAnalysisResult result;
        if ("image".equals(method)) {
            if (imageBase64 == null || imageBase64.isBlank()) {
                throw ApiException.badRequest("An image is required for image-based detection");
            }
            validateImage(imageBase64);
            result = visionService.analyzeImage(imageBase64);
        } else if ("symptom".equals(method)) {
            if (symptoms == null || symptoms.isEmpty()) {
                throw ApiException.badRequest("At least one symptom is required");
            }
            result = visionService.analyzeSymptoms(symptoms);
        } else {
            throw ApiException.badRequest("Invalid detection method");
        }

        DiseaseRecord record = new DiseaseRecord();
        record.setFarmerId(farmerId);
        record.setDetectionMethod(method);
        record.setImageUrl(imageUrl);
        record.setSymptoms(symptoms != null ? symptoms.toArray(String[]::new) : null);
        record.setDiseaseName(result.diseaseName());
        record.setConfidence(result.confidence());
        record.setSeverity(result.severity());
        record.setDescription(result.description());
        try {
            record.setAnalysisJson(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("Could not persist analysis JSON: {}", e.getMessage());
        }
        recordRepository.save(record);

        notificationService.notify(farmerId, "disease_result",
                "Disease analysis ready",
                "Analysis for your crop: " + result.diseaseName() + " ("
                        + result.confidence() + " confidence).");

        return Map.of(
                "analysis", result,
                "recordId", record.getId().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<DiseaseRecord> getMyRecords(UUID farmerId) {
        return recordRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId);
    }

    @Transactional(readOnly = true)
    public long recordCount(UUID farmerId) {
        return recordRepository.countByFarmerId(farmerId);
    }

    /**
     * Validate MIME type and approximate size (same rules as the original).
     */
    private void validateImage(String imageBase64) {
        if (imageBase64.startsWith("data:")) {
            String mime = imageBase64.substring(5, imageBase64.indexOf(';'));
            if (!List.of("image/jpeg", "image/png", "image/webp").contains(mime)) {
                throw ApiException.badRequest("Unsupported image format. Please upload JPEG, PNG, or WebP.");
            }
        }
        String base64Part = imageBase64.contains(",") ? imageBase64.split(",", 2)[1] : imageBase64;
        long estimatedBytes = (long) ((base64Part.length() * 3L) / 4);
        if (estimatedBytes > 10 * 1024 * 1024) {
            throw ApiException.badRequest("Image is too large. Please upload an image smaller than 10 MB.");
        }
    }
}
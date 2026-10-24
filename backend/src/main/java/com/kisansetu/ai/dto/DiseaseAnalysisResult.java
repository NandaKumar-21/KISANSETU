package com.kisansetu.ai.dto;

import java.util.List;

public record DiseaseAnalysisResult(
        String diseaseName,
        String confidence,
        String severity,
        String description,
        List<String> symptoms,
        List<Treatment> treatments,
        List<ApplicationStep> applicationGuide,
        List<String> preventionTips
) {

    public record Treatment(
            String name,
            String dosagePerAcre,
            String description,
            Double unitPrice,
            Double totalCost,
            String requiredQuantity,
            boolean pricingAvailable
    ) {
    }

    public record ApplicationStep(String step, String timing) {
    }
}
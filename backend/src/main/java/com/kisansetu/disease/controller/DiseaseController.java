package com.kisansetu.disease.controller;

import com.kisansetu.common.ApiResponse;
import com.kisansetu.disease.entity.DiseaseRecord;
import com.kisansetu.disease.service.DiseaseDetectionService;
import com.kisansetu.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disease")
@RequiredArgsConstructor
@Tag(name = "Disease Detection", description = "AI crop disease detection for farmers")
public class DiseaseController {

    private final DiseaseDetectionService diseaseService;

    @PostMapping("/detect")
    @Operation(summary = "Detect crop disease from image or symptoms")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<Map<String, Object>> detect(@RequestBody DetectRequest request) {
        return ApiResponse.ok(diseaseService.detect(
                CurrentUser.get().userId(),
                request.method(),
                request.image(),
                request.imageUrl(),
                request.symptoms()));
    }

    @GetMapping("/history")
    @Operation(summary = "My disease detection history")
    @PreAuthorize("hasAnyRole('FARMER')")
    public ApiResponse<List<DiseaseRecord>> history() {
        return ApiResponse.ok(diseaseService.getMyRecords(CurrentUser.get().userId()));
    }

    public record DetectRequest(
            String method,
            String image,
            String imageUrl,
            List<String> symptoms
    ) {
    }
}
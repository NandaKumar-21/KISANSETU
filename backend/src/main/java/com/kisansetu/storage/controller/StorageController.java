package com.kisansetu.storage.controller;

import com.kisansetu.common.ApiResponse;
import com.kisansetu.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Image uploads to Supabase Storage")
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    @Operation(summary = "Upload an image (base64 data URL) and return its public URL")
    public ApiResponse<Map<String, String>> upload(@RequestBody UploadRequest request) {
        String url = storageService.uploadBase64(request.image(), request.folder());
        return ApiResponse.ok(Map.of("url", url));
    }

    public record UploadRequest(
            @NotBlank(message = "Image data is required")
            String image,
            String folder
    ) {
    }
}
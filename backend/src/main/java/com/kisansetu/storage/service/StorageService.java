package com.kisansetu.storage.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.config.KisanSetuProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Uploads images to the Supabase Storage "avatars" bucket via the
 * service-role key (mirrors the original avatars bucket usage).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final KisanSetuProperties props;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * @param base64Data e.g. "data:image/png;base64,...." or raw base64
     * @param folder     sub-folder inside the bucket, e.g. "avatars" or "disease"
     * @return public URL of the uploaded file
     */
    public String uploadBase64(String base64Data, String folder) {
        if (base64Data == null || base64Data.isBlank()) {
            throw ApiException.badRequest("No file data provided");
        }
        String mime;
        String base64Part;
        if (base64Data.startsWith("data:")) {
            int comma = base64Data.indexOf(',');
            if (comma < 0) {
                throw ApiException.badRequest("Invalid data URL");
            }
            String header = base64Data.substring(5, comma);
            mime = header.contains(";") ? header.substring(0, header.indexOf(';')) : "application/octet-stream";
            base64Part = base64Data.substring(comma + 1);
        } else {
            mime = "image/png";
            base64Part = base64Data;
        }
        String ext = switch (mime) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "png";
        };
        if (!mime.startsWith("image/")) {
            throw ApiException.badRequest("Only image uploads are supported");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Part);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid base64 data");
        }

        String filePath = (folder == null || folder.isBlank() ? "uploads" : folder)
                + "/" + UUID.randomUUID() + "." + ext;
        String bucketUrl = props.storage().bucketUrl() + "/" + props.storage().bucket() + "/" + filePath;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(bucketUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + props.supabase().serviceRoleKey())
                    .header("Content-Type", mime)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("Upload failed ({}): {}", response.statusCode(), response.body());
                throw ApiException.serverError("File upload failed: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.serverError("File upload failed: " + e.getMessage());
        }
        return props.storage().publicBaseUrl() + "/" + filePath;
    }
}
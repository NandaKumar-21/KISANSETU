package com.kisansetu.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Liveness endpoint. Public (permitted in SecurityConfig); does not require
 * authentication and does not touch the database.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Liveness endpoint")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Liveness check")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
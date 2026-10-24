package com.kisansetu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "kisansetu")
public record KisanSetuProperties(
        Cors cors,
        Supabase supabase,
        Ai ai,
        Weather weather,
        Storage storage,
        Logistics logistics,
        Maps maps
) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Supabase(
            String url,
            String anonKey,
            String serviceRoleKey,
            String jwtSecret,
            String jwtIssuer,
            String jwtJwkUri
    ) {
    }

    public record Ai(
            String provider,
            String apiKey,
            String model,
            String baseUrl,
            long timeoutSeconds,
            int maxTokens
    ) {
    }

    public record Weather(String provider, String baseUrl, String apiKey) {
    }

    public record Storage(String bucket, String publicBaseUrl, String bucketUrl) {
    }

    public record Logistics(double earningPercentage, String defaultCity) {
    }

    public record Maps(String provider, String apiKey) {
    }
}
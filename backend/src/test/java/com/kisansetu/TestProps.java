package com.kisansetu;

import com.kisansetu.config.KisanSetuProperties;

import java.util.List;

/**
 * Shared test fixture builder for KisanSetuProperties.
 */
public final class TestProps {

    public static final String TEST_JWT_SECRET = "test-secret-0123456789-abcdefghij";

    private TestProps() {
    }

    public static KisanSetuProperties build() {
        return buildWith(TEST_JWT_SECRET);
    }

    public static KisanSetuProperties buildWith(String jwtSecret) {
        return new KisanSetuProperties(
                new KisanSetuProperties.Cors(List.of("http://localhost:3000")),
                new KisanSetuProperties.Supabase(
                        "https://your-project.supabase.co",
                        "test-anon-key",
                        "test-service-role-key",
                        jwtSecret,
                        "https://your-project.supabase.co/auth/v1",
                        "https://your-project.supabase.co/auth/v1/.well-known/jwks.json"),
                new KisanSetuProperties.Ai(
                        "groq",
                        "",
                        "meta-llama/llama-4-scout-17b-16e-instruct",
                        "https://api.groq.com/openai/v1",
                        60,
                        2048),
                new KisanSetuProperties.Weather("open-meteo", "https://api.open-meteo.com/v1", ""),
                new KisanSetuProperties.Storage("kisansetu", "", ""),
                new KisanSetuProperties.Logistics(5.0, "Kolhapur"),
                new KisanSetuProperties.Maps("osm", ""));
    }
}
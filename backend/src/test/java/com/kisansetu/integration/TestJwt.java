package com.kisansetu.integration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Builds HS256 JWTs matching the local dev Supabase JWT secret
 * (kisansetu.supabase.jwt-secret in application-test.yml).
 */
public final class TestJwt {

    public static final String SECRET = "test-secret-0123456789-abcdefghij";

    private TestJwt() {
    }

    public static String tokenFor(String sub, String email) {
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long exp = System.currentTimeMillis() / 1000 + 3600;
        String payload = b64("{\"sub\":\"" + sub + "\",\"email\":\"" + email
                + "\",\"aud\":\"authenticated\",\"role\":\"authenticated\",\"exp\":" + exp + "}");
        String signingInput = header + "." + payload;
        String signature;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return signingInput + "." + signature;
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
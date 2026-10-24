package com.kisansetu.security;

import com.kisansetu.config.KisanSetuProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates JWTs issued by Supabase Auth.
 * <p>
 * The key selector is chosen from the token's own {@code alg} header:
 * <ul>
 *   <li>{@code ES256} (current Supabase default, ECC P-256) — verifies against the project's
 *       JWKS endpoint, key selected by {@code kid}. The keyset is fetched once and cached for
 *       6 hours.</li>
 *   <li>{@code HS256} — legacy/local development only, verified against
 *       {@code SUPABASE_JWT_SECRET}. Never used when the token is ES256.</li>
 *   <li>Any other algorithm is rejected.</li>
 * </ul>
 * Issuer, audience and subject are validated after signature verification.
 */
@Slf4j
@Component
public class SupabaseJwtDecoder {

    private static final long JWKS_CACHE_MS = 6 * 60 * 60 * 1000L;
    private static final String PLACEHOLDER_PROJECT = "your-project.supabase.co";

    private final KisanSetuProperties props;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private volatile JWKSource<SecurityContext> jwkSource;
    private volatile long jwksFetchedAt;

    public SupabaseJwtDecoder(KisanSetuProperties props) {
        this.props = props;
    }

    /**
     * Parses and cryptographically verifies the token. Returns claims or throws.
     */
    public JWTClaimsSet verify(String token) {
        SignedJWT signedJwt;
        try {
            signedJwt = SignedJWT.parse(token);
        } catch (java.text.ParseException e) {
            throw new JwtValidationException("Malformed JWT");
        }

        JWSHeader header = signedJwt.getHeader();
        JWSAlgorithm algorithm = header.getAlgorithm();
        if (algorithm == null) {
            throw new JwtValidationException("JWT header missing algorithm");
        }
        if (!algorithm.equals(JWSAlgorithm.ES256) && !algorithm.equals(JWSAlgorithm.HS256)) {
            throw new JwtValidationException("Unsupported JWT algorithm: " + algorithm.getName());
        }

        var processor = new DefaultJWTProcessor<SecurityContext>();
        processor.setJWSKeySelector(resolveKeySelector(algorithm));

        JWTClaimsSet claims;
        try {
            claims = processor.process(signedJwt, null);
        } catch (com.nimbusds.jwt.proc.BadJWTException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("expired")) {
                throw new JwtValidationException("Token is expired");
            }
            throw new JwtValidationException("Token rejected: " + e.getMessage());
        } catch (com.nimbusds.jose.proc.BadJOSEException e) {
            throw new JwtValidationException("Token signature verification failed");
        } catch (Exception e) {
            log.debug("JWT processing failed: {}", e.getMessage());
            throw new JwtValidationException("Token verification failed");
        }

        verifyIssuer(claims);
        verifyAudience(claims);
        verifySubject(claims);
        return claims;
    }

    private void verifyIssuer(JWTClaimsSet claims) {
        String expectedIssuer = props.supabase().jwtIssuer();
        if (expectedIssuer == null || expectedIssuer.isBlank() || expectedIssuer.contains(PLACEHOLDER_PROJECT)) {
            return;
        }
        String actual = claims.getIssuer();
        if (actual == null || !actual.equals(expectedIssuer)) {
            throw new JwtValidationException("Token issuer mismatch: expected "
                    + expectedIssuer + " but got " + (actual == null ? "<none>" : actual));
        }
    }

    private void verifyAudience(JWTClaimsSet claims) {
        List<String> audience = claims.getAudience();
        String aud = audience == null || audience.isEmpty() ? null : audience.get(0);
        if (aud == null || !"authenticated".equals(aud)) {
            throw new JwtValidationException("Token audience mismatch: expected "
                    + "authenticated but got " + (aud == null ? "<none>" : aud));
        }
    }

    private void verifySubject(JWTClaimsSet claims) {
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new JwtValidationException("Token missing subject (sub)");
        }
        try {
            UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new JwtValidationException("Token subject is not a UUID");
        }
    }

    private JWSKeySelector<SecurityContext> resolveKeySelector(JWSAlgorithm algorithm) {
        if (JWSAlgorithm.HS256.equals(algorithm)) {
            String secret = props.supabase().jwtSecret();
            if (secret == null || secret.isBlank()) {
                throw new JwtValidationException(
                        "Token uses HS256 but no SUPABASE_JWT_SECRET is configured");
            }
            var key = new com.nimbusds.jose.jwk.OctetSequenceKey.Builder(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .algorithm(JWSAlgorithm.HS256)
                    .keyID("supabase-jwt-secret")
                    .build();
            return new JWSVerificationKeySelector<>(
                    JWSAlgorithm.HS256,
                    new ImmutableJWKSet<>(new JWKSet(key)));
        }
        // ES256 — current Supabase signing key (ECC P-256), verified against the JWKS.
        return new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, fetchJwks());
    }

    private JWKSource<SecurityContext> fetchJwks() {
        long now = System.currentTimeMillis();
        if (jwkSource != null && now - jwksFetchedAt < JWKS_CACHE_MS) {
            return jwkSource;
        }
        String uri = props.supabase().jwtJwkUri();
        try {
            URI jwksUri = URI.create(uri);
            if (jwksUri.getHost() == null) {
                throw new JwtValidationException("Unable to validate token: invalid JWKS URI " + uri);
            }
            // Resolve first so DNS failures are distinguishable from connection failures.
            java.net.InetAddress.getByName(jwksUri.getHost());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(jwksUri)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new JwtValidationException("Unable to validate token: JWKS fetch returned HTTP "
                        + response.statusCode() + " from " + uri);
            }
            JWKSet set = JWKSet.parse(response.body());
            if (set.getKeys().isEmpty()) {
                throw new JwtValidationException("Unable to validate token: JWKS contains no keys from " + uri);
            }
            jwkSource = new ImmutableJWKSet<>(set);
            jwksFetchedAt = now;
            log.debug("Fetched Supabase JWKS from {} ({} keys)", uri, set.getKeys().size());
            return jwkSource;
        } catch (JwtValidationException e) {
            throw e;
        } catch (UnknownHostException e) {
            throw new JwtValidationException(
                    "Unable to validate token: JWKS DNS lookup failed for host "
                            + URI.create(uri).getHost() + " (host does not resolve)");
        } catch (ConnectException | HttpTimeoutException e) {
            throw new JwtValidationException(
                    "Unable to validate token: JWKS connection to " + uri + " failed: " + e.getMessage());
        } catch (java.text.ParseException e) {
            throw new JwtValidationException(
                    "Unable to validate token: JWKS response from " + uri + " is not a valid key set");
        } catch (Exception e) {
            log.debug("JWKS fetch failed for {}: {}", uri, e.getMessage());
            throw new JwtValidationException("Unable to validate token: JWKS fetch failed");
        }
    }

    /** Convenience: pull the user id claim (sub) as UUID. */
    public Optional<UUID> extractUserId(JWTClaimsSet claims) {
        String sub = claims.getSubject();
        if (sub == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(sub));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
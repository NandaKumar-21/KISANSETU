package com.kisansetu.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisansetu.TestProps;
import com.kisansetu.config.KisanSetuProperties;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT verification for both supported Supabase signing modes:
 * <ul>
 *   <li>HS256 — legacy/local development via SUPABASE_JWT_SECRET</li>
 *   <li>ES256 — current Supabase default (ECC P-256), verified against a JWKS endpoint
 *       (served by an in-process HTTP server in these tests)</li>
 * </ul>
 */
class SupabaseJwtDecoderTest {

    private static final String SECRET = "test-secret-0123456789-abcdefghij";
    private static final String SUB = "a0000000-0000-4000-8000-000000000001";
    private static final String REAL_ISSUER = "https://lnkwazemdveueowvifni.supabase.co/auth/v1";
    private static final String REAL_JWKS =
            "https://lnkwazemdveueowvifni.supabase.co/auth/v1/.well-known/jwks.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private SupabaseJwtDecoder decoder;
    private HttpServer jwksServer;

    @BeforeEach
    void setUp() {
        decoder = new SupabaseJwtDecoder(TestProps.buildWith(SECRET));
    }

    @AfterEach
    void tearDown() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    // ---------------- HS256 legacy mode (SUPABASE_JWT_SECRET) ----------------

    private String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String signHs256(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return b64(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }

    private String createHs256Token(Map<String, Object> claims) throws Exception {
        String header = b64(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = b64(mapper.writeValueAsBytes(claims));
        return header + "." + payload + "." + signHs256(header + "." + payload);
    }

    private Map<String, Object> validClaims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", SUB);
        claims.put("email", "ramesh.farmer@demo.in");
        claims.put("aud", "authenticated");
        claims.put("role", "authenticated");
        claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());
        return claims;
    }

    @Test
    void verify_hs256ValidTokenReturnsClaims() throws Exception {
        JWTClaimsSet claims = decoder.verify(createHs256Token(validClaims()));
        assertEquals(SUB, claims.getSubject());
        assertEquals("ramesh.farmer@demo.in", claims.getStringClaim("email"));
        assertEquals("authenticated", claims.getAudience().get(0));
    }

    @Test
    void verify_hs256InvalidSignatureRejected() throws Exception {
        String token = createHs256Token(validClaims());
        String tampered = token.substring(0, token.length() - 2) + "AA";
        assertThrows(JwtValidationException.class, () -> decoder.verify(tampered));
    }

    @Test
    void verify_malformedTokenRejected() {
        assertThrows(JwtValidationException.class, () -> decoder.verify("not-a-jwt"));
        assertThrows(JwtValidationException.class, () -> decoder.verify("a.b.c.d"));
    }

    @Test
    void verify_hs256ExpiredTokenRejected() throws Exception {
        Map<String, Object> claims = validClaims();
        claims.put("exp", Instant.now().minusSeconds(180).getEpochSecond());
        assertThrows(JwtValidationException.class, () -> decoder.verify(createHs256Token(claims)));
    }

    @Test
    void verify_hs256WrongAudienceRejected() throws Exception {
        Map<String, Object> claims = validClaims();
        claims.put("aud", "service_role");
        assertThrows(JwtValidationException.class, () -> decoder.verify(createHs256Token(claims)));
    }

    @Test
    void verify_hs256CustomIssuerIsEnforcedWhenConfigured() throws Exception {
        KisanSetuProperties base = TestProps.build();
        KisanSetuProperties custom = new KisanSetuProperties(
                base.cors(),
                new KisanSetuProperties.Supabase(base.supabase().url(),
                        base.supabase().anonKey(),
                        base.supabase().serviceRoleKey(), SECRET,
                        "https://auth.kisansetu.in/auth/v1",
                        base.supabase().jwtJwkUri()),
                base.ai(), base.weather(), base.storage(), base.logistics(), base.maps());
        SupabaseJwtDecoder strict = new SupabaseJwtDecoder(custom);

        Map<String, Object> claims = validClaims();
        claims.put("iss", "https://wrong-issuer.supabase.co/auth/v1");
        assertThrows(JwtValidationException.class, () -> strict.verify(createHs256Token(claims)));

        claims.put("iss", "https://auth.kisansetu.in/auth/v1");
        assertDoesNotThrow(() -> strict.verify(createHs256Token(claims)));
    }

    @Test
    void verify_hs256RejectedWhenNoSecretConfigured() throws Exception {
        decoder = new SupabaseJwtDecoder(TestProps.buildWith(null));
        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createHs256Token(validClaims())));
        assertTrue(ex.getMessage().contains("no SUPABASE_JWT_SECRET"));
    }

    // ---------------- ES256 mode (JWKS) ----------------

    private KisanSetuProperties es256Props(String jwkUri) {
        KisanSetuProperties base = TestProps.build();
        return new KisanSetuProperties(
                base.cors(),
                new KisanSetuProperties.Supabase(base.supabase().url(),
                        base.supabase().anonKey(),
                        base.supabase().serviceRoleKey(), null,
                        REAL_ISSUER, jwkUri),
                base.ai(), base.weather(), base.storage(), base.logistics(), base.maps());
    }

    private String startJwksServer(JWKSet set) throws Exception {
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/.well-known/jwks.json", (HttpExchange exchange) -> {
            byte[] body = set.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        jwksServer.start();
        return "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/.well-known/jwks.json";
    }

    private String startJwksServerReturning(int status) throws Exception {
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/.well-known/jwks.json", (HttpExchange exchange) -> {
            exchange.sendResponseHeaders(status, 0);
            exchange.close();
        });
        jwksServer.start();
        return "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/.well-known/jwks.json";
    }

    private ECKey generateEcKey(String kid) throws Exception {
        return new ECKeyGenerator(Curve.P_256).keyID(kid).generate();
    }

    private String createEs256Token(ECKey signingKey, Map<String, Object> claims) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        claims.forEach(builder::claim);
        JWTClaimsSet claimSet = builder.build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(JOSEObjectType.JWT)
                .keyID(signingKey.getKeyID())
                .build();
        SignedJWT jwt = new SignedJWT(header, claimSet);
        jwt.sign(new ECDSASigner(signingKey));
        return jwt.serialize();
    }

    private Map<String, Object> validEs256Claims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", SUB);
        claims.put("email", "ramesh.farmer@demo.in");
        claims.put("aud", "authenticated");
        claims.put("role", "authenticated");
        claims.put("iss", REAL_ISSUER);
        claims.put("iat", new Date());
        claims.put("exp", Date.from(Instant.now().plusSeconds(3600)));
        return claims;
    }

    @Test
    void verify_es256ValidTokenReturnsClaims() throws Exception {
        ECKey signingKey = generateEcKey("kid-1");
        String jwksUri = startJwksServer(new JWKSet(List.of(signingKey.toPublicJWK())));
        decoder = new SupabaseJwtDecoder(es256Props(jwksUri));

        JWTClaimsSet claims = decoder.verify(createEs256Token(signingKey, validEs256Claims()));

        assertEquals(SUB, claims.getSubject());
        assertEquals("authenticated", claims.getAudience().get(0));
        assertEquals(REAL_ISSUER, claims.getIssuer());
    }

    @Test
    void verify_es256InvalidSignatureRejected() throws Exception {
        ECKey signingKey = generateEcKey("kid-1");
        String jwksUri = startJwksServer(new JWKSet(List.of(signingKey.toPublicJWK())));
        decoder = new SupabaseJwtDecoder(es256Props(jwksUri));

        String token = createEs256Token(signingKey, validEs256Claims());
        String tampered = token.substring(0, token.length() - 2) + "AA";

        JwtValidationException ex = assertThrows(JwtValidationException.class, () -> decoder.verify(tampered));
        assertTrue(ex.getMessage().contains("signature"));
    }

    @Test
    void verify_es256ExpiredRejected() throws Exception {
        ECKey signingKey = generateEcKey("kid-1");
        String jwksUri = startJwksServer(new JWKSet(List.of(signingKey.toPublicJWK())));
        decoder = new SupabaseJwtDecoder(es256Props(jwksUri));

        Map<String, Object> claims = validEs256Claims();
        claims.put("exp", Date.from(Instant.now().minusSeconds(180)));

        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(signingKey, claims)));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void verify_es256WrongIssuerRejected() throws Exception {
        ECKey signingKey = generateEcKey("kid-1");
        String jwksUri = startJwksServer(new JWKSet(List.of(signingKey.toPublicJWK())));
        decoder = new SupabaseJwtDecoder(es256Props(jwksUri));

        Map<String, Object> claims = validEs256Claims();
        claims.put("iss", "https://wrong-issuer.supabase.co/auth/v1");

        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(signingKey, claims)));
        assertTrue(ex.getMessage().contains("issuer mismatch"));
    }

    @Test
    void verify_es256WrongAudienceRejected() throws Exception {
        ECKey signingKey = generateEcKey("kid-1");
        String jwksUri = startJwksServer(new JWKSet(List.of(signingKey.toPublicJWK())));
        decoder = new SupabaseJwtDecoder(es256Props(jwksUri));

        Map<String, Object> claims = validEs256Claims();
        claims.put("aud", "service_role");

        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(signingKey, claims)));
        assertTrue(ex.getMessage().contains("audience mismatch"));
    }

    @Test
    void verify_es256MissingSubjectRejected() throws Exception {
        ECKey signingKey = generateEcKey("kid-1");
        String jwksUri = startJwksServer(new JWKSet(List.of(signingKey.toPublicJWK())));
        decoder = new SupabaseJwtDecoder(es256Props(jwksUri));

        Map<String, Object> claims = validEs256Claims();
        claims.remove("sub");
        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(signingKey, claims)));
        assertTrue(ex.getMessage().contains("subject"));

        claims.put("sub", "not-a-uuid");
        JwtValidationException ex2 = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(signingKey, claims)));
        assertTrue(ex2.getMessage().contains("not a UUID"));
    }

    @Test
    void verify_es256JwksUnavailableRejected() throws Exception {
        // Point at a port with no listener -> connection refused
        decoder = new SupabaseJwtDecoder(es256Props("http://127.0.0.1:1/.well-known/jwks.json"));

        ECKey signingKey = generateEcKey("kid-1");
        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(signingKey, validEs256Claims())));
        assertTrue(ex.getMessage().contains("JWKS"));
    }

    @Test
    void verify_es256JwksHttpErrorRejected() throws Exception {
        ECKey signingKey = generateEcKey("kid-1");
        String jwksUri = startJwksServerReturning(500);
        decoder = new SupabaseJwtDecoder(es256Props(jwksUri));

        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(signingKey, validEs256Claims())));
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    void verify_es256JwksDnsFailureRejected() throws Exception {
        decoder = new SupabaseJwtDecoder(
                es256Props("https://does-not-exist.invalid/.well-known/jwks.json"));

        ECKey signingKey = generateEcKey("kid-1");
        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(signingKey, validEs256Claims())));
        assertTrue(ex.getMessage().contains("DNS"));
    }

    @Test
    void verify_es256KeySelectedByKid() throws Exception {
        ECKey otherKey = generateEcKey("kid-other");
        ECKey signingKey = generateEcKey("kid-match");
        String jwksUri = startJwksServer(
                new JWKSet(List.of(otherKey.toPublicJWK(), signingKey.toPublicJWK())));
        decoder = new SupabaseJwtDecoder(es256Props(jwksUri));

        // Token signed with the key whose kid is in the JWKS -> accepted
        assertDoesNotThrow(() -> decoder.verify(createEs256Token(signingKey, validEs256Claims())));

        // Token carrying a kid that is not in the JWKS -> rejected
        ECKey unknownKey = generateEcKey("kid-unknown");
        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> decoder.verify(createEs256Token(unknownKey, validEs256Claims())));
        assertTrue(ex.getMessage().contains("signature"));
    }

    @Test
    void verify_unsupportedAlgorithmRejected() throws Exception {
        Map<String, Object> claims = validClaims();
        String header = b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64(mapper.writeValueAsBytes(claims));
        String token = header + "." + payload + "." + "dummy";

        JwtValidationException ex = assertThrows(JwtValidationException.class, () -> decoder.verify(token));
        assertTrue(ex.getMessage().contains("Unsupported JWT algorithm: RS256"));
    }

    @Test
    void extractUserId_parsesUuidOrEmpty() throws Exception {
        JWTClaimsSet claims = decoder.verify(createHs256Token(validClaims()));
        assertEquals(UUID.fromString(SUB), decoder.extractUserId(claims).orElseThrow());

        JWTClaimsSet noSub = new JWTClaimsSet.Builder().build();
        assertTrue(decoder.extractUserId(noSub).isEmpty());

        JWTClaimsSet badSub = new JWTClaimsSet.Builder().subject("not-a-uuid").build();
        assertTrue(decoder.extractUserId(badSub).isEmpty());
    }
}
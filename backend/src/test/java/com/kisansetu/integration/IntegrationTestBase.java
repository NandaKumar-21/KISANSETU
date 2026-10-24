package com.kisansetu.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.UUID;

/**
 * Base class for full-stack integration tests: real Spring context on a
 * random port, real PostgreSQL (kisansetu_test), Flyway-migrated schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    public static final UUID FARMER1 = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    public static final UUID FARMER2 = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    public static final UUID MERCHANT = UUID.fromString("a0000000-0000-4000-8000-000000000011");
    public static final UUID CUSTOMER = UUID.fromString("a0000000-0000-4000-8000-000000000021");
    public static final UUID PARTNER = UUID.fromString("a0000000-0000-4000-8000-000000000031");

    @LocalServerPort
    protected int port;

    protected final TestRestTemplate rest = new TestRestTemplate(
            new RestTemplateBuilder().requestFactory(JdkClientHttpRequestFactory.class));

    protected String farmer1Token() {
        return TestJwt.tokenFor(FARMER1.toString(), "ramesh.farmer@demo.in");
    }

    protected String farmer2Token() {
        return TestJwt.tokenFor(FARMER2.toString(), "sunita.farmer@demo.in");
    }

    protected String merchantToken() {
        return TestJwt.tokenFor(MERCHANT.toString(), "kisan.agro@demo.in");
    }

    protected String customerToken() {
        return TestJwt.tokenFor(CUSTOMER.toString(), "priya.customer@demo.in");
    }

    protected String partnerToken() {
        return TestJwt.tokenFor(PARTNER.toString(), "ravi.logistics@demo.in");
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    protected ResponseEntity<String> get(String path, String token) {
        RequestEntity<Void> request = RequestEntity.get(URI.create(url(path)))
                .headers(authHeaders(token))
                .build();
        return rest.exchange(request, String.class);
    }

    protected ResponseEntity<String> post(String path, String token, Object body) {
        RequestEntity<Object> request = RequestEntity.post(URI.create(url(path)))
                .headers(authHeaders(token))
                .body(body);
        return rest.exchange(request, String.class);
    }

    protected ResponseEntity<String> put(String path, String token, Object body) {
        RequestEntity<Object> request = RequestEntity.put(URI.create(url(path)))
                .headers(authHeaders(token))
                .body(body);
        return rest.exchange(request, String.class);
    }

    protected ResponseEntity<String> patch(String path, String token, Object body) {
        RequestEntity<Object> request = new RequestEntity<>(body, authHeaders(token),
                HttpMethod.PATCH, URI.create(url(path)));
        return rest.exchange(request, String.class);
    }

    protected ResponseEntity<String> delete(String path, String token) {
        RequestEntity<Void> request = RequestEntity.delete(URI.create(url(path)))
                .headers(authHeaders(token))
                .build();
        return rest.exchange(request, String.class);
    }
}
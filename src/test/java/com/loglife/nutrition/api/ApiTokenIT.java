package com.loglife.nutrition.api;

import com.loglife.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static-token guard end to end: with a token configured, /api/** demands the header (constant
 * response, token never echoed) while the PWA shell stays open.
 */
@TestPropertySource(properties = "loglife.security.api-token=segredo-de-teste")
class ApiTokenIT extends AbstractPostgresIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    private String base() {
        return "http://localhost:" + port;
    }

    @Test
    void apiDemandsTokenWhenConfigured() throws Exception {
        HttpResponse<String> noToken = send(HttpRequest.newBuilder(
                URI.create(base() + "/api/v1/food-logs?date=2026-06-12")).GET().build());
        assertThat(noToken.statusCode()).isEqualTo(401);
        assertThat(noToken.body()).doesNotContain("segredo-de-teste");

        HttpResponse<String> wrongToken = send(HttpRequest.newBuilder(
                        URI.create(base() + "/api/v1/food-logs?date=2026-06-12"))
                .header("X-Api-Token", "errado").GET().build());
        assertThat(wrongToken.statusCode()).isEqualTo(401);

        HttpResponse<String> withToken = send(HttpRequest.newBuilder(
                        URI.create(base() + "/api/v1/food-logs?date=2026-06-12"))
                .header("X-Api-Token", "segredo-de-teste").GET().build());
        assertThat(withToken.statusCode()).isEqualTo(200);
    }

    @Test
    void pwaShellStaysOpenWithoutToken() throws Exception {
        HttpResponse<String> shell = send(HttpRequest.newBuilder(
                URI.create(base() + "/")).GET().build());
        assertThat(shell.statusCode()).isEqualTo(200);
        assertThat(shell.body()).contains("LogLife");
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}

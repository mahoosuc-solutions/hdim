package com.healthdata.payer.service;

import com.healthdata.payer.revenue.dto.ClaimSubmissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("RestClearinghouseSubmissionAdapter Container Tests")
class RestClearinghouseSubmissionAdapterContainerTest {

    @Container
    static final GenericContainer<?> wiremock = new GenericContainer<>(
            DockerImageName.parse("wiremock/wiremock:3.13.1")
    ).withExposedPorts(8080);

    private RestClearinghouseSubmissionAdapter adapter;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://" + wiremock.getHost() + ":" + wiremock.getMappedPort(8080);
        adapter = new RestClearinghouseSubmissionAdapter(new RestTemplate(), baseUrl);
        resetMappings();
    }

    @Test
    @DisplayName("submit succeeds against deployed stub container")
    void shouldSubmitAgainstContainerizedStub() throws Exception {
        registerStub(200, "{\"accepted\":true,\"externalTrackingId\":\"container-track-001\"}");

        ClearinghouseSubmissionResult result = adapter.submit(sampleRequest(), 1);

        assertThat(result.accepted()).isTrue();
        assertThat(result.externalTrackingId()).isEqualTo("container-track-001");
    }

    @Test
    @DisplayName("submit maps 5xx from containerized stub to retryable exception")
    void shouldMapContainerized5xxToRetryable() throws Exception {
        registerStub(500, "{\"error\":\"upstream failure\"}");

        assertThatThrownBy(() -> adapter.submit(sampleRequest(), 1))
                .isInstanceOf(RetryableClearinghouseException.class);
    }

    private void resetMappings() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/__admin/mappings/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private void registerStub(int status, String body) throws IOException, InterruptedException {
        String mappingJson = """
                {
                  "request": {
                    "method": "POST",
                    "urlPath": "/api/v1/clearinghouse/claims/submissions"
                  },
                  "response": {
                    "status": %d,
                    "headers": {
                      "Content-Type": "application/json"
                    },
                    "jsonBody": %s
                  }
                }
                """.formatted(status, body);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/__admin/mappings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mappingJson))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Failed to register WireMock mapping: " + response.body());
        }
    }

    private ClaimSubmissionRequest sampleRequest() {
        return ClaimSubmissionRequest.builder()
                .tenantId("tenant-a")
                .claimId("clm-container-001")
                .patientId("pat-container-001")
                .payerId("payer-a")
                .totalAmount(BigDecimal.valueOf(100))
                .idempotencyKey("idem-container-001")
                .correlationId("corr-container-001")
                .actor("test")
                .build();
    }
}

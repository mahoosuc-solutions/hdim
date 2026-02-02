package com.healthdata.aiassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.aiassistant.config.ClaudeConfig;
import com.healthdata.aiassistant.dto.ChatMessage;
import com.healthdata.aiassistant.dto.ChatRequest;
import com.healthdata.aiassistant.dto.ChatResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@DisplayName("ClaudeService")
@Tag("slow")
class ClaudeServiceTest {

    @Test
    @DisplayName("Should return chat response when Claude returns text")
    void shouldReturnChatResponse() {
        String responseJson = """
            {"content":[{"type":"text","text":"Hello"}],
             "usage":{"input_tokens":12,"output_tokens":34}}
            """;
        ClaudeService service = buildService(jsonExchange(responseJson));

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Explain care gaps.")
            .tenantId("tenant-1")
            .messages(List.of(ChatMessage.user("previous message")))
            .build();

        ChatResponse response = service.chatCached(request);

        assertThat(response.getResponse()).isEqualTo("Hello");
        assertThat(response.getQueryType()).isEqualTo("care_gaps");
        assertThat(response.getModel()).isEqualTo("test-model");
        assertThat(response.getInputTokens()).isEqualTo(12);
        assertThat(response.getOutputTokens()).isEqualTo(34);
        assertThat(response.isError()).isFalse();
    }

    @Test
    @DisplayName("Should default token usage to zero when usage is missing")
    void shouldHandleMissingUsage() {
        String responseJson = """
            {"content":[{"type":"text","text":"Hello"}]}
            """;
        ClaudeService service = buildService(jsonExchange(responseJson));

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Explain care gaps.")
            .tenantId("tenant-1")
            .build();

        ChatResponse response = service.chat(request);

        assertThat(response.getInputTokens()).isZero();
        assertThat(response.getOutputTokens()).isZero();
    }

    @Test
    @DisplayName("Should return default message when Claude response lacks text blocks")
    void shouldHandleMissingTextBlocks() {
        String responseJson = """
            {"content":[{"type":"tool","text":"ignored"}]}
            """;
        ClaudeService service = buildService(jsonExchange(responseJson));

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Explain care gaps.")
            .tenantId("tenant-1")
            .build();

        ChatResponse response = service.chat(request);

        assertThat(response.getResponse()).isEqualTo("No response generated");
    }

    @Test
    @DisplayName("Should return error response when Claude response is empty")
    void shouldHandleEmptyResponse() {
        ClaudeService service = buildService(jsonExchange("{\"content\":[]}"));

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Any output?")
            .tenantId("tenant-1")
            .build();

        ChatResponse response = service.chat(request);

        assertThat(response.isError()).isTrue();
        assertThat(response.getErrorMessage()).contains("Empty response");
        assertThat(response.getResponse()).contains("Unable to process request");
    }

    @Test
    @DisplayName("Should return error response when Claude call fails")
    void shouldHandleClaudeErrors() {
        ClaudeService service = buildService(request -> Mono.error(new RuntimeException("boom")));

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Any output?")
            .tenantId("tenant-1")
            .build();

        ChatResponse response = service.chat(request);

        assertThat(response.isError()).isTrue();
        assertThat(response.getErrorMessage()).contains("AI service unavailable");
    }

    @Test
    @DisplayName("Should build fallback response when circuit breaker triggers")
    void shouldBuildFallbackResponse() {
        ClaudeService service = buildService(jsonExchange("{\"content\":[]}"));
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Any output?")
            .tenantId("tenant-1")
            .build();

        ChatResponse response = service.chatFallback(request, new RuntimeException("offline"));

        assertThat(response.isError()).isTrue();
        assertThat(response.getModel()).isEqualTo("fallback");
        assertThat(response.getQueryType()).isEqualTo("care_gaps");
        assertThat(response.getErrorMessage()).contains("Service temporarily unavailable");
    }

    @Test
    @DisplayName("Should build prompts for patient summary, care gaps, and clinical query")
    void shouldBuildPrompts() {
        ClaudeService service = spy(baseService());
        doReturn(ChatResponse.builder().build()).when(service).chat(any(ChatRequest.class));

        service.generatePatientSummary("patient-123", "summary data");
        service.analyzeCareGaps("gap data");
        service.answerClinicalQuery("What is diabetes?", "context data");

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(service, times(3)).chat(captor.capture());

        List<ChatRequest> requests = captor.getAllValues();
        assertThat(requests.get(0).getQueryType()).isEqualTo("patient_summary");
        assertThat(requests.get(0).getQuery()).contains("patient-123").contains("summary data");
        assertThat(requests.get(1).getQueryType()).isEqualTo("care_gaps");
        assertThat(requests.get(1).getQuery()).contains("gap data");
        assertThat(requests.get(2).getQueryType()).isEqualTo("quality_measures");
        assertThat(requests.get(2).getQuery()).contains("What is diabetes?").contains("context data");
    }

    @Test
    @DisplayName("Should initialize WebClient with configured values")
    void shouldInitializeWebClient() {
        ClaudeService service = baseService();

        service.init();

        Object webClient = ReflectionTestUtils.getField(service, "webClient");
        assertThat(webClient).isNotNull();
    }

    private static ClaudeService buildService(ExchangeFunction exchangeFunction) {
        ClaudeService service = baseService();
        WebClient client = WebClient.builder().exchangeFunction(exchangeFunction).build();
        ReflectionTestUtils.setField(service, "webClient", client);
        return service;
    }

    private static ClaudeService baseService() {
        ClaudeConfig config = new ClaudeConfig();
        config.setApiUrl("http://localhost");
        config.setApiKey("test-key");
        config.setModel("test-model");
        config.setMaxTokens(256);
        config.setTemperature(0.2);
        config.setTimeoutSeconds(1);
        return new ClaudeService(config, new ObjectMapper());
    }

    private static ExchangeFunction jsonExchange(String json) {
        return request -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Flux.just(new DefaultDataBufferFactory()
                    .wrap(json.getBytes(StandardCharsets.UTF_8))))
                .build()
        );
    }
}

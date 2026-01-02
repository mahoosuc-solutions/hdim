package com.healthdata.aiassistant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthdata.aiassistant.config.ClaudeConfig;
import com.healthdata.aiassistant.dto.ChatRequest;
import com.healthdata.aiassistant.dto.ChatResponse;
import com.healthdata.aiassistant.service.ClaudeService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("AiAssistantController Unit Tests")
class AiAssistantControllerUnitTest {

    @Test
    @DisplayName("Should return disabled response when service is missing")
    void shouldReturnDisabledWhenServiceMissing() {
        ClaudeConfig config = new ClaudeConfig();
        config.setEnabled(true);
        AiAssistantController controller = new AiAssistantController(Optional.empty(), config);

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("test")
            .tenantId("tenant-1")
            .build();

        ResponseEntity<ChatResponse> response = controller.chat(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isError()).isTrue();
    }

    @Test
    @DisplayName("Should handle chat requests when authentication is absent")
    void shouldHandleChatWithoutAuthentication() {
        ClaudeConfig config = new ClaudeConfig();
        config.setEnabled(true);
        ClaudeService service = mock(ClaudeService.class);
        ChatResponse serviceResponse = ChatResponse.builder().build();
        when(service.chat(any(ChatRequest.class))).thenReturn(serviceResponse);

        AiAssistantController controller = new AiAssistantController(Optional.of(service), config);

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("test")
            .tenantId("tenant-1")
            .build();

        ResponseEntity<ChatResponse> response = controller.chat(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
        verify(service).chat(eq(request));
    }

    @Test
    @DisplayName("Should allow patient summary when enabled and authentication is absent")
    void shouldGeneratePatientSummaryWithoutAuthentication() {
        ClaudeConfig config = new ClaudeConfig();
        config.setEnabled(true);
        ClaudeService service = mock(ClaudeService.class);
        ChatResponse serviceResponse = ChatResponse.builder().response("summary").build();
        when(service.generatePatientSummary(eq("patient-123"), eq("payload")))
            .thenReturn(serviceResponse);

        AiAssistantController controller = new AiAssistantController(Optional.of(service), config);

        ResponseEntity<ChatResponse> response = controller.generatePatientSummary("patient-123", "payload", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponse()).isEqualTo("summary");
        verify(service).generatePatientSummary(eq("patient-123"), eq("payload"));
    }

    @Test
    @DisplayName("Should analyze care gaps when enabled and authentication is absent")
    void shouldAnalyzeCareGapsWithoutAuthentication() {
        ClaudeConfig config = new ClaudeConfig();
        config.setEnabled(true);
        ClaudeService service = mock(ClaudeService.class);
        ChatResponse serviceResponse = ChatResponse.builder().response("analysis").build();
        when(service.analyzeCareGaps(eq("gaps"))).thenReturn(serviceResponse);

        AiAssistantController controller = new AiAssistantController(Optional.of(service), config);

        ResponseEntity<ChatResponse> response = controller.analyzeCareGaps("gaps", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponse()).isEqualTo("analysis");
        verify(service).analyzeCareGaps(eq("gaps"));
    }

    @Test
    @DisplayName("Should default context and handle long queries without authentication")
    void shouldAnswerQueryWithDefaultContext() {
        ClaudeConfig config = new ClaudeConfig();
        config.setEnabled(true);
        ClaudeService service = mock(ClaudeService.class);
        ChatResponse serviceResponse = ChatResponse.builder().response("answer").build();
        when(service.answerClinicalQuery(eq(longQuery()), eq(""))).thenReturn(serviceResponse);

        AiAssistantController controller = new AiAssistantController(Optional.of(service), config);

        ResponseEntity<ChatResponse> response = controller.answerQuery(longQuery(), null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponse()).isEqualTo("answer");
        verify(service).answerClinicalQuery(eq(longQuery()), eq(""));
    }

    private static String longQuery() {
        return "What is the best clinical protocol for managing diabetes in adults with comorbidities?";
    }
}

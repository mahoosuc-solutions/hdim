package com.healthdata.quality.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthdata.quality.service.notification.TemplateRenderer;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Template Preview Controller Tests")
class TemplatePreviewControllerTest {

    @Test
    @DisplayName("Should render template with defaults")
    void shouldRenderTemplateWithDefaults() {
        TemplateRenderer renderer = Mockito.mock(TemplateRenderer.class);
        when(renderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        TemplatePreviewController controller = new TemplatePreviewController(renderer);

        ResponseEntity<String> response = controller.previewTemplateWithDefaults("critical-alert", "EMAIL");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("ok");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor =
            (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        verify(renderer).render(eq("critical-alert"), captor.capture());
        assertThat(captor.getValue().get("channel")).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("Should add channel when missing in custom data")
    void shouldAddChannelWhenMissing() {
        TemplateRenderer renderer = Mockito.mock(TemplateRenderer.class);
        when(renderer.render(eq("care-gap"), any())).thenReturn("rendered");
        TemplatePreviewController controller = new TemplatePreviewController(renderer);

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientName", "Jane Doe");

        ResponseEntity<String> response = controller.previewTemplateWithCustomData("care-gap", variables);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor =
            (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        verify(renderer).render(eq("care-gap"), captor.capture());
        assertThat(captor.getValue().get("channel")).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("Should return error when render fails")
    void shouldReturnErrorWhenRenderFails() {
        TemplateRenderer renderer = Mockito.mock(TemplateRenderer.class);
        when(renderer.render(eq("lab-result"), any())).thenThrow(new RuntimeException("boom"));
        TemplatePreviewController controller = new TemplatePreviewController(renderer);

        ResponseEntity<String> response = controller.previewTemplateWithDefaults("lab-result", "EMAIL");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("boom");
    }

    @Test
    @DisplayName("Should report template existence and list templates")
    void shouldReportTemplateExistenceAndListTemplates() {
        TemplateRenderer renderer = Mockito.mock(TemplateRenderer.class);
        when(renderer.templateExists("digest")).thenReturn(true);
        TemplatePreviewController controller = new TemplatePreviewController(renderer);

        ResponseEntity<Map<String, Object>> exists = controller.checkTemplateExists("digest");
        assertThat(exists.getBody()).containsEntry("templateId", "digest");
        assertThat(exists.getBody()).containsEntry("exists", true);

        ResponseEntity<Map<String, Object>> list = controller.listTemplates();
        assertThat(list.getBody().get("count")).isEqualTo(7);
    }

    @Test
    @DisplayName("Should provide sample data for each template")
    void shouldProvideSampleDataForEachTemplate() {
        TemplatePreviewController controller = new TemplatePreviewController(Mockito.mock(TemplateRenderer.class));

        Map<String, Object> critical = controller.getSampleDataStructure("critical-alert").getBody();
        assertThat(critical).containsKeys("channel", "details", "recommendedActions");

        Map<String, Object> careGap = controller.getSampleDataStructure("care-gap").getBody();
        assertThat(careGap).containsKeys("gapType", "recommendedActions");

        Map<String, Object> healthScore = controller.getSampleDataStructure("health-score").getBody();
        assertThat(healthScore).containsKeys("currentScore", "contributingFactors");

        Map<String, Object> appointment = controller.getSampleDataStructure("appointment-reminder").getBody();
        assertThat(appointment).containsKeys("appointmentDate", "instructions");

        Map<String, Object> medication = controller.getSampleDataStructure("medication-reminder").getBody();
        assertThat(medication).containsKeys("medicationName", "warnings");

        Map<String, Object> lab = controller.getSampleDataStructure("lab-result").getBody();
        assertThat(lab).containsKeys("testName", "nextSteps");

        Map<String, Object> digest = controller.getSampleDataStructure("digest").getBody();
        assertThat(digest).containsKeys("criticalAlerts", "careGaps", "appointments", "labResults");
    }
}

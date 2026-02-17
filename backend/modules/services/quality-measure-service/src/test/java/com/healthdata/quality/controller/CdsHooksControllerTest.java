package com.healthdata.quality.controller;

import com.healthdata.quality.dto.CdsRecommendationDTO;
import com.healthdata.quality.service.CdsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CDS Hooks Controller Tests")
class CdsHooksControllerTest {

    @Mock
    private CdsService cdsService;

    @Test
    @DisplayName("Should expose patient-view service in discovery")
    void shouldExposeDiscoveryEndpoint() {
        CdsHooksController controller = new CdsHooksController(cdsService);

        var response = controller.discoverServices();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getServices()).hasSize(2);
        assertThat(response.getBody().getServices()).anySatisfy(service -> {
            assertThat(service.getId()).isEqualTo("patient-view");
            assertThat(service.getHook()).isEqualTo("patient-view");
        });
        assertThat(response.getBody().getServices()).anySatisfy(service -> {
            assertThat(service.getId()).isEqualTo("order-select");
            assertThat(service.getHook()).isEqualTo("order-select");
        });
    }

    @Test
    @DisplayName("Should return cards for active recommendations")
    void shouldReturnCardsForPatientView() {
        CdsHooksController controller = new CdsHooksController(cdsService);
        UUID patientId = UUID.randomUUID();

        when(cdsService.getActiveRecommendations("acme-health", patientId))
            .thenReturn(List.of(
                CdsRecommendationDTO.builder()
                    .title("Close diabetes gap")
                    .description("HbA1c is overdue.")
                    .urgency("URGENT")
                    .dueDate(Instant.parse("2026-03-01T00:00:00Z"))
                    .build()
            ));

        CdsHooksController.CdsHooksRequest request = CdsHooksController.CdsHooksRequest.builder()
            .hook("patient-view")
            .context(CdsHooksController.CdsContext.builder()
                .patientId(patientId.toString())
                .build())
            .build();

        var response = controller.patientView("acme-health", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCards()).hasSize(1);
        assertThat(response.getBody().getCards().get(0).getSummary()).isEqualTo("Close diabetes gap");
        assertThat(response.getBody().getCards().get(0).getIndicator()).isEqualTo("critical");
        assertThat(response.getBody().getCards().get(0).getLinks()).hasSize(1);
        assertThat(response.getBody().getCards().get(0).getLinks().get(0).getType()).isEqualTo("smart");
    }

    @Test
    @DisplayName("Should return warning card when patientId missing")
    void shouldHandleMissingPatientId() {
        CdsHooksController controller = new CdsHooksController(cdsService);

        CdsHooksController.CdsHooksRequest request = CdsHooksController.CdsHooksRequest.builder()
            .hook("patient-view")
            .context(CdsHooksController.CdsContext.builder().build())
            .build();

        var response = controller.patientView("acme-health", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCards()).hasSize(1);
        assertThat(response.getBody().getCards().get(0).getSummary()).contains("Invalid CDS Hooks request");
    }

    @Test
    @DisplayName("Should return cards with suggestions for order-select")
    void shouldReturnCardsForOrderSelect() {
        CdsHooksController controller = new CdsHooksController(cdsService);
        UUID patientId = UUID.randomUUID();

        when(cdsService.getActiveRecommendations("acme-health", patientId))
            .thenReturn(List.of(
                CdsRecommendationDTO.builder()
                    .title("Annual wellness visit")
                    .description("Patient is due for annual wellness visit.")
                    .urgency("SOON")
                    .build()
            ));

        CdsHooksController.CdsHooksRequest request = CdsHooksController.CdsHooksRequest.builder()
            .hook("order-select")
            .context(CdsHooksController.CdsContext.builder()
                .patientId(patientId.toString())
                .build())
            .build();

        var response = controller.orderSelect("acme-health", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCards()).hasSize(1);
        assertThat(response.getBody().getCards().get(0).getIndicator()).isEqualTo("warning");
        assertThat(response.getBody().getCards().get(0).getSuggestions()).hasSize(1);
        assertThat(response.getBody().getCards().get(0).getSuggestions().get(0).getActions()).hasSize(1);
        assertThat(response.getBody().getCards().get(0).getSuggestions().get(0).getActions().get(0).getType())
            .isEqualTo("create");
        assertThat(response.getBody().getCards().get(0).getSuggestions().get(0).getActions().get(0).getResource())
            .isNotNull();
        assertThat(response.getBody().getCards().get(0).getSuggestions().get(0).getActions().get(0).getResource().getResourceType())
            .isEqualTo("ServiceRequest");
        assertThat(response.getBody().getCards().get(0).getLinks()).hasSize(1);
        assertThat(response.getBody().getCards().get(0).getLinks().get(0).getType()).isEqualTo("smart");
    }

    @Test
    @DisplayName("Should return warning card when order-select patientId missing")
    void shouldHandleMissingPatientIdForOrderSelect() {
        CdsHooksController controller = new CdsHooksController(cdsService);

        CdsHooksController.CdsHooksRequest request = CdsHooksController.CdsHooksRequest.builder()
            .hook("order-select")
            .context(CdsHooksController.CdsContext.builder().build())
            .build();

        var response = controller.orderSelect("acme-health", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCards()).hasSize(1);
        assertThat(response.getBody().getCards().get(0).getDetail()).contains("context.patientId is required");
    }
}

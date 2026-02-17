package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.AlertDto;
import com.healthdata.analytics.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRuleV1ControllerTest {

    @Mock
    private AlertService alertService;

    @Mock
    private Authentication authentication;

    private AlertRuleV1Controller controller;

    @BeforeEach
    void setUp() {
        controller = new AlertRuleV1Controller(alertService);
    }

    @Test
    void getActiveAlertRulesReturnsPayload() {
        when(alertService.getActiveAlertRules("tenant-a")).thenReturn(List.of(
            AlertDto.builder().name("High RAF").metricType("RAF_SCORE").build()
        ));

        var response = controller.getActiveAlertRules("tenant-a");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("High RAF");
    }

    @Test
    void createAlertRuleReturnsCreatedStatus() {
        AlertDto dto = AlertDto.builder()
            .name("Projected Quality Risk")
            .metricType("QUALITY_SCORE")
            .conditionOperator("CHANGE_PCT")
            .thresholdValue(BigDecimal.TEN)
            .build();

        when(authentication.getName()).thenReturn("analyst-1");
        when(alertService.createAlertRule(dto, "tenant-a", "analyst-1")).thenReturn(dto);

        var response = controller.createAlertRule(dto, "tenant-a", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConditionOperator()).isEqualTo("CHANGE_PCT");
    }

    @Test
    void predictiveCheckReturnsTriggeredPredictions() {
        when(alertService.checkPredictiveAlerts("tenant-a", 14)).thenReturn(List.of(
            new AlertService.PredictiveAlertDto(
                UUID.randomUUID(),
                "Projected RAF Spike",
                "RAF_SCORE",
                "Average RAF Score",
                BigDecimal.valueOf(1.1),
                BigDecimal.valueOf(1.3),
                BigDecimal.valueOf(18.0),
                "HIGH",
                "Predictive threshold breach forecast"
            )
        ));

        var response = controller.predictiveCheck("tenant-a", 14);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).ruleName()).isEqualTo("Projected RAF Spike");
    }
}

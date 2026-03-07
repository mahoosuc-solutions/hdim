package com.healthdata.hedisadapter.measures;

import com.healthdata.hedisadapter.measures.CqlDelegationService.CalculationRequest;
import com.healthdata.hedisadapter.measures.CqlDelegationService.CalculationResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CqlDelegationService")
class CqlDelegationServiceTest {

    @Mock
    private RestTemplate cqlRestTemplate;

    private CqlDelegationService service;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        service = new CqlDelegationService(cqlRestTemplate, registry);
    }

    @Test
    @DisplayName("should delegate HEDIS measure calculation")
    void calculateMeasure_shouldDelegateToHedis() {
        CalculationRequest request = CalculationRequest.builder()
                .measureId("BCS")
                .measureType("HEDIS")
                .tenantId("tenant-1")
                .patientId("patient-123")
                .build();

        CalculationResult expected = CalculationResult.builder()
                .measureId("BCS")
                .status("NUMERATOR")
                .score(1.0)
                .build();

        when(cqlRestTemplate.postForObject(eq("/api/measures/calculate"), any(), eq(CalculationResult.class)))
                .thenReturn(expected);

        CalculationResult result = service.calculateMeasure(request);

        assertThat(result.getMeasureId()).isEqualTo("BCS");
        assertThat(result.getStatus()).isEqualTo("NUMERATOR");
        verify(cqlRestTemplate).postForObject(eq("/api/measures/calculate"), eq(request), eq(CalculationResult.class));
    }

    @Test
    @DisplayName("should delegate STAR measure calculation to different endpoint")
    void calculateStarMeasure_shouldUseStarEndpoint() {
        CalculationRequest request = CalculationRequest.builder()
                .measureId("C01")
                .measureType("STAR")
                .tenantId("tenant-1")
                .patientId("patient-456")
                .build();

        CalculationResult expected = CalculationResult.builder()
                .measureId("C01")
                .measureType("STAR")
                .status("DENOMINATOR")
                .score(0.0)
                .build();

        when(cqlRestTemplate.postForObject(eq("/api/star/calculate"), any(), eq(CalculationResult.class)))
                .thenReturn(expected);

        CalculationResult result = service.calculateStarMeasure(request);

        assertThat(result.getMeasureId()).isEqualTo("C01");
        verify(cqlRestTemplate).postForObject(eq("/api/star/calculate"), eq(request), eq(CalculationResult.class));
    }
}

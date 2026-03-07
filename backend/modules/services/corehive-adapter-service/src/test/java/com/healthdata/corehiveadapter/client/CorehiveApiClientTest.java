package com.healthdata.corehiveadapter.client;

import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.model.CareGapScoringResponse;
import com.healthdata.corehiveadapter.model.VbcRoiRequest;
import com.healthdata.corehiveadapter.model.VbcRoiResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CorehiveApiClient")
class CorehiveApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private CorehiveApiClient client;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        client = new CorehiveApiClient(restTemplate, registry);
    }

    @Test
    @DisplayName("should send scoring request to CoreHive API")
    void scoreCareGaps_shouldPostToCorrectEndpoint() {
        CareGapScoringRequest request = CareGapScoringRequest.builder()
                .syntheticPatientId("synth-001")
                .careGaps(List.of())
                .build();

        CareGapScoringResponse expected = CareGapScoringResponse.builder()
                .overallRiskScore(0.75)
                .build();

        when(restTemplate.postForObject(eq("/api/healthcare/score"), any(), eq(CareGapScoringResponse.class)))
                .thenReturn(expected);

        CareGapScoringResponse result = client.scoreCareGaps(request);

        assertThat(result.getOverallRiskScore()).isEqualTo(0.75);
        verify(restTemplate).postForObject(eq("/api/healthcare/score"), eq(request), eq(CareGapScoringResponse.class));
    }

    @Test
    @DisplayName("should send ROI request to CoreHive API")
    void calculateRoi_shouldPostToCorrectEndpoint() {
        VbcRoiRequest request = VbcRoiRequest.builder()
                .contractId("contract-1")
                .totalLives(10000)
                .build();

        VbcRoiResponse expected = VbcRoiResponse.builder()
                .contractId("contract-1")
                .estimatedRoi(new BigDecimal("3.2"))
                .build();

        when(restTemplate.postForObject(eq("/api/healthcare/roi"), any(), eq(VbcRoiResponse.class)))
                .thenReturn(expected);

        VbcRoiResponse result = client.calculateRoi(request);

        assertThat(result.getEstimatedRoi()).isEqualTo(new BigDecimal("3.2"));
    }

    @Test
    @DisplayName("should propagate exception through circuit breaker")
    void scoreCareGaps_shouldPropagateException() {
        CareGapScoringRequest request = CareGapScoringRequest.builder()
                .syntheticPatientId("synth-002")
                .careGaps(List.of())
                .build();

        when(restTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new ResourceAccessException("CoreHive down"));

        assertThatThrownBy(() -> client.scoreCareGaps(request))
                .isInstanceOf(ResourceAccessException.class);
    }
}

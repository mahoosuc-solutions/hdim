package com.healthdata.corehiveadapter.service;

import com.healthdata.corehiveadapter.client.CorehiveApiClient;
import com.healthdata.corehiveadapter.config.CorehiveProperties;
import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.model.CareGapScoringResponse;
import com.healthdata.corehiveadapter.model.VbcRoiRequest;
import com.healthdata.corehiveadapter.model.VbcRoiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CorehiveAdapterServiceTest {

    @Mock
    private CorehiveApiClient apiClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PhiDeIdentificationService deIdentificationService;
    private CorehiveProperties properties;
    private CorehiveAdapterService service;

    @BeforeEach
    void setUp() {
        deIdentificationService = new PhiDeIdentificationService();
        properties = new CorehiveProperties();
        properties.setEnabled(true);
        service = new CorehiveAdapterService(apiClient, deIdentificationService, kafkaTemplate, properties);
    }

    @Test
    void scoreCareGaps_shouldCallApiAndPublishEvent() {
        CareGapScoringRequest request = CareGapScoringRequest.builder()
                .syntheticPatientId("550e8400-e29b-41d4-a716-446655440000")
                .tenantId("tenant-1")
                .careGaps(List.of(
                        CareGapScoringRequest.CareGapItem.builder()
                                .syntheticGapId("gap-uuid-1")
                                .measureId("BCS")
                                .measureCode("HEDIS-BCS")
                                .gapStatus("OPEN")
                                .daysSinceIdentified(30)
                                .complianceScore(0.6)
                                .build()))
                .build();

        CareGapScoringResponse expectedResponse = CareGapScoringResponse.builder()
                .syntheticPatientId("550e8400-e29b-41d4-a716-446655440000")
                .overallRiskScore(0.85)
                .build();

        when(apiClient.scoreCareGaps(any())).thenReturn(expectedResponse);

        CareGapScoringResponse result = service.scoreCareGaps(request, "tenant-1");

        assertThat(result.getOverallRiskScore()).isEqualTo(0.85);
        verify(kafkaTemplate).send(eq("external.corehive.decisions"), eq("tenant-1"), any());
    }

    @Test
    void scoreCareGaps_shouldRejectRequestWithRealPatientId() {
        CareGapScoringRequest request = CareGapScoringRequest.builder()
                .syntheticPatientId("john.doe@example.com")
                .tenantId("tenant-1")
                .careGaps(List.of())
                .build();

        assertThatThrownBy(() -> service.scoreCareGaps(request, "tenant-1"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("PHI detected");
    }

    @Test
    void calculateRoi_shouldCallApiAndPublishEvent() {
        VbcRoiRequest request = VbcRoiRequest.builder()
                .contractId("contract-1")
                .tenantId("tenant-1")
                .totalLives(50000)
                .pmpm(new BigDecimal("450"))
                .build();

        VbcRoiResponse expectedResponse = VbcRoiResponse.builder()
                .contractId("contract-1")
                .estimatedRoi(new BigDecimal("2.5"))
                .build();

        when(apiClient.calculateRoi(any())).thenReturn(expectedResponse);

        VbcRoiResponse result = service.calculateRoi(request, "tenant-1");

        assertThat(result.getEstimatedRoi()).isEqualTo(new BigDecimal("2.5"));
        verify(kafkaTemplate).send(eq("external.corehive.roi"), eq("tenant-1"), any());
    }
}

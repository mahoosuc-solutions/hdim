package com.healthdata.events.intelligence.service;

import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingType;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.Severity;
import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import com.healthdata.events.intelligence.repository.IntelligenceTenantTrustProjectionRepository;
import com.healthdata.events.intelligence.repository.IntelligenceValidationFindingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantTrustProjectionService Tests")
class TenantTrustProjectionServiceTest {

    @Mock
    private IntelligenceValidationFindingRepository findingRepository;

    @Mock
    private IntelligenceTenantTrustProjectionRepository projectionRepository;

    @InjectMocks
    private TenantTrustProjectionService service;

    @Test
    @DisplayName("refreshForTenant should calculate trust score and aggregate counts")
    void refreshForTenantShouldCalculateTrustScoreAndCounts() {
        when(findingRepository.findByTenantIdAndStatusOrderByCreatedAtDesc("tenant-a", FindingStatus.OPEN))
                .thenReturn(List.of(
                        finding("tenant-a", Severity.HIGH, FindingType.CONSISTENCY),
                        finding("tenant-a", Severity.MEDIUM, FindingType.DATA_COMPLETENESS)
                ));

        when(projectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dashboard = service.refreshForTenant("tenant-a");

        assertThat(dashboard.tenantId()).isEqualTo("tenant-a");
        assertThat(dashboard.trustScore()).isEqualTo(70);
        assertThat(dashboard.totalOpenFindings()).isEqualTo(2);
        assertThat(dashboard.highSeverityOpenFindings()).isEqualTo(1);
    }

    private IntelligenceValidationFindingEntity finding(String tenantId, Severity severity, FindingType type) {
        return IntelligenceValidationFindingEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientRef("patient-1")
                .sourceEventId("evt-1")
                .ruleCode("RULE")
                .title("title")
                .description("desc")
                .severity(severity)
                .findingType(type)
                .status(FindingStatus.OPEN)
                .details("{}")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}

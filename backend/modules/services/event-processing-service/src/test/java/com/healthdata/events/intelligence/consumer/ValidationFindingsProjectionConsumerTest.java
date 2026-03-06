package com.healthdata.events.intelligence.consumer;

import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationFindingsProjectionConsumer Tests")
class ValidationFindingsProjectionConsumerTest {

    @Mock
    private TenantTrustProjectionService tenantTrustProjectionService;

    @InjectMocks
    private ValidationFindingsProjectionConsumer consumer;

    @Test
    @DisplayName("should refresh projection when tenantId is present")
    void shouldRefreshProjectionWhenTenantIdPresent() {
        consumer.handleValidationFinding(Map.of("tenantId", "tenant-a", "ruleCode", "RULE"));

        verify(tenantTrustProjectionService, times(1)).refreshForTenant("tenant-a");
    }

    @Test
    @DisplayName("should ignore event without tenantId")
    void shouldIgnoreEventWithoutTenantId() {
        consumer.handleValidationFinding(Map.of("ruleCode", "RULE"));

        verify(tenantTrustProjectionService, never()).refreshForTenant("tenant-a");
    }
}

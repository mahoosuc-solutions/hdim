package com.healthdata.payer.service;

import com.healthdata.payer.domain.PilotReadiness;
import com.healthdata.payer.domain.PilotReadiness.*;
import com.healthdata.payer.dto.PilotOnboardRequest;
import com.healthdata.payer.dto.PilotReadinessResponse;
import com.healthdata.payer.repository.PilotReadinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Pilot Readiness Service Tests")
class PilotReadinessServiceTest {

    @Mock
    private PilotReadinessRepository repository;

    private PilotReadinessService service;

    private static final String TENANT_ID = "test-tenant";

    @BeforeEach
    void setUp() {
        service = new PilotReadinessService(repository);
    }

    // ===== Onboarding =====

    @Test
    @DisplayName("Should onboard new pilot customer")
    void shouldOnboardNewCustomer() {
        when(repository.findByCustomerIdAndTenantId("cust-1", TENANT_ID))
                .thenReturn(Optional.empty());
        when(repository.save(any(PilotReadiness.class))).thenAnswer(inv -> {
            PilotReadiness saved = inv.getArgument(0);
            saved.setId("generated-uuid");
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        PilotOnboardRequest request = PilotOnboardRequest.builder()
                .customerId("cust-1")
                .customerName("Acme Health")
                .ehrType("EPIC")
                .fhirEndpointUrl("https://fhir.acme.com/R4")
                .build();

        PilotReadinessResponse result = service.onboard(request, TENANT_ID);

        assertThat(result.getCustomerId()).isEqualTo("cust-1");
        assertThat(result.getCustomerName()).isEqualTo("Acme Health");
        assertThat(result.getEhrType()).isEqualTo("EPIC");
        assertThat(result.getIntegrationStatus()).isEqualTo("NOT_STARTED");
        assertThat(result.getReadinessScore()).isEqualTo(0);
        verify(repository).save(any(PilotReadiness.class));
    }

    @Test
    @DisplayName("Should reject duplicate onboarding")
    void shouldRejectDuplicate() {
        when(repository.findByCustomerIdAndTenantId("cust-1", TENANT_ID))
                .thenReturn(Optional.of(PilotReadiness.builder().build()));

        PilotOnboardRequest request = PilotOnboardRequest.builder()
                .customerId("cust-1")
                .customerName("Acme Health")
                .ehrType("EPIC")
                .build();

        assertThatThrownBy(() -> service.onboard(request, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already onboarded");
    }

    @Test
    @DisplayName("Should reject unknown EHR type")
    void shouldRejectUnknownEhrType() {
        PilotOnboardRequest request = PilotOnboardRequest.builder()
                .customerId("cust-1")
                .customerName("Acme Health")
                .ehrType("UNKNOWN_EHR")
                .build();

        assertThatThrownBy(() -> service.onboard(request, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown EHR type");
    }

    // ===== Readiness Score =====

    @Test
    @DisplayName("Should calculate readiness score correctly")
    void shouldCalculateReadinessScore() {
        PilotReadiness pilot = PilotReadiness.builder()
                .integrationStatus(IntegrationStatus.CONNECTED) // +30
                .dataIngestionStatus(DataStatus.COMPLETE)        // +25
                .demoDataSeeded(true)                            // +15
                .qualityMeasuresConfigured(true)                 // +15
                .userAccountsProvisioned(true)                   // +15
                .readinessScore(0)
                .build();

        int score = pilot.calculateReadinessScore();
        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("Should calculate partial readiness score")
    void shouldCalculatePartialScore() {
        PilotReadiness pilot = PilotReadiness.builder()
                .integrationStatus(IntegrationStatus.TESTING)   // +20
                .dataIngestionStatus(DataStatus.IN_PROGRESS)    // +15
                .demoDataSeeded(false)                           // +0
                .qualityMeasuresConfigured(true)                 // +15
                .userAccountsProvisioned(false)                  // +0
                .readinessScore(0)
                .build();

        int score = pilot.calculateReadinessScore();
        assertThat(score).isEqualTo(50);
    }

    // ===== Get Readiness =====

    @Test
    @DisplayName("Should return readiness for existing customer")
    void shouldReturnReadinessForExisting() {
        PilotReadiness pilot = buildPilot("cust-1", "Acme");
        when(repository.findByCustomerIdAndTenantId("cust-1", TENANT_ID))
                .thenReturn(Optional.of(pilot));

        Optional<PilotReadinessResponse> result = service.getReadiness("cust-1", TENANT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getCustomerId()).isEqualTo("cust-1");
    }

    @Test
    @DisplayName("Should return empty for non-existent customer")
    void shouldReturnEmptyForMissing() {
        when(repository.findByCustomerIdAndTenantId("cust-999", TENANT_ID))
                .thenReturn(Optional.empty());

        Optional<PilotReadinessResponse> result = service.getReadiness("cust-999", TENANT_ID);
        assertThat(result).isEmpty();
    }

    // ===== List All =====

    @Test
    @DisplayName("Should list all pilot customers ordered by score")
    void shouldListAllCustomers() {
        when(repository.findByTenantIdOrderByReadinessScoreDesc(TENANT_ID))
                .thenReturn(List.of(
                        buildPilot("cust-1", "Acme"),
                        buildPilot("cust-2", "Beta")));

        List<PilotReadinessResponse> results = service.listAll(TENANT_ID);
        assertThat(results).hasSize(2);
    }

    private PilotReadiness buildPilot(String customerId, String name) {
        PilotReadiness p = PilotReadiness.builder()
                .id("uuid-" + customerId)
                .tenantId(TENANT_ID)
                .customerId(customerId)
                .customerName(name)
                .ehrType(EhrType.EPIC)
                .integrationStatus(IntegrationStatus.NOT_STARTED)
                .dataIngestionStatus(DataStatus.NOT_STARTED)
                .demoDataSeeded(false)
                .qualityMeasuresConfigured(false)
                .userAccountsProvisioned(false)
                .readinessScore(0)
                .build();
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }
}

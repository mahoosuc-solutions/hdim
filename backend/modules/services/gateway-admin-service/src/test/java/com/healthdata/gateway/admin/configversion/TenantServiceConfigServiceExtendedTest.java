package com.healthdata.gateway.admin.configversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extended unit tests for TenantServiceConfigService.
 * Covers requestApproval(), listAudit(), listApprovals(), and getVersion()
 * which are not covered by the base TenantServiceConfigServiceTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant Service Config Service Extended Tests")
@Tag("unit")
class TenantServiceConfigServiceExtendedTest {

    @Mock
    private TenantServiceConfigVersionRepository versionRepository;

    @Mock
    private TenantServiceConfigCurrentRepository currentRepository;

    @Mock
    private TenantServiceConfigAuditRepository auditRepository;

    @Mock
    private TenantServiceConfigApprovalRepository approvalRepository;

    @Captor
    private ArgumentCaptor<TenantServiceConfigVersion> versionCaptor;

    @Captor
    private ArgumentCaptor<TenantServiceConfigApproval> approvalCaptor;

    @Captor
    private ArgumentCaptor<TenantServiceConfigAudit> auditCaptor;

    private TenantServiceConfigService service;
    private ObjectMapper objectMapper;
    private ConfigPromotionProperties promotionProperties;

    private static final String DEMO_TENANT_ID = "demo-tenant";
    private static final String PROD_TENANT_ID = "prod-tenant";
    private static final String SERVICE_NAME = "test-service";
    private static final String ACTOR = "admin@test.com";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        promotionProperties = new ConfigPromotionProperties();
        promotionProperties.setDemoTenantId(DEMO_TENANT_ID);
        promotionProperties.setRequireDemo(true);
        promotionProperties.setRequireTwoPersonApproval(true);

        service = new TenantServiceConfigService(
                versionRepository,
                currentRepository,
                auditRepository,
                approvalRepository,
                objectMapper,
                promotionProperties
        );
    }

    private TenantServiceConfigVersion createVersion(UUID id, String tenantId, int versionNumber) {
        TenantServiceConfigVersion version = new TenantServiceConfigVersion();
        version.setId(id);
        version.setTenantId(tenantId);
        version.setServiceName(SERVICE_NAME);
        version.setVersionNumber(versionNumber);
        version.setStatus(TenantServiceConfigVersion.Status.DRAFT);
        version.setConfigJson("{\"timeout\": 5000}");
        version.setConfigHash("abc123");
        version.setCreatedBy(ACTOR);
        return version;
    }

    @Nested
    @DisplayName("Request Approval Tests")
    class RequestApprovalTests {

        @Test
        @DisplayName("Should set pending approval when requesting approval on draft version")
        void shouldSetPendingApproval_WhenRequestApprovalOnDraft() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.DRAFT);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.requestApproval(PROD_TENANT_ID, SERVICE_NAME, versionId, ACTOR, "Please review");

            // Then
            verify(versionRepository).save(versionCaptor.capture());
            assertThat(versionCaptor.getValue().getStatus())
                    .isEqualTo(TenantServiceConfigVersion.Status.PENDING_APPROVAL);

            verify(approvalRepository).save(approvalCaptor.capture());
            assertThat(approvalCaptor.getValue().getAction())
                    .isEqualTo(TenantServiceConfigApproval.Action.REQUESTED);
        }

        @Test
        @DisplayName("Should throw when requesting approval on superseded version")
        void shouldThrow_WhenRequestApprovalOnSuperseded() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.SUPERSEDED);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));

            // When/Then
            assertThatThrownBy(() ->
                    service.requestApproval(PROD_TENANT_ID, SERVICE_NAME, versionId, ACTOR, "Review please"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("superseded");
        }

        @Test
        @DisplayName("Should record approval audit entry on request")
        void shouldRecordApprovalAuditEntry() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.DRAFT);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.requestApproval(PROD_TENANT_ID, SERVICE_NAME, versionId, ACTOR, "Review needed");

            // Then
            verify(auditRepository).save(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction())
                    .isEqualTo(TenantServiceConfigAudit.Action.APPROVAL_REQUESTED);
            assertThat(auditCaptor.getValue().getActor()).isEqualTo(ACTOR);
        }
    }

    @Nested
    @DisplayName("Query Method Tests")
    class QueryMethodTests {

        @Test
        @DisplayName("Should return audit list when listAudit called")
        void shouldReturnAuditList_WhenListAuditCalled() {
            // Given
            TenantServiceConfigAudit audit1 = new TenantServiceConfigAudit();
            audit1.setTenantId(PROD_TENANT_ID);
            audit1.setServiceName(SERVICE_NAME);
            audit1.setAction(TenantServiceConfigAudit.Action.CREATE);

            TenantServiceConfigAudit audit2 = new TenantServiceConfigAudit();
            audit2.setTenantId(PROD_TENANT_ID);
            audit2.setServiceName(SERVICE_NAME);
            audit2.setAction(TenantServiceConfigAudit.Action.ACTIVATE);

            when(auditRepository.findByTenantIdAndServiceNameOrderByCreatedAtDesc(PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(List.of(audit1, audit2));

            // When
            List<TenantServiceConfigAudit> result = service.listAudit(PROD_TENANT_ID, SERVICE_NAME);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return approval list when listApprovals called")
        void shouldReturnApprovalList_WhenListApprovalsCalled() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigApproval approval1 = new TenantServiceConfigApproval();
            approval1.setAction(TenantServiceConfigApproval.Action.REQUESTED);

            TenantServiceConfigApproval approval2 = new TenantServiceConfigApproval();
            approval2.setAction(TenantServiceConfigApproval.Action.APPROVED);

            when(approvalRepository.findByTenantIdAndServiceNameAndVersionIdOrderByCreatedAtAsc(
                    PROD_TENANT_ID, SERVICE_NAME, versionId))
                    .thenReturn(List.of(approval1, approval2));

            // When
            List<TenantServiceConfigApproval> result = service.listApprovals(
                    PROD_TENANT_ID, SERVICE_NAME, versionId);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return version when found")
        void shouldReturnVersion_WhenFound() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));

            // When
            Optional<TenantServiceConfigVersion> result = service.getVersion(
                    PROD_TENANT_ID, SERVICE_NAME, versionId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(versionId);
        }

        @Test
        @DisplayName("Should return empty when version not found")
        void shouldReturnEmpty_WhenVersionNotFound() {
            // Given
            UUID versionId = UUID.randomUUID();

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.empty());

            // When
            Optional<TenantServiceConfigVersion> result = service.getVersion(
                    PROD_TENANT_ID, SERVICE_NAME, versionId);

            // Then
            assertThat(result).isEmpty();
        }
    }
}

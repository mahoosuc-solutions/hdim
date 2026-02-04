package com.healthdata.gateway.admin.configversion;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantServiceConfigService.
 * Tests configuration versioning, approval workflows, and promotion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant Service Config Service Tests")
@Tag("unit")
class TenantServiceConfigServiceTest {

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
    private ArgumentCaptor<TenantServiceConfigAudit> auditCaptor;

    @Captor
    private ArgumentCaptor<TenantServiceConfigApproval> approvalCaptor;

    private TenantServiceConfigService service;
    private ObjectMapper objectMapper;
    private ConfigPromotionProperties promotionProperties;

    private static final String DEMO_TENANT_ID = "demo-tenant";
    private static final String PROD_TENANT_ID = "prod-tenant";
    private static final String SERVICE_NAME = "test-service";
    private static final String ACTOR = "admin@test.com";
    private static final String SECOND_ACTOR = "reviewer@test.com";

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

    private JsonNode createTestConfig() {
        return objectMapper.createObjectNode()
                .put("timeout", 5000)
                .put("retries", 3);
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
    @DisplayName("Create Version Tests")
    class CreateVersionTests {

        @Test
        @DisplayName("Should create version for demo tenant")
        void shouldCreateVersionForDemoTenant() {
            // Given
            JsonNode config = createTestConfig();
            when(versionRepository.findTopByTenantIdAndServiceNameOrderByVersionNumberDesc(DEMO_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.empty());
            when(versionRepository.save(any(TenantServiceConfigVersion.class)))
                    .thenAnswer(inv -> {
                        TenantServiceConfigVersion v = inv.getArgument(0);
                        v.setId(UUID.randomUUID());
                        return v;
                    });

            // When
            TenantServiceConfigVersion result = service.createVersion(
                    DEMO_TENANT_ID, SERVICE_NAME, config, "Initial config", false, ACTOR);

            // Then
            assertThat(result).isNotNull();
            verify(versionRepository).save(versionCaptor.capture());
            TenantServiceConfigVersion saved = versionCaptor.getValue();
            assertThat(saved.getTenantId()).isEqualTo(DEMO_TENANT_ID);
            assertThat(saved.getVersionNumber()).isEqualTo(1);
            assertThat(saved.getStatus()).isEqualTo(TenantServiceConfigVersion.Status.DRAFT);
        }

        @Test
        @DisplayName("Should increment version number")
        void shouldIncrementVersionNumber() {
            // Given
            JsonNode config = createTestConfig();
            TenantServiceConfigVersion existingVersion = createVersion(UUID.randomUUID(), DEMO_TENANT_ID, 3);
            when(versionRepository.findTopByTenantIdAndServiceNameOrderByVersionNumberDesc(DEMO_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(existingVersion));
            when(versionRepository.save(any(TenantServiceConfigVersion.class)))
                    .thenAnswer(inv -> {
                        TenantServiceConfigVersion v = inv.getArgument(0);
                        v.setId(UUID.randomUUID());
                        return v;
                    });

            // When
            service.createVersion(DEMO_TENANT_ID, SERVICE_NAME, config, "Update", false, ACTOR);

            // Then
            verify(versionRepository).save(versionCaptor.capture());
            assertThat(versionCaptor.getValue().getVersionNumber()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should reject non-demo tenant when requireDemo is true")
        void shouldRejectNonDemoTenantWhenRequired() {
            // Given
            JsonNode config = createTestConfig();

            // When/Then
            assertThatThrownBy(() ->
                    service.createVersion(PROD_TENANT_ID, SERVICE_NAME, config, "Config", false, ACTOR))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("demo tenant");
        }

        @Test
        @DisplayName("Should record audit on create")
        void shouldRecordAuditOnCreate() {
            // Given
            JsonNode config = createTestConfig();
            when(versionRepository.findTopByTenantIdAndServiceNameOrderByVersionNumberDesc(DEMO_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.empty());
            when(versionRepository.save(any(TenantServiceConfigVersion.class)))
                    .thenAnswer(inv -> {
                        TenantServiceConfigVersion v = inv.getArgument(0);
                        v.setId(UUID.randomUUID());
                        return v;
                    });

            // When
            service.createVersion(DEMO_TENANT_ID, SERVICE_NAME, config, "Initial config", false, ACTOR);

            // Then
            verify(auditRepository).save(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo(TenantServiceConfigAudit.Action.CREATE);
            assertThat(auditCaptor.getValue().getActor()).isEqualTo(ACTOR);
        }
    }

    @Nested
    @DisplayName("Promotion Tests")
    class PromotionTests {

        @Test
        @DisplayName("Should promote active version from demo to prod with pending approval")
        void shouldPromoteWithPendingApproval() {
            // Given
            UUID sourceVersionId = UUID.randomUUID();
            TenantServiceConfigVersion sourceVersion = createVersion(sourceVersionId, DEMO_TENANT_ID, 1);
            sourceVersion.setStatus(TenantServiceConfigVersion.Status.ACTIVE);

            when(versionRepository.findById(sourceVersionId)).thenReturn(Optional.of(sourceVersion));
            when(versionRepository.findTopByTenantIdAndServiceNameOrderByVersionNumberDesc(PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.empty());
            when(versionRepository.save(any(TenantServiceConfigVersion.class)))
                    .thenAnswer(inv -> {
                        TenantServiceConfigVersion v = inv.getArgument(0);
                        v.setId(UUID.randomUUID());
                        return v;
                    });

            // When
            TenantServiceConfigVersion result = service.promoteFromDemo(
                    PROD_TENANT_ID, SERVICE_NAME, sourceVersionId, "Promote to prod", true, ACTOR);

            // Then
            assertThat(result).isNotNull();
            verify(versionRepository, times(1)).save(versionCaptor.capture());
            TenantServiceConfigVersion saved = versionCaptor.getValue();
            assertThat(saved.getTenantId()).isEqualTo(PROD_TENANT_ID);
            assertThat(saved.getStatus()).isEqualTo(TenantServiceConfigVersion.Status.PENDING_APPROVAL);
        }

        @Test
        @DisplayName("Should reject promotion from non-demo tenant")
        void shouldRejectPromotionFromNonDemoTenant() {
            // Given
            UUID sourceVersionId = UUID.randomUUID();
            TenantServiceConfigVersion sourceVersion = createVersion(sourceVersionId, PROD_TENANT_ID, 1);
            sourceVersion.setStatus(TenantServiceConfigVersion.Status.ACTIVE);

            when(versionRepository.findById(sourceVersionId)).thenReturn(Optional.of(sourceVersion));

            // When/Then
            assertThatThrownBy(() ->
                    service.promoteFromDemo(PROD_TENANT_ID, SERVICE_NAME, sourceVersionId, "Bad promote", true, ACTOR))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("demo tenant");
        }

        @Test
        @DisplayName("Should reject promotion of inactive source when required")
        void shouldRejectPromotionOfInactiveSource() {
            // Given
            UUID sourceVersionId = UUID.randomUUID();
            TenantServiceConfigVersion sourceVersion = createVersion(sourceVersionId, DEMO_TENANT_ID, 1);
            sourceVersion.setStatus(TenantServiceConfigVersion.Status.DRAFT); // Not active

            when(versionRepository.findById(sourceVersionId)).thenReturn(Optional.of(sourceVersion));

            // When/Then
            assertThatThrownBy(() ->
                    service.promoteFromDemo(PROD_TENANT_ID, SERVICE_NAME, sourceVersionId, "Promote draft", true, ACTOR))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("active");
        }
    }

    @Nested
    @DisplayName("Approval Workflow Tests")
    class ApprovalWorkflowTests {

        @Test
        @DisplayName("Should request approval and update status")
        void shouldRequestApprovalAndUpdateStatus() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);

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
        @DisplayName("Should reject self-approval")
        void shouldRejectSelfApproval() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.PENDING_APPROVAL);
            version.setCreatedBy(ACTOR);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));

            // When/Then
            assertThatThrownBy(() ->
                    service.approveVersion(PROD_TENANT_ID, SERVICE_NAME, versionId, ACTOR, "Self-approve"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("different user");
        }

        @Test
        @DisplayName("Should allow approval by different user")
        void shouldAllowApprovalByDifferentUser() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.PENDING_APPROVAL);
            version.setCreatedBy(ACTOR);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));
            when(approvalRepository.existsByTenantIdAndServiceNameAndVersionIdAndActorAndAction(
                    any(), any(), any(), any(), any())).thenReturn(false);
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRepository.countDistinctActors(any(), any(), any(), any())).thenReturn(1L);
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            TenantServiceConfigApproval result = service.approveVersion(
                    PROD_TENANT_ID, SERVICE_NAME, versionId, SECOND_ACTOR, "Approved");

            // Then
            assertThat(result).isNotNull();
            verify(approvalRepository).save(approvalCaptor.capture());
            assertThat(approvalCaptor.getValue().getAction())
                    .isEqualTo(TenantServiceConfigApproval.Action.APPROVED);
            assertThat(approvalCaptor.getValue().getActor()).isEqualTo(SECOND_ACTOR);
        }

        @Test
        @DisplayName("Should reject duplicate approval by same user")
        void shouldRejectDuplicateApproval() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.PENDING_APPROVAL);
            version.setCreatedBy(ACTOR);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));
            when(approvalRepository.existsByTenantIdAndServiceNameAndVersionIdAndActorAndAction(
                    PROD_TENANT_ID, SERVICE_NAME, versionId, SECOND_ACTOR, TenantServiceConfigApproval.Action.APPROVED))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() ->
                    service.approveVersion(PROD_TENANT_ID, SERVICE_NAME, versionId, SECOND_ACTOR, "Double approve"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already recorded");
        }

        @Test
        @DisplayName("Should reject already active version")
        void shouldRejectAlreadyActiveVersion() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.ACTIVE);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));

            // When/Then
            assertThatThrownBy(() ->
                    service.approveVersion(PROD_TENANT_ID, SERVICE_NAME, versionId, SECOND_ACTOR, "Approve active"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already active");
        }
    }

    @Nested
    @DisplayName("Rejection Tests")
    class RejectionTests {

        @Test
        @DisplayName("Should reject version and update status")
        void shouldRejectVersionAndUpdateStatus() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.PENDING_APPROVAL);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));
            when(approvalRepository.existsByTenantIdAndServiceNameAndVersionIdAndActorAndAction(
                    any(), any(), any(), any(), eq(TenantServiceConfigApproval.Action.REJECTED)))
                    .thenReturn(false);
            when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            TenantServiceConfigApproval result = service.rejectVersion(
                    PROD_TENANT_ID, SERVICE_NAME, versionId, SECOND_ACTOR, "Security concerns");

            // Then
            verify(versionRepository).save(versionCaptor.capture());
            assertThat(versionCaptor.getValue().getStatus())
                    .isEqualTo(TenantServiceConfigVersion.Status.REJECTED);

            verify(approvalRepository).save(approvalCaptor.capture());
            assertThat(approvalCaptor.getValue().getAction())
                    .isEqualTo(TenantServiceConfigApproval.Action.REJECTED);
        }

        @Test
        @DisplayName("Should not allow rejection of active version")
        void shouldNotAllowRejectionOfActiveVersion() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.ACTIVE);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));

            // When/Then
            assertThatThrownBy(() ->
                    service.rejectVersion(PROD_TENANT_ID, SERVICE_NAME, versionId, ACTOR, "Reject active"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("active");
        }
    }

    @Nested
    @DisplayName("Activation Tests")
    class ActivationTests {

        @Test
        @DisplayName("Should activate approved version")
        void shouldActivateApprovedVersion() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.APPROVED);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));
            when(approvalRepository.countDistinctActors(any(), any(), any(), any())).thenReturn(1L);
            when(currentRepository.findByTenantIdAndServiceName(PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.empty());
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(currentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            TenantServiceConfigVersion result = service.activateVersion(
                    PROD_TENANT_ID, SERVICE_NAME, versionId, ACTOR, "Activating");

            // Then
            verify(versionRepository).save(versionCaptor.capture());
            assertThat(versionCaptor.getValue().getStatus())
                    .isEqualTo(TenantServiceConfigVersion.Status.ACTIVE);
        }

        @Test
        @DisplayName("Should supersede previous active version")
        void shouldSupersedePreviousActiveVersion() {
            // Given
            UUID oldVersionId = UUID.randomUUID();
            UUID newVersionId = UUID.randomUUID();

            TenantServiceConfigVersion oldVersion = createVersion(oldVersionId, PROD_TENANT_ID, 1);
            oldVersion.setStatus(TenantServiceConfigVersion.Status.ACTIVE);

            TenantServiceConfigVersion newVersion = createVersion(newVersionId, PROD_TENANT_ID, 2);
            newVersion.setStatus(TenantServiceConfigVersion.Status.APPROVED);

            TenantServiceConfigCurrent current = new TenantServiceConfigCurrent();
            current.setActiveVersionId(oldVersionId);

            when(versionRepository.findByIdAndTenantIdAndServiceName(newVersionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(newVersion));
            when(approvalRepository.countDistinctActors(any(), any(), any(), any())).thenReturn(1L);
            when(currentRepository.findByTenantIdAndServiceName(PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(current));
            when(versionRepository.findById(oldVersionId)).thenReturn(Optional.of(oldVersion));
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(currentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.activateVersion(PROD_TENANT_ID, SERVICE_NAME, newVersionId, ACTOR, "Upgrade");

            // Then
            verify(versionRepository, times(2)).save(versionCaptor.capture());
            List<TenantServiceConfigVersion> savedVersions = versionCaptor.getAllValues();

            // First save is the old version being superseded
            assertThat(savedVersions.get(0).getStatus())
                    .isEqualTo(TenantServiceConfigVersion.Status.SUPERSEDED);
            // Second save is the new version being activated
            assertThat(savedVersions.get(1).getStatus())
                    .isEqualTo(TenantServiceConfigVersion.Status.ACTIVE);
        }

        @Test
        @DisplayName("Should reject activation of rejected version")
        void shouldRejectActivationOfRejectedVersion() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.REJECTED);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));

            // When/Then
            assertThatThrownBy(() ->
                    service.activateVersion(PROD_TENANT_ID, SERVICE_NAME, versionId, ACTOR, "Activate rejected"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("rejected");
        }

        @Test
        @DisplayName("Should require approval before activation")
        void shouldRequireApprovalBeforeActivation() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.PENDING_APPROVAL);

            when(versionRepository.findByIdAndTenantIdAndServiceName(versionId, PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(version));
            when(approvalRepository.countDistinctActors(any(), any(), any(), any())).thenReturn(0L);

            // When/Then
            assertThatThrownBy(() ->
                    service.activateVersion(PROD_TENANT_ID, SERVICE_NAME, versionId, ACTOR, "Activate"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("approval required");
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should list versions for tenant and service")
        void shouldListVersionsForTenantAndService() {
            // Given
            List<TenantServiceConfigVersion> versions = List.of(
                    createVersion(UUID.randomUUID(), PROD_TENANT_ID, 2),
                    createVersion(UUID.randomUUID(), PROD_TENANT_ID, 1)
            );
            when(versionRepository.findByTenantIdAndServiceNameOrderByVersionNumberDesc(PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(versions);

            // When
            List<TenantServiceConfigVersion> result = service.listVersions(PROD_TENANT_ID, SERVICE_NAME);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getVersionNumber()).isEqualTo(2);
            assertThat(result.get(1).getVersionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get current active version")
        void shouldGetCurrentActiveVersion() {
            // Given
            UUID versionId = UUID.randomUUID();
            TenantServiceConfigVersion version = createVersion(versionId, PROD_TENANT_ID, 1);
            version.setStatus(TenantServiceConfigVersion.Status.ACTIVE);

            TenantServiceConfigCurrent current = new TenantServiceConfigCurrent();
            current.setActiveVersionId(versionId);

            when(currentRepository.findByTenantIdAndServiceName(PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.of(current));
            when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));

            // When
            Optional<TenantServiceConfigVersion> result = service.getCurrentVersion(PROD_TENANT_ID, SERVICE_NAME);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(TenantServiceConfigVersion.Status.ACTIVE);
        }

        @Test
        @DisplayName("Should return empty when no current version exists")
        void shouldReturnEmptyWhenNoCurrentVersion() {
            // Given
            when(currentRepository.findByTenantIdAndServiceName(PROD_TENANT_ID, SERVICE_NAME))
                    .thenReturn(Optional.empty());

            // When
            Optional<TenantServiceConfigVersion> result = service.getCurrentVersion(PROD_TENANT_ID, SERVICE_NAME);

            // Then
            assertThat(result).isEmpty();
        }
    }
}

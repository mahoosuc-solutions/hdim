package com.healthdata.agent.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentContext Tests")
class AgentContextTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build context with all fields")
        void buildWithAllFields() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .sessionId("session-789")
                .correlationId("corr-001")
                .roles(Set.of("ADMIN", "CLINICAL_USER"))
                .permissions(Set.of("read:patients", "write:patients"))
                .patientId("patient-001")
                .encounterId("encounter-001")
                .agentId("agent-001")
                .agentType("clinical-decision")
                .origin("web")
                .dataRegion("us-east-1")
                .aiDataSharingConsented(true)
                .consentedJurisdictions(Set.of("US", "EU"))
                .build();

            assertThat(context.getTenantId()).isEqualTo("tenant-123");
            assertThat(context.getUserId()).isEqualTo("user-456");
            assertThat(context.getSessionId()).isEqualTo("session-789");
            assertThat(context.getCorrelationId()).isEqualTo("corr-001");
            assertThat(context.getRoles()).containsExactlyInAnyOrder("ADMIN", "CLINICAL_USER");
            assertThat(context.getPermissions()).containsExactlyInAnyOrder("read:patients", "write:patients");
            assertThat(context.getPatientId()).isEqualTo("patient-001");
            assertThat(context.getEncounterId()).isEqualTo("encounter-001");
            assertThat(context.getAgentId()).isEqualTo("agent-001");
            assertThat(context.getAgentType()).isEqualTo("clinical-decision");
            assertThat(context.getOrigin()).isEqualTo("web");
            assertThat(context.getDataRegion()).isEqualTo("us-east-1");
            assertThat(context.isAiDataSharingConsented()).isTrue();
            assertThat(context.getConsentedJurisdictions()).containsExactlyInAnyOrder("US", "EU");
        }

        @Test
        @DisplayName("should have default values")
        void defaultValues() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build();

            assertThat(context.getRoles()).isEmpty();
            assertThat(context.getPermissions()).isEmpty();
            assertThat(context.getMetadata()).isEmpty();
            assertThat(context.isAiDataSharingConsented()).isFalse();
            assertThat(context.getConsentedJurisdictions()).isEmpty();
            assertThat(context.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should create from security context")
        void fromSecurityContext() {
            AgentContext context = AgentContext.fromSecurityContext(
                "tenant-123",
                "user-456",
                Set.of("CLINICAL_USER")
            ).build();

            assertThat(context.getTenantId()).isEqualTo("tenant-123");
            assertThat(context.getUserId()).isEqualTo("user-456");
            assertThat(context.getRoles()).containsExactly("CLINICAL_USER");
            assertThat(context.getCorrelationId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Permission Tests")
    class PermissionTests {

        @Test
        @DisplayName("should check if has specific permission")
        void hasPermission() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .permissions(Set.of("read:patients", "write:patients"))
                .build();

            assertThat(context.hasPermission("read:patients")).isTrue();
            assertThat(context.hasPermission("delete:patients")).isFalse();
        }

        @Test
        @DisplayName("should allow wildcard permission")
        void wildcardPermission() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .permissions(Set.of("*"))
                .build();

            assertThat(context.hasPermission("read:anything")).isTrue();
            assertThat(context.hasPermission("delete:anything")).isTrue();
        }
    }

    @Nested
    @DisplayName("Role Tests")
    class RoleTests {

        @Test
        @DisplayName("should check if has specific role")
        void hasRole() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .roles(Set.of("ADMIN", "CLINICAL_USER"))
                .build();

            assertThat(context.hasRole("ADMIN")).isTrue();
            assertThat(context.hasRole("SUPER_ADMIN")).isFalse();
        }

        @Test
        @DisplayName("should check if has any of roles")
        void hasAnyRole() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .roles(Set.of("VIEWER"))
                .build();

            assertThat(context.hasAnyRole("ADMIN", "VIEWER")).isTrue();
            assertThat(context.hasAnyRole("ADMIN", "SUPER_ADMIN")).isFalse();
        }
    }

    @Nested
    @DisplayName("Validity Tests")
    class ValidityTests {

        @Test
        @DisplayName("should be valid when tenantId and userId are set")
        void validContext() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build();

            assertThat(context.isValid()).isTrue();
        }

        @Test
        @DisplayName("should be invalid when tenantId is missing")
        void invalidWithoutTenantId() {
            AgentContext context = AgentContext.builder()
                .userId("user-456")
                .build();

            assertThat(context.isValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when userId is missing")
        void invalidWithoutUserId() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .build();

            assertThat(context.isValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when expired")
        void invalidWhenExpired() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

            assertThat(context.isExpired()).isTrue();
            assertThat(context.isValid()).isFalse();
        }

        @Test
        @DisplayName("should be valid when not yet expired")
        void validWhenNotExpired() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

            assertThat(context.isExpired()).isFalse();
            assertThat(context.isValid()).isTrue();
        }

        @Test
        @DisplayName("should not be expired when expiresAt is null")
        void notExpiredWhenNull() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build();

            assertThat(context.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Patient Context Tests")
    class PatientContextTests {

        @Test
        @DisplayName("should detect patient-specific context")
        void isPatientContext() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .patientId("patient-001")
                .build();

            assertThat(context.isPatientContext()).isTrue();
        }

        @Test
        @DisplayName("should not be patient context when patientId is null")
        void notPatientContextWhenNull() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build();

            assertThat(context.isPatientContext()).isFalse();
        }
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("should get metadata value with correct type")
        void getMetadataWithType() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build()
                .withMetadata("count", 42)
                .withMetadata("name", "test");

            Integer count = context.getMetadata("count", Integer.class);
            String name = context.getMetadata("name", String.class);

            assertThat(count).isEqualTo(42);
            assertThat(name).isEqualTo("test");
        }

        @Test
        @DisplayName("should return null for non-existent metadata")
        void getMetadataNonExistent() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build();

            String value = context.getMetadata("nonExistent", String.class);

            assertThat(value).isNull();
        }

        @Test
        @DisplayName("should return null for wrong type")
        void getMetadataWrongType() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build()
                .withMetadata("count", "not a number");

            Integer count = context.getMetadata("count", Integer.class);

            assertThat(count).isNull();
        }

        @Test
        @DisplayName("should chain metadata additions")
        void chainMetadata() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build()
                .withMetadata("key1", "value1")
                .withMetadata("key2", "value2")
                .withMetadata("key3", "value3");

            assertThat(context.getMetadata()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Child Context Tests")
    class ChildContextTests {

        @Test
        @DisplayName("should create child context with new correlation ID")
        void createChildContext() {
            AgentContext parent = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .sessionId("session-789")
                .correlationId("parent-corr")
                .roles(Set.of("ADMIN"))
                .permissions(Set.of("read:all"))
                .patientId("patient-001")
                .agentType("clinical-decision")
                .build()
                .withMetadata("key", "value");

            AgentContext child = parent.createChildContext("child-corr");

            // Should inherit these
            assertThat(child.getTenantId()).isEqualTo("tenant-123");
            assertThat(child.getUserId()).isEqualTo("user-456");
            assertThat(child.getSessionId()).isEqualTo("session-789");
            assertThat(child.getRoles()).containsExactly("ADMIN");
            assertThat(child.getPermissions()).containsExactly("read:all");
            assertThat(child.getPatientId()).isEqualTo("patient-001");
            assertThat(child.getAgentType()).isEqualTo("clinical-decision");

            // Should have new values
            assertThat(child.getCorrelationId()).isEqualTo("child-corr");
            assertThat(child.getCreatedAt()).isNotEqualTo(parent.getCreatedAt());

            // Metadata should be copied (not shared)
            child.withMetadata("childKey", "childValue");
            assertThat(parent.getMetadata()).doesNotContainKey("childKey");
        }
    }
}

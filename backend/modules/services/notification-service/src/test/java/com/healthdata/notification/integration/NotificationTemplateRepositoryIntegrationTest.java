package com.healthdata.notification.integration;

import com.healthdata.notification.config.BaseIntegrationTest;
import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationTemplate;
import com.healthdata.notification.domain.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notification Template Repository Integration Tests
 *
 * Tests database operations for notification template persistence with real PostgreSQL
 * via Testcontainers. Covers:
 * - Basic CRUD operations
 * - Template code uniqueness per tenant
 * - Active/inactive template filtering
 * - Channel-based filtering
 * - Multi-tenant data isolation
 */
@BaseIntegrationTest
@DisplayName("NotificationTemplateRepository Integration Tests")
class NotificationTemplateRepositoryIntegrationTest {

    @Autowired
    private NotificationTemplateRepository templateRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";

    private NotificationTemplate emailTemplate;
    private NotificationTemplate smsTemplate;
    private NotificationTemplate inactiveTemplate;

    @BeforeEach
    void setUp() {
        emailTemplate = createTemplate(TENANT_ID, "CARE_GAP_ALERT", "Care Gap Alert Email",
                NotificationChannel.EMAIL, true);
        smsTemplate = createTemplate(TENANT_ID, "APPOINTMENT_REMINDER", "Appointment Reminder SMS",
                NotificationChannel.SMS, true);
        inactiveTemplate = createTemplate(TENANT_ID, "OLD_TEMPLATE", "Old Template",
                NotificationChannel.EMAIL, false);

        emailTemplate = templateRepository.save(emailTemplate);
        smsTemplate = templateRepository.save(smsTemplate);
        inactiveTemplate = templateRepository.save(inactiveTemplate);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve template by ID")
        void shouldSaveAndRetrieve() {
            Optional<NotificationTemplate> found = templateRepository.findById(emailTemplate.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getCode()).isEqualTo("CARE_GAP_ALERT");
            assertThat(found.get().getName()).isEqualTo("Care Gap Alert Email");
        }

        @Test
        @DisplayName("Should find template by ID and tenant")
        void shouldFindByIdAndTenant() {
            Optional<NotificationTemplate> found = templateRepository.findByIdAndTenantId(
                    emailTemplate.getId(), TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should not find template with wrong tenant")
        void shouldNotFindWithWrongTenant() {
            Optional<NotificationTemplate> found = templateRepository.findByIdAndTenantId(
                    emailTemplate.getId(), OTHER_TENANT);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should update template")
        void shouldUpdate() {
            emailTemplate.setSubjectTemplate("Updated Subject: {{patientName}}");
            templateRepository.save(emailTemplate);

            Optional<NotificationTemplate> found = templateRepository.findById(emailTemplate.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getSubjectTemplate()).contains("Updated Subject");
        }

        @Test
        @DisplayName("Should delete template")
        void shouldDelete() {
            UUID id = emailTemplate.getId();
            templateRepository.delete(emailTemplate);

            Optional<NotificationTemplate> found = templateRepository.findById(id);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should auto-generate timestamp on create")
        void shouldAutoGenerateTimestamp() {
            NotificationTemplate newTemplate = createTemplate(TENANT_ID, "NEW_TEMPLATE", "New Template",
                    NotificationChannel.PUSH, true);
            newTemplate.setCreatedAt(null);

            NotificationTemplate saved = templateRepository.save(newTemplate);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Code-Based Queries")
    class CodeBasedQueryTests {

        @Test
        @DisplayName("Should find template by tenant and code")
        void shouldFindByTenantAndCode() {
            Optional<NotificationTemplate> found = templateRepository.findByTenantIdAndCode(
                    TENANT_ID, "CARE_GAP_ALERT");

            assertThat(found).isPresent();
            assertThat(found.get().getCode()).isEqualTo("CARE_GAP_ALERT");
        }

        @Test
        @DisplayName("Should find active template by tenant and code")
        void shouldFindActiveByTenantAndCode() {
            Optional<NotificationTemplate> found = templateRepository.findByTenantIdAndCodeAndActiveTrue(
                    TENANT_ID, "CARE_GAP_ALERT");

            assertThat(found).isPresent();
            assertThat(found.get().getActive()).isTrue();
        }

        @Test
        @DisplayName("Should not find inactive template when querying for active")
        void shouldNotFindInactiveTemplate() {
            Optional<NotificationTemplate> found = templateRepository.findByTenantIdAndCodeAndActiveTrue(
                    TENANT_ID, "OLD_TEMPLATE");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should check if template code exists")
        void shouldCheckTemplateCodeExists() {
            boolean exists = templateRepository.existsByTenantIdAndCode(TENANT_ID, "CARE_GAP_ALERT");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existent template code")
        void shouldReturnFalseForNonExistentCode() {
            boolean exists = templateRepository.existsByTenantIdAndCode(TENANT_ID, "NON_EXISTENT");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Paginated Queries")
    class PaginatedQueryTests {

        @Test
        @DisplayName("Should find templates by tenant with pagination")
        void shouldFindByTenantWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationTemplate> page = templateRepository.findByTenantId(TENANT_ID, pageable);

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find only active templates")
        void shouldFindOnlyActiveTemplates() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationTemplate> page = templateRepository.findByTenantIdAndActiveTrue(TENANT_ID, pageable);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).allMatch(t -> t.getActive());
        }
    }

    @Nested
    @DisplayName("Channel-Based Queries")
    class ChannelBasedQueryTests {

        @Test
        @DisplayName("Should find templates by channel")
        void shouldFindByChannel() {
            List<NotificationTemplate> emailTemplates = templateRepository.findByTenantIdAndChannel(
                    TENANT_ID, NotificationChannel.EMAIL);

            assertThat(emailTemplates).hasSize(2); // emailTemplate and inactiveTemplate
            assertThat(emailTemplates).allMatch(t -> t.getChannel() == NotificationChannel.EMAIL);
        }

        @Test
        @DisplayName("Should find only active templates by channel")
        void shouldFindActiveByChannel() {
            List<NotificationTemplate> emailTemplates = templateRepository.findByTenantIdAndChannelAndActiveTrue(
                    TENANT_ID, NotificationChannel.EMAIL);

            assertThat(emailTemplates).hasSize(1);
            assertThat(emailTemplates.get(0).getActive()).isTrue();
        }

        @Test
        @DisplayName("Should support all channel types")
        void shouldSupportAllChannelTypes() {
            NotificationTemplate pushTemplate = createTemplate(TENANT_ID, "PUSH_ALERT", "Push Alert",
                    NotificationChannel.PUSH, true);
            NotificationTemplate inAppTemplate = createTemplate(TENANT_ID, "IN_APP_ALERT", "In-App Alert",
                    NotificationChannel.IN_APP, true);
            templateRepository.save(pushTemplate);
            templateRepository.save(inAppTemplate);

            List<NotificationTemplate> pushTemplates = templateRepository.findByTenantIdAndChannel(
                    TENANT_ID, NotificationChannel.PUSH);
            List<NotificationTemplate> inAppTemplates = templateRepository.findByTenantIdAndChannel(
                    TENANT_ID, NotificationChannel.IN_APP);

            assertThat(pushTemplates).hasSize(1);
            assertThat(inAppTemplates).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Template Variables")
    class TemplateVariableTests {

        @Test
        @DisplayName("Should store and retrieve template variables")
        void shouldStoreAndRetrieveVariables() {
            emailTemplate.setVariables(List.of("patientName", "measureName", "dueDate"));
            templateRepository.save(emailTemplate);

            Optional<NotificationTemplate> found = templateRepository.findById(emailTemplate.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getVariables()).containsExactly("patientName", "measureName", "dueDate");
        }

        @Test
        @DisplayName("Should store HTML template content")
        void shouldStoreHtmlTemplate() {
            String htmlContent = "<html><body><h1>Hello {{patientName}}</h1><p>{{body}}</p></body></html>";
            emailTemplate.setHtmlTemplate(htmlContent);
            templateRepository.save(emailTemplate);

            Optional<NotificationTemplate> found = templateRepository.findById(emailTemplate.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getHtmlTemplate()).contains("{{patientName}}");
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate templates between tenants")
        void shouldIsolateTemplatesBetweenTenants() {
            NotificationTemplate otherTenantTemplate = createTemplate(OTHER_TENANT, "CARE_GAP_ALERT",
                    "Other Tenant Template", NotificationChannel.EMAIL, true);
            templateRepository.save(otherTenantTemplate);

            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationTemplate> tenant1Page = templateRepository.findByTenantId(TENANT_ID, pageable);
            Page<NotificationTemplate> tenant2Page = templateRepository.findByTenantId(OTHER_TENANT, pageable);

            assertThat(tenant1Page.getContent()).noneMatch(t -> t.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Page.getContent()).noneMatch(t -> t.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should allow same template code in different tenants")
        void shouldAllowSameCodeInDifferentTenants() {
            NotificationTemplate otherTenantTemplate = createTemplate(OTHER_TENANT, "CARE_GAP_ALERT",
                    "Other Tenant Care Gap Alert", NotificationChannel.EMAIL, true);
            templateRepository.save(otherTenantTemplate);

            Optional<NotificationTemplate> tenant1Template = templateRepository.findByTenantIdAndCode(
                    TENANT_ID, "CARE_GAP_ALERT");
            Optional<NotificationTemplate> tenant2Template = templateRepository.findByTenantIdAndCode(
                    OTHER_TENANT, "CARE_GAP_ALERT");

            assertThat(tenant1Template).isPresent();
            assertThat(tenant2Template).isPresent();
            assertThat(tenant1Template.get().getId()).isNotEqualTo(tenant2Template.get().getId());
        }

        @Test
        @DisplayName("Should not allow cross-tenant access via ID query")
        void shouldNotAllowCrossTenantAccessById() {
            Optional<NotificationTemplate> result = templateRepository.findByIdAndTenantId(
                    emailTemplate.getId(), OTHER_TENANT);

            assertThat(result).isEmpty();
        }
    }

    // Helper method
    private NotificationTemplate createTemplate(String tenantId, String code, String name,
                                                 NotificationChannel channel, boolean active) {
        return NotificationTemplate.builder()
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .description("Template for " + name)
                .channel(channel)
                .subjectTemplate("Subject: {{subject}}")
                .bodyTemplate("Dear {{patientName}}, {{body}}")
                .active(active)
                .version(1)
                .createdBy("test-system")
                .build();
    }
}

package com.healthdata.notification.application;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationTemplate;
import com.healthdata.notification.domain.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TemplateService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateService Tests")
class TemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    private TemplateService templateService;

    private static final String TENANT_ID = "TENANT001";

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateRepository);
    }

    @Nested
    @DisplayName("Template Rendering Tests")
    class TemplateRenderingTests {

        @Test
        @DisplayName("should render template with simple variables")
        void renderTemplateWithSimpleVariables() {
            // Given
            String template = "Hello {{ name }}, your appointment is on {{ date }}.";
            Map<String, Object> variables = Map.of(
                "name", "John",
                "date", "2025-01-15"
            );

            // When
            String result = templateService.renderTemplate(template, variables);

            // Then
            assertThat(result).isEqualTo("Hello John, your appointment is on 2025-01-15.");
        }

        @Test
        @DisplayName("should render template with nested variables")
        void renderTemplateWithNestedVariables() {
            // Given
            String template = "Patient {{ patient.name }} has a care gap: {{ measure.name }}";
            Map<String, Object> variables = Map.of(
                "patient", Map.of("name", "Jane Doe", "id", "P123"),
                "measure", Map.of("name", "HbA1c Screening", "code", "NQF0059")
            );

            // When
            String result = templateService.renderTemplate(template, variables);

            // Then
            assertThat(result).isEqualTo("Patient Jane Doe has a care gap: HbA1c Screening");
        }

        @Test
        @DisplayName("should handle missing variables gracefully")
        void handleMissingVariablesGracefully() {
            // Given
            String template = "Hello {{ name }}, your score is {{ score }}.";
            Map<String, Object> variables = Map.of("name", "John");

            // When
            String result = templateService.renderTemplate(template, variables);

            // Then
            assertThat(result).isEqualTo("Hello John, your score is .");
        }

        @Test
        @DisplayName("should return original template when variables are null")
        void returnOriginalTemplateWhenVariablesNull() {
            // Given
            String template = "Hello {{ name }}";

            // When
            String result = templateService.renderTemplate(template, null);

            // Then
            assertThat(result).isEqualTo(template);
        }

        @Test
        @DisplayName("should return original template when variables are empty")
        void returnOriginalTemplateWhenVariablesEmpty() {
            // Given
            String template = "Hello {{ name }}";

            // When
            String result = templateService.renderTemplate(template, Map.of());

            // Then
            assertThat(result).isEqualTo(template);
        }

        @Test
        @DisplayName("should return null when template is null")
        void returnNullWhenTemplateNull() {
            // When
            String result = templateService.renderTemplate(null, Map.of("name", "John"));

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle deeply nested variables")
        void handleDeeplyNestedVariables() {
            // Given
            String template = "Organization: {{ org.department.name }}";
            Map<String, Object> variables = Map.of(
                "org", Map.of(
                    "department", Map.of("name", "Cardiology")
                )
            );

            // When
            String result = templateService.renderTemplate(template, variables);

            // Then
            assertThat(result).isEqualTo("Organization: Cardiology");
        }
    }

    @Nested
    @DisplayName("Variable Extraction Tests")
    class VariableExtractionTests {

        @Test
        @DisplayName("should extract simple variables")
        void extractSimpleVariables() {
            // Given
            String template = "Hello {{ name }}, your code is {{ code }}.";

            // When
            List<String> variables = templateService.extractVariables(template);

            // Then
            assertThat(variables).containsExactlyInAnyOrder("name", "code");
        }

        @Test
        @DisplayName("should extract nested variable paths")
        void extractNestedVariablePaths() {
            // Given
            String template = "Patient {{ patient.name }} (ID: {{ patient.id }})";

            // When
            List<String> variables = templateService.extractVariables(template);

            // Then
            assertThat(variables).containsExactlyInAnyOrder("patient.name", "patient.id");
        }

        @Test
        @DisplayName("should return empty list for template without variables")
        void returnEmptyListForTemplateWithoutVariables() {
            // Given
            String template = "This is a static message.";

            // When
            List<String> variables = templateService.extractVariables(template);

            // Then
            assertThat(variables).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null template")
        void returnEmptyListForNullTemplate() {
            // When
            List<String> variables = templateService.extractVariables(null);

            // Then
            assertThat(variables).isEmpty();
        }

        @Test
        @DisplayName("should handle variables with extra whitespace")
        void handleVariablesWithExtraWhitespace() {
            // Given
            String template = "Hello {{  name  }} and {{   date   }}";

            // When
            List<String> variables = templateService.extractVariables(template);

            // Then
            assertThat(variables).containsExactlyInAnyOrder("name", "date");
        }
    }

    @Nested
    @DisplayName("Create Template Tests")
    class CreateTemplateTests {

        @Test
        @DisplayName("should create template successfully")
        void createTemplateSuccessfully() {
            // Given
            TemplateService.CreateTemplateRequest request = TemplateService.CreateTemplateRequest.builder()
                .tenantId(TENANT_ID)
                .code("care-gap-alert")
                .name("Care Gap Alert Template")
                .description("Email template for care gap alerts")
                .channel(NotificationChannel.EMAIL)
                .subjectTemplate("Care Gap Alert: {{ measure.name }}")
                .bodyTemplate("Dear {{ patient.name }}, you have an open care gap for {{ measure.name }}.")
                .createdBy("admin")
                .build();

            when(templateRepository.existsByTenantIdAndCode(TENANT_ID, "care-gap-alert")).thenReturn(false);
            when(templateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> {
                    NotificationTemplate t = invocation.getArgument(0);
                    t.setId(UUID.randomUUID());
                    return t;
                });

            // When
            NotificationTemplate result = templateService.createTemplate(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("care-gap-alert");
            assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(result.getActive()).isTrue();

            ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
            verify(templateRepository).save(captor.capture());
            assertThat(captor.getValue().getVariables())
                .containsExactlyInAnyOrder("measure.name", "patient.name");
        }

        @Test
        @DisplayName("should throw exception when template code already exists")
        void throwExceptionWhenTemplateCodeExists() {
            // Given
            TemplateService.CreateTemplateRequest request = TemplateService.CreateTemplateRequest.builder()
                .tenantId(TENANT_ID)
                .code("existing-template")
                .bodyTemplate("Test body")
                .build();

            when(templateRepository.existsByTenantIdAndCode(TENANT_ID, "existing-template")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("Update Template Tests")
    class UpdateTemplateTests {

        @Test
        @DisplayName("should update template successfully")
        void updateTemplateSuccessfully() {
            // Given
            UUID templateId = UUID.randomUUID();
            NotificationTemplate existingTemplate = NotificationTemplate.builder()
                .id(templateId)
                .tenantId(TENANT_ID)
                .code("care-gap-alert")
                .name("Original Name")
                .bodyTemplate("Original body")
                .version(1)
                .build();

            TemplateService.UpdateTemplateRequest request = TemplateService.UpdateTemplateRequest.builder()
                .name("Updated Name")
                .bodyTemplate("Updated body with {{ variable }}")
                .updatedBy("admin")
                .build();

            when(templateRepository.findByIdAndTenantId(templateId, TENANT_ID))
                .thenReturn(Optional.of(existingTemplate));
            when(templateRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            NotificationTemplate result = templateService.updateTemplate(templateId, TENANT_ID, request);

            // Then
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getBodyTemplate()).contains("Updated body");
            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getVariables()).contains("variable");
        }

        @Test
        @DisplayName("should throw exception when template not found")
        void throwExceptionWhenTemplateNotFound() {
            // Given
            UUID templateId = UUID.randomUUID();
            when(templateRepository.findByIdAndTenantId(templateId, TENANT_ID))
                .thenReturn(Optional.empty());

            TemplateService.UpdateTemplateRequest request = TemplateService.UpdateTemplateRequest.builder()
                .name("New Name")
                .build();

            // When/Then
            assertThatThrownBy(() -> templateService.updateTemplate(templateId, TENANT_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Get Template Tests")
    class GetTemplateTests {

        @Test
        @DisplayName("should return template by ID")
        void returnTemplateById() {
            // Given
            UUID templateId = UUID.randomUUID();
            NotificationTemplate template = createTemplate(templateId);

            when(templateRepository.findByIdAndTenantId(templateId, TENANT_ID))
                .thenReturn(Optional.of(template));

            // When
            Optional<NotificationTemplate> result = templateService.getTemplate(templateId, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(templateId);
        }

        @Test
        @DisplayName("should return template by code")
        void returnTemplateByCode() {
            // Given
            NotificationTemplate template = createTemplate(UUID.randomUUID());

            when(templateRepository.findByTenantIdAndCodeAndActiveTrue(TENANT_ID, "care-gap-alert"))
                .thenReturn(Optional.of(template));

            // When
            Optional<NotificationTemplate> result = templateService.getTemplateByCode(TENANT_ID, "care-gap-alert");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo("care-gap-alert");
        }

        @Test
        @DisplayName("should return paginated templates")
        void returnPaginatedTemplates() {
            // Given
            PageRequest pageable = PageRequest.of(0, 10);
            List<NotificationTemplate> templates = List.of(
                createTemplate(UUID.randomUUID()),
                createTemplate(UUID.randomUUID())
            );
            Page<NotificationTemplate> page = new PageImpl<>(templates, pageable, 2);

            when(templateRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

            // When
            Page<NotificationTemplate> result = templateService.getTemplates(TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return templates by channel")
        void returnTemplatesByChannel() {
            // Given
            List<NotificationTemplate> templates = List.of(createTemplate(UUID.randomUUID()));

            when(templateRepository.findByTenantIdAndChannelAndActiveTrue(TENANT_ID, NotificationChannel.EMAIL))
                .thenReturn(templates);

            // When
            List<NotificationTemplate> result = templateService.getTemplatesByChannel(
                TENANT_ID, NotificationChannel.EMAIL);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Delete Template Tests")
    class DeleteTemplateTests {

        @Test
        @DisplayName("should soft delete template by setting active to false")
        void softDeleteTemplate() {
            // Given
            UUID templateId = UUID.randomUUID();
            NotificationTemplate template = createTemplate(templateId);
            template.setActive(true);

            when(templateRepository.findByIdAndTenantId(templateId, TENANT_ID))
                .thenReturn(Optional.of(template));

            // When
            templateService.deleteTemplate(templateId, TENANT_ID);

            // Then
            ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
            verify(templateRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isFalse();
        }

        @Test
        @DisplayName("should throw exception when deleting non-existent template")
        void throwExceptionWhenDeletingNonExistent() {
            // Given
            UUID templateId = UUID.randomUUID();
            when(templateRepository.findByIdAndTenantId(templateId, TENANT_ID))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> templateService.deleteTemplate(templateId, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        }
    }

    private NotificationTemplate createTemplate(UUID id) {
        return NotificationTemplate.builder()
            .id(id)
            .tenantId(TENANT_ID)
            .code("care-gap-alert")
            .name("Care Gap Alert")
            .channel(NotificationChannel.EMAIL)
            .subjectTemplate("Alert: {{ measure }}")
            .bodyTemplate("Dear {{ name }}, please address care gap.")
            .variables(List.of("measure", "name"))
            .active(true)
            .version(1)
            .build();
    }
}

package com.healthdata.agentbuilder.service;

import com.healthdata.agentbuilder.domain.entity.PromptTemplate;
import com.healthdata.agentbuilder.domain.entity.PromptTemplate.TemplateCategory;
import com.healthdata.agentbuilder.domain.entity.PromptTemplate.TemplateVariable;
import com.healthdata.agentbuilder.repository.PromptTemplateRepository;
import com.healthdata.agentbuilder.service.PromptTemplateService.TemplateValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private PromptTemplateRepository templateRepository;

    @InjectMocks
    private PromptTemplateService service;

    private String tenantId;
    private String userId;
    private PromptTemplate testTemplate;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        userId = "test-user";

        testTemplate = PromptTemplate.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .name("Test Template")
            .description("Test Description")
            .category(TemplateCategory.CLINICAL_SAFETY)
            .content("Hello {{patient_name}}, your appointment is on {{date}}.")
            .variables(List.of(
                new TemplateVariable("patient_name", "Patient name", null, true),
                new TemplateVariable("date", "Appointment date", null, true)
            ))
            .isSystem(false)
            .build();
    }

    @Test
    void create_shouldExtractVariablesAndCreateTemplate() {
        // Given
        when(templateRepository.existsByTenantIdAndName(tenantId, testTemplate.getName())).thenReturn(false);
        when(templateRepository.save(any(PromptTemplate.class))).thenReturn(testTemplate);

        // When
        PromptTemplate result = service.create(testTemplate, userId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getVariables());
        assertTrue(result.getVariables().stream().anyMatch(v -> v.getName().equals("patient_name")));
        assertTrue(result.getVariables().stream().anyMatch(v -> v.getName().equals("date")));
        verify(templateRepository).save(any(PromptTemplate.class));
    }

    @Test
    void create_shouldThrowExceptionWhenNameExists() {
        // Given
        when(templateRepository.existsByTenantIdAndName(tenantId, testTemplate.getName())).thenReturn(true);

        // When & Then
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.create(testTemplate, userId));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateTemplateAndReExtractVariables() {
        // Given
        PromptTemplate existing = PromptTemplate.builder()
            .id(testTemplate.getId())
            .tenantId(tenantId)
            .name("Old Template")
            .content("Old {{var1}}")
            .isSystem(false)
            .build();

        PromptTemplate updates = PromptTemplate.builder()
            .tenantId(tenantId)
            .content("New {{var2}} and {{var3}}")
            .build();

        when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
            .thenReturn(Optional.of(existing));
        when(templateRepository.save(any(PromptTemplate.class))).thenReturn(existing);

        // When
        PromptTemplate result = service.update(testTemplate.getId(), updates, userId);

        // Then
        assertNotNull(result);
        assertEquals("New {{var2}} and {{var3}}", result.getContent());
        assertTrue(result.getVariables().stream().anyMatch(v -> v.getName().equals("var2")));
        assertTrue(result.getVariables().stream().anyMatch(v -> v.getName().equals("var3")));
    }

    @Test
    void update_shouldThrowExceptionForSystemTemplate() {
        // Given
        testTemplate.setIsSystem(true);
        when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
            .thenReturn(Optional.of(testTemplate));

        // When & Then
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.update(testTemplate.getId(), testTemplate, userId));
    }

    @Test
    void renderContent_shouldReplaceVariables() {
        // Given
        String content = "Hello {{name}}, you have {{count}} messages.";
        Map<String, String> variables = Map.of(
            "name", "John",
            "count", "5"
        );

        // When
        String result = service.renderContent(content, variables);

        // Then
        assertEquals("Hello John, you have 5 messages.", result);
    }

    @Test
    void renderContent_shouldHandleVariablesWithSpaces() {
        // Given
        String content = "Hello {{ name }}, welcome!";
        Map<String, String> variables = Map.of("name", "Alice");

        // When
        String result = service.renderContent(content, variables);

        // Then
        assertEquals("Hello Alice, welcome!", result);
    }

    @Test
    void renderContent_shouldHandleMissingVariables() {
        // Given
        String content = "Hello {{name}}, you have {{count}} messages.";
        Map<String, String> variables = Map.of("name", "John");

        // When
        String result = service.renderContent(content, variables);

        // Then
        assertEquals("Hello John, you have {{count}} messages.", result);
    }

    @Test
    void validateTemplate_shouldReturnValidForGoodTemplate() {
        // Given
        String content = "Hello {{name}}, your balance is {{balance}}.";

        // When
        TemplateValidationResult result = service.validateTemplate(content);

        // Then
        assertTrue(result.valid());
        assertEquals(2, result.variables().size());
        assertTrue(result.variables().contains("name"));
        assertTrue(result.variables().contains("balance"));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void validateTemplate_shouldDetectUnbalancedBraces() {
        // Given
        String content = "Hello {{name}, missing closing brace";

        // When
        TemplateValidationResult result = service.validateTemplate(content);

        // Then
        assertFalse(result.valid());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.errors().get(0).contains("Unbalanced"));
    }

    @Test
    void validateTemplate_shouldDetectEmptyVariables() {
        // Given
        String content = "Hello {{}}, empty variable";

        // When
        TemplateValidationResult result = service.validateTemplate(content);

        // Then
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Empty variable")));
    }

    @Test
    void validateTemplate_shouldWarnAboutPotentiallyMissingPatientVariable() {
        // Given
        String content = "The patient needs to schedule an appointment.";

        // When
        TemplateValidationResult result = service.validateTemplate(content);

        // Then
        assertTrue(result.valid()); // Still valid, just warnings
        assertFalse(result.warnings().isEmpty());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("patient")));
    }

    @Test
    void delete_shouldDeleteNonSystemTemplate() {
        // Given
        when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
            .thenReturn(Optional.of(testTemplate));

        // When
        service.delete(tenantId, testTemplate.getId());

        // Then
        verify(templateRepository).delete(testTemplate);
    }

    @Test
    void delete_shouldThrowExceptionForSystemTemplate() {
        // Given
        testTemplate.setIsSystem(true);
        when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
            .thenReturn(Optional.of(testTemplate));

        // When & Then
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.delete(tenantId, testTemplate.getId()));
        verify(templateRepository, never()).delete(any());
    }

    @Test
    void cloneTemplate_shouldCloneTenantTemplate() {
        // Given
        String newName = "Cloned Template";
        when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
            .thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(any(PromptTemplate.class))).thenReturn(testTemplate);

        // When
        PromptTemplate result = service.cloneTemplate(tenantId, testTemplate.getId(), newName, userId);

        // Then
        assertNotNull(result);
        verify(templateRepository).save(argThat(template ->
            !template.getIsSystem() &&
            template.getContent().equals(testTemplate.getContent())
        ));
    }

    @Test
    void cloneTemplate_shouldCloneSystemTemplate() {
        // Given
        testTemplate.setIsSystem(true);
        String newName = "Cloned System Template";
        when(templateRepository.findByTenantIdAndId(tenantId, testTemplate.getId()))
            .thenReturn(Optional.empty());
        when(templateRepository.findById(testTemplate.getId()))
            .thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(any(PromptTemplate.class))).thenReturn(testTemplate);

        // When
        PromptTemplate result = service.cloneTemplate(tenantId, testTemplate.getId(), newName, userId);

        // Then
        assertNotNull(result);
        verify(templateRepository).save(argThat(template ->
            !template.getIsSystem() &&
            template.getTenantId().equals(tenantId)
        ));
    }

    @Test
    void getAvailableTemplates_shouldReturnTenantAndSystemTemplates() {
        // Given
        List<PromptTemplate> templates = List.of(testTemplate);
        when(templateRepository.findAvailableTemplates(tenantId)).thenReturn(templates);

        // When
        List<PromptTemplate> result = service.getAvailableTemplates(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(templateRepository).findAvailableTemplates(tenantId);
    }
}

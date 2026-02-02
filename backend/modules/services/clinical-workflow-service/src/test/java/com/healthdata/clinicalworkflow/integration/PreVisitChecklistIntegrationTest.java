package com.healthdata.clinicalworkflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.domain.model.PreVisitChecklistEntity;
import com.healthdata.clinicalworkflow.domain.repository.PreVisitChecklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Pre-Visit Checklist Service (Tier 3 - Validation)
 *
 * Tests pre-visit checklist management end-to-end including:
 * - Complete checklist workflow (create → complete items → verify progress)
 * - Checklist templates by appointment type
 * - Custom checklist items
 * - Critical items identification
 * - Completion progress tracking
 * - Multi-tenant isolation
 * - Concurrent checklist operations
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
@DisplayName("Pre-Visit Checklist Integration Tests")
class PreVisitChecklistIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinical_workflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PreVisitChecklistRepository checklistRepository;

    private static final String TENANT_ID_A = "TENANT_A";
    private static final String TENANT_ID_B = "TENANT_B";
    private static final String USER_ID = "nurse@example.com";
    private static final String PATIENT_ID = UUID.randomUUID().toString();
    private static final String ENCOUNTER_ID = "ENC001";

    @BeforeEach
    void setUp() {
        checklistRepository.deleteAll();
    }

    // ================================
    // Complete Checklist Workflow Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Complete checklist workflow: create → complete items → verify progress")
    void testCompleteChecklistWorkflow() throws Exception {
        // Step 1: Create checklist
        CreateChecklistRequest createRequest = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.encounterId").value(ENCOUNTER_ID))
                .andExpect(jsonPath("$.appointmentType").value("ANNUAL_PHYSICAL"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.completionPercentage").value(0))
                .andReturn();

        String checklistJson = createResult.getResponse().getContentAsString();
        ChecklistResponse checklistResponse = objectMapper.readValue(checklistJson, ChecklistResponse.class);
        UUID checklistId = checklistResponse.getId();

        // Verify entity in database
        Optional<PreVisitChecklistEntity> checklistEntityOpt = checklistRepository.findById(checklistId);
        assertThat(checklistEntityOpt).isPresent();
        PreVisitChecklistEntity checklistEntity = checklistEntityOpt.get();
        assertThat(checklistEntity.getTenantId()).isEqualTo(TENANT_ID_A);
        assertThat(checklistEntity.getPatientId()).isEqualTo(UUID.fromString(PATIENT_ID));

        // Step 2: Complete first checklist item
        List<ChecklistItemResponse> items = checklistResponse.getItems();
        assertThat(items).isNotEmpty();

        String firstItemCode = items.get(0).getItemCode();
        ChecklistItemUpdateRequest updateRequest = ChecklistItemUpdateRequest.builder()
                .itemCode(firstItemCode)
                .completed(true)
                .completionNotes("Check-in completed")
                .build();

        mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/item", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].completed").value(true))
                .andExpect(jsonPath("$.items[0].completedBy").value(USER_ID))
                .andExpect(jsonPath("$.items[0].completedAt").exists());

        // Step 3: Get progress
        mockMvc.perform(get("/api/v1/pre-visit/{checklistId}/progress", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercentage").value(greaterThan(0)))
                .andExpect(jsonPath("$.completedItems").value(1))
                .andExpect(jsonPath("$.totalItems").value(items.size()));

        // Step 4: Complete remaining items
        for (int i = 1; i < items.size(); i++) {
            String itemCode = items.get(i).getItemCode();
            ChecklistItemUpdateRequest completeRequest = ChecklistItemUpdateRequest.builder()
                    .itemCode(itemCode)
                    .completed(true)
                    .completionNotes("Item " + (i + 1) + " completed")
                    .build();

            mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/item", checklistId)
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completeRequest)))
                    .andExpect(status().isOk());
        }

        // Step 5: Verify 100% completion
        mockMvc.perform(get("/api/v1/pre-visit/{checklistId}/progress", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercentage").value(100))
                .andExpect(jsonPath("$.allCompleted").value(true));
    }

    // ================================
    // Checklist Template Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Checklist templates: verify template items match appointment type")
    void testChecklistTemplates() throws Exception {
        // Test ANNUAL_PHYSICAL template
        mockMvc.perform(get("/api/v1/pre-visit/type/{appointmentType}", "ANNUAL_PHYSICAL")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentType").value("ANNUAL_PHYSICAL"))
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));

        // Test SICK_VISIT template
        mockMvc.perform(get("/api/v1/pre-visit/type/{appointmentType}", "SICK_VISIT")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentType").value("SICK_VISIT"))
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));

        // Test FOLLOW_UP template
        mockMvc.perform(get("/api/v1/pre-visit/type/{appointmentType}", "FOLLOW_UP")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentType").value("FOLLOW_UP"))
                .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Create checklist from template")
    void testCreateChecklistFromTemplate() throws Exception {
        // Get template first
        MvcResult templateResult = mockMvc.perform(get("/api/v1/pre-visit/type/{appointmentType}", "ANNUAL_PHYSICAL")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andReturn();

        ChecklistResponse template = objectMapper.readValue(
                templateResult.getResponse().getContentAsString(), ChecklistResponse.class);

        // Create checklist from template
        CreateChecklistRequest createRequest = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ChecklistResponse checklist = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ChecklistResponse.class);

        // Verify checklist has same items as template
        assertThat(checklist.getItems()).hasSameSizeAs(template.getItems());
    }

    // ================================
    // Custom Items Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Custom items: add custom item → verify appears in progress")
    void testCustomChecklistItems() throws Exception {
        // Create checklist
        CreateChecklistRequest createRequest = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ChecklistResponse checklist = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ChecklistResponse.class);
        UUID checklistId = checklist.getId();

        int initialItemCount = checklist.getItems().size();

        // Add custom item
        CustomChecklistItemRequest customItemRequest = CustomChecklistItemRequest.builder()
                .displayName("Special Lab Test")
                .description("Order fasting glucose test")
                .category("CLINICAL")
                .required(true)
                .sequenceNumber(initialItemCount + 1)
                .build();

        mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/custom-item", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customItemRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.displayName == 'Special Lab Test')]").exists());

        // Verify custom item appears in checklist
        mockMvc.perform(get("/api/v1/pre-visit/patient/{patientId}", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(initialItemCount + 1)))
                .andExpect(jsonPath("$.items[?(@.displayName == 'Special Lab Test')]").exists());
    }

    // ================================
    // Critical Items Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Critical items identification: verify getIncompleteCriticalItems returns only critical items")
    void testCriticalItemsIdentification() throws Exception {
        // Create checklist
        CreateChecklistRequest createRequest = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ChecklistResponse checklist = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ChecklistResponse.class);
        UUID checklistId = checklist.getId();

        // Complete non-critical items only
        List<ChecklistItemResponse> items = checklist.getItems();
        for (ChecklistItemResponse item : items) {
            if (Boolean.FALSE.equals(item.getRequired())) {
                ChecklistItemUpdateRequest updateRequest = ChecklistItemUpdateRequest.builder()
                        .itemCode(item.getItemCode())
                        .completed(true)
                        .completionNotes("Non-critical item completed")
                        .build();

                mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/item", checklistId)
                                .header("X-Tenant-ID", TENANT_ID_A)
                                .header("X-User-ID", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isOk());
            }
        }

        // Get incomplete critical items
        mockMvc.perform(get("/api/v1/pre-visit/{checklistId}/critical-items", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].required", everyItem(is(true))))
                .andExpect(jsonPath("$[*].completed", everyItem(is(false))));
    }

    // ================================
    // Multi-Tenant Isolation Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Multi-tenant isolation: tenant A cannot access tenant B's checklists")
    void testMultiTenantChecklistIsolation() throws Exception {
        // Create checklist in tenant A
        CreateChecklistRequest request = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ChecklistResponse checklist = objectMapper.readValue(
                result.getResponse().getContentAsString(), ChecklistResponse.class);
        UUID checklistId = checklist.getId();

        // Try to access from tenant B - should fail
        mockMvc.perform(get("/api/v1/pre-visit/patient/{patientId}", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_B))
                .andExpect(status().isNotFound());

        // Try to update from tenant B - should fail
        ChecklistItemUpdateRequest updateRequest = ChecklistItemUpdateRequest.builder()
                .itemCode(checklist.getItems().get(0).getItemCode())
                .completed(true)
                .build();

        mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/item", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_B)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        // Verify tenant A can still access
        mockMvc.perform(get("/api/v1/pre-visit/patient/{patientId}", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(checklistId.toString()));
    }

    // ================================
    // Concurrent Operations Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Concurrent checklist operations: multiple staff updating checklist items")
    void testConcurrentChecklistOperations() throws Exception {
        // Create checklist
        CreateChecklistRequest createRequest = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ChecklistResponse checklist = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ChecklistResponse.class);
        UUID checklistId = checklist.getId();

        List<ChecklistItemResponse> items = checklist.getItems();
        String[] users = {"nurse1@example.com", "nurse2@example.com", "ma@example.com"};

        // Simulate concurrent updates by different users
        for (int i = 0; i < Math.min(3, items.size()); i++) {
            String itemCode = items.get(i).getItemCode();
            ChecklistItemUpdateRequest updateRequest = ChecklistItemUpdateRequest.builder()
                    .itemCode(itemCode)
                    .completed(true)
                    .completionNotes("Completed by " + users[i])
                    .build();

            mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/item", checklistId)
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", users[i])
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[?(@.itemCode == '" + itemCode + "')].completedBy")
                            .value(hasItem(users[i])));
        }

        // Verify all updates succeeded
        mockMvc.perform(get("/api/v1/pre-visit/{checklistId}/progress", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedItems").value(3));
    }

    // ================================
    // Error Scenario Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Error: Duplicate checklist (patient already has active checklist)")
    void testDuplicateChecklist() throws Exception {
        // Create first checklist
        CreateChecklistRequest request = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to create duplicate - should fail
        mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Error: Checklist not found (404)")
    void testChecklistNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        // Try to get non-existent checklist
        mockMvc.perform(get("/api/v1/pre-visit/{checklistId}/progress", nonExistentId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Error: Invalid template type (404)")
    void testInvalidTemplateType() throws Exception {
        mockMvc.perform(get("/api/v1/pre-visit/type/{appointmentType}", "INVALID_TYPE")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isNotFound());
    }

    // ================================
    // Additional Test Scenarios
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Get patient's active checklist")
    void testGetPatientActiveChecklist() throws Exception {
        // Create checklist
        CreateChecklistRequest request = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ChecklistResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ChecklistResponse.class);

        // Get patient's checklist
        mockMvc.perform(get("/api/v1/pre-visit/patient/{patientId}", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId().toString()))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Completion progress tracking")
    void testCompletionProgressTracking() throws Exception {
        // Create checklist
        CreateChecklistRequest createRequest = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ChecklistResponse checklist = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ChecklistResponse.class);
        UUID checklistId = checklist.getId();

        int totalItems = checklist.getItems().size();

        // Complete half the items
        List<ChecklistItemResponse> items = checklist.getItems();
        int itemsToComplete = totalItems / 2;

        for (int i = 0; i < itemsToComplete; i++) {
            String itemCode = items.get(i).getItemCode();
            ChecklistItemUpdateRequest updateRequest = ChecklistItemUpdateRequest.builder()
                    .itemCode(itemCode)
                    .completed(true)
                    .completionNotes("Item " + (i + 1) + " completed")
                    .build();

            mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/item", checklistId)
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());
        }

        // Verify progress is approximately 50%
        mockMvc.perform(get("/api/v1/pre-visit/{checklistId}/progress", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedItems").value(itemsToComplete))
                .andExpect(jsonPath("$.totalItems").value(totalItems))
                .andExpect(jsonPath("$.completionPercentage").value(closeTo(50, 10)));
    }

    @Test
    @WithMockUser(roles = "PROVIDER")
    @DisplayName("Verify role-based access control")
    void testRoleBasedAccessControl() throws Exception {
        // PROVIDER role can view checklist
        CreateChecklistRequest request = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", "provider@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/pre-visit/patient/{patientId}", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Mark checklist item as incomplete")
    void testMarkItemIncomplete() throws Exception {
        // Create checklist and complete an item
        CreateChecklistRequest createRequest = CreateChecklistRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .appointmentType("ANNUAL_PHYSICAL")
                .useCustomTemplate(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/pre-visit")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ChecklistResponse checklist = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ChecklistResponse.class);
        UUID checklistId = checklist.getId();
        String firstItemCode = checklist.getItems().get(0).getItemCode();

        // Complete item
        ChecklistItemUpdateRequest completeRequest = ChecklistItemUpdateRequest.builder()
                .itemCode(firstItemCode)
                .completed(true)
                .completionNotes("Completed")
                .build();

        mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/item", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].completed").value(true));

        // Mark as incomplete
        ChecklistItemUpdateRequest incompleteRequest = ChecklistItemUpdateRequest.builder()
                .itemCode(firstItemCode)
                .completed(false)
                .completionNotes("Needs to be redone")
                .build();

        mockMvc.perform(put("/api/v1/pre-visit/{checklistId}/item", checklistId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].completed").value(false));
    }
}

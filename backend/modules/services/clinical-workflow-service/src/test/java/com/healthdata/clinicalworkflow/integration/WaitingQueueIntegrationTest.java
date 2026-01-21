package com.healthdata.clinicalworkflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.domain.model.WaitingQueueEntity;
import com.healthdata.clinicalworkflow.domain.repository.WaitingQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Waiting Queue Service (Tier 3 - Validation)
 *
 * Tests waiting queue management end-to-end including:
 * - Complete queue workflow (add → call → remove)
 * - Priority grouping and ordering
 * - Wait time calculations
 * - Queue reordering with priorities
 * - Multi-tenant isolation
 * - Concurrent queue operations
 * - Queue statistics and metrics
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("Waiting Queue Integration Tests")
class WaitingQueueIntegrationTest {

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
    private WaitingQueueRepository queueRepository;

    private static final String TENANT_ID_A = "TENANT_A";
    private static final String TENANT_ID_B = "TENANT_B";
    private static final String USER_ID = "receptionist@example.com";
    private static final String PATIENT_ID = "PATIENT001";
    private static final String ENCOUNTER_ID = "ENC001";
    private static final String PROVIDER_ID = "PROV001";

    @BeforeEach
    void setUp() {
        queueRepository.deleteAll();
    }

    // ================================
    // Complete Queue Workflow Tests
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Complete queue workflow: add patient → call patient → remove from queue")
    void testCompleteQueueWorkflow() throws Exception {
        // Step 1: Add patient to queue
        QueueEntryRequest entryRequest = QueueEntryRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .queueType("VITALS")
                .enteredQueueAt(LocalDateTime.now())
                .priority("ROUTINE")
                .visitType("Annual Physical")
                .providerId(PROVIDER_ID)
                .build();

        MvcResult addResult = mockMvc.perform(post("/api/v1/queue/entry")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.queueType").value("VITALS"))
                .andExpect(jsonPath("$.queuePosition").exists())
                .andExpect(jsonPath("$.estimatedWaitMinutes").exists())
                .andReturn();

        String positionJson = addResult.getResponse().getContentAsString();
        QueuePositionResponse position = objectMapper.readValue(positionJson, QueuePositionResponse.class);

        // Verify entity in database
        Optional<WaitingQueueEntity> queueEntityOpt = queueRepository.findByTenantIdAndPatientId(TENANT_ID_A, PATIENT_ID);
        assertThat(queueEntityOpt).isPresent();
        WaitingQueueEntity queueEntity = queueEntityOpt.get();
        assertThat(queueEntity.getQueueType()).isEqualTo("VITALS");
        assertThat(queueEntity.getPriority()).isEqualTo("ROUTINE");
        assertThat(queueEntity.getAddedBy()).isEqualTo(USER_ID);
        assertThat(queueEntity.isActive()).isTrue();

        // Step 2: Call patient
        mockMvc.perform(post("/api/v1/queue/patient/{patientId}/call", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", "nurse@example.com"))
                .andExpect(status().isOk());

        // Verify called status
        queueEntity = queueRepository.findByTenantIdAndPatientId(TENANT_ID_A, PATIENT_ID).orElseThrow();
        assertThat(queueEntity.getCalledBy()).isEqualTo("nurse@example.com");
        assertThat(queueEntity.getCalledAt()).isNotNull();

        // Step 3: Remove from queue
        mockMvc.perform(delete("/api/v1/queue/patient/{patientId}", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk());

        // Verify removed from queue
        queueEntity = queueRepository.findByTenantIdAndPatientId(TENANT_ID_A, PATIENT_ID).orElseThrow();
        assertThat(queueEntity.isActive()).isFalse();
        assertThat(queueEntity.getRemovedBy()).isEqualTo(USER_ID);
        assertThat(queueEntity.getRemovedAt()).isNotNull();
    }

    // ================================
    // Priority Grouping Tests
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Priority grouping: verify patients grouped correctly by priority")
    void testPriorityGrouping() throws Exception {
        // Add patients with different priorities
        String[] priorities = {"URGENT", "ROUTINE", "URGENT", "ROUTINE", "STAT"};
        for (int i = 0; i < priorities.length; i++) {
            QueueEntryRequest request = QueueEntryRequest.builder()
                    .patientId("PATIENT00" + (i + 1))
                    .encounterId("ENC00" + (i + 1))
                    .queueType("VITALS")
                    .enteredQueueAt(LocalDateTime.now().minusMinutes(5 - i))
                    .priority(priorities[i])
                    .visitType("Follow-up")
                    .build();

            mockMvc.perform(post("/api/v1/queue/entry")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get queue status
        MvcResult result = mockMvc.perform(get("/api/v1/queue")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInQueue").value(5))
                .andExpect(jsonPath("$.byPriority.STAT").value(1))
                .andExpect(jsonPath("$.byPriority.URGENT").value(2))
                .andExpect(jsonPath("$.byPriority.ROUTINE").value(2))
                .andReturn();

        // Verify database grouping
        List<WaitingQueueEntity> statQueue = queueRepository.findByTenantIdAndPriorityAndActive(TENANT_ID_A, "STAT", true);
        List<WaitingQueueEntity> urgentQueue = queueRepository.findByTenantIdAndPriorityAndActive(TENANT_ID_A, "URGENT", true);
        List<WaitingQueueEntity> routineQueue = queueRepository.findByTenantIdAndPriorityAndActive(TENANT_ID_A, "ROUTINE", true);

        assertThat(statQueue).hasSize(1);
        assertThat(urgentQueue).hasSize(2);
        assertThat(routineQueue).hasSize(2);
    }

    // ================================
    // Wait Time Calculation Tests
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Wait time calculations: verify accurate wait times by priority")
    void testWaitTimeCalculations() throws Exception {
        // Add patients with different priorities
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(30);

        // STAT patient (should have shortest wait)
        QueueEntryRequest statRequest = QueueEntryRequest.builder()
                .patientId("PATIENT_STAT")
                .encounterId("ENC_STAT")
                .queueType("VITALS")
                .enteredQueueAt(baseTime)
                .priority("STAT")
                .visitType("Emergency")
                .build();

        MvcResult statResult = mockMvc.perform(post("/api/v1/queue/entry")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // ROUTINE patient (should have longer wait)
        QueueEntryRequest routineRequest = QueueEntryRequest.builder()
                .patientId("PATIENT_ROUTINE")
                .encounterId("ENC_ROUTINE")
                .queueType("VITALS")
                .enteredQueueAt(baseTime)
                .priority("ROUTINE")
                .visitType("Check-up")
                .build();

        MvcResult routineResult = mockMvc.perform(post("/api/v1/queue/entry")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(routineRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        QueuePositionResponse statPosition = objectMapper.readValue(
                statResult.getResponse().getContentAsString(), QueuePositionResponse.class);
        QueuePositionResponse routinePosition = objectMapper.readValue(
                routineResult.getResponse().getContentAsString(), QueuePositionResponse.class);

        // STAT patient should be ahead in queue
        assertThat(statPosition.getQueuePosition()).isLessThan(routinePosition.getQueuePosition());

        // Get wait times
        mockMvc.perform(get("/api/v1/queue/wait-times")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageWaitMinutes").exists())
                .andExpect(jsonPath("$.byPriority").exists());
    }

    // ================================
    // Queue Reordering Tests
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Queue reordering: add urgent patient after others, verify correct reordering")
    void testQueueReordering() throws Exception {
        // Add 3 ROUTINE patients
        for (int i = 1; i <= 3; i++) {
            QueueEntryRequest request = QueueEntryRequest.builder()
                    .patientId("PATIENT_ROUTINE_" + i)
                    .encounterId("ENC_ROUTINE_" + i)
                    .queueType("VITALS")
                    .enteredQueueAt(LocalDateTime.now().minusMinutes(10 - i))
                    .priority("ROUTINE")
                    .visitType("Check-up")
                    .build();

            mockMvc.perform(post("/api/v1/queue/entry")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Add URGENT patient (should jump to front)
        QueueEntryRequest urgentRequest = QueueEntryRequest.builder()
                .patientId("PATIENT_URGENT")
                .encounterId("ENC_URGENT")
                .queueType("VITALS")
                .enteredQueueAt(LocalDateTime.now())
                .priority("URGENT")
                .visitType("Urgent Care")
                .build();

        MvcResult urgentResult = mockMvc.perform(post("/api/v1/queue/entry")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(urgentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        QueuePositionResponse urgentPosition = objectMapper.readValue(
                urgentResult.getResponse().getContentAsString(), QueuePositionResponse.class);

        // Urgent patient should be at position 1 (ahead of all ROUTINE patients)
        assertThat(urgentPosition.getQueuePosition()).isEqualTo(1);

        // Get queue and verify ordering
        mockMvc.perform(get("/api/v1/queue")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueEntries[0].priority").value("URGENT"))
                .andExpect(jsonPath("$.queueEntries[1].priority").value("ROUTINE"));
    }

    // ================================
    // Multi-Tenant Isolation Tests
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Multi-tenant isolation: tenant A cannot access tenant B's queue")
    void testMultiTenantQueueIsolation() throws Exception {
        // Add patient to tenant A queue
        QueueEntryRequest request = QueueEntryRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .queueType("VITALS")
                .enteredQueueAt(LocalDateTime.now())
                .priority("ROUTINE")
                .visitType("Check-up")
                .build();

        mockMvc.perform(post("/api/v1/queue/entry")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to access from tenant B
        mockMvc.perform(get("/api/v1/queue/patient/{patientId}", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_B))
                .andExpect(status().isNotFound());

        // Verify tenant A can access
        mockMvc.perform(get("/api/v1/queue/patient/{patientId}", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Queue status shows only tenant-specific entries")
    void testQueueStatusTenantIsolation() throws Exception {
        // Add 3 patients to tenant A
        for (int i = 1; i <= 3; i++) {
            QueueEntryRequest request = QueueEntryRequest.builder()
                    .patientId("PATIENT_A_" + i)
                    .encounterId("ENC_A_" + i)
                    .queueType("VITALS")
                    .enteredQueueAt(LocalDateTime.now())
                    .priority("ROUTINE")
                    .visitType("Check-up")
                    .build();

            mockMvc.perform(post("/api/v1/queue/entry")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Add 2 patients to tenant B
        for (int i = 1; i <= 2; i++) {
            QueueEntryRequest request = QueueEntryRequest.builder()
                    .patientId("PATIENT_B_" + i)
                    .encounterId("ENC_B_" + i)
                    .queueType("VITALS")
                    .enteredQueueAt(LocalDateTime.now())
                    .priority("ROUTINE")
                    .visitType("Check-up")
                    .build();

            mockMvc.perform(post("/api/v1/queue/entry")
                            .header("X-Tenant-ID", TENANT_ID_B)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Tenant A should see only 3 entries
        mockMvc.perform(get("/api/v1/queue")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInQueue").value(3));

        // Tenant B should see only 2 entries
        mockMvc.perform(get("/api/v1/queue")
                        .header("X-Tenant-ID", TENANT_ID_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInQueue").value(2));
    }

    // ================================
    // Concurrent Queue Operations Tests
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Concurrent queue operations: simulate multiple patients joining queue")
    void testConcurrentQueueOperations() throws Exception {
        // Simulate 5 patients joining queue simultaneously
        for (int i = 1; i <= 5; i++) {
            QueueEntryRequest request = QueueEntryRequest.builder()
                    .patientId("PATIENT00" + i)
                    .encounterId("ENC00" + i)
                    .queueType("VITALS")
                    .enteredQueueAt(LocalDateTime.now().minusSeconds(i))
                    .priority("ROUTINE")
                    .visitType("Check-up")
                    .build();

            mockMvc.perform(post("/api/v1/queue/entry")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", "user" + i + "@example.com")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verify all 5 are in queue
        mockMvc.perform(get("/api/v1/queue")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInQueue").value(5));

        // Verify all entries in database
        List<WaitingQueueEntity> queueEntries = queueRepository.findByTenantIdAndActive(TENANT_ID_A, true);
        assertThat(queueEntries).hasSize(5);
    }

    // ================================
    // Queue Type Management Tests
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Multiple queue types: VITALS, PROVIDER, CHECKOUT")
    void testMultipleQueueTypes() throws Exception {
        String[] queueTypes = {"VITALS", "PROVIDER", "CHECKOUT"};

        // Add patient to each queue type
        for (int i = 0; i < queueTypes.length; i++) {
            QueueEntryRequest request = QueueEntryRequest.builder()
                    .patientId("PATIENT00" + (i + 1))
                    .encounterId("ENC00" + (i + 1))
                    .queueType(queueTypes[i])
                    .enteredQueueAt(LocalDateTime.now())
                    .priority("ROUTINE")
                    .visitType("Check-up")
                    .build();

            mockMvc.perform(post("/api/v1/queue/entry")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verify queue status shows breakdown by type
        mockMvc.perform(get("/api/v1/queue")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInQueue").value(3))
                .andExpect(jsonPath("$.byQueueType.VITALS").value(1))
                .andExpect(jsonPath("$.byQueueType.PROVIDER").value(1))
                .andExpect(jsonPath("$.byQueueType.CHECKOUT").value(1));
    }

    // ================================
    // Error Scenario Tests
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Error: Duplicate queue entry (patient already in queue)")
    void testDuplicateQueueEntry() throws Exception {
        // Add patient to queue
        QueueEntryRequest request = QueueEntryRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .queueType("VITALS")
                .enteredQueueAt(LocalDateTime.now())
                .priority("ROUTINE")
                .visitType("Check-up")
                .build();

        mockMvc.perform(post("/api/v1/queue/entry")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to add same patient again - should fail
        mockMvc.perform(post("/api/v1/queue/entry")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Error: Patient not in queue (404)")
    void testPatientNotInQueue() throws Exception {
        String nonExistentPatient = "PATIENT_NONEXISTENT";

        // Try to get position for non-existent patient
        mockMvc.perform(get("/api/v1/queue/patient/{patientId}", nonExistentPatient)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isNotFound());

        // Try to call non-existent patient
        mockMvc.perform(post("/api/v1/queue/patient/{patientId}/call", nonExistentPatient)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isNotFound());
    }

    // ================================
    // Additional Test Scenarios
    // ================================

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Get next patient in queue")
    void testGetNextPatientInQueue() throws Exception {
        // Add 3 patients
        for (int i = 1; i <= 3; i++) {
            QueueEntryRequest request = QueueEntryRequest.builder()
                    .patientId("PATIENT00" + i)
                    .encounterId("ENC00" + i)
                    .queueType("VITALS")
                    .enteredQueueAt(LocalDateTime.now().minusMinutes(10 - i))
                    .priority("ROUTINE")
                    .visitType("Check-up")
                    .build();

            mockMvc.perform(post("/api/v1/queue/entry")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get next patient
        mockMvc.perform(get("/api/v1/queue/next")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .param("queueType", "VITALS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value("PATIENT001"));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Verify role-based access control")
    void testRoleBasedAccessControl() throws Exception {
        // NURSE role can view queue
        mockMvc.perform(get("/api/v1/queue")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Update patient priority in queue")
    void testUpdatePatientPriority() throws Exception {
        // Add patient with ROUTINE priority
        QueueEntryRequest request = QueueEntryRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .queueType("VITALS")
                .enteredQueueAt(LocalDateTime.now())
                .priority("ROUTINE")
                .visitType("Check-up")
                .build();

        mockMvc.perform(post("/api/v1/queue/entry")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Update priority to URGENT
        mockMvc.perform(put("/api/v1/queue/patient/{patientId}/priority", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .param("newPriority", "URGENT"))
                .andExpect(status().isOk());

        // Verify priority updated
        WaitingQueueEntity entity = queueRepository.findByTenantIdAndPatientId(TENANT_ID_A, PATIENT_ID).orElseThrow();
        assertThat(entity.getPriority()).isEqualTo("URGENT");
    }
}

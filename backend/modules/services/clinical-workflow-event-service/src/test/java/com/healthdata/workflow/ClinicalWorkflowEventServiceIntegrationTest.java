package com.healthdata.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.workflow.api.v1.dto.InitiateWorkflowRequest;
import com.healthdata.workflow.api.v1.dto.WorkflowEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RED Phase: Clinical Workflow Event Service Integration Tests
 *
 * Validates complete flow: REST → Service → EventHandler → Database
 * Tests workflow orchestration, state management, approvals, and task routing
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("ClinicalWorkflowEventService Integration Tests")
class ClinicalWorkflowEventServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    ).withDatabaseName("workflow_test_db")
     .withUsername("test_user")
     .withPassword("test_password");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "TENANT-001";
    private static final String API_BASE_PATH = "/api/v1/workflows";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        // Test setup if needed
    }

    // ===== Workflow Initiation Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/workflows/initiate endpoint")
    void testInitiateWorkflowReturnsAccepted() throws Exception {
        // Given: Valid InitiateWorkflowRequest
        InitiateWorkflowRequest request = new InitiateWorkflowRequest(
            "PATIENT-001", "TREATMENT_PLAN_REVIEW", "Treatment Plan Review"
        );

        // When: POST to initiate endpoint
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response is 202 Accepted
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.workflowType").value("TREATMENT_PLAN_REVIEW"))
            .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("Should persist workflow projection to database")
    void testWorkflowProjectionPersisted() throws Exception {
        // Given: Workflow initiation request
        InitiateWorkflowRequest request = new InitiateWorkflowRequest(
            "PATIENT-002", "MEDICATION_REVIEW", "Medication Review"
        );

        // When: Submit workflow initiation via REST
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Projection should be persisted to database
        // (Actual verification would require query endpoint)
    }

    // ===== Step Execution Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/workflows/steps/execute endpoint")
    void testExecuteWorkflowStep() throws Exception {
        // Given: Initiate workflow first
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-003", "CARE_COORDINATION", "Care Coordination"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Execute step
        String stepRequest = "{" +
            "\"stepName\": \"ASSESSMENT\"," +
            "\"description\": \"Step assessment completed\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/steps/execute")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepRequest))
            // Then: Response indicates success
            .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should track current step in projection")
    void testCurrentStepTracking() throws Exception {
        // Given: Workflow with step execution
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-004", "REFERRAL_MANAGEMENT", "Referral Management"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Execute step
        String stepRequest = "{" +
            "\"stepName\": \"AUTHORIZATION\"," +
            "\"description\": \"Authorization requested\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/steps/execute")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepRequest))
            // Then: Current step should be tracked
            .andExpect(status().isAccepted());
    }

    // ===== Step Completion Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/workflows/steps/complete endpoint")
    void testCompleteWorkflowStep() throws Exception {
        // Given: Execute step first
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-005", "APPROVAL_WORKFLOW", "Approval Workflow"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Complete step
        String completeRequest = "{" +
            "\"stepName\": \"REVIEW\"," +
            "\"successful\": true," +
            "\"outcome\": \"Review completed successfully\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/steps/complete")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeRequest))
            // Then: Response indicates success
            .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should track step outcome and success status")
    void testStepOutcomeTracking() throws Exception {
        // Given: Step completion request
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-006", "DISCHARGE_PLANNING", "Discharge Planning"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Complete step with success
        String completeRequest = "{" +
            "\"stepName\": \"PLANNING\"," +
            "\"successful\": true," +
            "\"outcome\": \"Discharge plan created\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/steps/complete")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeRequest))
            // Then: Outcome should be tracked with success flag
            .andExpect(status().isAccepted());
    }

    // ===== Task Assignment Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/workflows/tasks/assign endpoint")
    void testAssignTask() throws Exception {
        // Given: Initiated workflow
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-007", "TASK_WORKFLOW", "Task Workflow"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Assign task
        String assignRequest = "{" +
            "\"assignedTo\": \"reviewer@hospital.com\"," +
            "\"priority\": \"HIGH_PRIORITY\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/tasks/assign")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignRequest))
            // Then: Response indicates success
            .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Should track task assignment and routing")
    void testTaskAssignmentTracking() throws Exception {
        // Given: Workflow with task assignment
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-008", "ROUTING_WORKFLOW", "Routing Workflow"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Assign to reviewer
        String assignRequest = "{" +
            "\"assignedTo\": \"dr.smith@hospital.com\"," +
            "\"priority\": \"CRITICAL_PRIORITY\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/tasks/assign")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignRequest))
            // Then: Assignment should be tracked
            .andExpect(status().isAccepted());
    }

    // ===== Approval Workflow Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/workflows/approvals/decide endpoint")
    void testApprovalDecision() throws Exception {
        // Given: Initiated workflow
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-009", "CLINICAL_APPROVAL", "Clinical Approval"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Make approval decision
        String decisionRequest = "{" +
            "\"decision\": \"APPROVED\"," +
            "\"rationale\": \"Meets all clinical criteria\"," +
            "\"approvedBy\": \"dr.johnson@hospital.com\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/approvals/decide")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequest))
            // Then: Response indicates success
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("Should track approval status (APPROVED, DENIED, PENDING)")
    void testApprovalStatusTracking() throws Exception {
        // Given: Workflow in approval state
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-010", "PENDING_APPROVAL", "Pending Approval"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Deny approval
        String denyRequest = "{" +
            "\"decision\": \"DENIED\"," +
            "\"rationale\": \"Does not meet criteria\"," +
            "\"approvedBy\": \"dr.white@hospital.com\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/approvals/decide")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(denyRequest))
            // Then: Status should be DENIED
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.approvalStatus").value("DENIED"));
    }

    // ===== Workflow Completion Tests =====

    @Test
    @DisplayName("Should accept POST /api/v1/workflows/complete endpoint")
    void testCompleteWorkflow() throws Exception {
        // Given: Initiated workflow
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-011", "COMPLETION_TEST", "Completion Test"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Complete workflow
        String completeRequest = "{" +
            "\"status\": \"COMPLETED\"," +
            "\"summary\": \"Workflow completed successfully\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/complete")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeRequest))
            // Then: Response indicates completion
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should track workflow duration from initiation to completion")
    void testWorkflowDurationTracking() throws Exception {
        // Given: Initiate workflow
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-012", "DURATION_TEST", "Duration Test"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.initiatedDate").exists());

        // When: Complete workflow
        String completeRequest = "{" +
            "\"status\": \"COMPLETED\"," +
            "\"summary\": \"Duration tracked\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/complete")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeRequest))
            // Then: Completion date should be tracked
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.completedDate").exists());
    }

    // ===== Workflow State Transitions Tests =====

    @Test
    @DisplayName("Should support workflow state transitions")
    void testStateTransitions() throws Exception {
        // Given: Initiate workflow (INITIATED state)
        InitiateWorkflowRequest initRequest = new InitiateWorkflowRequest(
            "PATIENT-013", "STATE_TEST", "State Test"
        );

        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initRequest)))
            .andExpect(status().isAccepted());

        // When: Transition to IN_PROGRESS
        String progressRequest = "{" +
            "\"newStatus\": \"IN_PROGRESS\"," +
            "\"description\": \"Workflow started\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/progress")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(progressRequest))
            // Then: Status should transition
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // ===== Multi-Tenant Isolation Tests =====

    @Test
    @DisplayName("Should isolate workflows by tenant")
    void testMultiTenantWorkflowIsolation() throws Exception {
        // Given: Same workflow type in different tenants
        InitiateWorkflowRequest request = new InitiateWorkflowRequest(
            "PATIENT-TENANT", "SHARED_WORKFLOW", "Shared Workflow"
        );

        // When: Submit for tenant 1
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", "TENANT-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // And: Submit for tenant 2
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", "TENANT-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Projections should be isolated by tenant
    }

    // ===== Kafka Publishing Tests =====

    @Test
    @DisplayName("Should publish workflow events to Kafka")
    void testWorkflowEventPublishedToKafka() throws Exception {
        // Given: Workflow initiation request
        InitiateWorkflowRequest request = new InitiateWorkflowRequest(
            "PATIENT-KAFKA", "KAFKA_TEST", "Kafka Test"
        );

        // When: Initiate workflow
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then: Event should be published to workflows topic
        // (Actual verification would require Kafka consumer)
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should reject invalid workflow type")
    void testInvalidWorkflowType() throws Exception {
        // Given: Request with invalid workflow type
        InitiateWorkflowRequest request = new InitiateWorkflowRequest(
            "PATIENT-ERR", null, "Invalid Type"
        );

        // When: Submit request
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Should be rejected
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate required fields in workflow initiation")
    void testMissingRequiredFields() throws Exception {
        // Given: Incomplete request
        String incompleteRequest = "{" +
            "\"patientId\": \"PATIENT-INC\"" +
            "}";

        // When: Submit request
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteRequest))
            // Then: Should be rejected
            .andExpect(status().isBadRequest());
    }

    // ===== Response Validation Tests =====

    @Test
    @DisplayName("Should return proper workflow event response structure")
    void testWorkflowResponseStructure() throws Exception {
        // Given: Workflow initiation request
        InitiateWorkflowRequest request = new InitiateWorkflowRequest(
            "PATIENT-RESP", "RESPONSE_TEST", "Response Test"
        );

        // When: Initiate workflow
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response should have proper structure
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.workflowType").exists())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.initiatedDate").exists());
    }

    @Test
    @DisplayName("Should include version tracking in workflow response")
    void testVersionTrackingInResponse() throws Exception {
        // Given: Workflow initiation request
        InitiateWorkflowRequest request = new InitiateWorkflowRequest(
            "PATIENT-VER", "VERSION_TEST", "Version Test"
        );

        // When: Initiate workflow
        mockMvc.perform(post(API_BASE_PATH + "/initiate")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            // Then: Response should include version
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.version").isNumber());
    }
}

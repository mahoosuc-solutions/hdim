package com.healthdata.clinicalworkflowevent.api;

import com.healthdata.clinicalworkflowevent.projection.WorkflowProjection;
import com.healthdata.clinicalworkflowevent.repository.WorkflowProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowProjectionController Unit Tests")
class WorkflowProjectionControllerTest {

    private static final String TENANT_ID = "test-tenant-001";
    private static final UUID WORKFLOW_ID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private static final UUID PATIENT_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String ASSIGNED_USER = "dr.smith";
    private static final String BASE_URL = "/api/v1/workflow-projections";

    @Mock
    private WorkflowProjectionRepository workflowRepository;

    @InjectMocks
    private WorkflowProjectionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private WorkflowProjection buildProjection(UUID workflowId, String status) {
        return WorkflowProjection.builder()
                .id(TENANT_ID + "_" + workflowId)
                .tenantId(TENANT_ID)
                .workflowId(workflowId)
                .patientId(PATIENT_ID)
                .workflowType("MEDICATION_REVIEW")
                .status(status)
                .priority("HIGH")
                .description("Test workflow")
                .assignedTo(ASSIGNED_USER)
                .createdAt(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("GET /{workflowId}")
    class GetWorkflowProjection {

        @Test
        @DisplayName("should return 200 with projection when found")
        void shouldReturnProjectionWhenFound() throws Exception {
            WorkflowProjection projection = buildProjection(WORKFLOW_ID, "PENDING");
            when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
                    .thenReturn(Optional.of(projection));

            mockMvc.perform(get(BASE_URL + "/{workflowId}", WORKFLOW_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workflowId").value(WORKFLOW_ID.toString()))
                    .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(workflowRepository).findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID);
        }

        @Test
        @DisplayName("should return 404 when workflow not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_URL + "/{workflowId}", WORKFLOW_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(workflowRepository).findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID);
        }
    }

    @Nested
    @DisplayName("GET /patient/{patientId}")
    class GetPatientWorkflows {

        @Test
        @DisplayName("should return list of workflows for patient")
        void shouldReturnWorkflowsForPatient() throws Exception {
            List<WorkflowProjection> workflows = List.of(
                    buildProjection(WORKFLOW_ID, "PENDING"),
                    buildProjection(UUID.randomUUID(), "IN_PROGRESS")
            );
            when(workflowRepository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(TENANT_ID, PATIENT_ID))
                    .thenReturn(workflows);

            mockMvc.perform(get(BASE_URL + "/patient/{patientId}", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].patientId").value(PATIENT_ID.toString()));

            verify(workflowRepository).findByTenantIdAndPatientIdOrderByCreatedAtDesc(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("should return empty list when no workflows exist for patient")
        void shouldReturnEmptyListWhenNoWorkflows() throws Exception {
            when(workflowRepository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(TENANT_ID, PATIENT_ID))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL + "/patient/{patientId}", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /patient/{patientId}/pending")
    class GetPendingForPatient {

        @Test
        @DisplayName("should return pending workflows for patient")
        void shouldReturnPendingWorkflows() throws Exception {
            List<WorkflowProjection> workflows = List.of(buildProjection(WORKFLOW_ID, "PENDING"));
            when(workflowRepository.findPendingForPatient(TENANT_ID, PATIENT_ID))
                    .thenReturn(workflows);

            mockMvc.perform(get(BASE_URL + "/patient/{patientId}/pending", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            verify(workflowRepository).findPendingForPatient(TENANT_ID, PATIENT_ID);
        }
    }

    @Nested
    @DisplayName("GET /assigned-to/{assignedTo}")
    class GetAssignedTo {

        @Test
        @DisplayName("should return workflows assigned to user")
        void shouldReturnAssignedWorkflows() throws Exception {
            List<WorkflowProjection> workflows = List.of(buildProjection(WORKFLOW_ID, "IN_PROGRESS"));
            when(workflowRepository.findAssignedTo(TENANT_ID, ASSIGNED_USER))
                    .thenReturn(workflows);

            mockMvc.perform(get(BASE_URL + "/assigned-to/{assignedTo}", ASSIGNED_USER)
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].assignedTo").value(ASSIGNED_USER));

            verify(workflowRepository).findAssignedTo(TENANT_ID, ASSIGNED_USER);
        }
    }

    @Nested
    @DisplayName("GET /overdue")
    class GetOverdue {

        @Test
        @DisplayName("should return overdue workflows")
        void shouldReturnOverdueWorkflows() throws Exception {
            WorkflowProjection overdue = buildProjection(WORKFLOW_ID, "PENDING");
            overdue.setIsOverdue(true);
            when(workflowRepository.findOverdue(TENANT_ID))
                    .thenReturn(List.of(overdue));

            mockMvc.perform(get(BASE_URL + "/overdue")
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].isOverdue").value(true));

            verify(workflowRepository).findOverdue(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("GET /requiring-review")
    class GetRequiringReview {

        @Test
        @DisplayName("should return workflows requiring review")
        void shouldReturnWorkflowsRequiringReview() throws Exception {
            WorkflowProjection review = buildProjection(WORKFLOW_ID, "IN_PROGRESS");
            review.setRequiresReview(true);
            when(workflowRepository.findRequiringReview(TENANT_ID))
                    .thenReturn(List.of(review));

            mockMvc.perform(get(BASE_URL + "/requiring-review")
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].requiresReview").value(true));

            verify(workflowRepository).findRequiringReview(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("GET /by-status/{status} (paginated)")
    class GetByStatus {

        @Test
        @DisplayName("should return paginated workflows by status")
        void shouldReturnPaginatedWorkflowsByStatus() throws Exception {
            WorkflowProjection projection = buildProjection(WORKFLOW_ID, "PENDING");
            Page<WorkflowProjection> page = new PageImpl<>(
                    List.of(projection), PageRequest.of(0, 20), 1);
            when(workflowRepository.findByStatus(eq(TENANT_ID), eq("PENDING"), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/by-status/{status}", "PENDING")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("page", "0")
                            .param("size", "20")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(workflowRepository).findByStatus(eq(TENANT_ID), eq("PENDING"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /stats")
    class GetStatistics {

        @Test
        @DisplayName("should return workflow statistics with pending and overdue counts")
        void shouldReturnWorkflowStatistics() throws Exception {
            when(workflowRepository.countPending(TENANT_ID)).thenReturn(12L);
            when(workflowRepository.countOverdue(TENANT_ID)).thenReturn(3L);

            mockMvc.perform(get(BASE_URL + "/stats")
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPending").value(12))
                    .andExpect(jsonPath("$.totalOverdue").value(3));

            verify(workflowRepository).countPending(TENANT_ID);
            verify(workflowRepository).countOverdue(TENANT_ID);
        }

        @Test
        @DisplayName("should return zero counts when no workflows exist")
        void shouldReturnZeroCountsWhenEmpty() throws Exception {
            when(workflowRepository.countPending(TENANT_ID)).thenReturn(0L);
            when(workflowRepository.countOverdue(TENANT_ID)).thenReturn(0L);

            mockMvc.perform(get(BASE_URL + "/stats")
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPending").value(0))
                    .andExpect(jsonPath("$.totalOverdue").value(0));
        }
    }

    @Nested
    @DisplayName("GET /health")
    class Health {

        @Test
        @DisplayName("should return healthy status message")
        void shouldReturnHealthyStatus() throws Exception {
            mockMvc.perform(get(BASE_URL + "/health")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Clinical workflow event service is healthy"));

            verifyNoInteractions(workflowRepository);
        }
    }
}

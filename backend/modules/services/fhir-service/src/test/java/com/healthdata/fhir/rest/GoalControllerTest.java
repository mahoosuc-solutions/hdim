package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Goal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.GoalService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Goal Controller Tests")
class GoalControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private GoalService goalService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GoalController controller = new GoalController(goalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create goal")
    void shouldCreateGoal() throws Exception {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        when(goalService.createGoal(eq(TENANT_ID), any(Goal.class), eq("user")))
                .thenReturn(goal);

        mockMvc.perform(post("/Goal")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(goal)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(goal.getId())));
    }

    @Test
    @DisplayName("Should get goal")
    void shouldGetGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(goalId.toString());
        when(goalService.getGoal(TENANT_ID, goalId)).thenReturn(Optional.of(goal));

        mockMvc.perform(get("/Goal/{id}", goalId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(goal.getId())));
    }

    @Test
    @DisplayName("Should return not found when goal missing")
    void shouldReturnNotFoundWhenGoalMissing() throws Exception {
        UUID goalId = UUID.randomUUID();
        when(goalService.getGoal(TENANT_ID, goalId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/Goal/{id}", goalId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update goal")
    void shouldUpdateGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(goalId.toString());
        when(goalService.updateGoal(eq(TENANT_ID), eq(goalId), any(Goal.class), eq("user")))
                .thenReturn(goal);

        mockMvc.perform(put("/Goal/{id}", goalId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(goal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(goal.getId())));
    }

    @Test
    @DisplayName("Should delete goal")
    void shouldDeleteGoal() throws Exception {
        UUID goalId = UUID.randomUUID();

        mockMvc.perform(delete("/Goal/{id}", goalId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search goals with patient reference")
    void shouldSearchGoalsWithPatientReference() throws Exception {
        UUID patientId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        when(goalService.searchGoals(
                eq(TENANT_ID),
                eq(patientId),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(goal)));

        mockMvc.perform(get("/Goal")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")))
                .andExpect(content().string(containsString(goal.getId())));

        verify(goalService).searchGoals(
                eq(TENANT_ID),
                eq(patientId),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class));
    }

    @Test
    @DisplayName("Should get goals by patient")
    void shouldGetGoalsByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        when(goalService.getGoalsByPatient(TENANT_ID, patientId))
                .thenReturn(List.of(goal));

        mockMvc.perform(get("/Goal/patient/{patientId}", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(goal.getId())));
    }

    @Test
    @DisplayName("Should get active goals")
    void shouldGetActiveGoals() throws Exception {
        UUID patientId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        when(goalService.getActiveGoals(TENANT_ID, patientId))
                .thenReturn(List.of(goal));

        mockMvc.perform(get("/Goal/patient/{patientId}/active", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(goal.getId())));
    }

    @Test
    @DisplayName("Should get overdue goals")
    void shouldGetOverdueGoals() throws Exception {
        UUID patientId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        when(goalService.getOverdueGoals(TENANT_ID, patientId))
                .thenReturn(List.of(goal));

        mockMvc.perform(get("/Goal/patient/{patientId}/overdue", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(goal.getId())));
    }

    @Test
    @DisplayName("Should get high priority goals")
    void shouldGetHighPriorityGoals() throws Exception {
        UUID patientId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        when(goalService.getHighPriorityGoals(TENANT_ID, patientId))
                .thenReturn(List.of(goal));

        mockMvc.perform(get("/Goal/patient/{patientId}/high-priority", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(goal.getId())));
    }

    @Test
    @DisplayName("Should get goals by condition")
    void shouldGetGoalsByCondition() throws Exception {
        UUID conditionId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        when(goalService.getGoalsByCondition(TENANT_ID, conditionId))
                .thenReturn(List.of(goal));

        mockMvc.perform(get("/Goal/condition/{conditionId}", conditionId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(goal.getId())));
    }

    @Test
    @DisplayName("Should get goals due in range")
    void shouldGetGoalsDueInRange() throws Exception {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID().toString());
        when(goalService.getGoalsDueInRange(eq(TENANT_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(goal));

        mockMvc.perform(get("/Goal/due")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("start", "2025-01-01")
                        .param("end", "2025-02-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(goal.getId())));
    }
}

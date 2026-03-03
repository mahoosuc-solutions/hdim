package com.healthdata.gateway.admin.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("OperationsController")
class OperationsControllerTest {

    @Mock
    private OperationsService operationsService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        OperationsController controller = new OperationsController(operationsService, objectMapper);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(converter)
            .build();
    }

    private OperationRun createRun(UUID id) {
        OperationRun run = new OperationRun();
        run.setId(id);
        run.setOperationType(OperationRun.OperationType.STACK_START);
        run.setStatus(OperationRun.RunStatus.QUEUED);
        run.setRequestedBy("admin@test.com");
        run.setParametersJson("{}");
        run.setSummary("Queued");
        return run;
    }

    @Nested
    @DisplayName("Stack Endpoints")
    class StackEndpoints {

        @Test
        @DisplayName("should return 200 when startStack succeeds")
        void shouldReturnOk_WhenStartStackSucceeds() throws Exception {
            UUID runId = UUID.randomUUID();
            when(operationsService.startStack(any(), any())).thenReturn(runId);

            mockMvc.perform(post("/api/v1/ops/stack/start")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idempotencyKey\": \"key1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.message").value("Stack start queued"));
        }

        @Test
        @DisplayName("should return 409 when concurrent run blocks")
        void shouldReturn409_WhenConcurrentRunBlocks() throws Exception {
            when(operationsService.startStack(any(), any()))
                .thenThrow(new IllegalStateException("Another operation running"));

            mockMvc.perform(post("/api/v1/ops/stack/start")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idempotencyKey\": \"key1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Another operation running"));
        }

        @Test
        @DisplayName("should return 200 when restartStack succeeds")
        void shouldReturnOk_WhenRestartStackSucceeds() throws Exception {
            UUID runId = UUID.randomUUID();
            when(operationsService.restartStack(any(), any())).thenReturn(runId);

            mockMvc.perform(post("/api/v1/ops/stack/restart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idempotencyKey\": \"key1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.message").value("Stack restart queued"));
        }
    }

    @Nested
    @DisplayName("Seed Endpoints")
    class SeedEndpoints {

        @Test
        @DisplayName("should pass profile and scheduleMode when running seed")
        void shouldPassProfileAndScheduleMode_WhenRunSeed() throws Exception {
            UUID runId = UUID.randomUUID();
            when(operationsService.runSeed(any(), eq("full"), eq("encounter"), any())).thenReturn(runId);

            mockMvc.perform(post("/api/v1/ops/seed/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"profile\":\"full\",\"scheduleMode\":\"encounter\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()));

            verify(operationsService).runSeed(eq("system"), eq("full"), eq("encounter"), any());
        }
    }

    @Nested
    @DisplayName("Run Management")
    class RunManagement {

        @Test
        @DisplayName("should return 200 when cancel succeeds")
        void shouldReturnOk_WhenCancelSucceeds() throws Exception {
            UUID runId = UUID.randomUUID();
            when(operationsService.cancelRun(any(), any())).thenReturn(true);

            mockMvc.perform(post("/api/v1/ops/runs/{runId}/cancel", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.message").value("Cancellation requested"));
        }

        @Test
        @DisplayName("should return 400 when cancel fails")
        void shouldReturnBadRequest_WhenCancelFails() throws Exception {
            UUID runId = UUID.randomUUID();
            when(operationsService.cancelRun(any(), any())).thenReturn(false);

            mockMvc.perform(post("/api/v1/ops/runs/{runId}/cancel", runId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Run cannot be cancelled"));
        }

        @Test
        @DisplayName("should return run detail when run exists")
        void shouldReturnRunDetail_WhenRunExists() throws Exception {
            UUID runId = UUID.randomUUID();
            OperationRun run = createRun(runId);
            when(operationsService.getRun(any())).thenReturn(Optional.of(run));
            when(operationsService.getRunSteps(any())).thenReturn(Collections.emptyList());
            when(operationsService.getValidationScorecard(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/ops/runs/{runId}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.id").value(runId.toString()))
                .andExpect(jsonPath("$.run.operationType").value("STACK_START"))
                .andExpect(jsonPath("$.run.status").value("QUEUED"))
                .andExpect(jsonPath("$.steps").isArray());
        }

        @Test
        @DisplayName("should return 404 when run not found")
        void shouldReturn404_WhenRunNotFound() throws Exception {
            UUID runId = UUID.randomUUID();
            when(operationsService.getRun(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/ops/runs/{runId}", runId))
                .andExpect(status().isNotFound());
        }
    }
}

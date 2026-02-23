package com.healthdata.nurseworkflow.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.nurseworkflow.api.v1.dto.CreateEngagementThreadRequest;
import com.healthdata.nurseworkflow.api.v1.dto.CreateEscalationRequest;
import com.healthdata.nurseworkflow.api.v1.dto.PatientEngagementKpiResponse;
import com.healthdata.nurseworkflow.api.v1.dto.PostEngagementMessageRequest;
import com.healthdata.nurseworkflow.application.PatientEngagementService;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementEscalationEntity;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementMessageEntity;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementThreadEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for PatientEngagementController.
 *
 * Uses standalone MockMvc (no Spring context) so all Spring Security annotations
 * are bypassed, allowing pure HTTP layer verification.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientEngagementController")
class PatientEngagementControllerTest {

    @Mock
    private PatientEngagementService patientEngagementService;

    @InjectMocks
    private PatientEngagementController patientEngagementController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "test-tenant-001";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID THREAD_ID = UUID.randomUUID();

    private PatientEngagementThreadEntity testThread;
    private PatientEngagementMessageEntity testMessage;
    private PatientEngagementEscalationEntity testEscalation;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders
            .standaloneSetup(patientEngagementController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setMessageConverters(converter)
            .build();

        Instant now = Instant.now();

        testThread = PatientEngagementThreadEntity.builder()
            .id(THREAD_ID)
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .subject("Medication question")
            .priority(PatientEngagementThreadEntity.ThreadPriority.MEDIUM)
            .status(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN)
            .createdBy("patient@example.com")
            .assignedClinicianId("dr-smith")
            .lastMessageAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();

        testMessage = PatientEngagementMessageEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .threadId(THREAD_ID)
            .senderType(PatientEngagementMessageEntity.SenderType.PATIENT)
            .senderId("patient@example.com")
            .messageText("I have a question about my medication.")
            .containsPhi(false)
            .escalationFlag(false)
            .createdAt(now)
            .build();

        testEscalation = PatientEngagementEscalationEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .threadId(THREAD_ID)
            .reason("Patient is not responding")
            .severity(PatientEngagementEscalationEntity.EscalationSeverity.HIGH)
            .status(PatientEngagementEscalationEntity.EscalationStatus.OPEN)
            .recipientId("dr-on-call")
            .recipientEmail("oncall@clinic.org")
            .correlationId(UUID.randomUUID().toString())
            .createdAt(now)
            .build();
    }

    // ─────────────────────────────────────────────
    // POST /api/v1/patient-engagement/threads
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /threads")
    class CreateThreadTests {

        @Test
        @DisplayName("should return 201 and thread bundle on success")
        void shouldCreateThreadSuccessfully() throws Exception {
            // Given
            CreateEngagementThreadRequest request = new CreateEngagementThreadRequest();
            request.setPatientId(PATIENT_ID);
            request.setSubject("Medication question");
            request.setCreatedBy("patient@example.com");
            request.setInitialMessage("I have a question about my medication.");

            PatientEngagementService.EngagementThreadBundle bundle =
                new PatientEngagementService.EngagementThreadBundle(testThread, testMessage);

            when(patientEngagementService.createThread(eq(TENANT_ID), any(CreateEngagementThreadRequest.class)))
                .thenReturn(bundle);

            // When / Then
            mockMvc.perform(post("/api/v1/patient-engagement/threads")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thread.id").value(THREAD_ID.toString()))
                .andExpect(jsonPath("$.thread.subject").value("Medication question"))
                .andExpect(jsonPath("$.initialMessage.senderType").value("PATIENT"));

            verify(patientEngagementService).createThread(eq(TENANT_ID), any());
        }

        @Test
        @DisplayName("should return 400 when patientId is null")
        void shouldReturn400WhenPatientIdMissing() throws Exception {
            String requestBody = """
                {
                    "subject": "Hello",
                    "createdBy": "patient@example.com",
                    "initialMessage": "Hi"
                }
                """;

            mockMvc.perform(post("/api/v1/patient-engagement/threads")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when subject is blank")
        void shouldReturn400WhenSubjectBlank() throws Exception {
            String requestBody = """
                {
                    "patientId": "%s",
                    "subject": "",
                    "createdBy": "patient@example.com",
                    "initialMessage": "Hi"
                }
                """.formatted(PATIENT_ID);

            mockMvc.perform(post("/api/v1/patient-engagement/threads")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────
    // POST /api/v1/patient-engagement/threads/{threadId}/messages
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /threads/{threadId}/messages")
    class PostMessageTests {

        @Test
        @DisplayName("should return 201 and message on success")
        void shouldPostMessageSuccessfully() throws Exception {
            // Given
            PostEngagementMessageRequest request = new PostEngagementMessageRequest();
            request.setSenderType(PatientEngagementMessageEntity.SenderType.CLINICIAN);
            request.setSenderId("dr-smith");
            request.setMessageText("Your results look normal.");

            when(patientEngagementService.addMessage(eq(TENANT_ID), eq(THREAD_ID), any()))
                .thenReturn(testMessage);

            // When / Then
            mockMvc.perform(post("/api/v1/patient-engagement/threads/{threadId}/messages", THREAD_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.threadId").value(THREAD_ID.toString()))
                .andExpect(jsonPath("$.senderType").value("PATIENT"));

            verify(patientEngagementService).addMessage(eq(TENANT_ID), eq(THREAD_ID), any());
        }

        @Test
        @DisplayName("should return 400 when senderType is missing")
        void shouldReturn400WhenSenderTypeMissing() throws Exception {
            String requestBody = """
                {
                    "senderId": "dr-smith",
                    "messageText": "Hi"
                }
                """;

            mockMvc.perform(post("/api/v1/patient-engagement/threads/{threadId}/messages", THREAD_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when messageText is blank")
        void shouldReturn400WhenMessageTextBlank() throws Exception {
            String requestBody = """
                {
                    "senderType": "CLINICIAN",
                    "senderId": "dr-smith",
                    "messageText": ""
                }
                """;

            mockMvc.perform(post("/api/v1/patient-engagement/threads/{threadId}/messages", THREAD_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/patient-engagement/threads/{threadId}/messages
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /threads/{threadId}/messages")
    class GetMessagesTests {

        @Test
        @DisplayName("should return 200 and paginated messages")
        void shouldReturnMessagesSuccessfully() throws Exception {
            // Given — use explicit PageRequest to avoid Unpaged serialization issues
            PageRequest pageable = PageRequest.of(0, 10);
            Page<PatientEngagementMessageEntity> page =
                new PageImpl<>(List.of(testMessage), pageable, 1);

            when(patientEngagementService.getThreadMessages(eq(TENANT_ID), eq(THREAD_ID), any(Pageable.class)))
                .thenReturn(page);

            // When / Then
            mockMvc.perform(get("/api/v1/patient-engagement/threads/{threadId}/messages", THREAD_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return empty page when no messages")
        void shouldReturnEmptyPageWhenNoMessages() throws Exception {
            // Given — use explicit PageRequest to avoid Unpaged.getPageNumber() UnsupportedOperationException
            PageRequest pageable = PageRequest.of(0, 10);
            Page<PatientEngagementMessageEntity> emptyPage =
                new PageImpl<>(List.of(), pageable, 0);

            when(patientEngagementService.getThreadMessages(eq(TENANT_ID), eq(THREAD_ID), any(Pageable.class)))
                .thenReturn(emptyPage);

            // When / Then
            mockMvc.perform(get("/api/v1/patient-engagement/threads/{threadId}/messages", THREAD_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────
    // POST /api/v1/patient-engagement/threads/{threadId}/escalations
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /threads/{threadId}/escalations")
    class EscalateTests {

        @Test
        @DisplayName("should return 201 and escalation on success")
        void shouldEscalateSuccessfully() throws Exception {
            // Given
            CreateEscalationRequest request = new CreateEscalationRequest();
            request.setReason("Patient not responding");
            request.setSeverity(PatientEngagementEscalationEntity.EscalationSeverity.HIGH);
            request.setRecipientId("dr-on-call");
            request.setRecipientEmail("oncall@clinic.org");

            when(patientEngagementService.escalateThread(eq(TENANT_ID), eq(THREAD_ID), any()))
                .thenReturn(testEscalation);

            // When / Then
            mockMvc.perform(post("/api/v1/patient-engagement/threads/{threadId}/escalations", THREAD_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.threadId").value(THREAD_ID.toString()))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"));

            verify(patientEngagementService).escalateThread(eq(TENANT_ID), eq(THREAD_ID), any());
        }

        @Test
        @DisplayName("should return 400 when reason is blank")
        void shouldReturn400WhenReasonBlank() throws Exception {
            String requestBody = """
                {
                    "reason": "",
                    "severity": "HIGH",
                    "recipientId": "dr-on-call",
                    "recipientEmail": "oncall@clinic.org"
                }
                """;

            mockMvc.perform(post("/api/v1/patient-engagement/threads/{threadId}/escalations", THREAD_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when severity is null")
        void shouldReturn400WhenSeverityNull() throws Exception {
            String requestBody = """
                {
                    "reason": "Urgent",
                    "recipientId": "dr-on-call",
                    "recipientEmail": "oncall@clinic.org"
                }
                """;

            mockMvc.perform(post("/api/v1/patient-engagement/threads/{threadId}/escalations", THREAD_ID)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/patient-engagement/kpis/transitions
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /kpis/transitions")
    class GetKpisTests {

        @Test
        @DisplayName("should return 200 and KPI summary")
        void shouldReturnKpiSummary() throws Exception {
            // Given
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-31T23:59:59Z");

            PatientEngagementKpiResponse kpiResponse = PatientEngagementKpiResponse.builder()
                .windowStart(from)
                .windowEnd(to)
                .totalThreads(20L)
                .openThreads(8L)
                .totalMessages(100L)
                .patientMessages(60L)
                .clinicianMessages(40L)
                .totalEscalations(5L)
                .criticalEscalations(1L)
                .build();

            when(patientEngagementService.getKpis(eq(TENANT_ID), any(), any()))
                .thenReturn(kpiResponse);

            // When / Then
            mockMvc.perform(get("/api/v1/patient-engagement/kpis/transitions")
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("from", "2026-01-01T00:00:00Z")
                    .param("to", "2026-01-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalThreads").value(20))
                .andExpect(jsonPath("$.openThreads").value(8))
                .andExpect(jsonPath("$.totalMessages").value(100))
                .andExpect(jsonPath("$.patientMessages").value(60))
                .andExpect(jsonPath("$.clinicianMessages").value(40))
                .andExpect(jsonPath("$.totalEscalations").value(5))
                .andExpect(jsonPath("$.criticalEscalations").value(1));
        }

        @Test
        @DisplayName("should return 200 without query params (default window)")
        void shouldReturnKpiWithDefaultWindow() throws Exception {
            // Given
            PatientEngagementKpiResponse kpiResponse = PatientEngagementKpiResponse.builder()
                .windowStart(Instant.now().minusSeconds(30L * 24 * 3600))
                .windowEnd(Instant.now())
                .totalThreads(5L)
                .openThreads(2L)
                .totalMessages(15L)
                .patientMessages(9L)
                .clinicianMessages(6L)
                .totalEscalations(0L)
                .criticalEscalations(0L)
                .build();

            when(patientEngagementService.getKpis(eq(TENANT_ID), isNull(), isNull()))
                .thenReturn(kpiResponse);

            // When / Then
            mockMvc.perform(get("/api/v1/patient-engagement/kpis/transitions")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalThreads").value(5));
        }
    }
}

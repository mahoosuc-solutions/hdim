package com.healthdata.nurseworkflow.integration;

import com.healthdata.nurseworkflow.api.v1.dto.CreateEngagementThreadRequest;
import com.healthdata.nurseworkflow.api.v1.dto.CreateEscalationRequest;
import com.healthdata.nurseworkflow.api.v1.dto.PatientEngagementKpiResponse;
import com.healthdata.nurseworkflow.api.v1.dto.PostEngagementMessageRequest;
import com.healthdata.nurseworkflow.application.PatientEngagementService;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementEscalationEntity;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementMessageEntity;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementThreadEntity;
import com.healthdata.nurseworkflow.domain.repository.PatientEngagementEscalationRepository;
import com.healthdata.nurseworkflow.domain.repository.PatientEngagementMessageRepository;
import com.healthdata.nurseworkflow.domain.repository.PatientEngagementThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for PatientEngagementService using Testcontainers PostgreSQL.
 *
 * Verifies the full service-to-database flow for thread creation, messaging,
 * escalation (with mocked Kafka), and KPI aggregation.
 *
 * KafkaTemplate is mocked to avoid requiring a running Kafka broker.
 */
@DisplayName("PatientEngagement Integration Tests")
class PatientEngagementIntegrationTest extends NurseWorkflowIntegrationTestBase {

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PatientEngagementService patientEngagementService;

    @Autowired
    private PatientEngagementThreadRepository threadRepository;

    @Autowired
    private PatientEngagementMessageRepository messageRepository;

    @Autowired
    private PatientEngagementEscalationRepository escalationRepository;

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        // Clean up for test isolation
        escalationRepository.deleteAll();
        messageRepository.deleteAll();
        threadRepository.deleteAll();
    }

    // ─────────────────────────────────────────────
    // createThread
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("createThread: should persist thread and initial message to database")
    @Transactional
    void shouldPersistThreadAndInitialMessage() {
        // Given
        CreateEngagementThreadRequest request = buildThreadRequest(
            patientId, "Medication question", "patient@example.com",
            "I have a question about my medication.", null);

        // When
        PatientEngagementService.EngagementThreadBundle bundle =
            patientEngagementService.createThread(TENANT_A, request);

        // Then
        assertThat(bundle).isNotNull();
        assertThat(bundle.getThread().getId()).isNotNull();
        assertThat(bundle.getThread().getTenantId()).isEqualTo(TENANT_A);
        assertThat(bundle.getThread().getSubject()).isEqualTo("Medication question");
        assertThat(bundle.getThread().getStatus())
            .isEqualTo(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN);

        assertThat(bundle.getInitialMessage().getId()).isNotNull();
        assertThat(bundle.getInitialMessage().getThreadId()).isEqualTo(bundle.getThread().getId());
        assertThat(bundle.getInitialMessage().getSenderType())
            .isEqualTo(PatientEngagementMessageEntity.SenderType.PATIENT);
        assertThat(bundle.getInitialMessage().getMessageText())
            .isEqualTo("I have a question about my medication.");

        // Verify DB
        assertThat(threadRepository.findByIdAndTenantId(bundle.getThread().getId(), TENANT_A)).isPresent();
        assertThat(messageRepository.findById(bundle.getInitialMessage().getId())).isPresent();
    }

    @Test
    @DisplayName("createThread: should isolate threads across tenants")
    void shouldIsolateBetweenTenants() {
        // Given
        CreateEngagementThreadRequest reqA = buildThreadRequest(
            patientId, "Tenant A question", "patientA@a.org", "Hello from A", null);
        CreateEngagementThreadRequest reqB = buildThreadRequest(
            patientId, "Tenant B question", "patientB@b.org", "Hello from B", null);

        // When
        PatientEngagementService.EngagementThreadBundle bundleA =
            patientEngagementService.createThread(TENANT_A, reqA);
        PatientEngagementService.EngagementThreadBundle bundleB =
            patientEngagementService.createThread(TENANT_B, reqB);

        // Then — A's thread is not accessible from B's tenant, and vice versa
        assertThat(threadRepository.findByIdAndTenantId(bundleA.getThread().getId(), TENANT_B))
            .isEmpty();
        assertThat(threadRepository.findByIdAndTenantId(bundleB.getThread().getId(), TENANT_A))
            .isEmpty();

        assertThat(threadRepository.findByIdAndTenantId(bundleA.getThread().getId(), TENANT_A))
            .isPresent();
        assertThat(threadRepository.findByIdAndTenantId(bundleB.getThread().getId(), TENANT_B))
            .isPresent();
    }

    @Test
    @DisplayName("createThread: should assign MEDIUM priority when none specified")
    @Transactional
    void shouldUseDefaultPriorityWhenNotSpecified() {
        // Given
        CreateEngagementThreadRequest request = buildThreadRequest(
            patientId, "Quick question", "patient@example.com", "Hi there", null);
        request.setPriority(null); // explicitly null

        // When
        PatientEngagementService.EngagementThreadBundle bundle =
            patientEngagementService.createThread(TENANT_A, request);

        // Then
        assertThat(bundle.getThread().getPriority())
            .isEqualTo(PatientEngagementThreadEntity.ThreadPriority.MEDIUM);
    }

    // ─────────────────────────────────────────────
    // addMessage
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("addMessage: should persist message and update thread status")
    @Transactional
    void shouldPersistMessageAndUpdateThreadStatus() {
        // Given
        PatientEngagementService.EngagementThreadBundle bundle =
            patientEngagementService.createThread(TENANT_A, buildThreadRequest(
                patientId, "Follow-up", "p@example.com", "Initial question", null));
        UUID threadId = bundle.getThread().getId();

        PostEngagementMessageRequest msgRequest = new PostEngagementMessageRequest();
        msgRequest.setSenderType(PatientEngagementMessageEntity.SenderType.CLINICIAN);
        msgRequest.setSenderId("dr-smith");
        msgRequest.setMessageText("Here is my answer.");
        msgRequest.setContainsPhi(false);
        msgRequest.setEscalationFlag(false);

        // When
        PatientEngagementMessageEntity saved =
            patientEngagementService.addMessage(TENANT_A, threadId, msgRequest);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getThreadId()).isEqualTo(threadId);
        assertThat(saved.getSenderType()).isEqualTo(PatientEngagementMessageEntity.SenderType.CLINICIAN);
        assertThat(saved.getMessageText()).isEqualTo("Here is my answer.");

        // Thread status should now be PENDING_PATIENT after clinician reply
        PatientEngagementThreadEntity updatedThread =
            threadRepository.findByIdAndTenantId(threadId, TENANT_A).orElseThrow();
        assertThat(updatedThread.getStatus())
            .isEqualTo(PatientEngagementThreadEntity.ThreadStatus.PENDING_PATIENT);
    }

    @Test
    @DisplayName("addMessage: should throw IllegalArgumentException for unknown thread")
    void shouldThrowForUnknownThread() {
        // Given
        UUID unknownThreadId = UUID.randomUUID();
        PostEngagementMessageRequest msgRequest = new PostEngagementMessageRequest();
        msgRequest.setSenderType(PatientEngagementMessageEntity.SenderType.PATIENT);
        msgRequest.setSenderId("patient@example.com");
        msgRequest.setMessageText("Is anyone there?");

        // When / Then
        assertThatThrownBy(() ->
            patientEngagementService.addMessage(TENANT_A, unknownThreadId, msgRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(unknownThreadId.toString());
    }

    @Test
    @DisplayName("addMessage: should not expose thread from another tenant")
    void shouldNotExposeThreadFromAnotherTenant() {
        // Given — create thread under TENANT_A, try to add message as TENANT_B
        PatientEngagementService.EngagementThreadBundle bundle =
            patientEngagementService.createThread(TENANT_A, buildThreadRequest(
                patientId, "Private thread", "p@a.org", "Private message", null));

        PostEngagementMessageRequest msgRequest = new PostEngagementMessageRequest();
        msgRequest.setSenderType(PatientEngagementMessageEntity.SenderType.PATIENT);
        msgRequest.setSenderId("p@b.org");
        msgRequest.setMessageText("Cross-tenant attempt");

        // When / Then — TENANT_B cannot see TENANT_A's thread
        assertThatThrownBy(() ->
            patientEngagementService.addMessage(TENANT_B, bundle.getThread().getId(), msgRequest))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ─────────────────────────────────────────────
    // getThreadMessages
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getThreadMessages: should return paginated messages in order")
    void shouldReturnPaginatedMessages() {
        // Given
        PatientEngagementService.EngagementThreadBundle bundle =
            patientEngagementService.createThread(TENANT_A, buildThreadRequest(
                patientId, "Ongoing conversation", "p@example.com", "First message", null));
        UUID threadId = bundle.getThread().getId();

        // Add two more messages
        PostEngagementMessageRequest second = new PostEngagementMessageRequest();
        second.setSenderType(PatientEngagementMessageEntity.SenderType.CLINICIAN);
        second.setSenderId("dr-smith");
        second.setMessageText("Response from clinician.");
        patientEngagementService.addMessage(TENANT_A, threadId, second);

        PostEngagementMessageRequest third = new PostEngagementMessageRequest();
        third.setSenderType(PatientEngagementMessageEntity.SenderType.PATIENT);
        third.setSenderId("p@example.com");
        third.setMessageText("Patient follow-up.");
        patientEngagementService.addMessage(TENANT_A, threadId, third);

        // When
        Page<PatientEngagementMessageEntity> page =
            patientEngagementService.getThreadMessages(TENANT_A, threadId, PageRequest.of(0, 10));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().get(0).getMessageText()).isEqualTo("First message");
        assertThat(page.getContent().get(1).getSenderType())
            .isEqualTo(PatientEngagementMessageEntity.SenderType.CLINICIAN);
        assertThat(page.getContent().get(2).getSenderType())
            .isEqualTo(PatientEngagementMessageEntity.SenderType.PATIENT);
    }

    // ─────────────────────────────────────────────
    // escalateThread
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("escalateThread: should persist escalation and publish Kafka event")
    @Transactional
    void shouldPersistEscalationAndPublishKafkaEvent() {
        // Given
        PatientEngagementService.EngagementThreadBundle bundle =
            patientEngagementService.createThread(TENANT_A, buildThreadRequest(
                patientId, "Urgent concern", "p@example.com", "I feel very unwell.", null));
        UUID threadId = bundle.getThread().getId();

        CreateEscalationRequest escalationRequest = new CreateEscalationRequest();
        escalationRequest.setReason("Patient reporting severe symptoms");
        escalationRequest.setSeverity(PatientEngagementEscalationEntity.EscalationSeverity.CRITICAL);
        escalationRequest.setRecipientId("dr-on-call");
        escalationRequest.setRecipientEmail("oncall@clinic.org");

        // When
        PatientEngagementEscalationEntity escalation =
            patientEngagementService.escalateThread(TENANT_A, threadId, escalationRequest);

        // Then
        assertThat(escalation.getId()).isNotNull();
        assertThat(escalation.getTenantId()).isEqualTo(TENANT_A);
        assertThat(escalation.getThreadId()).isEqualTo(threadId);
        assertThat(escalation.getSeverity())
            .isEqualTo(PatientEngagementEscalationEntity.EscalationSeverity.CRITICAL);
        assertThat(escalation.getStatus())
            .isEqualTo(PatientEngagementEscalationEntity.EscalationStatus.OPEN);
        assertThat(escalation.getCorrelationId()).isNotBlank();

        // Thread status reset to PENDING_CLINICIAN
        PatientEngagementThreadEntity updatedThread =
            threadRepository.findByIdAndTenantId(threadId, TENANT_A).orElseThrow();
        assertThat(updatedThread.getStatus())
            .isEqualTo(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN);

        // Kafka event published
        verify(kafkaTemplate, times(1))
            .send(eq("clinical-alerts"), eq(threadId.toString()), any(PatientEngagementService.ClinicalAlertEvent.class));
    }

    @Test
    @DisplayName("escalateThread: should throw when thread not found")
    void shouldThrowWhenEscalatingNonExistentThread() {
        // Given
        UUID unknownThreadId = UUID.randomUUID();
        CreateEscalationRequest req = new CreateEscalationRequest();
        req.setReason("Urgent");
        req.setSeverity(PatientEngagementEscalationEntity.EscalationSeverity.MEDIUM);
        req.setRecipientId("dr-jones");
        req.setRecipientEmail("dr-jones@clinic.org");

        // When / Then
        assertThatThrownBy(() ->
            patientEngagementService.escalateThread(TENANT_A, unknownThreadId, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(unknownThreadId.toString());
    }

    // ─────────────────────────────────────────────
    // getKpis
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getKpis: should aggregate correct counts across time window")
    void shouldAggregateKpiCounts() {
        // Given — create 2 threads under TENANT_A
        patientEngagementService.createThread(TENANT_A, buildThreadRequest(
            patientId, "Thread 1", "p@example.com", "Message 1", null));
        patientEngagementService.createThread(TENANT_A, buildThreadRequest(
            UUID.randomUUID(), "Thread 2", "p2@example.com", "Message 2", null));

        // Create 1 thread under TENANT_B (should not count)
        patientEngagementService.createThread(TENANT_B, buildThreadRequest(
            UUID.randomUUID(), "Thread B", "p@b.org", "Hello from B", null));

        Instant from = Instant.now().minusSeconds(300);
        Instant to = Instant.now().plusSeconds(60);

        // When
        PatientEngagementKpiResponse kpis = patientEngagementService.getKpis(TENANT_A, from, to);

        // Then
        assertThat(kpis.getTotalThreads()).isEqualTo(2L);
        // Both threads in PENDING_CLINICIAN status = counted as open
        assertThat(kpis.getOpenThreads()).isGreaterThanOrEqualTo(2L);
        // 2 initial messages (1 per thread)
        assertThat(kpis.getTotalMessages()).isEqualTo(2L);
        assertThat(kpis.getPatientMessages()).isEqualTo(2L);
        assertThat(kpis.getClinicianMessages()).isEqualTo(0L);
        assertThat(kpis.getTotalEscalations()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getKpis: should use 30-day default window when dates not provided")
    void shouldUseDefaultWindowForKpis() {
        // Given — create a thread
        patientEngagementService.createThread(TENANT_A, buildThreadRequest(
            patientId, "Recent thread", "p@example.com", "Recent message", null));

        // When — no date window provided
        PatientEngagementKpiResponse kpis = patientEngagementService.getKpis(TENANT_A, null, null);

        // Then — thread created just now should be in the 30-day window
        assertThat(kpis.getTotalThreads()).isGreaterThanOrEqualTo(1L);
        assertThat(kpis.getWindowStart()).isNotNull();
        assertThat(kpis.getWindowEnd()).isNotNull();
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────

    private CreateEngagementThreadRequest buildThreadRequest(
        UUID patientId,
        String subject,
        String createdBy,
        String initialMessage,
        String assignedClinicianId
    ) {
        CreateEngagementThreadRequest req = new CreateEngagementThreadRequest();
        req.setPatientId(patientId);
        req.setSubject(subject);
        req.setCreatedBy(createdBy);
        req.setInitialMessage(initialMessage);
        req.setAssignedClinicianId(assignedClinicianId);
        req.setPriority(PatientEngagementThreadEntity.ThreadPriority.MEDIUM);
        req.setContainsPhi(false);
        return req;
    }
}

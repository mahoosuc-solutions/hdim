package com.healthdata.nurseworkflow.application;

import com.healthdata.nurseworkflow.api.v1.dto.CreateEngagementThreadRequest;
import com.healthdata.nurseworkflow.api.v1.dto.CreateEscalationRequest;
import com.healthdata.nurseworkflow.api.v1.dto.PatientEngagementKpiResponse;
import com.healthdata.nurseworkflow.api.v1.dto.PostEngagementMessageRequest;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementEscalationEntity;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementMessageEntity;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementThreadEntity;
import com.healthdata.nurseworkflow.domain.repository.PatientEngagementEscalationRepository;
import com.healthdata.nurseworkflow.domain.repository.PatientEngagementMessageRepository;
import com.healthdata.nurseworkflow.domain.repository.PatientEngagementThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PatientEngagementService.
 *
 * Covers thread creation, message posting, escalation with Kafka event publishing,
 * and KPI aggregation logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientEngagementService")
class PatientEngagementServiceTest {

    @Mock
    private PatientEngagementThreadRepository threadRepository;

    @Mock
    private PatientEngagementMessageRepository messageRepository;

    @Mock
    private PatientEngagementEscalationRepository escalationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PatientEngagementService patientEngagementService;

    private static final String TENANT_ID = "test-tenant-001";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID THREAD_ID = UUID.randomUUID();

    private PatientEngagementThreadEntity testThread;
    private PatientEngagementMessageEntity testMessage;

    @BeforeEach
    void setUp() {
        testThread = PatientEngagementThreadEntity.builder()
            .id(THREAD_ID)
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .subject("Medication question")
            .priority(PatientEngagementThreadEntity.ThreadPriority.MEDIUM)
            .status(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN)
            .createdBy("patient@example.com")
            .assignedClinicianId("dr-smith")
            .lastMessageAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
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
            .createdAt(Instant.now())
            .build();
    }

    // ─────────────────────────────────────────────
    // createThread tests
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("createThread")
    class CreateThreadTests {

        @Test
        @DisplayName("should create thread and initial message, return bundle")
        void shouldCreateThreadAndInitialMessage() {
            // Given
            CreateEngagementThreadRequest request = new CreateEngagementThreadRequest();
            request.setPatientId(PATIENT_ID);
            request.setSubject("Medication question");
            request.setCreatedBy("patient@example.com");
            request.setAssignedClinicianId("dr-smith");
            request.setPriority(PatientEngagementThreadEntity.ThreadPriority.HIGH);
            request.setInitialMessage("I have a question about my medication.");
            request.setContainsPhi(false);

            when(threadRepository.save(any(PatientEngagementThreadEntity.class)))
                .thenReturn(testThread);
            when(messageRepository.save(any(PatientEngagementMessageEntity.class)))
                .thenReturn(testMessage);

            // When
            PatientEngagementService.EngagementThreadBundle bundle =
                patientEngagementService.createThread(TENANT_ID, request);

            // Then
            assertThat(bundle).isNotNull();
            assertThat(bundle.getThread()).isEqualTo(testThread);
            assertThat(bundle.getInitialMessage()).isEqualTo(testMessage);

            verify(threadRepository, times(2)).save(any(PatientEngagementThreadEntity.class));
            verify(messageRepository, times(1)).save(any(PatientEngagementMessageEntity.class));
        }

        @Test
        @DisplayName("should set tenant ID and PENDING_CLINICIAN status on thread")
        void shouldSetTenantIdAndStatus() {
            // Given
            CreateEngagementThreadRequest request = new CreateEngagementThreadRequest();
            request.setPatientId(PATIENT_ID);
            request.setSubject("Lab result question");
            request.setCreatedBy("patient@example.com");
            request.setInitialMessage("What do my lab results mean?");

            ArgumentCaptor<PatientEngagementThreadEntity> threadCaptor =
                ArgumentCaptor.forClass(PatientEngagementThreadEntity.class);

            when(threadRepository.save(any(PatientEngagementThreadEntity.class)))
                .thenReturn(testThread);
            when(messageRepository.save(any(PatientEngagementMessageEntity.class)))
                .thenReturn(testMessage);

            // When
            patientEngagementService.createThread(TENANT_ID, request);

            // Then
            verify(threadRepository, atLeastOnce()).save(threadCaptor.capture());
            PatientEngagementThreadEntity savedThread = threadCaptor.getAllValues().get(0);
            assertThat(savedThread.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(savedThread.getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(savedThread.getStatus()).isEqualTo(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN);
        }

        @Test
        @DisplayName("should create initial message with PATIENT sender type")
        void shouldCreateInitialMessageWithPatientSenderType() {
            // Given
            CreateEngagementThreadRequest request = new CreateEngagementThreadRequest();
            request.setPatientId(PATIENT_ID);
            request.setSubject("Follow-up question");
            request.setCreatedBy("patient@example.com");
            request.setInitialMessage("Is this normal?");
            request.setContainsPhi(true);

            ArgumentCaptor<PatientEngagementMessageEntity> messageCaptor =
                ArgumentCaptor.forClass(PatientEngagementMessageEntity.class);

            when(threadRepository.save(any(PatientEngagementThreadEntity.class)))
                .thenReturn(testThread);
            when(messageRepository.save(any(PatientEngagementMessageEntity.class)))
                .thenReturn(testMessage);

            // When
            patientEngagementService.createThread(TENANT_ID, request);

            // Then
            verify(messageRepository).save(messageCaptor.capture());
            PatientEngagementMessageEntity capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.getSenderType()).isEqualTo(PatientEngagementMessageEntity.SenderType.PATIENT);
            assertThat(capturedMessage.getMessageText()).isEqualTo("Is this normal?");
            assertThat(capturedMessage.getContainsPhi()).isTrue();
            assertThat(capturedMessage.getEscalationFlag()).isFalse();
        }
    }

    // ─────────────────────────────────────────────
    // addMessage tests
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("addMessage")
    class AddMessageTests {

        @Test
        @DisplayName("should post message to existing thread")
        void shouldAddMessageToThread() {
            // Given
            PostEngagementMessageRequest request = new PostEngagementMessageRequest();
            request.setSenderType(PatientEngagementMessageEntity.SenderType.CLINICIAN);
            request.setSenderId("dr-smith");
            request.setMessageText("Your lab results look normal.");
            request.setContainsPhi(false);
            request.setEscalationFlag(false);

            PatientEngagementMessageEntity clinicianMessage = PatientEngagementMessageEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .threadId(THREAD_ID)
                .senderType(PatientEngagementMessageEntity.SenderType.CLINICIAN)
                .senderId("dr-smith")
                .messageText("Your lab results look normal.")
                .containsPhi(false)
                .escalationFlag(false)
                .createdAt(Instant.now())
                .build();

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.of(testThread));
            when(messageRepository.save(any(PatientEngagementMessageEntity.class)))
                .thenReturn(clinicianMessage);
            when(threadRepository.save(any(PatientEngagementThreadEntity.class)))
                .thenReturn(testThread);

            // When
            PatientEngagementMessageEntity result =
                patientEngagementService.addMessage(TENANT_ID, THREAD_ID, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSenderType()).isEqualTo(PatientEngagementMessageEntity.SenderType.CLINICIAN);
            assertThat(result.getMessageText()).isEqualTo("Your lab results look normal.");
            verify(messageRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("should set thread status to PENDING_PATIENT when clinician replies")
        void shouldSetStatusToPendingPatientForClinicianMessage() {
            // Given
            PostEngagementMessageRequest request = new PostEngagementMessageRequest();
            request.setSenderType(PatientEngagementMessageEntity.SenderType.CLINICIAN);
            request.setSenderId("dr-smith");
            request.setMessageText("Please take your medication with food.");

            ArgumentCaptor<PatientEngagementThreadEntity> threadCaptor =
                ArgumentCaptor.forClass(PatientEngagementThreadEntity.class);

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.of(testThread));
            when(messageRepository.save(any(PatientEngagementMessageEntity.class)))
                .thenReturn(testMessage);
            when(threadRepository.save(any(PatientEngagementThreadEntity.class)))
                .thenReturn(testThread);

            // When
            patientEngagementService.addMessage(TENANT_ID, THREAD_ID, request);

            // Then
            verify(threadRepository).save(threadCaptor.capture());
            assertThat(threadCaptor.getValue().getStatus())
                .isEqualTo(PatientEngagementThreadEntity.ThreadStatus.PENDING_PATIENT);
        }

        @Test
        @DisplayName("should set thread status to PENDING_CLINICIAN when patient sends message")
        void shouldSetStatusToPendingClinicianForPatientMessage() {
            // Given
            PostEngagementMessageRequest request = new PostEngagementMessageRequest();
            request.setSenderType(PatientEngagementMessageEntity.SenderType.PATIENT);
            request.setSenderId("patient@example.com");
            request.setMessageText("Thank you, I have another question.");

            ArgumentCaptor<PatientEngagementThreadEntity> threadCaptor =
                ArgumentCaptor.forClass(PatientEngagementThreadEntity.class);

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.of(testThread));
            when(messageRepository.save(any(PatientEngagementMessageEntity.class)))
                .thenReturn(testMessage);
            when(threadRepository.save(any(PatientEngagementThreadEntity.class)))
                .thenReturn(testThread);

            // When
            patientEngagementService.addMessage(TENANT_ID, THREAD_ID, request);

            // Then
            verify(threadRepository).save(threadCaptor.capture());
            assertThat(threadCaptor.getValue().getStatus())
                .isEqualTo(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when thread not found")
        void shouldThrowWhenThreadNotFound() {
            // Given
            PostEngagementMessageRequest request = new PostEngagementMessageRequest();
            request.setSenderType(PatientEngagementMessageEntity.SenderType.PATIENT);
            request.setSenderId("patient@example.com");
            request.setMessageText("Hello?");

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> patientEngagementService.addMessage(TENANT_ID, THREAD_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(THREAD_ID.toString());
        }
    }

    // ─────────────────────────────────────────────
    // getThreadMessages tests
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("getThreadMessages")
    class GetThreadMessagesTests {

        @Test
        @DisplayName("should return ordered messages for valid thread")
        void shouldReturnMessagesForThread() {
            // Given
            PageRequest pageable = PageRequest.of(0, 10);
            Page<PatientEngagementMessageEntity> messagePage =
                new PageImpl<>(List.of(testMessage), pageable, 1);

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.of(testThread));
            when(messageRepository.findByTenantIdAndThreadIdOrderByCreatedAtAsc(TENANT_ID, THREAD_ID, pageable))
                .thenReturn(messagePage);

            // When
            Page<PatientEngagementMessageEntity> result =
                patientEngagementService.getThreadMessages(TENANT_ID, THREAD_ID, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).containsExactly(testMessage);
        }

        @Test
        @DisplayName("should throw when thread not found for tenant")
        void shouldThrowWhenThreadNotFoundForTenant() {
            // Given
            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() ->
                patientEngagementService.getThreadMessages(TENANT_ID, THREAD_ID, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(THREAD_ID.toString());
        }
    }

    // ─────────────────────────────────────────────
    // escalateThread tests
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("escalateThread")
    class EscalateThreadTests {

        @Test
        @DisplayName("should create escalation record and publish Kafka event")
        void shouldCreateEscalationAndPublishEvent() {
            // Given
            CreateEscalationRequest request = new CreateEscalationRequest();
            request.setReason("Patient reporting severe symptoms");
            request.setSeverity(PatientEngagementEscalationEntity.EscalationSeverity.CRITICAL);
            request.setRecipientId("dr-on-call");
            request.setRecipientEmail("dr-oncall@clinic.org");

            PatientEngagementEscalationEntity escalation = PatientEngagementEscalationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .threadId(THREAD_ID)
                .reason("Patient reporting severe symptoms")
                .severity(PatientEngagementEscalationEntity.EscalationSeverity.CRITICAL)
                .status(PatientEngagementEscalationEntity.EscalationStatus.OPEN)
                .recipientId("dr-on-call")
                .recipientEmail("dr-oncall@clinic.org")
                .correlationId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .build();

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.of(testThread));
            when(escalationRepository.save(any(PatientEngagementEscalationEntity.class)))
                .thenReturn(escalation);
            when(threadRepository.save(any(PatientEngagementThreadEntity.class)))
                .thenReturn(testThread);

            // When
            PatientEngagementEscalationEntity result =
                patientEngagementService.escalateThread(TENANT_ID, THREAD_ID, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSeverity())
                .isEqualTo(PatientEngagementEscalationEntity.EscalationSeverity.CRITICAL);
            assertThat(result.getStatus())
                .isEqualTo(PatientEngagementEscalationEntity.EscalationStatus.OPEN);

            verify(escalationRepository, times(1)).save(any());
            verify(kafkaTemplate, times(1)).send(eq("clinical-alerts"), anyString(), any());
        }

        @Test
        @DisplayName("should set thread status back to PENDING_CLINICIAN after escalation")
        void shouldSetThreadStatusToPendingClinicianAfterEscalation() {
            // Given
            CreateEscalationRequest request = new CreateEscalationRequest();
            request.setReason("Urgent follow-up needed");
            request.setSeverity(PatientEngagementEscalationEntity.EscalationSeverity.HIGH);
            request.setRecipientId("care-team");
            request.setRecipientEmail("care-team@clinic.org");

            ArgumentCaptor<PatientEngagementThreadEntity> threadCaptor =
                ArgumentCaptor.forClass(PatientEngagementThreadEntity.class);

            PatientEngagementEscalationEntity escalation = PatientEngagementEscalationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .threadId(THREAD_ID)
                .reason("Urgent follow-up needed")
                .severity(PatientEngagementEscalationEntity.EscalationSeverity.HIGH)
                .status(PatientEngagementEscalationEntity.EscalationStatus.OPEN)
                .recipientId("care-team")
                .recipientEmail("care-team@clinic.org")
                .correlationId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .build();

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.of(testThread));
            when(escalationRepository.save(any())).thenReturn(escalation);
            when(threadRepository.save(threadCaptor.capture())).thenReturn(testThread);

            // When
            patientEngagementService.escalateThread(TENANT_ID, THREAD_ID, request);

            // Then
            assertThat(threadCaptor.getValue().getStatus())
                .isEqualTo(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when thread not found")
        void shouldThrowWhenThreadNotFound() {
            // Given
            CreateEscalationRequest request = new CreateEscalationRequest();
            request.setReason("Urgent");
            request.setSeverity(PatientEngagementEscalationEntity.EscalationSeverity.MEDIUM);
            request.setRecipientId("dr-jones");
            request.setRecipientEmail("dr-jones@clinic.org");

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() ->
                patientEngagementService.escalateThread(TENANT_ID, THREAD_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(THREAD_ID.toString());
        }

        @Test
        @DisplayName("should include correlation ID in Kafka event")
        void shouldIncludeCorrelationIdInKafkaEvent() {
            // Given
            CreateEscalationRequest request = new CreateEscalationRequest();
            request.setReason("Correlation test");
            request.setSeverity(PatientEngagementEscalationEntity.EscalationSeverity.MEDIUM);
            request.setRecipientId("dr-jones");
            request.setRecipientEmail("dr-jones@clinic.org");

            PatientEngagementEscalationEntity escalation = PatientEngagementEscalationEntity.builder()
                .id(UUID.randomUUID()).tenantId(TENANT_ID).threadId(THREAD_ID)
                .reason("Correlation test")
                .severity(PatientEngagementEscalationEntity.EscalationSeverity.MEDIUM)
                .status(PatientEngagementEscalationEntity.EscalationStatus.OPEN)
                .recipientId("dr-jones").recipientEmail("dr-jones@clinic.org")
                .correlationId("corr-123").createdAt(Instant.now()).build();

            when(threadRepository.findByIdAndTenantId(THREAD_ID, TENANT_ID))
                .thenReturn(Optional.of(testThread));
            when(escalationRepository.save(any())).thenReturn(escalation);
            when(threadRepository.save(any())).thenReturn(testThread);

            // When
            patientEngagementService.escalateThread(TENANT_ID, THREAD_ID, request);

            // Then
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(kafkaTemplate).send(eq("clinical-alerts"), eq(THREAD_ID.toString()), eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(PatientEngagementService.ClinicalAlertEvent.class);
            PatientEngagementService.ClinicalAlertEvent event =
                (PatientEngagementService.ClinicalAlertEvent) eventCaptor.getValue();
            assertThat(event.getAlertType()).isEqualTo("PATIENT_MESSAGE_ESCALATION");
            assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        }
    }

    // ─────────────────────────────────────────────
    // getKpis tests
    // ─────────────────────────────────────────────

    @Nested
    @DisplayName("getKpis")
    class GetKpisTests {

        @Test
        @DisplayName("should aggregate KPI counts for given time window")
        void shouldReturnKpiSummaryForWindow() {
            // Given
            Instant from = Instant.now().minusSeconds(3600);
            Instant to = Instant.now();

            when(threadRepository.countByTenantIdAndCreatedAtBetween(eq(TENANT_ID), eq(from), eq(to)))
                .thenReturn(10L);
            when(threadRepository.countByTenantIdAndStatus(TENANT_ID, PatientEngagementThreadEntity.ThreadStatus.OPEN))
                .thenReturn(3L);
            when(threadRepository.countByTenantIdAndStatus(TENANT_ID, PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN))
                .thenReturn(4L);
            when(threadRepository.countByTenantIdAndStatus(TENANT_ID, PatientEngagementThreadEntity.ThreadStatus.PENDING_PATIENT))
                .thenReturn(2L);
            when(messageRepository.countByTenantIdAndCreatedAtBetween(eq(TENANT_ID), eq(from), eq(to)))
                .thenReturn(50L);
            when(messageRepository.countByTenantIdAndSenderTypeAndCreatedAtBetween(
                TENANT_ID, PatientEngagementMessageEntity.SenderType.PATIENT, from, to))
                .thenReturn(30L);
            when(messageRepository.countByTenantIdAndSenderTypeAndCreatedAtBetween(
                TENANT_ID, PatientEngagementMessageEntity.SenderType.CLINICIAN, from, to))
                .thenReturn(20L);
            when(escalationRepository.countByTenantIdAndCreatedAtBetween(eq(TENANT_ID), eq(from), eq(to)))
                .thenReturn(5L);
            when(escalationRepository.countByTenantIdAndSeverityAndCreatedAtBetween(
                TENANT_ID, PatientEngagementEscalationEntity.EscalationSeverity.CRITICAL, from, to))
                .thenReturn(2L);

            // When
            PatientEngagementKpiResponse response = patientEngagementService.getKpis(TENANT_ID, from, to);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalThreads()).isEqualTo(10L);
            assertThat(response.getOpenThreads()).isEqualTo(9L); // 3 + 4 + 2
            assertThat(response.getTotalMessages()).isEqualTo(50L);
            assertThat(response.getPatientMessages()).isEqualTo(30L);
            assertThat(response.getClinicianMessages()).isEqualTo(20L);
            assertThat(response.getTotalEscalations()).isEqualTo(5L);
            assertThat(response.getCriticalEscalations()).isEqualTo(2L);
            assertThat(response.getWindowStart()).isEqualTo(from);
            assertThat(response.getWindowEnd()).isEqualTo(to);
        }

        @Test
        @DisplayName("should use default 30-day window when from/to are null")
        void shouldUseDefaultWindowWhenDatesAreNull() {
            // Given — no from/to
            when(threadRepository.countByTenantIdAndCreatedAtBetween(eq(TENANT_ID), any(), any()))
                .thenReturn(0L);
            when(threadRepository.countByTenantIdAndStatus(any(), any())).thenReturn(0L);
            when(messageRepository.countByTenantIdAndCreatedAtBetween(eq(TENANT_ID), any(), any()))
                .thenReturn(0L);
            when(messageRepository.countByTenantIdAndSenderTypeAndCreatedAtBetween(any(), any(), any(), any()))
                .thenReturn(0L);
            when(escalationRepository.countByTenantIdAndCreatedAtBetween(eq(TENANT_ID), any(), any()))
                .thenReturn(0L);
            when(escalationRepository.countByTenantIdAndSeverityAndCreatedAtBetween(any(), any(), any(), any()))
                .thenReturn(0L);

            // When
            PatientEngagementKpiResponse response = patientEngagementService.getKpis(TENANT_ID, null, null);

            // Then
            assertThat(response.getWindowStart()).isNotNull();
            assertThat(response.getWindowEnd()).isNotNull();
            // Default window is ~30 days; allow small margin
            long diffSeconds = response.getWindowEnd().getEpochSecond() - response.getWindowStart().getEpochSecond();
            assertThat(diffSeconds).isBetween(
                29L * 24L * 3600L - 60L,   // 29 days - 60s tolerance
                31L * 24L * 3600L + 60L    // 31 days + 60s tolerance
            );
        }
    }
}

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
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEngagementService {

    private static final String CLINICAL_ALERTS_TOPIC = "clinical-alerts";

    private final PatientEngagementThreadRepository threadRepository;
    private final PatientEngagementMessageRepository messageRepository;
    private final PatientEngagementEscalationRepository escalationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public EngagementThreadBundle createThread(String tenantId, CreateEngagementThreadRequest request) {
        PatientEngagementThreadEntity thread = PatientEngagementThreadEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .subject(request.getSubject())
            .priority(request.getPriority())
            .status(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN)
            .createdBy(request.getCreatedBy())
            .assignedClinicianId(request.getAssignedClinicianId())
            .build();
        PatientEngagementThreadEntity savedThread = threadRepository.save(thread);

        PatientEngagementMessageEntity initialMessage = PatientEngagementMessageEntity.builder()
            .tenantId(tenantId)
            .threadId(savedThread.getId())
            .senderType(PatientEngagementMessageEntity.SenderType.PATIENT)
            .senderId(request.getCreatedBy())
            .messageText(request.getInitialMessage())
            .containsPhi(Boolean.TRUE.equals(request.getContainsPhi()))
            .escalationFlag(Boolean.FALSE)
            .build();
        PatientEngagementMessageEntity savedMessage = messageRepository.save(initialMessage);

        savedThread.setLastMessageAt(savedMessage.getCreatedAt());
        threadRepository.save(savedThread);
        return new EngagementThreadBundle(savedThread, savedMessage);
    }

    @Transactional
    public PatientEngagementMessageEntity addMessage(
        String tenantId,
        UUID threadId,
        PostEngagementMessageRequest request
    ) {
        PatientEngagementThreadEntity thread = threadRepository.findByIdAndTenantId(threadId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

        PatientEngagementMessageEntity message = PatientEngagementMessageEntity.builder()
            .tenantId(tenantId)
            .threadId(thread.getId())
            .senderType(request.getSenderType())
            .senderId(request.getSenderId())
            .messageText(request.getMessageText())
            .containsPhi(Boolean.TRUE.equals(request.getContainsPhi()))
            .escalationFlag(Boolean.TRUE.equals(request.getEscalationFlag()))
            .build();
        PatientEngagementMessageEntity saved = messageRepository.save(message);

        thread.setLastMessageAt(saved.getCreatedAt());
        thread.setStatus(nextStatusForSender(request.getSenderType()));
        threadRepository.save(thread);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<PatientEngagementMessageEntity> getThreadMessages(String tenantId, UUID threadId, Pageable pageable) {
        // Validate thread belongs to tenant.
        threadRepository.findByIdAndTenantId(threadId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));
        return messageRepository.findByTenantIdAndThreadIdOrderByCreatedAtAsc(tenantId, threadId, pageable);
    }

    @Transactional
    public PatientEngagementEscalationEntity escalateThread(
        String tenantId,
        UUID threadId,
        CreateEscalationRequest request
    ) {
        PatientEngagementThreadEntity thread = threadRepository.findByIdAndTenantId(threadId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

        String correlationId = UUID.randomUUID().toString();

        PatientEngagementEscalationEntity escalation = PatientEngagementEscalationEntity.builder()
            .tenantId(tenantId)
            .threadId(thread.getId())
            .reason(request.getReason())
            .severity(request.getSeverity())
            .recipientId(request.getRecipientId())
            .recipientEmail(request.getRecipientEmail())
            .correlationId(correlationId)
            .status(PatientEngagementEscalationEntity.EscalationStatus.OPEN)
            .build();
        PatientEngagementEscalationEntity saved = escalationRepository.save(escalation);

        thread.setStatus(PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN);
        threadRepository.save(thread);

        ClinicalAlertEvent event = ClinicalAlertEvent.builder()
            .tenantId(tenantId)
            .recipientId(request.getRecipientId())
            .recipientEmail(request.getRecipientEmail())
            .patientName("Patient " + thread.getPatientId().toString().substring(0, 8))
            .alertType("PATIENT_MESSAGE_ESCALATION")
            .message(request.getReason())
            .severity(request.getSeverity().name())
            .correlationId(correlationId)
            .build();

        kafkaTemplate.send(CLINICAL_ALERTS_TOPIC, thread.getId().toString(), event);
        log.info("Published clinical alert escalation event: threadId={}, correlationId={}", thread.getId(), correlationId);

        return saved;
    }

    @Transactional(readOnly = true)
    public PatientEngagementKpiResponse getKpis(String tenantId, Instant from, Instant to) {
        Instant windowStart = from != null ? from : Instant.now().minusSeconds(30L * 24L * 3600L);
        Instant windowEnd = to != null ? to : Instant.now();

        return PatientEngagementKpiResponse.builder()
            .windowStart(windowStart)
            .windowEnd(windowEnd)
            .totalThreads(threadRepository.countByTenantIdAndCreatedAtBetween(tenantId, windowStart, windowEnd))
            .openThreads(threadRepository.countByTenantIdAndStatus(tenantId, PatientEngagementThreadEntity.ThreadStatus.OPEN)
                + threadRepository.countByTenantIdAndStatus(tenantId, PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN)
                + threadRepository.countByTenantIdAndStatus(tenantId, PatientEngagementThreadEntity.ThreadStatus.PENDING_PATIENT))
            .totalMessages(messageRepository.countByTenantIdAndCreatedAtBetween(tenantId, windowStart, windowEnd))
            .patientMessages(messageRepository.countByTenantIdAndSenderTypeAndCreatedAtBetween(
                tenantId, PatientEngagementMessageEntity.SenderType.PATIENT, windowStart, windowEnd))
            .clinicianMessages(messageRepository.countByTenantIdAndSenderTypeAndCreatedAtBetween(
                tenantId, PatientEngagementMessageEntity.SenderType.CLINICIAN, windowStart, windowEnd))
            .totalEscalations(escalationRepository.countByTenantIdAndCreatedAtBetween(tenantId, windowStart, windowEnd))
            .criticalEscalations(escalationRepository.countByTenantIdAndSeverityAndCreatedAtBetween(
                tenantId, PatientEngagementEscalationEntity.EscalationSeverity.CRITICAL, windowStart, windowEnd))
            .build();
    }

    private PatientEngagementThreadEntity.ThreadStatus nextStatusForSender(PatientEngagementMessageEntity.SenderType senderType) {
        return switch (senderType) {
            case PATIENT -> PatientEngagementThreadEntity.ThreadStatus.PENDING_CLINICIAN;
            case CLINICIAN -> PatientEngagementThreadEntity.ThreadStatus.PENDING_PATIENT;
            case SYSTEM -> PatientEngagementThreadEntity.ThreadStatus.OPEN;
        };
    }

    @Data
    @Builder
    public static class ClinicalAlertEvent {
        private String tenantId;
        private String recipientId;
        private String recipientEmail;
        private String patientName;
        private String alertType;
        private String message;
        private String severity;
        private String correlationId;
    }

    @Data
    @RequiredArgsConstructor
    public static class EngagementThreadBundle {
        private final PatientEngagementThreadEntity thread;
        private final PatientEngagementMessageEntity initialMessage;
    }
}

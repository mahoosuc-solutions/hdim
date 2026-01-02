package com.healthdata.sales.service;

import com.healthdata.sales.dto.*;
import com.healthdata.sales.entity.*;
import com.healthdata.sales.exception.*;
import com.healthdata.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing email sequences and enrollments
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSequenceService {

    private final EmailSequenceRepository sequenceRepository;
    private final SequenceEnrollmentRepository enrollmentRepository;
    private final LeadRepository leadRepository;
    private final ContactRepository contactRepository;

    // ==================== Sequence Management ====================

    @Transactional(readOnly = true)
    public Page<EmailSequenceDTO> findAllSequences(UUID tenantId, Pageable pageable) {
        return sequenceRepository.findByTenantId(tenantId, pageable)
            .map(this::toSequenceDTO);
    }

    @Transactional(readOnly = true)
    public EmailSequenceDTO findSequenceById(UUID tenantId, UUID id) {
        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(id));
        return toSequenceDTO(sequence);
    }

    @Transactional
    public EmailSequenceDTO createSequence(UUID tenantId, EmailSequenceDTO dto) {
        if (sequenceRepository.existsByNameAndTenantId(dto.getName(), tenantId)) {
            throw new DuplicateResourceException("Email sequence", dto.getName());
        }

        EmailSequence sequence = new EmailSequence();
        sequence.setId(UUID.randomUUID());
        sequence.setTenantId(tenantId);
        updateSequenceFromDTO(sequence, dto);

        sequence = sequenceRepository.save(sequence);
        log.info("Created email sequence {} for tenant {}", sequence.getId(), tenantId);
        return toSequenceDTO(sequence);
    }

    @Transactional
    public EmailSequenceDTO updateSequence(UUID tenantId, UUID id, EmailSequenceDTO dto) {
        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(id));

        updateSequenceFromDTO(sequence, dto);
        sequence = sequenceRepository.save(sequence);
        log.info("Updated email sequence {}", sequence.getId());
        return toSequenceDTO(sequence);
    }

    @Transactional
    public void deleteSequence(UUID tenantId, UUID id) {
        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(id));

        // Check for active enrollments
        Long activeCount = enrollmentRepository.countBySequenceIdAndStatus(id, EnrollmentStatus.ACTIVE);
        if (activeCount > 0) {
            throw new InvalidStageTransitionException("Cannot delete sequence with " + activeCount + " active enrollments");
        }

        sequenceRepository.delete(sequence);
        log.info("Deleted email sequence {}", id);
    }

    @Transactional
    public EmailSequenceDTO activateSequence(UUID tenantId, UUID id) {
        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(id));

        sequence.setActive(true);
        sequence = sequenceRepository.save(sequence);
        log.info("Activated email sequence {}", id);
        return toSequenceDTO(sequence);
    }

    @Transactional
    public EmailSequenceDTO deactivateSequence(UUID tenantId, UUID id) {
        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(id));

        sequence.setActive(false);
        sequence = sequenceRepository.save(sequence);
        log.info("Deactivated email sequence {}", id);
        return toSequenceDTO(sequence);
    }

    // ==================== Step Management ====================

    @Transactional
    public EmailSequenceDTO addStep(UUID tenantId, UUID sequenceId, EmailSequenceStepDTO stepDto) {
        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(sequenceId, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(sequenceId));

        EmailSequenceStep step = new EmailSequenceStep();
        step.setId(UUID.randomUUID());
        updateStepFromDTO(step, stepDto);

        // Set step order if not provided
        if (step.getStepOrder() == null) {
            step.setStepOrder(sequence.getSteps().size() + 1);
        }

        sequence.addStep(step);
        sequence = sequenceRepository.save(sequence);
        log.info("Added step {} to sequence {}", step.getId(), sequenceId);
        return toSequenceDTO(sequence);
    }

    @Transactional
    public EmailSequenceDTO updateStep(UUID tenantId, UUID sequenceId, UUID stepId, EmailSequenceStepDTO stepDto) {
        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(sequenceId, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(sequenceId));

        EmailSequenceStep step = sequence.getSteps().stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElseThrow(() -> new SalesException("Step not found: " + stepId, org.springframework.http.HttpStatus.NOT_FOUND));

        updateStepFromDTO(step, stepDto);
        sequence = sequenceRepository.save(sequence);
        log.info("Updated step {} in sequence {}", stepId, sequenceId);
        return toSequenceDTO(sequence);
    }

    @Transactional
    public EmailSequenceDTO deleteStep(UUID tenantId, UUID sequenceId, UUID stepId) {
        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(sequenceId, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(sequenceId));

        EmailSequenceStep step = sequence.getSteps().stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElseThrow(() -> new SalesException("Step not found: " + stepId, org.springframework.http.HttpStatus.NOT_FOUND));

        sequence.removeStep(step);

        // Reorder remaining steps
        int order = 1;
        for (EmailSequenceStep s : sequence.getSteps()) {
            s.setStepOrder(order++);
        }

        sequence = sequenceRepository.save(sequence);
        log.info("Deleted step {} from sequence {}", stepId, sequenceId);
        return toSequenceDTO(sequence);
    }

    // ==================== Enrollment Management ====================

    @Transactional
    public SequenceEnrollmentDTO enrollLead(UUID tenantId, UUID leadId, UUID sequenceId, UUID enrolledByUserId) {
        Lead lead = leadRepository.findByIdAndTenantId(leadId, tenantId)
            .orElseThrow(() -> new LeadNotFoundException(leadId));

        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(sequenceId, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(sequenceId));

        if (!sequence.getActive()) {
            throw new InvalidStageTransitionException("Cannot enroll in inactive sequence");
        }

        if (sequence.getTargetType() == TargetType.CONTACT) {
            throw new InvalidStageTransitionException("Sequence is for contacts only");
        }

        // Check for existing enrollment
        enrollmentRepository.findExistingEnrollment(sequenceId, leadId, null)
            .ifPresent(e -> {
                throw new DuplicateResourceException("Lead is already enrolled in this sequence");
            });

        SequenceEnrollment enrollment = createEnrollment(tenantId, sequence, lead.getEmail(),
            lead.getFirstName(), lead.getLastName(), enrolledByUserId);
        enrollment.setLeadId(leadId);

        enrollment = enrollmentRepository.save(enrollment);
        log.info("Enrolled lead {} in sequence {}", leadId, sequenceId);
        return toEnrollmentDTO(enrollment);
    }

    @Transactional
    public SequenceEnrollmentDTO enrollContact(UUID tenantId, UUID contactId, UUID sequenceId, UUID enrolledByUserId) {
        Contact contact = contactRepository.findByIdAndTenantId(contactId, tenantId)
            .orElseThrow(() -> new ContactNotFoundException(contactId));

        EmailSequence sequence = sequenceRepository.findByIdAndTenantId(sequenceId, tenantId)
            .orElseThrow(() -> new SequenceNotFoundException(sequenceId));

        if (!sequence.getActive()) {
            throw new InvalidStageTransitionException("Cannot enroll in inactive sequence");
        }

        if (sequence.getTargetType() == TargetType.LEAD) {
            throw new InvalidStageTransitionException("Sequence is for leads only");
        }

        // Check for existing enrollment
        enrollmentRepository.findExistingEnrollment(sequenceId, null, contactId)
            .ifPresent(e -> {
                throw new DuplicateResourceException("Contact is already enrolled in this sequence");
            });

        SequenceEnrollment enrollment = createEnrollment(tenantId, sequence, contact.getEmail(),
            contact.getFirstName(), contact.getLastName(), enrolledByUserId);
        enrollment.setContactId(contactId);

        enrollment = enrollmentRepository.save(enrollment);
        log.info("Enrolled contact {} in sequence {}", contactId, sequenceId);
        return toEnrollmentDTO(enrollment);
    }

    private SequenceEnrollment createEnrollment(UUID tenantId, EmailSequence sequence, String email,
                                                  String firstName, String lastName, UUID enrolledByUserId) {
        SequenceEnrollment enrollment = new SequenceEnrollment();
        enrollment.setId(UUID.randomUUID());
        enrollment.setTenantId(tenantId);
        enrollment.setSequence(sequence);
        enrollment.setEmail(email);
        enrollment.setFirstName(firstName);
        enrollment.setLastName(lastName);
        enrollment.setCurrentStep(0);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledByUserId(enrolledByUserId);
        enrollment.setUnsubscribeToken(UUID.randomUUID().toString());

        // Calculate first email send time
        if (!sequence.getSteps().isEmpty()) {
            EmailSequenceStep firstStep = sequence.getSteps().get(0);
            enrollment.setNextEmailAt(calculateNextEmailTime(firstStep));
        }

        return enrollment;
    }

    @Transactional
    public SequenceEnrollmentDTO pauseEnrollment(UUID tenantId, UUID enrollmentId, String reason) {
        SequenceEnrollment enrollment = enrollmentRepository.findByIdAndTenantId(enrollmentId, tenantId)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        enrollment.pause(reason);
        enrollment = enrollmentRepository.save(enrollment);
        log.info("Paused enrollment {}", enrollmentId);
        return toEnrollmentDTO(enrollment);
    }

    @Transactional
    public SequenceEnrollmentDTO resumeEnrollment(UUID tenantId, UUID enrollmentId) {
        SequenceEnrollment enrollment = enrollmentRepository.findByIdAndTenantId(enrollmentId, tenantId)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        enrollment.resume();

        // Recalculate next email time
        EmailSequence sequence = enrollment.getSequence();
        if (enrollment.getCurrentStep() < sequence.getSteps().size()) {
            EmailSequenceStep nextStep = sequence.getSteps().get(enrollment.getCurrentStep());
            enrollment.setNextEmailAt(calculateNextEmailTime(nextStep));
        }

        enrollment = enrollmentRepository.save(enrollment);
        log.info("Resumed enrollment {}", enrollmentId);
        return toEnrollmentDTO(enrollment);
    }

    @Transactional
    public void unenroll(UUID tenantId, UUID enrollmentId) {
        SequenceEnrollment enrollment = enrollmentRepository.findByIdAndTenantId(enrollmentId, tenantId)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollment.setNextEmailAt(null);
        enrollmentRepository.save(enrollment);
        log.info("Unenrolled {}", enrollmentId);
    }

    @Transactional(readOnly = true)
    public Page<SequenceEnrollmentDTO> findEnrollmentsBySequence(UUID sequenceId, Pageable pageable) {
        return enrollmentRepository.findBySequenceId(sequenceId, pageable)
            .map(this::toEnrollmentDTO);
    }

    @Transactional(readOnly = true)
    public List<SequenceEnrollmentDTO> findActiveEnrollmentsForLead(UUID tenantId, UUID leadId) {
        return enrollmentRepository.findActiveEnrollmentsForLead(tenantId, leadId).stream()
            .map(this::toEnrollmentDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SequenceEnrollmentDTO> findActiveEnrollmentsForContact(UUID tenantId, UUID contactId) {
        return enrollmentRepository.findActiveEnrollmentsForContact(tenantId, contactId).stream()
            .map(this::toEnrollmentDTO)
            .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private LocalDateTime calculateNextEmailTime(EmailSequenceStep step) {
        LocalDateTime nextTime = LocalDateTime.now();

        // Add delay
        if (step.getDelayDays() != null && step.getDelayDays() > 0) {
            nextTime = nextTime.plusDays(step.getDelayDays());
        }
        if (step.getDelayHours() != null && step.getDelayHours() > 0) {
            nextTime = nextTime.plusHours(step.getDelayHours());
        }

        // Skip weekends if configured
        if (Boolean.TRUE.equals(step.getSkipWeekends())) {
            while (nextTime.getDayOfWeek().getValue() > 5) {
                nextTime = nextTime.plusDays(1);
            }
        }

        // Apply send time preference
        if (step.getSendTimePreference() != null) {
            try {
                String[] parts = step.getSendTimePreference().split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                nextTime = nextTime.withHour(hour).withMinute(minute);
            } catch (Exception e) {
                // Use default time
            }
        }

        return nextTime;
    }

    private void updateSequenceFromDTO(EmailSequence sequence, EmailSequenceDTO dto) {
        sequence.setName(dto.getName());
        sequence.setDescription(dto.getDescription());
        sequence.setSequenceType(dto.getSequenceType());
        sequence.setTargetType(dto.getTargetType());
        if (dto.getActive() != null) sequence.setActive(dto.getActive());
        sequence.setFromName(dto.getFromName());
        sequence.setFromEmail(dto.getFromEmail());
        sequence.setReplyToEmail(dto.getReplyToEmail());
        if (dto.getIncludeUnsubscribeLink() != null) sequence.setIncludeUnsubscribeLink(dto.getIncludeUnsubscribeLink());
        if (dto.getTrackOpens() != null) sequence.setTrackOpens(dto.getTrackOpens());
        if (dto.getTrackClicks() != null) sequence.setTrackClicks(dto.getTrackClicks());
        sequence.setOwnerUserId(dto.getOwnerUserId());
    }

    private void updateStepFromDTO(EmailSequenceStep step, EmailSequenceStepDTO dto) {
        step.setStepOrder(dto.getStepOrder());
        step.setDelayDays(dto.getDelayDays() != null ? dto.getDelayDays() : 0);
        step.setDelayHours(dto.getDelayHours() != null ? dto.getDelayHours() : 0);
        step.setSubject(dto.getSubject());
        step.setPreviewText(dto.getPreviewText());
        step.setBodyHtml(dto.getBodyHtml());
        step.setBodyText(dto.getBodyText());
        step.setTemplateId(dto.getTemplateId());
        step.setStepType(dto.getStepType() != null ? dto.getStepType() : EmailSequenceStep.StepType.EMAIL);
        if (dto.getActive() != null) step.setActive(dto.getActive());
        step.setSendTimePreference(dto.getSendTimePreference());
        if (dto.getSkipWeekends() != null) step.setSkipWeekends(dto.getSkipWeekends());
        step.setConditionField(dto.getConditionField());
        step.setConditionOperator(dto.getConditionOperator());
        step.setConditionValue(dto.getConditionValue());
    }

    private EmailSequenceDTO toSequenceDTO(EmailSequence sequence) {
        Long totalEnrollments = enrollmentRepository.countBySequenceId(sequence.getId());
        Long activeEnrollments = enrollmentRepository.countBySequenceIdAndStatus(sequence.getId(), EnrollmentStatus.ACTIVE);

        return EmailSequenceDTO.builder()
            .id(sequence.getId())
            .tenantId(sequence.getTenantId())
            .name(sequence.getName())
            .description(sequence.getDescription())
            .sequenceType(sequence.getSequenceType())
            .targetType(sequence.getTargetType())
            .active(sequence.getActive())
            .fromName(sequence.getFromName())
            .fromEmail(sequence.getFromEmail())
            .replyToEmail(sequence.getReplyToEmail())
            .includeUnsubscribeLink(sequence.getIncludeUnsubscribeLink())
            .trackOpens(sequence.getTrackOpens())
            .trackClicks(sequence.getTrackClicks())
            .ownerUserId(sequence.getOwnerUserId())
            .steps(sequence.getSteps().stream().map(this::toStepDTO).collect(Collectors.toList()))
            .stepCount(sequence.getStepCount())
            .totalDurationDays(sequence.getTotalDurationDays())
            .totalEnrollments(totalEnrollments)
            .activeEnrollments(activeEnrollments)
            .createdAt(sequence.getCreatedAt())
            .updatedAt(sequence.getUpdatedAt())
            .build();
    }

    private EmailSequenceStepDTO toStepDTO(EmailSequenceStep step) {
        return EmailSequenceStepDTO.builder()
            .id(step.getId())
            .sequenceId(step.getSequence() != null ? step.getSequence().getId() : null)
            .stepOrder(step.getStepOrder())
            .delayDays(step.getDelayDays())
            .delayHours(step.getDelayHours())
            .subject(step.getSubject())
            .previewText(step.getPreviewText())
            .bodyHtml(step.getBodyHtml())
            .bodyText(step.getBodyText())
            .templateId(step.getTemplateId())
            .stepType(step.getStepType())
            .active(step.getActive())
            .sendTimePreference(step.getSendTimePreference())
            .skipWeekends(step.getSkipWeekends())
            .conditionField(step.getConditionField())
            .conditionOperator(step.getConditionOperator())
            .conditionValue(step.getConditionValue())
            .createdAt(step.getCreatedAt())
            .updatedAt(step.getUpdatedAt())
            .build();
    }

    private SequenceEnrollmentDTO toEnrollmentDTO(SequenceEnrollment enrollment) {
        return SequenceEnrollmentDTO.builder()
            .id(enrollment.getId())
            .tenantId(enrollment.getTenantId())
            .sequenceId(enrollment.getSequence() != null ? enrollment.getSequence().getId() : null)
            .sequenceName(enrollment.getSequence() != null ? enrollment.getSequence().getName() : null)
            .leadId(enrollment.getLeadId())
            .contactId(enrollment.getContactId())
            .email(enrollment.getEmail())
            .firstName(enrollment.getFirstName())
            .lastName(enrollment.getLastName())
            .displayName(enrollment.getDisplayName())
            .currentStep(enrollment.getCurrentStep())
            .status(enrollment.getStatus())
            .nextEmailAt(enrollment.getNextEmailAt())
            .lastEmailSentAt(enrollment.getLastEmailSentAt())
            .emailsSent(enrollment.getEmailsSent())
            .emailsOpened(enrollment.getEmailsOpened())
            .emailsClicked(enrollment.getEmailsClicked())
            .emailsBounced(enrollment.getEmailsBounced())
            .enrolledByUserId(enrollment.getEnrolledByUserId())
            .pauseReason(enrollment.getPauseReason())
            .completionReason(enrollment.getCompletionReason())
            .createdAt(enrollment.getCreatedAt())
            .updatedAt(enrollment.getUpdatedAt())
            .build();
    }
}

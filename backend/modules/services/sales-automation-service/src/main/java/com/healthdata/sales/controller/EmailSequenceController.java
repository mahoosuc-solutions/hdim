package com.healthdata.sales.controller;

import com.healthdata.sales.dto.*;
import com.healthdata.sales.service.EmailAutomationService;
import com.healthdata.sales.service.EmailSequenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for email sequences and automation
 */
@RestController
@RequestMapping("/api/sales/sequences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Sequences", description = "Email sequence management and enrollment endpoints")
public class EmailSequenceController {

    private final EmailSequenceService sequenceService;
    private final EmailAutomationService automationService;

    // ==================== Sequence CRUD ====================

    @GetMapping
    @Operation(summary = "List email sequences", description = "Get all email sequences with pagination")
    public ResponseEntity<Page<EmailSequenceDTO>> findAll(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(sequenceService.findAllSequences(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sequence by ID", description = "Get email sequence details with steps")
    public ResponseEntity<EmailSequenceDTO> findById(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(sequenceService.findSequenceById(tenantId, id));
    }

    @PostMapping
    @Operation(summary = "Create email sequence", description = "Create a new email sequence")
    public ResponseEntity<EmailSequenceDTO> create(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody EmailSequenceDTO dto
    ) {
        EmailSequenceDTO created = sequenceService.createSequence(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update email sequence", description = "Update an existing email sequence")
    public ResponseEntity<EmailSequenceDTO> update(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @Valid @RequestBody EmailSequenceDTO dto
    ) {
        return ResponseEntity.ok(sequenceService.updateSequence(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete email sequence", description = "Delete a sequence (cannot have active enrollments)")
    public ResponseEntity<Void> delete(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        sequenceService.deleteSequence(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate sequence", description = "Activate an email sequence")
    public ResponseEntity<EmailSequenceDTO> activate(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(sequenceService.activateSequence(tenantId, id));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate sequence", description = "Deactivate an email sequence")
    public ResponseEntity<EmailSequenceDTO> deactivate(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(sequenceService.deactivateSequence(tenantId, id));
    }

    // ==================== Step Management ====================

    @PostMapping("/{sequenceId}/steps")
    @Operation(summary = "Add step to sequence", description = "Add a new email step to a sequence")
    public ResponseEntity<EmailSequenceDTO> addStep(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID sequenceId,
        @Valid @RequestBody EmailSequenceStepDTO stepDto
    ) {
        return ResponseEntity.ok(sequenceService.addStep(tenantId, sequenceId, stepDto));
    }

    @PutMapping("/{sequenceId}/steps/{stepId}")
    @Operation(summary = "Update step", description = "Update an existing email step")
    public ResponseEntity<EmailSequenceDTO> updateStep(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID sequenceId,
        @PathVariable UUID stepId,
        @Valid @RequestBody EmailSequenceStepDTO stepDto
    ) {
        return ResponseEntity.ok(sequenceService.updateStep(tenantId, sequenceId, stepId, stepDto));
    }

    @DeleteMapping("/{sequenceId}/steps/{stepId}")
    @Operation(summary = "Delete step", description = "Remove a step from a sequence")
    public ResponseEntity<EmailSequenceDTO> deleteStep(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID sequenceId,
        @PathVariable UUID stepId
    ) {
        return ResponseEntity.ok(sequenceService.deleteStep(tenantId, sequenceId, stepId));
    }

    // ==================== Enrollment Management ====================

    @GetMapping("/{sequenceId}/enrollments")
    @Operation(summary = "List enrollments", description = "Get all enrollments for a sequence")
    public ResponseEntity<Page<SequenceEnrollmentDTO>> getEnrollments(
        @PathVariable UUID sequenceId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(sequenceService.findEnrollmentsBySequence(sequenceId, pageable));
    }

    @PostMapping("/{sequenceId}/enroll/lead/{leadId}")
    @Operation(summary = "Enroll lead", description = "Enroll a lead in an email sequence")
    public ResponseEntity<SequenceEnrollmentDTO> enrollLead(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestHeader(value = "X-User-ID", required = false) UUID userId,
        @PathVariable UUID sequenceId,
        @PathVariable UUID leadId
    ) {
        SequenceEnrollmentDTO enrollment = sequenceService.enrollLead(tenantId, leadId, sequenceId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PostMapping("/{sequenceId}/enroll/contact/{contactId}")
    @Operation(summary = "Enroll contact", description = "Enroll a contact in an email sequence")
    public ResponseEntity<SequenceEnrollmentDTO> enrollContact(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestHeader(value = "X-User-ID", required = false) UUID userId,
        @PathVariable UUID sequenceId,
        @PathVariable UUID contactId
    ) {
        SequenceEnrollmentDTO enrollment = sequenceService.enrollContact(tenantId, contactId, sequenceId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PostMapping("/enrollments/{enrollmentId}/pause")
    @Operation(summary = "Pause enrollment", description = "Pause an active enrollment")
    public ResponseEntity<SequenceEnrollmentDTO> pauseEnrollment(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID enrollmentId,
        @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(sequenceService.pauseEnrollment(tenantId, enrollmentId, reason));
    }

    @PostMapping("/enrollments/{enrollmentId}/resume")
    @Operation(summary = "Resume enrollment", description = "Resume a paused enrollment")
    public ResponseEntity<SequenceEnrollmentDTO> resumeEnrollment(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID enrollmentId
    ) {
        return ResponseEntity.ok(sequenceService.resumeEnrollment(tenantId, enrollmentId));
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    @Operation(summary = "Unenroll", description = "Remove from sequence")
    public ResponseEntity<Void> unenroll(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID enrollmentId
    ) {
        sequenceService.unenroll(tenantId, enrollmentId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Lead/Contact Enrollment Lookup ====================

    @GetMapping("/leads/{leadId}/enrollments")
    @Operation(summary = "Get lead enrollments", description = "Get active enrollments for a lead")
    public ResponseEntity<List<SequenceEnrollmentDTO>> getLeadEnrollments(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID leadId
    ) {
        return ResponseEntity.ok(sequenceService.findActiveEnrollmentsForLead(tenantId, leadId));
    }

    @GetMapping("/contacts/{contactId}/enrollments")
    @Operation(summary = "Get contact enrollments", description = "Get active enrollments for a contact")
    public ResponseEntity<List<SequenceEnrollmentDTO>> getContactEnrollments(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID contactId
    ) {
        return ResponseEntity.ok(sequenceService.findActiveEnrollmentsForContact(tenantId, contactId));
    }

    // ==================== Manual Send ====================

    @PostMapping("/enrollments/{enrollmentId}/send")
    @Operation(summary = "Send next email", description = "Manually trigger the next email for an enrollment")
    public ResponseEntity<Map<String, Object>> sendNextEmail(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID enrollmentId
    ) {
        boolean sent = automationService.sendNextEmail(tenantId, enrollmentId);
        return ResponseEntity.ok(Map.of("sent", sent));
    }

    // ==================== Analytics ====================

    @GetMapping("/{sequenceId}/analytics")
    @Operation(summary = "Get sequence analytics", description = "Get performance analytics for a sequence")
    public ResponseEntity<EmailAutomationService.SequenceAnalytics> getAnalytics(
        @PathVariable UUID sequenceId
    ) {
        return ResponseEntity.ok(automationService.getSequenceAnalytics(sequenceId));
    }
}

package com.healthdata.sales.controller;

import com.healthdata.sales.dto.*;
import com.healthdata.sales.service.EmailAutomationService;
import com.healthdata.sales.service.EmailSequenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(
    name = "Email Sequences",
    description = """
        APIs for managing automated email sequences and enrollments.

        Email sequences enable automated multi-step email campaigns for lead nurturing:
        - Create and manage reusable email sequence templates
        - Define multi-step sequences with configurable delays
        - Enroll leads and contacts in sequences
        - Track sequence performance with analytics
        - Pause/resume enrollments as needed

        All endpoints require JWT authentication and X-Tenant-ID header.
        Sequences are isolated by tenant for multi-tenancy support.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class EmailSequenceController {

    private final EmailSequenceService sequenceService;
    private final EmailAutomationService automationService;

    // ==================== Sequence CRUD ====================

    @GetMapping
    @Operation(
        summary = "List email sequences",
        description = "Retrieves a paginated list of all email sequences for the tenant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved sequences"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<Page<EmailSequenceDTO>> findAll(
        @Parameter(description = "Tenant identifier", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(sequenceService.findAllSequences(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get sequence by ID",
        description = "Retrieves email sequence details including all steps and configuration."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved sequence"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Sequence not found")
    })
    public ResponseEntity<EmailSequenceDTO> findById(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(sequenceService.findSequenceById(tenantId, id));
    }

    @PostMapping
    @Operation(
        summary = "Create email sequence",
        description = """
            Creates a new email sequence with the specified steps.

            Sequences start in DRAFT status and must be activated to enroll leads.
            Each step defines the email content and delay before sending.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sequence successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<EmailSequenceDTO> create(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence details", required = true)
        @Valid @RequestBody EmailSequenceDTO dto
    ) {
        EmailSequenceDTO created = sequenceService.createSequence(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update email sequence",
        description = "Updates an existing email sequence. Active sequences cannot be modified."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sequence successfully updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request or sequence is active"),
        @ApiResponse(responseCode = "404", description = "Sequence not found")
    })
    public ResponseEntity<EmailSequenceDTO> update(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "Updated sequence details", required = true)
        @Valid @RequestBody EmailSequenceDTO dto
    ) {
        return ResponseEntity.ok(sequenceService.updateSequence(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete email sequence",
        description = "Deletes a sequence. Cannot delete sequences with active enrollments."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sequence deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot delete - has active enrollments"),
        @ApiResponse(responseCode = "404", description = "Sequence not found")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID id
    ) {
        sequenceService.deleteSequence(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @Operation(
        summary = "Activate sequence",
        description = "Activates a sequence so leads can be enrolled. Requires at least one step."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sequence activated"),
        @ApiResponse(responseCode = "400", description = "Cannot activate - no steps defined"),
        @ApiResponse(responseCode = "404", description = "Sequence not found")
    })
    public ResponseEntity<EmailSequenceDTO> activate(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(sequenceService.activateSequence(tenantId, id));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate sequence",
        description = "Deactivates a sequence. Existing enrollments will continue but no new enrollments allowed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sequence deactivated"),
        @ApiResponse(responseCode = "404", description = "Sequence not found")
    })
    public ResponseEntity<EmailSequenceDTO> deactivate(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(sequenceService.deactivateSequence(tenantId, id));
    }

    // ==================== Step Management ====================

    @PostMapping("/{sequenceId}/steps")
    @Operation(
        summary = "Add step to sequence",
        description = """
            Adds a new step to an email sequence.

            Step types: EMAIL (sends email), WAIT (delay), TASK (creates follow-up task).
            Steps are executed in order based on their step number.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Step added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid step configuration"),
        @ApiResponse(responseCode = "404", description = "Sequence not found")
    })
    public ResponseEntity<EmailSequenceDTO> addStep(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID sequenceId,
        @Parameter(description = "Step configuration", required = true)
        @Valid @RequestBody EmailSequenceStepDTO stepDto
    ) {
        return ResponseEntity.ok(sequenceService.addStep(tenantId, sequenceId, stepDto));
    }

    @PutMapping("/{sequenceId}/steps/{stepId}")
    @Operation(
        summary = "Update step",
        description = "Updates an existing step's configuration. Cannot modify steps in active sequences."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Step updated"),
        @ApiResponse(responseCode = "400", description = "Invalid configuration or sequence is active"),
        @ApiResponse(responseCode = "404", description = "Step or sequence not found")
    })
    public ResponseEntity<EmailSequenceDTO> updateStep(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID sequenceId,
        @Parameter(description = "Step ID", required = true)
        @PathVariable UUID stepId,
        @Parameter(description = "Updated step configuration", required = true)
        @Valid @RequestBody EmailSequenceStepDTO stepDto
    ) {
        return ResponseEntity.ok(sequenceService.updateStep(tenantId, sequenceId, stepId, stepDto));
    }

    @DeleteMapping("/{sequenceId}/steps/{stepId}")
    @Operation(
        summary = "Delete step",
        description = "Removes a step from the sequence. Remaining steps are renumbered."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Step removed"),
        @ApiResponse(responseCode = "400", description = "Cannot modify active sequence"),
        @ApiResponse(responseCode = "404", description = "Step or sequence not found")
    })
    public ResponseEntity<EmailSequenceDTO> deleteStep(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID sequenceId,
        @Parameter(description = "Step ID", required = true)
        @PathVariable UUID stepId
    ) {
        return ResponseEntity.ok(sequenceService.deleteStep(tenantId, sequenceId, stepId));
    }

    // ==================== Enrollment Management ====================

    @GetMapping("/{sequenceId}/enrollments")
    @Operation(
        summary = "List enrollments",
        description = "Retrieves all enrollments for a sequence with their current progress."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved enrollments"),
        @ApiResponse(responseCode = "404", description = "Sequence not found")
    })
    public ResponseEntity<Page<SequenceEnrollmentDTO>> getEnrollments(
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID sequenceId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(sequenceService.findEnrollmentsBySequence(sequenceId, pageable));
    }

    @PostMapping("/{sequenceId}/enroll/lead/{leadId}")
    @Operation(
        summary = "Enroll lead",
        description = """
            Enrolls a lead in an email sequence.

            The lead will start receiving automated emails based on the sequence steps.
            A lead can only be enrolled in the same sequence once (no duplicates).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lead successfully enrolled"),
        @ApiResponse(responseCode = "400", description = "Invalid enrollment - sequence inactive or lead already enrolled"),
        @ApiResponse(responseCode = "404", description = "Sequence or lead not found")
    })
    public ResponseEntity<SequenceEnrollmentDTO> enrollLead(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "User ID for attribution (optional)")
        @RequestHeader(value = "X-User-ID", required = false) UUID userId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID sequenceId,
        @Parameter(description = "Lead ID", required = true)
        @PathVariable UUID leadId
    ) {
        SequenceEnrollmentDTO enrollment = sequenceService.enrollLead(tenantId, leadId, sequenceId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PostMapping("/{sequenceId}/enroll/contact/{contactId}")
    @Operation(
        summary = "Enroll contact",
        description = """
            Enrolls a contact in an email sequence.

            Similar to lead enrollment but for converted contacts.
            Useful for ongoing nurture campaigns post-conversion.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Contact successfully enrolled"),
        @ApiResponse(responseCode = "400", description = "Invalid enrollment"),
        @ApiResponse(responseCode = "404", description = "Sequence or contact not found")
    })
    public ResponseEntity<SequenceEnrollmentDTO> enrollContact(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "User ID for attribution (optional)")
        @RequestHeader(value = "X-User-ID", required = false) UUID userId,
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID sequenceId,
        @Parameter(description = "Contact ID", required = true)
        @PathVariable UUID contactId
    ) {
        SequenceEnrollmentDTO enrollment = sequenceService.enrollContact(tenantId, contactId, sequenceId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PostMapping("/enrollments/{enrollmentId}/pause")
    @Operation(
        summary = "Pause enrollment",
        description = "Pauses an active enrollment. No further emails will be sent until resumed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Enrollment paused"),
        @ApiResponse(responseCode = "400", description = "Enrollment not in active state"),
        @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    public ResponseEntity<SequenceEnrollmentDTO> pauseEnrollment(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Enrollment ID", required = true)
        @PathVariable UUID enrollmentId,
        @Parameter(description = "Optional reason for pausing", example = "Customer requested delay")
        @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(sequenceService.pauseEnrollment(tenantId, enrollmentId, reason));
    }

    @PostMapping("/enrollments/{enrollmentId}/resume")
    @Operation(
        summary = "Resume enrollment",
        description = "Resumes a paused enrollment. Continues from where it left off."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Enrollment resumed"),
        @ApiResponse(responseCode = "400", description = "Enrollment not in paused state"),
        @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    public ResponseEntity<SequenceEnrollmentDTO> resumeEnrollment(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Enrollment ID", required = true)
        @PathVariable UUID enrollmentId
    ) {
        return ResponseEntity.ok(sequenceService.resumeEnrollment(tenantId, enrollmentId));
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    @Operation(
        summary = "Unenroll",
        description = "Removes a lead/contact from the sequence. No further emails will be sent."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Successfully unenrolled"),
        @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    public ResponseEntity<Void> unenroll(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Enrollment ID", required = true)
        @PathVariable UUID enrollmentId
    ) {
        sequenceService.unenroll(tenantId, enrollmentId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Lead/Contact Enrollment Lookup ====================

    @GetMapping("/leads/{leadId}/enrollments")
    @Operation(
        summary = "Get lead enrollments",
        description = "Retrieves all active sequence enrollments for a specific lead."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved enrollments"),
        @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    public ResponseEntity<List<SequenceEnrollmentDTO>> getLeadEnrollments(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Lead ID", required = true)
        @PathVariable UUID leadId
    ) {
        return ResponseEntity.ok(sequenceService.findActiveEnrollmentsForLead(tenantId, leadId));
    }

    @GetMapping("/contacts/{contactId}/enrollments")
    @Operation(
        summary = "Get contact enrollments",
        description = "Retrieves all active sequence enrollments for a specific contact."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved enrollments"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<List<SequenceEnrollmentDTO>> getContactEnrollments(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Contact ID", required = true)
        @PathVariable UUID contactId
    ) {
        return ResponseEntity.ok(sequenceService.findActiveEnrollmentsForContact(tenantId, contactId));
    }

    // ==================== Manual Send ====================

    @PostMapping("/enrollments/{enrollmentId}/send")
    @Operation(
        summary = "Send next email",
        description = """
            Manually triggers the next scheduled email for an enrollment.

            Useful for testing or when immediate follow-up is needed.
            Bypasses the normal delay timer.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email send attempted",
            content = @Content(schema = @Schema(example = "{\"sent\": true}"))),
        @ApiResponse(responseCode = "400", description = "No pending emails or enrollment not active"),
        @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    public ResponseEntity<Map<String, Object>> sendNextEmail(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Enrollment ID", required = true)
        @PathVariable UUID enrollmentId
    ) {
        boolean sent = automationService.sendNextEmail(tenantId, enrollmentId);
        return ResponseEntity.ok(Map.of("sent", sent));
    }

    // ==================== Analytics ====================

    @GetMapping("/{sequenceId}/analytics")
    @Operation(
        summary = "Get sequence analytics",
        description = """
            Retrieves performance analytics for an email sequence.

            Includes metrics such as:
            - Total emails sent
            - Open rate
            - Click rate
            - Reply rate
            - Enrollment completion rate
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved analytics"),
        @ApiResponse(responseCode = "404", description = "Sequence not found")
    })
    public ResponseEntity<EmailAutomationService.SequenceAnalytics> getAnalytics(
        @Parameter(description = "Sequence ID", required = true)
        @PathVariable UUID sequenceId
    ) {
        return ResponseEntity.ok(automationService.getSequenceAnalytics(sequenceId));
    }
}

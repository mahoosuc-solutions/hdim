package com.healthdata.sales.controller;

import com.healthdata.sales.dto.ActivityDTO;
import com.healthdata.sales.entity.ActivityType;
import com.healthdata.sales.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/sales/activities")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Activities",
    description = """
        APIs for managing sales activities and tasks.

        Activities track all sales engagement with leads, contacts, and accounts:
        - CALL: Phone calls and voicemails
        - EMAIL: Email correspondence
        - MEETING: In-person or virtual meetings
        - DEMO: Product demonstrations
        - TASK: Follow-up tasks and reminders
        - NOTE: General notes and observations

        Activities can be associated with leads, contacts, accounts, or opportunities.
        They support scheduling, assignment, completion tracking, and outcome recording.

        All endpoints require JWT authentication and X-Tenant-ID header.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(
        summary = "List all activities",
        description = "Retrieves a paginated list of all sales activities for the tenant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved activities"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    })
    public ResponseEntity<Page<ActivityDTO>> findAll(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters (page, size, sort)")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get activity by ID",
        description = "Retrieves detailed information about a specific activity."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved activity"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<ActivityDTO> findById(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Activity ID", required = true)
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(activityService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(
        summary = "Create a new activity",
        description = """
            Creates a new sales activity.

            Required fields: type, subject, dueDate
            Optional: description, assignedTo, leadId, contactId, accountId, opportunityId
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Activity created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors")
    })
    public ResponseEntity<ActivityDTO> create(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Activity details", required = true)
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing activity",
        description = "Updates an existing activity with new information."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Activity updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<ActivityDTO> update(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Activity ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "Updated activity details", required = true)
        @Valid @RequestBody ActivityDTO dto
    ) {
        return ResponseEntity.ok(activityService.update(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete an activity",
        description = "Deletes a sales activity."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Activity deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Activity ID", required = true)
        @PathVariable UUID id
    ) {
        activityService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @Operation(
        summary = "Mark activity as complete",
        description = "Marks an activity as completed with an optional outcome description."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Activity marked as complete"),
        @ApiResponse(responseCode = "400", description = "Activity already completed"),
        @ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ResponseEntity<ActivityDTO> markComplete(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Activity ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "Outcome description", example = "Left voicemail, will call back")
        @RequestParam(required = false) String outcome
    ) {
        return ResponseEntity.ok(activityService.markComplete(tenantId, id, outcome));
    }

    // ==================== Filter Endpoints ====================

    @GetMapping("/lead/{leadId}")
    @Operation(
        summary = "Get activities for a lead",
        description = "Retrieves all activities associated with a specific lead."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved lead activities"),
        @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    public ResponseEntity<Page<ActivityDTO>> findByLead(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Lead ID", required = true)
        @PathVariable UUID leadId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByLead(tenantId, leadId, pageable));
    }

    @GetMapping("/contact/{contactId}")
    @Operation(
        summary = "Get activities for a contact",
        description = "Retrieves all activities associated with a specific contact."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contact activities"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<Page<ActivityDTO>> findByContact(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Contact ID", required = true)
        @PathVariable UUID contactId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByContact(tenantId, contactId, pageable));
    }

    @GetMapping("/opportunity/{opportunityId}")
    @Operation(
        summary = "Get activities for an opportunity",
        description = "Retrieves all activities associated with a specific opportunity/deal."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved opportunity activities"),
        @ApiResponse(responseCode = "404", description = "Opportunity not found")
    })
    public ResponseEntity<Page<ActivityDTO>> findByOpportunity(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Opportunity ID", required = true)
        @PathVariable UUID opportunityId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByOpportunity(tenantId, opportunityId, pageable));
    }

    @GetMapping("/account/{accountId}")
    @Operation(
        summary = "Get activities for an account",
        description = "Retrieves all activities associated with a specific account/organization."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved account activities"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<Page<ActivityDTO>> findByAccount(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID accountId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByAccount(tenantId, accountId, pageable));
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "Get activities by type",
        description = "Retrieves activities filtered by type (CALL, EMAIL, MEETING, DEMO, TASK, NOTE)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved activities by type"),
        @ApiResponse(responseCode = "400", description = "Invalid activity type")
    })
    public ResponseEntity<Page<ActivityDTO>> findByType(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Activity type", required = true,
            schema = @Schema(allowableValues = {"CALL", "EMAIL", "MEETING", "DEMO", "TASK", "NOTE"}))
        @PathVariable ActivityType type,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByType(tenantId, type, pageable));
    }

    @GetMapping("/assigned/{userId}")
    @Operation(
        summary = "Get activities assigned to a user",
        description = "Retrieves all activities assigned to a specific user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's activities")
    })
    public ResponseEntity<Page<ActivityDTO>> findByAssignedUser(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID userId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByAssignedUser(tenantId, userId, pageable));
    }

    // ==================== Task Views ====================

    @GetMapping("/overdue")
    @Operation(
        summary = "Get overdue activities",
        description = "Retrieves incomplete activities that are past their due date. Critical for sales follow-up."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved overdue activities")
    })
    public ResponseEntity<Page<ActivityDTO>> findOverdueActivities(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findOverdueActivities(tenantId, pageable));
    }

    @GetMapping("/upcoming")
    @Operation(
        summary = "Get upcoming activities",
        description = "Retrieves activities scheduled within the specified number of days ahead."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved upcoming activities")
    })
    public ResponseEntity<Page<ActivityDTO>> findUpcomingActivities(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Number of days ahead to look", example = "7",
            schema = @Schema(defaultValue = "7", minimum = "1", maximum = "90"))
        @RequestParam(defaultValue = "7") int daysAhead,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findUpcomingActivities(tenantId, daysAhead, pageable));
    }

    @GetMapping("/my-pending")
    @Operation(
        summary = "Get my pending activities",
        description = "Retrieves pending (incomplete) activities assigned to the current user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's pending activities")
    })
    public ResponseEntity<Page<ActivityDTO>> findMyPendingActivities(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Current user ID", required = true)
        @RequestHeader("X-User-ID") UUID userId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findPendingActivitiesForUser(tenantId, userId, pageable));
    }

    @GetMapping("/pending/count")
    @Operation(
        summary = "Count pending activities",
        description = "Returns the total count of pending (incomplete) activities for the tenant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved pending count")
    })
    public ResponseEntity<Long> countPendingActivities(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(activityService.countPendingActivities(tenantId));
    }

    // ==================== Quick Log Endpoints ====================

    @PostMapping("/log/call")
    @Operation(
        summary = "Log a call",
        description = """
            Quick endpoint to log a call activity.

            Automatically sets type to CALL and marks as completed with the current timestamp.
            Use for logging calls that have already occurred.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Call logged successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors")
    })
    public ResponseEntity<ActivityDTO> logCall(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Call details", required = true)
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.logCall(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/log/email")
    @Operation(
        summary = "Log an email",
        description = """
            Quick endpoint to log an email activity.

            Automatically sets type to EMAIL and marks as completed.
            Use for logging emails sent outside the automated system.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Email logged successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors")
    })
    public ResponseEntity<ActivityDTO> logEmail(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Email details", required = true)
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.logEmail(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/log/meeting")
    @Operation(
        summary = "Log a meeting",
        description = """
            Quick endpoint to log a meeting activity.

            Automatically sets type to MEETING and marks as completed.
            Use for logging meetings that have already occurred.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Meeting logged successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors")
    })
    public ResponseEntity<ActivityDTO> logMeeting(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Meeting details", required = true)
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.logMeeting(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/schedule/demo")
    @Operation(
        summary = "Schedule a demo",
        description = """
            Schedule a product demonstration activity.

            Creates a DEMO type activity with status PENDING.
            Requires a future dueDate for the scheduled demo time.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Demo scheduled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors or past date")
    })
    public ResponseEntity<ActivityDTO> scheduleDemo(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Demo details", required = true)
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.scheduleDemo(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/task")
    @Operation(
        summary = "Create a task",
        description = """
            Create a follow-up task.

            Creates a TASK type activity with status PENDING.
            Use for reminders, follow-ups, and to-do items.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors")
    })
    public ResponseEntity<ActivityDTO> createTask(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Task details", required = true)
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.createTask(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

package com.healthdata.sales.controller;

import com.healthdata.sales.dto.ActivityDTO;
import com.healthdata.sales.entity.ActivityType;
import com.healthdata.sales.service.ActivityService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/sales/activities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Activities", description = "Activity/Task management endpoints")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "List all activities", description = "Get paginated list of activities")
    public ResponseEntity<Page<ActivityDTO>> findAll(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID")
    public ResponseEntity<ActivityDTO> findById(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(activityService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new activity")
    public ResponseEntity<ActivityDTO> create(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing activity")
    public ResponseEntity<ActivityDTO> update(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @Valid @RequestBody ActivityDTO dto
    ) {
        return ResponseEntity.ok(activityService.update(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an activity")
    public ResponseEntity<Void> delete(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        activityService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark activity as complete")
    public ResponseEntity<ActivityDTO> markComplete(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @RequestParam(required = false) String outcome
    ) {
        return ResponseEntity.ok(activityService.markComplete(tenantId, id, outcome));
    }

    // ==================== Filter Endpoints ====================

    @GetMapping("/lead/{leadId}")
    @Operation(summary = "Get activities for a lead")
    public ResponseEntity<Page<ActivityDTO>> findByLead(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID leadId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByLead(tenantId, leadId, pageable));
    }

    @GetMapping("/contact/{contactId}")
    @Operation(summary = "Get activities for a contact")
    public ResponseEntity<Page<ActivityDTO>> findByContact(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID contactId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByContact(tenantId, contactId, pageable));
    }

    @GetMapping("/opportunity/{opportunityId}")
    @Operation(summary = "Get activities for an opportunity")
    public ResponseEntity<Page<ActivityDTO>> findByOpportunity(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID opportunityId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByOpportunity(tenantId, opportunityId, pageable));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get activities for an account")
    public ResponseEntity<Page<ActivityDTO>> findByAccount(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID accountId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByAccount(tenantId, accountId, pageable));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get activities by type", description = "Filter by CALL, EMAIL, MEETING, DEMO, etc.")
    public ResponseEntity<Page<ActivityDTO>> findByType(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable ActivityType type,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByType(tenantId, type, pageable));
    }

    @GetMapping("/assigned/{userId}")
    @Operation(summary = "Get activities assigned to a user")
    public ResponseEntity<Page<ActivityDTO>> findByAssignedUser(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID userId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findByAssignedUser(tenantId, userId, pageable));
    }

    // ==================== Task Views ====================

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue activities", description = "Past due incomplete activities")
    public ResponseEntity<Page<ActivityDTO>> findOverdueActivities(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findOverdueActivities(tenantId, pageable));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming activities", description = "Activities scheduled in the next N days")
    public ResponseEntity<Page<ActivityDTO>> findUpcomingActivities(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestParam(defaultValue = "7") int daysAhead,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findUpcomingActivities(tenantId, daysAhead, pageable));
    }

    @GetMapping("/my-pending")
    @Operation(summary = "Get my pending activities", description = "Pending activities for current user")
    public ResponseEntity<Page<ActivityDTO>> findMyPendingActivities(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestHeader("X-User-ID") UUID userId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(activityService.findPendingActivitiesForUser(tenantId, userId, pageable));
    }

    @GetMapping("/pending/count")
    @Operation(summary = "Count pending activities")
    public ResponseEntity<Long> countPendingActivities(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        return ResponseEntity.ok(activityService.countPendingActivities(tenantId));
    }

    // ==================== Quick Log Endpoints ====================

    @PostMapping("/log/call")
    @Operation(summary = "Log a call", description = "Quick log a call activity")
    public ResponseEntity<ActivityDTO> logCall(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.logCall(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/log/email")
    @Operation(summary = "Log an email", description = "Quick log an email activity")
    public ResponseEntity<ActivityDTO> logEmail(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.logEmail(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/log/meeting")
    @Operation(summary = "Log a meeting", description = "Quick log a meeting activity")
    public ResponseEntity<ActivityDTO> logMeeting(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.logMeeting(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/schedule/demo")
    @Operation(summary = "Schedule a demo", description = "Schedule a demo activity")
    public ResponseEntity<ActivityDTO> scheduleDemo(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.scheduleDemo(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/task")
    @Operation(summary = "Create a task", description = "Create a follow-up task")
    public ResponseEntity<ActivityDTO> createTask(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody ActivityDTO dto
    ) {
        ActivityDTO created = activityService.createTask(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

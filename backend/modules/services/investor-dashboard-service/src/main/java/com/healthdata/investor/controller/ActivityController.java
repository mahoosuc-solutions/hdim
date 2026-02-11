package com.healthdata.investor.controller;

import com.healthdata.investor.dto.ActivityDTO;
import com.healthdata.investor.security.UserPrincipal;
import com.healthdata.investor.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller for managing outreach activities.
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activities", description = "Outreach activity management endpoints")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "Get all activities")
    public ResponseEntity<List<ActivityDTO>> getAllActivities() {
        return ResponseEntity.ok(activityService.getAllActivities());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID")
    public ResponseEntity<ActivityDTO> getActivity(@PathVariable UUID id) {
        return ResponseEntity.ok(activityService.getActivity(id));
    }

    @GetMapping("/contact/{contactId}")
    @Operation(summary = "Get activities for a contact")
    public ResponseEntity<List<ActivityDTO>> getActivitiesByContact(@PathVariable UUID contactId) {
        return ResponseEntity.ok(activityService.getActivitiesByContact(contactId));
    }

    @GetMapping("/type/{activityType}")
    @Operation(summary = "Get activities by type")
    public ResponseEntity<List<ActivityDTO>> getActivitiesByType(@PathVariable String activityType) {
        return ResponseEntity.ok(activityService.getActivitiesByType(activityType));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get activities within a date range")
    public ResponseEntity<List<ActivityDTO>> getActivitiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(activityService.getActivitiesByDateRange(start, end));
    }

    @GetMapping("/linkedin")
    @Operation(summary = "Get all LinkedIn activities")
    public ResponseEntity<List<ActivityDTO>> getLinkedInActivities() {
        return ResponseEntity.ok(activityService.getLinkedInActivities());
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending scheduled activities")
    public ResponseEntity<List<ActivityDTO>> getPendingActivities() {
        return ResponseEntity.ok(activityService.getPendingScheduledActivities());
    }

    @PostMapping
    @Operation(summary = "Create a new activity")
    public ResponseEntity<ActivityDTO> createActivity(
            @Valid @RequestBody ActivityDTO.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.createActivity(request, principal.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an activity")
    public ResponseEntity<ActivityDTO> updateActivity(
            @PathVariable UUID id,
            @RequestBody ActivityDTO.UpdateRequest request) {
        return ResponseEntity.ok(activityService.updateActivity(id, request));
    }

    @PatchMapping("/{id}/responded")
    @Operation(summary = "Mark activity as responded")
    public ResponseEntity<ActivityDTO> markAsResponded(
            @PathVariable UUID id,
            @RequestParam(required = false) String responseContent) {
        return ResponseEntity.ok(activityService.markAsResponded(id, responseContent));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an activity")
    public ResponseEntity<Void> deleteActivity(@PathVariable UUID id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }
}

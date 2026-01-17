package com.healthdata.clinicalworkflow.api.v1;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.application.RoomManagementService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for exam room management
 *
 * Handles room assignments, status updates, and room availability tracking.
 * Provides real-time room status board for clinical staff.
 *
 * Security: All endpoints require authentication and X-Tenant-ID header
 * HIPAA: All operations are audited for compliance
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Validated
@Tag(name = "Room Management", description = "Exam room tracking and assignment")
@SecurityRequirement(name = "bearer-jwt")
public class RoomController {

    private final RoomManagementService roomService;

    /**
     * Get room status board for all rooms
     */
    @GetMapping
    @Operation(
        summary = "Get room status board",
        description = "Retrieve status of all exam rooms with current occupancy and wait times"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Room board retrieved",
            content = @Content(schema = @Schema(implementation = RoomBoardResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<RoomBoardResponse> getRoomBoard(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {

        RoomBoardResponse response = roomService.getRoomBoard(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get details for a specific room
     */
    @GetMapping("/{roomNumber}")
    @Operation(
        summary = "Get room details",
        description = "Retrieve current status and assignment details for a specific room"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Room details retrieved",
            content = @Content(schema = @Schema(implementation = RoomStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Room not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT', 'PROVIDER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<RoomStatusResponse> getRoomDetails(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Room number", required = true, example = "Room 3")
            @PathVariable String roomNumber) {

        RoomStatusResponse response = roomService.getRoomDetails(tenantId, roomNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Get available rooms only
     */
    @GetMapping("/available")
    @Operation(
        summary = "Get available rooms",
        description = "Retrieve list of currently available rooms ready for patient assignment"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Available rooms retrieved",
            content = @Content(schema = @Schema(implementation = RoomStatusResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<List<RoomStatusResponse>> getAvailableRooms(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<RoomStatusResponse> rooms = roomService.getAvailableRooms(tenantId);
        return ResponseEntity.ok(rooms);
    }

    /**
     * Assign patient to a room
     *
     * Example request:
     * <pre>
     * POST /api/v1/rooms/Room%203/assign
     * X-Tenant-ID: TENANT001
     * {
     *   "patientId": "PATIENT001",
     *   "encounterId": "ENC001",
     *   "priority": "ROUTINE",
     *   "providerId": "PROV001"
     * }
     * </pre>
     */
    @PostMapping("/{roomNumber}/assign")
    @Operation(
        summary = "Assign patient to room",
        description = "Assign a patient to an available exam room. Room must be in AVAILABLE status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Patient assigned to room",
            content = @Content(schema = @Schema(implementation = RoomStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or room not available",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Room already occupied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<RoomStatusResponse> assignPatientToRoom(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User making assignment", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Room number", required = true)
            @PathVariable String roomNumber,
            @Parameter(description = "Room assignment details", required = true)
            @Valid @RequestBody RoomAssignmentRequest request) {

        RoomStatusResponse response = roomService.assignPatientToRoom(tenantId, roomNumber, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update room status
     */
    @PutMapping("/{roomNumber}/status")
    @Operation(
        summary = "Update room status",
        description = "Update the operational status of a room (AVAILABLE, OCCUPIED, CLEANING, OUT_OF_SERVICE)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Room status updated",
            content = @Content(schema = @Schema(implementation = RoomStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Room not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<RoomStatusResponse> updateRoomStatus(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User updating status", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Room number", required = true)
            @PathVariable String roomNumber,
            @Parameter(description = "Status update details", required = true)
            @Valid @RequestBody RoomStatusUpdateRequest request) {

        RoomStatusResponse response = roomService.updateRoomStatus(tenantId, roomNumber, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark room as ready
     */
    @PutMapping("/{roomNumber}/ready")
    @Operation(
        summary = "Mark room as ready",
        description = "Mark room as cleaned and ready for next patient (changes status to AVAILABLE)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Room marked as ready",
            content = @Content(schema = @Schema(implementation = RoomStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Room not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'MEDICAL_ASSISTANT')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<RoomStatusResponse> markRoomReady(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User marking room ready", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Room number", required = true)
            @PathVariable String roomNumber) {

        RoomStatusResponse response = roomService.markRoomReady(tenantId, roomNumber, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Discharge patient from room
     */
    @PostMapping("/{roomNumber}/discharge")
    @Operation(
        summary = "Discharge patient from room",
        description = "Remove patient from room and change status to CLEANING"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Patient discharged from room",
            content = @Content(schema = @Schema(implementation = RoomStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Room not found or not occupied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'PROVIDER')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<RoomStatusResponse> dischargePatient(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User performing discharge", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Room number", required = true)
            @PathVariable String roomNumber) {

        RoomStatusResponse response = roomService.dischargePatient(tenantId, roomNumber, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Schedule room cleaning
     */
    @PostMapping("/{roomNumber}/schedule-cleaning")
    @Operation(
        summary = "Schedule room cleaning",
        description = "Schedule room for cleaning (changes status to CLEANING)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cleaning scheduled",
            content = @Content(schema = @Schema(implementation = RoomStatusResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Room not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    public ResponseEntity<RoomStatusResponse> scheduleCleaning(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User scheduling cleaning", required = false)
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Room number", required = true)
            @PathVariable String roomNumber) {

        RoomStatusResponse response = roomService.scheduleCleaning(tenantId, roomNumber, userId);
        return ResponseEntity.ok(response);
    }
}

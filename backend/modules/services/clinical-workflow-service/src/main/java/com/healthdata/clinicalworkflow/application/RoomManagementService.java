package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.api.v1.dto.RoomAssignmentRequest;
import com.healthdata.clinicalworkflow.api.v1.dto.RoomStatusUpdateRequest;
import com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity;
import com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Room Management Service
 *
 * Manages exam room allocation and status for Medical Assistants:
 * - Room assignment to patients
 * - Room status tracking (available, occupied, cleaning, reserved)
 * - Room cleaning workflow
 * - Occupancy board display
 *
 * HIPAA Compliance:
 * - All methods enforce multi-tenant isolation
 * - Audit logging via @Audited annotations (to be added at controller level)
 * - Cache TTL <= 5 minutes for PHI data
 *
 * Integration Points:
 * - FHIR Encounter: Links room to encounter
 * - FHIR Location: Maps to physical location resources
 * - WebSocket: Real-time room status updates
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoomManagementService {

    private final RoomAssignmentRepository roomRepository;
    private final CacheManager cacheManager;

    /**
     * Assign room to patient
     *
     * Main workflow for room assignment. Validates room availability
     * and creates assignment record.
     *
     * @param patientId the patient ID
     * @param appointmentId the appointment ID (optional)
     * @param tenantId the tenant ID
     * @return created room assignment
     */
    @Transactional
    public RoomAssignmentEntity assignRoom(UUID patientId, String appointmentId, String tenantId) {
        log.debug("Assigning room for patient {} in tenant {}", patientId, tenantId);

        // Check if patient already has active room
        Optional<RoomAssignmentEntity> existingRoom = roomRepository
                .findActiveRoomForPatient(tenantId, patientId);
        if (existingRoom.isPresent()) {
            throw new IllegalStateException(
                    "Patient already assigned to room: " + existingRoom.get().getRoomNumber());
        }

        // Find available room
        List<RoomAssignmentEntity> availableRooms = roomRepository
                .findAvailableRoomsByTenant(tenantId);
        if (availableRooms.isEmpty()) {
            throw new IllegalStateException("No available rooms in tenant: " + tenantId);
        }

        RoomAssignmentEntity availableRoom = availableRooms.get(0);

        // Create new assignment with occupied status
        RoomAssignmentEntity assignment = RoomAssignmentEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .appointmentId(appointmentId)
                .roomNumber(availableRoom.getRoomNumber())
                .location(availableRoom.getLocation())
                .roomType(availableRoom.getRoomType())
                .status("occupied")
                .assignedBy("system") // Set by controller
                .assignedAt(Instant.now())
                .build();

        RoomAssignmentEntity saved = roomRepository.save(assignment);

        log.info("Room {} assigned to patient {} in tenant {}",
                saved.getRoomNumber(), patientId, tenantId);

        return saved;
    }

    /**
     * Mark room as ready
     *
     * Updates room status to ready after cleaning or setup.
     *
     * @param roomNumber the room number
     * @param tenantId the tenant ID
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity markRoomReady(String roomNumber, String tenantId) {
        log.debug("Marking room {} ready in tenant {}", roomNumber, tenantId);

        RoomAssignmentEntity room = roomRepository.findRoomByNumberAndTenant(roomNumber, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found: " + roomNumber));

        room.setStatus("available");
        room.setRoomReadyAt(Instant.now());

        // If cleaning in progress, mark completed
        if (room.getCleaningStartedAt() != null && room.getCleaningCompletedAt() == null) {
            room.setCleaningCompletedAt(Instant.now());
        }

        RoomAssignmentEntity updated = roomRepository.save(room);

        log.info("Room {} marked ready in tenant {}", roomNumber, tenantId);

        return updated;
    }

    /**
     * Discharge patient from room
     *
     * Updates room status when patient leaves.
     *
     * @param roomNumber the room number
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity dischargePatient(String roomNumber, UUID patientId, String tenantId) {
        log.debug("Discharging patient {} from room {} in tenant {}",
                patientId, roomNumber, tenantId);

        RoomAssignmentEntity room = roomRepository.findActiveRoomForPatient(tenantId, patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active room for patient: " + patientId));

        if (!room.getRoomNumber().equals(roomNumber)) {
            throw new IllegalArgumentException(
                    "Patient not in room: " + roomNumber);
        }

        room.setDischargedAt(Instant.now());
        room.setStatus("cleaning"); // Room needs cleaning after discharge

        RoomAssignmentEntity updated = roomRepository.save(room);

        log.info("Patient {} discharged from room {} in tenant {}",
                patientId, roomNumber, tenantId);

        return updated;
    }

    /**
     * Schedule room cleaning
     *
     * Initiates cleaning workflow for room.
     *
     * @param roomNumber the room number
     * @param cleaningMinutes estimated cleaning time
     * @param tenantId the tenant ID
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity scheduleRoomCleaning(
            String roomNumber, int cleaningMinutes, String tenantId) {
        log.debug("Scheduling cleaning for room {} in tenant {}", roomNumber, tenantId);

        RoomAssignmentEntity room = roomRepository.findRoomByNumberAndTenant(roomNumber, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found: " + roomNumber));

        room.setStatus("cleaning");
        room.setCleaningStartedAt(Instant.now());

        RoomAssignmentEntity updated = roomRepository.save(room);

        log.info("Cleaning scheduled for room {} in tenant {} (estimated {} minutes)",
                roomNumber, tenantId, cleaningMinutes);

        return updated;
    }

    /**
     * Mark room as out of service
     *
     * Removes room from available pool due to maintenance, repair, or safety issues.
     * Room cannot be assigned to patients until restored.
     *
     * @param roomNumber the room number
     * @param tenantId the tenant ID
     * @param reason optional reason for out-of-service status
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity markRoomOutOfService(
            String roomNumber, String tenantId, String reason) {
        log.debug("Marking room {} out of service in tenant {}: {}",
                roomNumber, tenantId, reason);

        RoomAssignmentEntity room = roomRepository.findRoomByNumberAndTenant(roomNumber, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found: " + roomNumber));

        // Ensure room is not currently occupied
        if ("occupied".equalsIgnoreCase(room.getStatus())) {
            throw new IllegalStateException(
                    "Cannot mark occupied room as out of service: " + roomNumber);
        }

        room.setStatus("out-of-service");

        // Store reason in notes field
        if (reason != null && !reason.isBlank()) {
            String existingNotes = room.getNotes() != null ? room.getNotes() : "";
            String outOfServiceNote = String.format(
                    "[OUT OF SERVICE] %s - %s",
                    Instant.now(),
                    reason
            );
            room.setNotes(existingNotes.isBlank() ? outOfServiceNote : existingNotes + "\n" + outOfServiceNote);
        }

        RoomAssignmentEntity updated = roomRepository.save(room);

        log.info("Room {} marked out of service in tenant {}", roomNumber, tenantId);

        // Evict cache to prevent out-of-service room from appearing in available list
        evictAvailableRoomsCache(tenantId);

        return updated;
    }

    /**
     * Restore room from out-of-service status
     *
     * Returns room to available pool after maintenance/repair completed.
     * Marks room as available and ready for patient assignment.
     *
     * @param roomNumber the room number
     * @param tenantId the tenant ID
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity restoreRoomFromOutOfService(
            String roomNumber, String tenantId) {
        log.debug("Restoring room {} from out of service in tenant {}", roomNumber, tenantId);

        RoomAssignmentEntity room = roomRepository.findRoomByNumberAndTenant(roomNumber, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found: " + roomNumber));

        if (!"out-of-service".equalsIgnoreCase(room.getStatus())) {
            throw new IllegalStateException(
                    "Room is not out of service: " + roomNumber);
        }

        room.setStatus("available");
        room.setRoomReadyAt(Instant.now());

        // Add restoration note
        String existingNotes = room.getNotes() != null ? room.getNotes() : "";
        String restorationNote = String.format(
                "[RESTORED] %s - Room returned to service",
                Instant.now()
        );
        room.setNotes(existingNotes.isBlank() ? restorationNote : existingNotes + "\n" + restorationNote);

        RoomAssignmentEntity updated = roomRepository.save(room);

        log.info("Room {} restored to service in tenant {}", roomNumber, tenantId);

        // Evict cache to include restored room in available list
        evictAvailableRoomsCache(tenantId);

        return updated;
    }

    /**
     * Evict available rooms cache for tenant
     *
     * @param tenantId the tenant ID
     */
    private void evictAvailableRoomsCache(String tenantId) {
        try {
            cacheManager.getCache("availableRooms").evict(tenantId);
            log.debug("Evicted available rooms cache for tenant {}", tenantId);
        } catch (Exception e) {
            log.warn("Failed to evict available rooms cache for tenant {}: {}",
                    tenantId, e.getMessage());
        }
    }

    /**
     * Get available rooms
     *
     * @param tenantId the tenant ID
     * @return list of available rooms
     */
    @Cacheable(value = "availableRooms", key = "#tenantId")
    public List<RoomAssignmentEntity> getAvailableRooms(String tenantId) {
        log.debug("Retrieving available rooms for tenant {}", tenantId);

        return roomRepository.findAvailableRoomsByTenant(tenantId);
    }

    /**
     * Get room status
     *
     * @param roomNumber the room number
     * @param tenantId the tenant ID
     * @return room assignment
     */
    public RoomAssignmentEntity getRoomStatus(String roomNumber, String tenantId) {
        log.debug("Retrieving status for room {} in tenant {}", roomNumber, tenantId);

        return roomRepository.findRoomByNumberAndTenant(roomNumber, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found: " + roomNumber));
    }

    /**
     * Get occupancy board
     *
     * Retrieves all non-available rooms for display board.
     *
     * @param tenantId the tenant ID
     * @return list of room assignments
     */
    @Cacheable(value = "occupancyBoard", key = "#tenantId")
    public List<RoomAssignmentEntity> getOccupancyBoard(String tenantId) {
        log.debug("Retrieving occupancy board for tenant {}", tenantId);

        return roomRepository.findOccupancyBoard(tenantId);
    }

    /**
     * Calculate occupancy duration
     *
     * @param roomAssignmentId the room assignment ID
     * @param tenantId the tenant ID
     * @return occupancy duration in minutes
     */
    public Integer calculateOccupancyDuration(UUID roomAssignmentId, String tenantId) {
        log.debug("Calculating occupancy duration for assignment {} in tenant {}",
                roomAssignmentId, tenantId);

        RoomAssignmentEntity room = roomRepository.findByIdAndTenantId(roomAssignmentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room assignment not found: " + roomAssignmentId));

        return room.getOccupancyDurationMinutes();
    }

    /**
     * Get room by ID
     *
     * @param roomAssignmentId the room assignment ID
     * @param tenantId the tenant ID
     * @return room assignment
     */
    public RoomAssignmentEntity getRoomById(UUID roomAssignmentId, String tenantId) {
        log.debug("Retrieving room assignment {} in tenant {}", roomAssignmentId, tenantId);

        return roomRepository.findByIdAndTenantId(roomAssignmentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room assignment not found: " + roomAssignmentId));
    }

    /**
     * Get rooms by status
     *
     * @param status the status filter
     * @param tenantId the tenant ID
     * @return list of room assignments
     */
    public List<RoomAssignmentEntity> getRoomsByStatus(String status, String tenantId) {
        log.debug("Retrieving rooms with status {} in tenant {}", status, tenantId);

        return roomRepository.findByTenantIdAndStatusOrderByAssignedAtDesc(tenantId, status);
    }

    /**
     * Get room history
     *
     * @param roomNumber the room number
     * @param tenantId the tenant ID
     * @return list of room assignments
     */
    public List<RoomAssignmentEntity> getRoomHistory(String roomNumber, String tenantId) {
        log.debug("Retrieving history for room {} in tenant {}", roomNumber, tenantId);

        return roomRepository.findByTenantIdAndRoomNumberOrderByAssignedAtDesc(
                tenantId, roomNumber);
    }

    /**
     * Count available rooms
     *
     * @param tenantId the tenant ID
     * @return count of available rooms
     */
    public long countAvailableRooms(String tenantId) {
        return roomRepository.countByTenantIdAndStatus(tenantId, "available");
    }

    /**
     * Get occupied rooms
     *
     * @param tenantId the tenant ID
     * @return list of occupied room assignments
     */
    public List<RoomAssignmentEntity> getOccupiedRooms(String tenantId) {
        log.debug("Retrieving occupied rooms for tenant {}", tenantId);

        return roomRepository.findCurrentOccupantsByTenant(tenantId);
    }

    // ============ TIER 1 FIXES ============

    /**
     * 3a. Rename getOccupancyBoard → getRoomBoard (Line 68)
     * Adapter method that delegates to existing getOccupancyBoard
     *
     * @param tenantId the tenant ID
     * @return list of room assignments
     */
    @Cacheable(value = "occupancyBoard", key = "#tenantId")
    public List<RoomAssignmentEntity> getRoomBoard(String tenantId) {
        return getOccupancyBoard(tenantId);  // Delegate to existing
    }

    /**
     * 3b. Rename/alias getRoomStatus → getRoomDetails (Line 100)
     * Adapter method that swaps parameter order
     *
     * @param tenantId the tenant ID
     * @param roomNumber the room number
     * @return room assignment
     */
    public RoomAssignmentEntity getRoomDetails(String tenantId, String roomNumber) {
        return getRoomStatus(roomNumber, tenantId);  // Swap parameter order
    }

    /**
     * 3c. Fix assignPatientToRoom - needs request DTO processing (Line 178)
     * Adapter method that processes RoomAssignmentRequest and extracts patientId
     *
     * @param tenantId the tenant ID
     * @param roomNumber the room number
     * @param request the room assignment request with patientId and encounterId
     * @param userId the user ID performing the assignment
     * @return created room assignment
     */
    @Transactional
    public RoomAssignmentEntity assignPatientToRoom(
            String tenantId,
            String roomNumber,
            RoomAssignmentRequest request,
            String userId) {
        log.debug("Assigning patient {} to room {} in tenant {}",
                request.getPatientId(), roomNumber, tenantId);

        UUID patientId;
        try {
            patientId = UUID.fromString(request.getPatientId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient ID format: " + request.getPatientId(), e);
        }

        // Use existing assignRoom but with extracted parameters
        return assignRoom(patientId, request.getEncounterId(), tenantId);
    }

    /**
     * 3d. Add updateRoomStatus method (Line 214)
     * Updates room status based on request with switch logic
     *
     * @param tenantId the tenant ID
     * @param roomNumber the room number
     * @param request the room status update request
     * @param userId the user ID performing the update
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity updateRoomStatus(
            String tenantId,
            String roomNumber,
            RoomStatusUpdateRequest request,
            String userId) {
        log.debug("Updating room {} status to {} in tenant {}",
                roomNumber, request.getStatus(), tenantId);

        RoomAssignmentEntity room = getRoomStatus(roomNumber, tenantId);

        switch (request.getStatus().toUpperCase()) {
            case "AVAILABLE":
                markRoomReady(roomNumber, tenantId);
                break;
            case "CLEANING":
                // Default 15 minutes for cleaning
                scheduleRoomCleaning(roomNumber, 15, tenantId);
                break;
            case "OUT_OF_SERVICE":
                markRoomOutOfService(roomNumber, tenantId, request.getReason());
                break;
            default:
                log.warn("Unknown room status: {}", request.getStatus());
        }

        return roomRepository.save(room);
    }

    /**
     * 3e. Fix markRoomReady parameter order (Line 248)
     * Adapter method that fixes parameter order (tenantId, roomNumber, userId)
     *
     * @param tenantId the tenant ID
     * @param roomNumber the room number
     * @param userId the user ID performing the action
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity markRoomReady(
            String tenantId,
            String roomNumber,
            String userId) {
        return markRoomReady(roomNumber, tenantId);  // Call existing, ignore userId
    }

    /**
     * 3f. Fix dischargePatient - add patientId extraction from room (Line 282)
     * Gets patient ID from room entity before discharge
     *
     * @param tenantId the tenant ID
     * @param roomNumber the room number
     * @param userId the user ID performing the discharge
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity dischargePatient(
            String tenantId,
            String roomNumber,
            String userId) {
        log.debug("Discharging patient from room {} in tenant {}", roomNumber, tenantId);

        // Get room and discharge the patient in it
        RoomAssignmentEntity room = getRoomStatus(roomNumber, tenantId);
        if (room.getPatientId() == null) {
            throw new IllegalStateException("Room not occupied: " + roomNumber);
        }

        return dischargePatient(roomNumber, room.getPatientId(), tenantId);
    }

    /**
     * 3g. Fix scheduleCleaning - add cleaning minutes (Line 316)
     * Adapter method that uses default 15 minutes for cleaning
     *
     * @param tenantId the tenant ID
     * @param roomNumber the room number
     * @param userId the user ID performing the action
     * @return updated room assignment
     */
    @Transactional
    public RoomAssignmentEntity scheduleCleaning(
            String tenantId,
            String roomNumber,
            String userId) {
        log.debug("Scheduling cleaning for room {} in tenant {}", roomNumber, tenantId);

        return scheduleRoomCleaning(roomNumber, 15, tenantId);  // Default 15 minutes
    }
}

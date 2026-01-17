package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity;
import com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}

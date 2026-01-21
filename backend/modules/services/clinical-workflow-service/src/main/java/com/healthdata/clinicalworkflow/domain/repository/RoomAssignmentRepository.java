package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RoomAssignmentEntity
 *
 * Provides data access for room assignments with multi-tenant isolation.
 * Includes queries for room availability and occupancy tracking.
 *
 * HIPAA Compliance:
 * - All PHI access is audited
 * - Cache TTL must be <= 5 minutes
 * - Multi-tenant filtering enforced on all queries
 */
@Repository
@Transactional(readOnly = true)
public interface RoomAssignmentRepository extends JpaRepository<RoomAssignmentEntity, UUID> {

    /**
     * Find room assignment by ID and tenant
     *
     * @param id UUID of the room assignment
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the room assignment if found
     */
    Optional<RoomAssignmentEntity> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find available rooms by tenant
     * Returns all rooms with status 'available' for assignment
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of room assignments with available status
     */
    @Query("""
        SELECT r FROM RoomAssignmentEntity r
        WHERE r.tenantId = :tenantId
        AND r.status = 'available'
        ORDER BY r.roomNumber ASC
    """)
    List<RoomAssignmentEntity> findAvailableRoomsByTenant(
        @Param("tenantId") String tenantId
    );

    /**
     * Find current occupants by tenant
     * Returns all rooms with patients currently in them
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of room assignments with occupied status
     */
    @Query("""
        SELECT r FROM RoomAssignmentEntity r
        WHERE r.tenantId = :tenantId
        AND r.status = 'occupied'
        ORDER BY r.assignedAt DESC
    """)
    List<RoomAssignmentEntity> findCurrentOccupantsByTenant(
        @Param("tenantId") String tenantId
    );

    /**
     * Find room by room number and tenant
     * Returns the current/latest assignment for a specific room
     *
     * @param roomNumber Room number/identifier
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the latest room assignment for the room
     */
    @Query("""
        SELECT r FROM RoomAssignmentEntity r
        WHERE r.tenantId = :tenantId
        AND r.roomNumber = :roomNumber
        ORDER BY r.assignedAt DESC
        LIMIT 1
    """)
    Optional<RoomAssignmentEntity> findRoomByNumberAndTenant(
        @Param("roomNumber") String roomNumber,
        @Param("tenantId") String tenantId
    );

    /**
     * Find room assignment history for a specific room from a date
     * Used for audit and utilization reporting
     *
     * @param roomNumber Room number/identifier
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param from Start date for history
     * @return List of room assignments for the room since the date
     */
    @Query("""
        SELECT r FROM RoomAssignmentEntity r
        WHERE r.tenantId = :tenantId
        AND r.roomNumber = :roomNumber
        AND r.assignedAt >= :from
        ORDER BY r.assignedAt DESC
    """)
    List<RoomAssignmentEntity> findRoomAssignmentHistory(
        @Param("roomNumber") String roomNumber,
        @Param("tenantId") String tenantId,
        @Param("from") LocalDateTime from
    );

    /**
     * Find all room assignments by status
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param status Room status (available, occupied, cleaning, reserved)
     * @return List of room assignments with the specified status
     */
    List<RoomAssignmentEntity> findByTenantIdAndStatusOrderByAssignedAtDesc(
        String tenantId,
        String status
    );

    /**
     * Find all assignments for a specific room
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param roomNumber Room number/identifier
     * @return List of all room assignments for the room
     */
    List<RoomAssignmentEntity> findByTenantIdAndRoomNumberOrderByAssignedAtDesc(
        String tenantId,
        String roomNumber
    );

    /**
     * Find active room assignment for patient
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param patientId UUID of the patient
     * @return Optional containing the active room assignment if found
     */
    @Query("""
        SELECT r FROM RoomAssignmentEntity r
        WHERE r.tenantId = :tenantId
        AND r.patientId = :patientId
        AND r.status IN ('occupied', 'reserved')
        ORDER BY r.assignedAt DESC
        LIMIT 1
    """)
    Optional<RoomAssignmentEntity> findActiveRoomForPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Count available rooms
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param status Room status to count
     * @return Count of rooms with the specified status
     */
    long countByTenantIdAndStatus(String tenantId, String status);

    /**
     * Find all assignments for occupancy board
     * Shows all non-available rooms (occupied, cleaning, reserved)
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of room assignments for occupancy board display
     */
    @Query("""
        SELECT r FROM RoomAssignmentEntity r
        WHERE r.tenantId = :tenantId
        AND r.status IN ('occupied', 'cleaning', 'reserved')
        ORDER BY r.roomNumber ASC
    """)
    List<RoomAssignmentEntity> findOccupancyBoard(@Param("tenantId") String tenantId);

    /**
     * Find rooms requiring cleaning
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of room assignments with cleaning status
     */
    @Query("""
        SELECT r FROM RoomAssignmentEntity r
        WHERE r.tenantId = :tenantId
        AND r.status = 'cleaning'
        ORDER BY r.cleaningStartedAt ASC NULLS LAST
    """)
    List<RoomAssignmentEntity> findRoomsRequiringCleaning(@Param("tenantId") String tenantId);
}

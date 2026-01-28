package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.api.v1.dto.RoomAssignmentRequest;
import com.healthdata.clinicalworkflow.api.v1.dto.RoomStatusUpdateRequest;
import com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity;
import com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomManagementServiceTest {

    @Mock
    private RoomAssignmentRepository roomRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache availableRoomsCache;

    @InjectMocks
    private RoomManagementService roomService;

    private static final String TENANT_ID = "TENANT001";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String ROOM_NUMBER = "ROOM-101";

    private RoomAssignmentEntity testRoom;

    @BeforeEach
    void setUp() {
        testRoom = RoomAssignmentEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .roomNumber(ROOM_NUMBER)
                .status("available")
                .roomType("standard")
                .location("Floor 1")
                .build();
    }

    @Test
    void assignRoom_ShouldAssignRoom_WhenAvailable() {
        // Given
        when(roomRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.empty());
        when(roomRepository.findAvailableRoomsByTenant(TENANT_ID))
                .thenReturn(List.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.assignRoom(PATIENT_ID, "Apt123", TENANT_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo("occupied");
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        verify(roomRepository).save(any(RoomAssignmentEntity.class));
    }

    @Test
    void assignRoom_ShouldThrowException_WhenPatientHasActiveRoom() {
        // Given
        when(roomRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.of(testRoom));

        // When/Then
        assertThatThrownBy(() -> roomService.assignRoom(PATIENT_ID, "Apt123", TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    void assignRoom_ShouldThrowException_WhenNoRoomsAvailable() {
        // Given
        when(roomRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.empty());
        when(roomRepository.findAvailableRoomsByTenant(TENANT_ID))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> roomService.assignRoom(PATIENT_ID, "Apt123", TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No available rooms");
    }

    @Test
    void markRoomReady_ShouldUpdateStatus_WhenRoomExists() {
        // Given
        testRoom.setStatus("cleaning");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.markRoomReady(ROOM_NUMBER, TENANT_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo("available");
        assertThat(result.getRoomReadyAt()).isNotNull();
    }

    @Test
    void dischargePatient_ShouldMarkCleaning_WhenPatientInRoom() {
        // Given
        testRoom.setStatus("occupied");
        testRoom.setPatientId(PATIENT_ID);
        when(roomRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.dischargePatient(
                ROOM_NUMBER, PATIENT_ID, TENANT_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo("cleaning");
        assertThat(result.getDischargedAt()).isNotNull();
    }

    @Test
    void dischargePatient_ShouldThrowException_WhenPatientNotInRoom() {
        // Given
        testRoom.setRoomNumber("DIFFERENT-ROOM");
        when(roomRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.of(testRoom));

        // When/Then
        assertThatThrownBy(() -> roomService.dischargePatient(
                ROOM_NUMBER, PATIENT_ID, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in room");
    }

    @Test
    void getAvailableRooms_ShouldReturnRooms_WhenAvailable() {
        // Given
        when(roomRepository.findAvailableRoomsByTenant(TENANT_ID))
                .thenReturn(List.of(testRoom));

        // When
        List<RoomAssignmentEntity> result = roomService.getAvailableRooms(TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getOccupancyBoard_ShouldReturnNonAvailableRooms() {
        // Given
        when(roomRepository.findOccupancyBoard(TENANT_ID))
                .thenReturn(List.of(testRoom));

        // When
        List<RoomAssignmentEntity> result = roomService.getOccupancyBoard(TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void calculateOccupancyDuration_ShouldReturnDuration_WhenValidAssignment() {
        // Given
        UUID assignmentId = UUID.randomUUID();
        testRoom.setAssignedAt(java.time.Instant.now().minusSeconds(45 * 60L));
        when(roomRepository.findByIdAndTenantId(assignmentId, TENANT_ID))
                .thenReturn(Optional.of(testRoom));

        // When
        Integer result = roomService.calculateOccupancyDuration(assignmentId, TENANT_ID);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void countAvailableRooms_ShouldReturnCount() {
        // Given
        when(roomRepository.countByTenantIdAndStatus(TENANT_ID, "available"))
                .thenReturn(5L);

        // When
        long result = roomService.countAvailableRooms(TENANT_ID);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    // ============ TIER 1 FIXES - NEW TESTS ============

    /**
     * 3a. Test getRoomBoard - alias for getOccupancyBoard
     */
    @Test
    void getRoomBoard_ShouldReturnOccupancyBoard() {
        // Given
        testRoom.setStatus("occupied");
        when(roomRepository.findOccupancyBoard(TENANT_ID))
                .thenReturn(List.of(testRoom));

        // When
        List<RoomAssignmentEntity> result = roomService.getRoomBoard(TENANT_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("occupied");
        verify(roomRepository).findOccupancyBoard(TENANT_ID);
    }

    /**
     * 3b. Test getRoomDetails - parameter order swap from getRoomStatus
     */
    @Test
    void getRoomDetails_ShouldReturnRoomStatus_WithSwappedParameters() {
        // Given
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));

        // When
        RoomAssignmentEntity result = roomService.getRoomDetails(TENANT_ID, ROOM_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRoomNumber()).isEqualTo(ROOM_NUMBER);
        verify(roomRepository).findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID);
    }

    @Test
    void getRoomDetails_ShouldThrowException_WhenRoomNotFound() {
        // Given
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> roomService.getRoomDetails(TENANT_ID, ROOM_NUMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room not found");
    }

    /**
     * 3c. Test assignPatientToRoom - processes RoomAssignmentRequest DTO
     */
    @Test
    void assignPatientToRoom_ShouldProcessRequestDTO_AndAssignRoom() {
        // Given
        RoomAssignmentRequest request = RoomAssignmentRequest.builder()
                .patientId(PATIENT_ID.toString())
                .encounterId("ENC001")
                .priority("ROUTINE")
                .build();

        when(roomRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.empty());
        when(roomRepository.findAvailableRoomsByTenant(TENANT_ID))
                .thenReturn(List.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.assignPatientToRoom(
                TENANT_ID, ROOM_NUMBER, request, "user123");

        // Then
        assertThat(result.getStatus()).isEqualTo("occupied");
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        verify(roomRepository).save(any(RoomAssignmentEntity.class));
    }

    @Test
    void assignPatientToRoom_ShouldThrowException_WhenInvalidPatientIdFormat() {
        // Given
        RoomAssignmentRequest request = RoomAssignmentRequest.builder()
                .patientId("INVALID-UUID")
                .encounterId("ENC001")
                .build();

        // When/Then
        assertThatThrownBy(() -> roomService.assignPatientToRoom(
                TENANT_ID, ROOM_NUMBER, request, "user123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid patient ID format");
    }

    /**
     * 3d. Test updateRoomStatus - status switching logic
     */
    @Test
    void updateRoomStatus_ShouldMarkAvailable_WhenStatusIsAvailable() {
        // Given
        testRoom.setStatus("cleaning");
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("AVAILABLE")
                .reason("Cleaning completed")
                .build();

        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.updateRoomStatus(
                TENANT_ID, ROOM_NUMBER, request, "user123");

        // Then
        assertThat(result.getStatus()).isEqualTo("available");
        verify(roomRepository, atLeastOnce()).save(any());
    }

    @Test
    void updateRoomStatus_ShouldScheduleCleaning_WhenStatusIsCleaning() {
        // Given
        testRoom.setStatus("occupied");
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("CLEANING")
                .reason("Patient discharged")
                .build();

        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.updateRoomStatus(
                TENANT_ID, ROOM_NUMBER, request, "user123");

        // Then
        assertThat(result.getStatus()).isEqualTo("cleaning");
        assertThat(result.getCleaningStartedAt()).isNotNull();
        verify(roomRepository, atLeastOnce()).save(any());
    }

    @Test
    void updateRoomStatus_ShouldSetOutOfService_WhenStatusIsOutOfService() {
        // Given
        testRoom.setStatus("available");
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("OUT_OF_SERVICE")
                .reason("Maintenance required")
                .build();

        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.updateRoomStatus(
                TENANT_ID, ROOM_NUMBER, request, "user123");

        // Then
        assertThat(result.getStatus()).isEqualTo("out-of-service");
        verify(roomRepository, atLeastOnce()).save(any());
    }

    /**
     * 3e. Test markRoomReady with corrected parameter order (tenantId, roomNumber, userId)
     */
    @Test
    void markRoomReady_WithUserId_ShouldUpdateStatus() {
        // Given
        testRoom.setStatus("cleaning");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.markRoomReady(
                TENANT_ID, ROOM_NUMBER, "user123");

        // Then
        assertThat(result.getStatus()).isEqualTo("available");
        assertThat(result.getRoomReadyAt()).isNotNull();
        verify(roomRepository).save(any());
    }

    /**
     * 3f. Test dischargePatient - extracts patientId from room
     */
    @Test
    void dischargePatient_WithUserId_ShouldExtractPatientIdAndDischarge() {
        // Given
        testRoom.setStatus("occupied");
        testRoom.setPatientId(PATIENT_ID);
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.dischargePatient(
                TENANT_ID, ROOM_NUMBER, "user123");

        // Then
        assertThat(result.getStatus()).isEqualTo("cleaning");
        assertThat(result.getDischargedAt()).isNotNull();
        verify(roomRepository).save(any());
    }

    @Test
    void dischargePatient_WithUserId_ShouldThrowException_WhenRoomNotOccupied() {
        // Given
        testRoom.setStatus("available");
        testRoom.setPatientId(null);
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));

        // When/Then
        assertThatThrownBy(() -> roomService.dischargePatient(
                TENANT_ID, ROOM_NUMBER, "user123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Room not occupied");
    }

    /**
     * 3g. Test scheduleCleaning - uses default 15 minutes
     */
    @Test
    void scheduleCleaning_WithUserId_ShouldUseDefaultCleaningMinutes() {
        // Given
        testRoom.setStatus("available");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomAssignmentEntity result = roomService.scheduleCleaning(
                TENANT_ID, ROOM_NUMBER, "user123");

        // Then
        assertThat(result.getStatus()).isEqualTo("cleaning");
        assertThat(result.getCleaningStartedAt()).isNotNull();
        verify(roomRepository).save(any());
    }

    // OUT_OF_SERVICE workflow tests

    @Test
    void markRoomOutOfService_ShouldUpdateStatus_WhenRoomAvailable() {
        // Given
        testRoom.setStatus("available");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("availableRooms")).thenReturn(availableRoomsCache);

        String reason = "HVAC system failure";

        // When
        RoomAssignmentEntity result = roomService.markRoomOutOfService(
                ROOM_NUMBER, TENANT_ID, reason);

        // Then
        assertThat(result.getStatus()).isEqualTo("out-of-service");
        assertThat(result.getNotes()).contains("[OUT OF SERVICE]");
        assertThat(result.getNotes()).contains(reason);
        verify(roomRepository).save(any(RoomAssignmentEntity.class));
        verify(availableRoomsCache).evict(TENANT_ID);
    }

    @Test
    void markRoomOutOfService_ShouldUpdateStatus_WhenRoomCleaning() {
        // Given
        testRoom.setStatus("cleaning");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("availableRooms")).thenReturn(availableRoomsCache);

        // When
        RoomAssignmentEntity result = roomService.markRoomOutOfService(
                ROOM_NUMBER, TENANT_ID, "Broken equipment");

        // Then
        assertThat(result.getStatus()).isEqualTo("out-of-service");
        verify(roomRepository).save(any());
    }

    @Test
    void markRoomOutOfService_ShouldThrowException_WhenRoomOccupied() {
        // Given
        testRoom.setStatus("occupied");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));

        // When/Then
        assertThatThrownBy(() -> roomService.markRoomOutOfService(
                ROOM_NUMBER, TENANT_ID, "Maintenance"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot mark occupied room");
    }

    @Test
    void markRoomOutOfService_ShouldAppendToExistingNotes() {
        // Given
        testRoom.setStatus("available");
        testRoom.setNotes("Previous note");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("availableRooms")).thenReturn(availableRoomsCache);

        // When
        RoomAssignmentEntity result = roomService.markRoomOutOfService(
                ROOM_NUMBER, TENANT_ID, "Water damage");

        // Then
        assertThat(result.getNotes()).contains("Previous note");
        assertThat(result.getNotes()).contains("[OUT OF SERVICE]");
        assertThat(result.getNotes()).contains("Water damage");
    }

    @Test
    void markRoomOutOfService_ShouldThrowException_WhenRoomNotFound() {
        // Given
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> roomService.markRoomOutOfService(
                ROOM_NUMBER, TENANT_ID, "Maintenance"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    void restoreRoomFromOutOfService_ShouldRestoreToAvailable() {
        // Given
        testRoom.setStatus("out-of-service");
        testRoom.setNotes("[OUT OF SERVICE] 2026-01-23T10:00:00Z - HVAC failure");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("availableRooms")).thenReturn(availableRoomsCache);

        // When
        RoomAssignmentEntity result = roomService.restoreRoomFromOutOfService(
                ROOM_NUMBER, TENANT_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo("available");
        assertThat(result.getRoomReadyAt()).isNotNull();
        assertThat(result.getNotes()).contains("[RESTORED]");
        assertThat(result.getNotes()).contains("Room returned to service");
        verify(roomRepository).save(any(RoomAssignmentEntity.class));
        verify(availableRoomsCache).evict(TENANT_ID);
    }

    @Test
    void restoreRoomFromOutOfService_ShouldThrowException_WhenNotOutOfService() {
        // Given
        testRoom.setStatus("available");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));

        // When/Then
        assertThatThrownBy(() -> roomService.restoreRoomFromOutOfService(
                ROOM_NUMBER, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Room is not out of service");
    }

    @Test
    void restoreRoomFromOutOfService_ShouldThrowException_WhenRoomNotFound() {
        // Given
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> roomService.restoreRoomFromOutOfService(
                ROOM_NUMBER, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    void updateRoomStatus_ShouldCallMarkRoomOutOfService_WhenStatusIsOutOfService() {
        // Given
        RoomStatusUpdateRequest request = RoomStatusUpdateRequest.builder()
                .status("OUT_OF_SERVICE")
                .reason("Emergency repairs needed")
                .build();

        testRoom.setStatus("available");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("availableRooms")).thenReturn(availableRoomsCache);

        // When
        RoomAssignmentEntity result = roomService.updateRoomStatus(
                TENANT_ID, ROOM_NUMBER, request, "user123");

        // Then
        assertThat(result.getStatus()).isEqualTo("out-of-service");
        assertThat(result.getNotes()).contains("Emergency repairs needed");
        verify(roomRepository, atLeastOnce()).save(any());
        verify(availableRoomsCache).evict(TENANT_ID);
    }

    @Test
    void markRoomOutOfService_ShouldHandleCacheEvictionFailure() {
        // Given
        testRoom.setStatus("available");
        when(roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID))
                .thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("availableRooms")).thenReturn(null);  // Cache not found

        // When
        RoomAssignmentEntity result = roomService.markRoomOutOfService(
                ROOM_NUMBER, TENANT_ID, "Maintenance");

        // Then - Should complete successfully despite cache failure
        assertThat(result.getStatus()).isEqualTo("out-of-service");
        verify(roomRepository).save(any());
    }
}

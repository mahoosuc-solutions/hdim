package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity;
import com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomManagementServiceTest {

    @Mock
    private RoomAssignmentRepository roomRepository;

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
}

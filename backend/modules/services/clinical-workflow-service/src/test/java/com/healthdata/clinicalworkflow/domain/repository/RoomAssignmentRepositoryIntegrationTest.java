package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.config.BaseIntegrationTest;
import com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Room Assignment Repository Integration Tests
 */
@BaseIntegrationTest
@DisplayName("RoomAssignmentRepository Integration Tests")
class RoomAssignmentRepositoryIntegrationTest {

    @Autowired
    private RoomAssignmentRepository roomAssignmentRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID_1 = UUID.randomUUID();
    private static final UUID PATIENT_ID_2 = UUID.randomUUID();

    private RoomAssignmentEntity room101Occupied;
    private RoomAssignmentEntity room102Available;
    private RoomAssignmentEntity room103Cleaning;

    @BeforeEach
    void setUp() {
        roomAssignmentRepository.deleteAll();
        
        room101Occupied = createRoomAssignment(TENANT_ID, PATIENT_ID_1, "101", "occupied");
        room102Available = createRoomAssignment(TENANT_ID, null, "102", "available");
        room103Cleaning = createRoomAssignment(TENANT_ID, PATIENT_ID_2, "103", "cleaning");

        room101Occupied = roomAssignmentRepository.save(room101Occupied);
        room102Available = roomAssignmentRepository.save(room102Available);
        room103Cleaning = roomAssignmentRepository.save(room103Cleaning);
    }

    @Nested
    @DisplayName("Room Availability Queries")
    class RoomAvailabilityTests {

        @Test
        @DisplayName("Should find available rooms by tenant")
        void shouldFindAvailableRoomsByTenant() {
            List<RoomAssignmentEntity> available = roomAssignmentRepository.findAvailableRoomsByTenant(TENANT_ID);

            assertThat(available).hasSize(1);
            assertThat(available.get(0).getRoomNumber()).isEqualTo("102");
            assertThat(available.get(0).getStatus()).isEqualTo("available");
        }

        @Test
        @DisplayName("Should find current occupants by tenant")
        void shouldFindCurrentOccupantsByTenant() {
            List<RoomAssignmentEntity> occupied = roomAssignmentRepository.findCurrentOccupantsByTenant(TENANT_ID);

            assertThat(occupied).hasSize(1);
            assertThat(occupied.get(0).getStatus()).isEqualTo("occupied");
            assertThat(occupied.get(0).getPatientId()).isEqualTo(PATIENT_ID_1);
        }

        @Test
        @DisplayName("Should count available rooms")
        void shouldCountAvailableRooms() {
            long count = roomAssignmentRepository.countByTenantIdAndStatus(TENANT_ID, "available");

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should find rooms requiring cleaning")
        void shouldFindRoomsRequiringCleaning() {
            List<RoomAssignmentEntity> cleaning = roomAssignmentRepository.findRoomsRequiringCleaning(TENANT_ID);

            assertThat(cleaning).hasSize(1);
            assertThat(cleaning.get(0).getRoomNumber()).isEqualTo("103");
        }
    }

    @Nested
    @DisplayName("Room Number Queries")
    class RoomNumberQueryTests {

        @Test
        @DisplayName("Should find room by number and tenant")
        void shouldFindRoomByNumberAndTenant() {
            Optional<RoomAssignmentEntity> found = roomAssignmentRepository.findRoomByNumberAndTenant("101", TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getRoomNumber()).isEqualTo("101");
            assertThat(found.get().getStatus()).isEqualTo("occupied");
        }

        @Test
        @DisplayName("Should find all assignments for room")
        void shouldFindAllAssignmentsForRoom() {
            // Create additional assignment for same room
            RoomAssignmentEntity oldAssignment = createRoomAssignment(TENANT_ID, UUID.randomUUID(), "101", "available");
            oldAssignment.setAssignedAt(Instant.now().minusSeconds(3600));
            roomAssignmentRepository.save(oldAssignment);

            List<RoomAssignmentEntity> assignments = roomAssignmentRepository.findByTenantIdAndRoomNumberOrderByAssignedAtDesc(TENANT_ID, "101");

            assertThat(assignments).hasSize(2);
            assertThat(assignments).allMatch(a -> a.getRoomNumber().equals("101"));
        }

        @Test
        @DisplayName("Should find room assignment history from date")
        void shouldFindRoomAssignmentHistory() {
            Instant from = Instant.now().minusSeconds(3600);

            List<RoomAssignmentEntity> history = roomAssignmentRepository.findRoomAssignmentHistory("101", TENANT_ID, from);

            assertThat(history).hasSize(1);
            assertThat(history.get(0).getRoomNumber()).isEqualTo("101");
        }
    }

    @Nested
    @DisplayName("Patient-Based Queries")
    class PatientBasedQueryTests {

        @Test
        @DisplayName("Should find active room for patient")
        void shouldFindActiveRoomForPatient() {
            Optional<RoomAssignmentEntity> found = roomAssignmentRepository.findActiveRoomForPatient(TENANT_ID, PATIENT_ID_1);

            assertThat(found).isPresent();
            assertThat(found.get().getRoomNumber()).isEqualTo("101");
            assertThat(found.get().getPatientId()).isEqualTo(PATIENT_ID_1);
        }

        @Test
        @DisplayName("Should not find active room for patient without assignment")
        void shouldNotFindActiveRoomForPatientWithoutAssignment() {
            UUID unassignedPatient = UUID.randomUUID();
            Optional<RoomAssignmentEntity> found = roomAssignmentRepository.findActiveRoomForPatient(TENANT_ID, unassignedPatient);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Occupancy Board Queries")
    class OccupancyBoardTests {

        @Test
        @DisplayName("Should find occupancy board data")
        void shouldFindOccupancyBoard() {
            List<RoomAssignmentEntity> occupancyBoard = roomAssignmentRepository.findOccupancyBoard(TENANT_ID);

            assertThat(occupancyBoard).hasSize(2); // occupied and cleaning (not available)
            assertThat(occupancyBoard).noneMatch(r -> r.getStatus().equals("available"));
        }

        @Test
        @DisplayName("Should order occupancy board by room number")
        void shouldOrderOccupancyBoardByRoomNumber() {
            List<RoomAssignmentEntity> occupancyBoard = roomAssignmentRepository.findOccupancyBoard(TENANT_ID);

            assertThat(occupancyBoard.get(0).getRoomNumber()).isEqualTo("101");
            assertThat(occupancyBoard.get(1).getRoomNumber()).isEqualTo("103");
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate room data between tenants")
        void shouldIsolateRoomDataBetweenTenants() {
            RoomAssignmentEntity otherTenantRoom = createRoomAssignment(OTHER_TENANT, UUID.randomUUID(), "201", "occupied");
            roomAssignmentRepository.save(otherTenantRoom);

            List<RoomAssignmentEntity> tenant1Rooms = roomAssignmentRepository.findAvailableRoomsByTenant(TENANT_ID);
            List<RoomAssignmentEntity> tenant2Rooms = roomAssignmentRepository.findAvailableRoomsByTenant(OTHER_TENANT);

            assertThat(tenant1Rooms).noneMatch(r -> r.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Rooms).noneMatch(r -> r.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should not find room across tenants")
        void shouldNotFindRoomAcrossTenants() {
            Optional<RoomAssignmentEntity> found = roomAssignmentRepository.findRoomByNumberAndTenant("101", OTHER_TENANT);

            assertThat(found).isEmpty();
        }
    }

    private RoomAssignmentEntity createRoomAssignment(String tenantId, UUID patientId, String roomNumber, String status) {
        return RoomAssignmentEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .roomNumber(roomNumber)
                .status(status)
                .roomType("standard")
                .assignedBy("test-ma")
                .assignedAt(Instant.now())
                .build();
    }
}

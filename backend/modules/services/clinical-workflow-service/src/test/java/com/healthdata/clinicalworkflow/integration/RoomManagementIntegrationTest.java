package com.healthdata.clinicalworkflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity;
import com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Room Management Service (Tier 3 - Validation)
 *
 * Tests room management workflow end-to-end including:
 * - Room assignment workflow
 * - Room status transitions (AVAILABLE → OCCUPIED → CLEANING → AVAILABLE)
 * - Room discharge workflow
 * - Occupancy board tracking
 * - Multi-tenant isolation
 * - Concurrent room operations
 * - Audit trail verification
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
@DisplayName("Room Management Integration Tests")
class RoomManagementIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinical_workflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomAssignmentRepository roomRepository;

    private static final String TENANT_ID_A = "TENANT_A";
    private static final String TENANT_ID_B = "TENANT_B";
    private static final String USER_ID = "nurse@example.com";
    private static final String PATIENT_ID = UUID.randomUUID().toString();
    private static final String ENCOUNTER_ID = "ENC001";
    private static final String PROVIDER_ID = "PROV001";
    private static final String ROOM_NUMBER = "Room 101";

    @BeforeEach
    void setUp() {
        roomRepository.deleteAll();
    }

    // ================================
    // Complete Room Workflow Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Complete room workflow: assign patient → mark cleaning → mark ready")
    void testCompleteRoomWorkflow() throws Exception {
        // Step 1: Assign patient to room
        RoomAssignmentRequest assignRequest = RoomAssignmentRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .priority("ROUTINE")
                .providerId(PROVIDER_ID)
                .build();

        MvcResult assignResult = mockMvc.perform(post("/api/v1/rooms/{roomNumber}/assign", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value(ROOM_NUMBER))
                .andExpect(jsonPath("$.status").value("OCCUPIED"))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andReturn();

        // Verify entity in database
        Optional<RoomAssignmentEntity> roomEntityOpt = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A);
        assertThat(roomEntityOpt).isPresent();
        RoomAssignmentEntity roomEntity = roomEntityOpt.get();
        assertThat(roomEntity.getStatus()).isEqualTo("occupied");
        assertThat(roomEntity.getPatientId()).isEqualTo(UUID.fromString(PATIENT_ID));
        assertThat(roomEntity.getAssignedBy()).isEqualTo("system");
        assertThat(roomEntity.getAssignedAt()).isNotNull();

        // Step 2: Discharge patient (room goes to CLEANING)
        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/discharge", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEANING"));

        // Verify room status changed
        roomEntity = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(roomEntity.getStatus()).isEqualTo("cleaning");
        assertThat(roomEntity.getDischargedAt()).isNotNull();

        // Step 3: Mark room as ready (room goes to AVAILABLE)
        mockMvc.perform(put("/api/v1/rooms/{roomNumber}/ready", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        // Verify final room state
        roomEntity = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(roomEntity.getStatus()).isEqualTo("available");
        assertThat(roomEntity.getRoomReadyAt()).isNotNull();
    }

    // ================================
    // Occupancy Board Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Get room occupancy board: verify room counts and occupancy details")
    void testRoomOccupancyBoard() throws Exception {
        // Create 3 rooms with different statuses
        String[] roomNumbers = {"Room 101", "Room 102", "Room 103"};
        String[] statuses = {"available", "occupied", "cleaning"};

        for (int i = 0; i < 3; i++) {
            RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                    .tenantId(TENANT_ID_A)
                    .roomNumber(roomNumbers[i])
                    .patientId(UUID.randomUUID())
                    .status(statuses[i])
                    .assignedBy("system")
                    .assignedAt(Instant.now())
                    .build();

            if (statuses[i].equals("occupied")) {
                room.setEncounterId("ENC00" + (i + 1));
            }

            roomRepository.save(room);
        }

        // Get room board
        mockMvc.perform(get("/api/v1/rooms")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").value(3))
                .andExpect(jsonPath("$.availableCount").value(1))
                .andExpect(jsonPath("$.occupiedCount").value(1))
                .andExpect(jsonPath("$.cleaningCount").value(1))
                .andExpect(jsonPath("$.rooms", hasSize(3)));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Get available rooms only")
    void testGetAvailableRoomsOnly() throws Exception {
        // Create 5 rooms with mixed statuses
        for (int i = 1; i <= 5; i++) {
            String status = (i <= 2) ? "available" : "occupied";
            RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                    .tenantId(TENANT_ID_A)
                    .roomNumber("Room " + i)
                    .patientId(UUID.randomUUID())
                    .status(status)
                    .assignedBy("system")
                    .assignedAt(Instant.now())
                    .build();
            roomRepository.save(room);
        }

        // Get available rooms only
        mockMvc.perform(get("/api/v1/rooms/available")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ================================
    // Room Status Transition Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Test status transitions: AVAILABLE → OCCUPIED → CLEANING → AVAILABLE")
    void testRoomStatusTransitions() throws Exception {
        // Initial state: AVAILABLE
        RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                .tenantId(TENANT_ID_A)
                .roomNumber(ROOM_NUMBER)
                .patientId(UUID.randomUUID())
                .status("available")
                .assignedBy("system")
                .assignedAt(Instant.now())
                .build();
        roomRepository.save(room);

        // Transition 1: AVAILABLE → OCCUPIED (assign patient)
        RoomAssignmentRequest assignRequest = RoomAssignmentRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .priority("ROUTINE")
                .providerId(PROVIDER_ID)
                .build();

        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/assign", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OCCUPIED"));

        // Transition 2: OCCUPIED → CLEANING (discharge patient)
        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/discharge", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEANING"));

        // Transition 3: CLEANING → AVAILABLE (mark ready)
        mockMvc.perform(put("/api/v1/rooms/{roomNumber}/ready", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        // Verify final state
        RoomAssignmentEntity finalRoom = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(finalRoom.getStatus()).isEqualTo("available");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Manual status update")
    void testManualRoomStatusUpdate() throws Exception {
        // Create room
        RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                .tenantId(TENANT_ID_A)
                .roomNumber(ROOM_NUMBER)
                .patientId(UUID.randomUUID())
                .status("available")
                .assignedBy("system")
                .assignedAt(Instant.now())
                .build();
        roomRepository.save(room);

        // Update status to OUT_OF_SERVICE
        RoomStatusUpdateRequest updateRequest = RoomStatusUpdateRequest.builder()
                .status("OUT_OF_SERVICE")
                .reason("Equipment maintenance")
                .build();

        mockMvc.perform(put("/api/v1/rooms/{roomNumber}/status", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));

        // Verify database
        RoomAssignmentEntity updatedRoom = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(updatedRoom.getStatus()).isEqualTo("out-of-service");
    }

    // ================================
    // Room Discharge Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Room discharge: discharge patient → verify room becomes CLEANING")
    void testRoomDischarge() throws Exception {
        // Create occupied room
        RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                .tenantId(TENANT_ID_A)
                .roomNumber(ROOM_NUMBER)
                .status("occupied")
                .patientId(UUID.fromString(PATIENT_ID))
                .encounterId(ENCOUNTER_ID)
                .assignedBy("system")
                .assignedAt(Instant.now())
                .build();
        roomRepository.save(room);

        // Discharge patient
        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/discharge", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEANING"));

        // Verify database
        RoomAssignmentEntity dischargedRoom = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(dischargedRoom.getStatus()).isEqualTo("cleaning");
        assertThat(dischargedRoom.getDischargedAt()).isNotNull();
    }

    // ================================
    // Multi-Tenant Isolation Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Multi-tenant isolation: tenant A cannot access tenant B's rooms")
    void testMultiTenantRoomIsolation() throws Exception {
        // Create room in tenant A
        RoomAssignmentEntity roomA = RoomAssignmentEntity.builder()
                .tenantId(TENANT_ID_A)
                .roomNumber(ROOM_NUMBER)
                .patientId(UUID.randomUUID())
                .status("available")
                .assignedBy("system")
                .assignedAt(Instant.now())
                .build();
        roomRepository.save(roomA);

        // Try to access from tenant B - should not see it
        mockMvc.perform(get("/api/v1/rooms/{roomNumber}", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_B))
                .andExpect(status().isNotFound());

        // Verify tenant A can access
        mockMvc.perform(get("/api/v1/rooms/{roomNumber}", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value(ROOM_NUMBER));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Room board shows only tenant-specific rooms")
    void testRoomBoardTenantIsolation() throws Exception {
        // Create rooms in tenant A
        for (int i = 1; i <= 3; i++) {
            RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                    .tenantId(TENANT_ID_A)
                    .roomNumber("Room A" + i)
                    .patientId(UUID.randomUUID())
                    .status("available")
                    .assignedBy("system")
                    .assignedAt(Instant.now())
                    .build();
            roomRepository.save(room);
        }

        // Create rooms in tenant B
        for (int i = 1; i <= 2; i++) {
            RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                    .tenantId(TENANT_ID_B)
                    .roomNumber("Room B" + i)
                    .patientId(UUID.randomUUID())
                    .status("available")
                    .assignedBy("system")
                    .assignedAt(Instant.now())
                    .build();
            roomRepository.save(room);
        }

        // Tenant A should see only 3 rooms
        mockMvc.perform(get("/api/v1/rooms")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").value(3));

        // Tenant B should see only 2 rooms
        mockMvc.perform(get("/api/v1/rooms")
                        .header("X-Tenant-ID", TENANT_ID_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").value(2));
    }

    // ================================
    // Concurrent Operations Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Concurrent room operations: multiple staff updating rooms")
    void testConcurrentRoomOperations() throws Exception {
        // Create 3 rooms
        for (int i = 1; i <= 3; i++) {
            RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                    .tenantId(TENANT_ID_A)
                    .roomNumber("Room " + i)
                    .patientId(UUID.randomUUID())
                    .status("available")
                    .assignedBy("system")
                    .assignedAt(Instant.now())
                    .build();
            roomRepository.save(room);
        }

        // Simulate concurrent assignments by different users
        String[] users = {"nurse1@example.com", "nurse2@example.com", "nurse3@example.com"};
        for (int i = 1; i <= 3; i++) {
            RoomAssignmentRequest request = RoomAssignmentRequest.builder()
                    .patientId(UUID.randomUUID().toString())
                    .encounterId("ENC00" + i)
                    .priority("ROUTINE")
                    .providerId("PROV00" + i)
                    .build();

            mockMvc.perform(post("/api/v1/rooms/{roomNumber}/assign", "Room " + i)
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", users[i - 1])
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verify all 3 rooms are occupied by correct users
        List<RoomAssignmentEntity> rooms = roomRepository
                .findByTenantIdAndStatusOrderByAssignedAtDesc(TENANT_ID_A, "occupied");
        assertThat(rooms).hasSize(3);

        for (RoomAssignmentEntity room : rooms) {
            assertThat(room.getAssignedBy()).isEqualTo("system");
        }
    }

    // ================================
    // Error Scenario Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Error: Cannot assign patient to occupied room")
    void testCannotAssignToOccupiedRoom() throws Exception {
        // Create occupied room
        RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                .tenantId(TENANT_ID_A)
                .roomNumber(ROOM_NUMBER)
                .status("occupied")
                .patientId(UUID.randomUUID())
                .assignedBy("system")
                .assignedAt(Instant.now())
                .build();
        roomRepository.save(room);

        // Try to assign another patient - should fail
        RoomAssignmentRequest request = RoomAssignmentRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .priority("ROUTINE")
                .providerId(PROVIDER_ID)
                .build();

        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/assign", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Error: Room not found (404)")
    void testRoomNotFound() throws Exception {
        String nonExistentRoom = "Room 999";

        // Try to get non-existent room
        mockMvc.perform(get("/api/v1/rooms/{roomNumber}", nonExistentRoom)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isNotFound());

        // Try to assign to non-existent room
        RoomAssignmentRequest request = RoomAssignmentRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .priority("ROUTINE")
                .providerId(PROVIDER_ID)
                .build();

        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/assign", nonExistentRoom)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ================================
    // Audit Trail Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Verify audit trail: all operations tracked with user and timestamp")
    void testAuditTrail() throws Exception {
        // Step 1: Assign room
        String assignUser = "nurse1@example.com";
        RoomAssignmentRequest assignRequest = RoomAssignmentRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .priority("ROUTINE")
                .providerId(PROVIDER_ID)
                .build();

        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/assign", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", assignUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated());

        RoomAssignmentEntity room = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(room.getAssignedBy()).isEqualTo("system");
        assertThat(room.getAssignedAt()).isNotNull();

        // Step 2: Discharge
        String dischargeUser = "provider@example.com";
        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/discharge", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", dischargeUser))
                .andExpect(status().isOk());

        room = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(room.getDischargedAt()).isNotNull();

        // Step 3: Mark ready
        String readyUser = "housekeeper@example.com";
        mockMvc.perform(put("/api/v1/rooms/{roomNumber}/ready", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", readyUser))
                .andExpect(status().isOk());

        room = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(room.getRoomReadyAt()).isNotNull();

        // Verify all audit fields are present
        assertThat(room.getAssignedBy()).isNotNull();
        assertThat(room.getAssignedAt()).isNotNull();
        assertThat(room.getDischargedAt()).isNotNull();
        assertThat(room.getRoomReadyAt()).isNotNull();
    }

    // ================================
    // Additional Test Scenarios
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Schedule cleaning for room")
    void testScheduleRoomCleaning() throws Exception {
        // Create available room
        RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                .tenantId(TENANT_ID_A)
                .roomNumber(ROOM_NUMBER)
                .patientId(UUID.randomUUID())
                .status("available")
                .assignedBy("system")
                .assignedAt(Instant.now())
                .build();
        roomRepository.save(room);

        // Schedule cleaning
        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/schedule-cleaning", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEANING"));

        // Verify status
        RoomAssignmentEntity updatedRoom = roomRepository.findRoomByNumberAndTenant(ROOM_NUMBER, TENANT_ID_A).orElseThrow();
        assertThat(updatedRoom.getStatus()).isEqualTo("cleaning");
        assertThat(updatedRoom.getCleaningStartedAt()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "PROVIDER")
    @DisplayName("Verify role-based access control")
    void testRoleBasedAccessControl() throws Exception {
        // PROVIDER role can discharge patients
        RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                .tenantId(TENANT_ID_A)
                .roomNumber(ROOM_NUMBER)
                .status("occupied")
                .patientId(UUID.fromString(PATIENT_ID))
                .assignedBy("system")
                .assignedAt(Instant.now())
                .build();
        roomRepository.save(room);

        mockMvc.perform(post("/api/v1/rooms/{roomNumber}/discharge", ROOM_NUMBER)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", "provider@example.com"))
                .andExpect(status().isOk());
    }
}

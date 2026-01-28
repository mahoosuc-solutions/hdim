package com.healthdata.clinicalworkflow.integration;

import com.healthdata.clinicalworkflow.application.*;
import com.healthdata.clinicalworkflow.domain.model.*;
import com.healthdata.clinicalworkflow.domain.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Clinical Workflow Service
 *
 * Tests all 5 services with real database using TestContainers:
 * - PatientCheckInService
 * - VitalSignsService
 * - RoomManagementService
 * - WaitingQueueService
 * - PreVisitChecklistService
 *
 * Uses PostgreSQL TestContainer for database isolation.
 */
@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClinicalWorkflowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinical_workflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private PatientCheckInService checkInService;

    @Autowired
    private VitalSignsService vitalsService;

    @Autowired
    private RoomManagementService roomService;

    @Autowired
    private WaitingQueueService queueService;

    @Autowired
    private PreVisitChecklistService checklistService;

    @Autowired
    private PatientCheckInRepository checkInRepository;

    @Autowired
    private VitalSignsRecordRepository vitalsRepository;

    @Autowired
    private RoomAssignmentRepository roomRepository;

    @Autowired
    private WaitingQueueRepository queueRepository;

    @Autowired
    private PreVisitChecklistRepository checklistRepository;

    private static final String TENANT_ID = "TEST_TENANT";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String APPOINTMENT_ID = "Appointment/123";

    @BeforeEach
    void setUp() {
        // Clean up before each test
        checkInRepository.deleteAll();
        vitalsRepository.deleteAll();
        roomRepository.deleteAll();
        queueRepository.deleteAll();
        checklistRepository.deleteAll();

        // Create an available room for tests
        RoomAssignmentEntity room = RoomAssignmentEntity.builder()
                .tenantId(TENANT_ID)
                .roomNumber("ROOM-101")
                .patientId(UUID.randomUUID())
                .status("available")
                .roomType("standard")
                .location("Floor 1")
                .assignedBy("system")
                .assignedAt(Instant.now())
                .build();
        roomRepository.save(room);
    }

    // ========== End-to-End Workflow Test ==========

    @Test
    @Order(1)
    void testCompletePatientVisitWorkflow() {
        // 1. Create pre-visit checklist
        PreVisitChecklistEntity checklist = checklistService.createChecklistForAppointment(
                "new-patient", PATIENT_ID, APPOINTMENT_ID, TENANT_ID);
        assertThat(checklist).isNotNull();
        assertThat(checklist.getStatus()).isEqualTo("pending");

        // 2. Complete some checklist items
        checklistService.completeChecklistItem(checklist.getId(), "verifyInsurance", TENANT_ID);
        checklistService.completeChecklistItem(checklist.getId(), "reviewMedicalHistory", TENANT_ID);

        // 3. Check in patient
        PatientCheckInEntity checkIn = checkInService.checkInPatientInternal(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID);
        assertThat(checkIn).isNotNull();
        assertThat(checkIn.getStatus()).isEqualTo("checked-in");

        // 4. Add to waiting queue
        WaitingQueueEntity queueEntry = queueService.addToQueueInternal(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID);
        assertThat(queueEntry).isNotNull();
        assertThat(queueEntry.getStatus()).isEqualTo("waiting");

        // 5. Call patient from queue
        WaitingQueueEntity calledPatient = queueService.callPatient(PATIENT_ID, TENANT_ID);
        assertThat(calledPatient.getStatus()).isEqualTo("called");

        // 6. Assign room
        RoomAssignmentEntity roomAssignment = roomService.assignRoom(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID);
        assertThat(roomAssignment).isNotNull();
        assertThat(roomAssignment.getStatus()).isEqualTo("occupied");

        // 7. Record vital signs
        VitalSignsService.VitalSignsRequest vitalsRequest = VitalSignsService.VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .recordedBy("test-ma")
                .systolicBp(new BigDecimal("120"))
                .diastolicBp(new BigDecimal("80"))
                .heartRate(new BigDecimal("75"))
                .temperatureF(new BigDecimal("98.6"))
                .oxygenSaturation(new BigDecimal("98"))
                .weightKg(new BigDecimal("70"))
                .heightCm(new BigDecimal("170"))
                .build();

        VitalSignsRecordEntity vitals = vitalsService.recordVitals(vitalsRequest, TENANT_ID);
        assertThat(vitals).isNotNull();
        assertThat(vitals.getAlertStatus()).isEqualTo("normal");
        assertThat(vitals.getBmi()).isNotNull();

        // 8. Discharge patient
        RoomAssignmentEntity discharged = roomService.dischargePatient(
                roomAssignment.getRoomNumber(), PATIENT_ID, TENANT_ID);
        assertThat(discharged.getStatus()).isEqualTo("cleaning");
        assertThat(discharged.getDischargedAt()).isNotNull();

        // 9. Remove from queue
        queueService.removeFromQueue(PATIENT_ID, TENANT_ID);

        // Verify final state
        assertThat(checkInService.getCheckInHistoryInternal(PATIENT_ID, TENANT_ID)).hasSize(1);
        assertThat(vitalsService.getAllPatientVitals(PATIENT_ID, TENANT_ID)).hasSize(1);
    }

    // ========== PatientCheckInService Integration Tests ==========

    @Test
    void testPatientCheckIn_MultipleCheckIns() {
        // Create multiple check-ins
        PatientCheckInEntity checkIn1 = checkInService.checkInPatientInternal(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID);
        PatientCheckInEntity checkIn2 = checkInService.checkInPatientInternal(
                PATIENT_ID, "Appointment/456", TENANT_ID);

        // Verify history
        List<PatientCheckInEntity> history = checkInService.getCheckInHistoryInternal(
                PATIENT_ID, TENANT_ID);
        assertThat(history).hasSize(2);
    }

    @Test
    void testPatientCheckIn_VerifyInsurance() {
        // Check in patient
        checkInService.checkInPatientInternal(PATIENT_ID, APPOINTMENT_ID, TENANT_ID);

        // Verify insurance
        PatientCheckInEntity verified = checkInService.verifyInsuranceInternal(PATIENT_ID, TENANT_ID);

        assertThat(verified.getInsuranceVerified()).isTrue();
    }

    @Test
    void testPatientCheckIn_ObtainConsent() {
        // Check in patient
        checkInService.checkInPatientInternal(PATIENT_ID, APPOINTMENT_ID, TENANT_ID);

        // Obtain consent
        PatientCheckInEntity withConsent = checkInService.obtainConsentInternal(PATIENT_ID, TENANT_ID);

        assertThat(withConsent.getConsentObtained()).isTrue();
    }

    // ========== VitalSignsService Integration Tests ==========

    @Test
    void testVitalSigns_CriticalAlert() {
        // Record critical vitals
        VitalSignsService.VitalSignsRequest request = VitalSignsService.VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .recordedBy("test-ma")
                .systolicBp(new BigDecimal("190")) // Critical
                .heartRate(new BigDecimal("140"))  // Critical
                .oxygenSaturation(new BigDecimal("80")) // Critical
                .build();

        VitalSignsRecordEntity vitals = vitalsService.recordVitals(request, TENANT_ID);

        assertThat(vitals.getAlertStatus()).isEqualTo("critical");
        assertThat(vitals.getAlertMessage()).contains("CRITICAL");
    }

    @Test
    void testVitalSigns_BMICalculation() {
        // Record vitals with weight and height
        VitalSignsService.VitalSignsRequest request = VitalSignsService.VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .recordedBy("test-ma")
                .weightKg(new BigDecimal("70"))
                .heightCm(new BigDecimal("170"))
                .systolicBp(new BigDecimal("120"))
                .build();

        VitalSignsRecordEntity vitals = vitalsService.recordVitals(request, TENANT_ID);

        assertThat(vitals.getBmi()).isNotNull();
        assertThat(vitals.getBmi()).isEqualByComparingTo("24.22");
    }

    @Test
    void testVitalSigns_HistoryQuery() {
        // Record multiple vitals
        for (int i = 0; i < 3; i++) {
            VitalSignsService.VitalSignsRequest request = VitalSignsService.VitalSignsRequest.builder()
                    .patientId(PATIENT_ID)
                    .recordedBy("test-ma")
                    .systolicBp(new BigDecimal("120"))
                    .build();
            vitalsService.recordVitals(request, TENANT_ID);
        }

        // Query history
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        List<VitalSignsRecordEntity> history = vitalsService.getPatientVitalsHistory(
                PATIENT_ID, TENANT_ID, from, to);

        assertThat(history).hasSize(3);
    }

    // ========== RoomManagementService Integration Tests ==========

    @Test
    void testRoomManagement_AssignAndDischarge() {
        // Assign room
        RoomAssignmentEntity assignment = roomService.assignRoom(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID);

        assertThat(assignment.getStatus()).isEqualTo("occupied");

        // Discharge patient
        RoomAssignmentEntity discharged = roomService.dischargePatient(
                assignment.getRoomNumber(), PATIENT_ID, TENANT_ID);

        assertThat(discharged.getStatus()).isEqualTo("cleaning");
    }

    @Test
    void testRoomManagement_CleaningWorkflow() {
        // Assign and discharge
        RoomAssignmentEntity assignment = roomService.assignRoom(
                PATIENT_ID, APPOINTMENT_ID, TENANT_ID);
        roomService.dischargePatient(assignment.getRoomNumber(), PATIENT_ID, TENANT_ID);

        // Schedule cleaning
        RoomAssignmentEntity cleaning = roomService.scheduleRoomCleaning(
                assignment.getRoomNumber(), 15, TENANT_ID);

        assertThat(cleaning.getStatus()).isEqualTo("cleaning");
        assertThat(cleaning.getCleaningStartedAt()).isNotNull();

        // Mark ready
        RoomAssignmentEntity ready = roomService.markRoomReady(
                assignment.getRoomNumber(), TENANT_ID);

        assertThat(ready.getStatus()).isEqualTo("available");
        assertThat(ready.getRoomReadyAt()).isNotNull();
    }

    // ========== WaitingQueueService Integration Tests ==========

    @Test
    void testWaitingQueue_PriorityOrdering() {
        // Add patients with different priorities
        UUID urgentPatient = UUID.randomUUID();
        UUID normalPatient = UUID.randomUUID();

        queueService.addToQueueWithPriority(normalPatient, "Apt1", "normal", TENANT_ID);
        queueService.addToQueueWithPriority(urgentPatient, "Apt2", "urgent", TENANT_ID);

        // Get next patient
        WaitingQueueEntity next = queueService.getNextPatient(TENANT_ID);

        assertThat(next.getPatientId()).isEqualTo(urgentPatient);
    }

    @Test
    void testWaitingQueue_StatusTransitions() {
        // Add to queue
        queueService.addToQueueInternal(PATIENT_ID, APPOINTMENT_ID, TENANT_ID);

        // Call patient
        WaitingQueueEntity called = queueService.callPatient(PATIENT_ID, TENANT_ID);
        assertThat(called.getStatus()).isEqualTo("called");
        assertThat(called.getCalledAt()).isNotNull();

        // Remove from queue
        queueService.removeFromQueue(PATIENT_ID, TENANT_ID);

        WaitingQueueEntity completed = queueRepository
                .findByTenantIdAndPatientIdOrderByEnteredQueueAtDesc(TENANT_ID, PATIENT_ID)
                .get(0);
        assertThat(completed.getStatus()).isEqualTo("completed");
    }

    // ========== PreVisitChecklistService Integration Tests ==========

    @Test
    void testPreVisitChecklist_CompletionTracking() {
        // Create checklist
        PreVisitChecklistEntity checklist = checklistService.createChecklist(
                "new-patient", PATIENT_ID, TENANT_ID);

        // Complete items
        checklistService.completeChecklistItem(checklist.getId(), "verifyInsurance", TENANT_ID);
        checklistService.completeChecklistItem(checklist.getId(), "reviewMedicalHistory", TENANT_ID);

        // Check completion
        PreVisitChecklistService.ChecklistCompletionStatus status =
                checklistService.getCompletionStatus(checklist.getId(), TENANT_ID);

        assertThat(status.getCompletedItems()).isEqualTo(2);
        assertThat(status.getTotalItems()).isEqualTo(8);
        assertThat(status.getCompletionPercentage()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void testPreVisitChecklist_CustomItems() {
        // Create checklist
        PreVisitChecklistEntity checklist = checklistService.createChecklist(
                "procedure-prep", PATIENT_ID, TENANT_ID);

        // Add custom items
        checklistService.addCustomItem(checklist.getId(), "Prepare surgical consent", TENANT_ID);
        checklistService.addCustomItem(checklist.getId(), "Verify NPO status", TENANT_ID);

        // Retrieve and verify
        PreVisitChecklistEntity updated = checklistService.getChecklistById(
                checklist.getId(), TENANT_ID);

        assertThat(updated.getCustomItems()).isNotNull();
        assertThat(updated.getCustomItems().size()).isEqualTo(2);
    }

    // ========== Multi-Tenant Isolation Tests ==========

    @Test
    void testMultiTenantIsolation_CheckIns() {
        String tenant1 = "TENANT_001";
        String tenant2 = "TENANT_002";

        // Create check-ins in different tenants
        checkInService.checkInPatientInternal(PATIENT_ID, APPOINTMENT_ID, tenant1);
        checkInService.checkInPatientInternal(PATIENT_ID, "Apt456", tenant2);

        // Verify isolation
        List<PatientCheckInEntity> tenant1CheckIns = checkInService.getCheckInHistoryInternal(
                PATIENT_ID, tenant1);
        List<PatientCheckInEntity> tenant2CheckIns = checkInService.getCheckInHistoryInternal(
                PATIENT_ID, tenant2);

        assertThat(tenant1CheckIns).hasSize(1);
        assertThat(tenant2CheckIns).hasSize(1);
        assertThat(tenant1CheckIns.get(0).getTenantId()).isEqualTo(tenant1);
        assertThat(tenant2CheckIns.get(0).getTenantId()).isEqualTo(tenant2);
    }
}

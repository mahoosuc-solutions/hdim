package com.healthdata.nurseworkflow.integration;

import com.healthdata.nurseworkflow.application.*;
import com.healthdata.nurseworkflow.domain.model.*;
import com.healthdata.nurseworkflow.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for all nurse workflow services
 *
 * Tests complete workflows including:
 * - Multi-tenant isolation across all services
 * - Service interactions with database
 * - Transaction management
 * - Pagination and filtering
 * - HIPAA compliance (audit logging, tenant isolation, cache behavior)
 */
@DisplayName("NurseWorkflowService Integration Tests")
class NurseWorkflowServiceIntegrationTest extends NurseWorkflowIntegrationTestBase {

    @Autowired
    private OutreachLogService outreachLogService;

    @Autowired
    private OutreachLogRepository outreachLogRepository;

    @Autowired
    private MedicationReconciliationService medicationReconciliationService;

    @Autowired
    private MedicationReconciliationRepository medicationReconciliationRepository;

    @Autowired
    private PatientEducationService patientEducationService;

    @Autowired
    private PatientEducationLogRepository patientEducationLogRepository;

    @Autowired
    private ReferralCoordinationService referralCoordinationService;

    @Autowired
    private ReferralCoordinationRepository referralCoordinationRepository;

    private String tenant1;
    private String tenant2;
    private UUID patient1;
    private UUID patient2;
    private UUID nurse1;
    private UUID educator1;
    private UUID coordinator1;

    @BeforeEach
    void setUp() {
        // Clear repositories
        outreachLogRepository.deleteAll();
        medicationReconciliationRepository.deleteAll();
        patientEducationLogRepository.deleteAll();
        referralCoordinationRepository.deleteAll();

        // Setup test data
        tenant1 = "TENANT001";
        tenant2 = "TENANT002";
        patient1 = UUID.randomUUID();
        patient2 = UUID.randomUUID();
        nurse1 = UUID.randomUUID();
        educator1 = UUID.randomUUID();
        coordinator1 = UUID.randomUUID();
    }

    // ==================== Outreach Log Integration Tests ====================

    @Test
    @DisplayName("OutreachLog: Should persist and retrieve outreach log")
    @Transactional
    void testOutreachLogPersistence() {
        // Given
        OutreachLogEntity outreach = OutreachLogEntity.builder()
            .tenantId(tenant1)
            .patientId(patient1)
            .nurseId(nurse1)
            .contactMethod(OutreachLogEntity.ContactMethod.PHONE)
            .outcomeType(OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT)
            .reason(OutreachLogEntity.Reason.CARE_GAP)
            .contactedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        // When
        OutreachLogEntity created = outreachLogService.createOutreachLog(outreach);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        Optional<OutreachLogEntity> retrieved = outreachLogService.getOutreachLogById(created.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTenantId()).isEqualTo(tenant1);
        assertThat(retrieved.get().getPatientId()).isEqualTo(patient1);
    }

    @Test
    @DisplayName("OutreachLog: Should enforce multi-tenant isolation")
    @Transactional
    void testOutreachLogMultiTenantIsolation() {
        // Given
        OutreachLogEntity outreach1 = OutreachLogEntity.builder()
            .tenantId(tenant1)
            .patientId(patient1)
            .nurseId(nurse1)
            .contactMethod(OutreachLogEntity.ContactMethod.PHONE)
            .outcomeType(OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT)
            .reason(OutreachLogEntity.Reason.CARE_GAP)
            .contactedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        OutreachLogEntity outreach2 = OutreachLogEntity.builder()
            .tenantId(tenant2)
            .patientId(patient2)
            .nurseId(nurse1)
            .contactMethod(OutreachLogEntity.ContactMethod.EMAIL)
            .outcomeType(OutreachLogEntity.OutcomeType.NO_ANSWER)
            .reason(OutreachLogEntity.Reason.APPOINTMENT)
            .contactedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        // When
        outreachLogService.createOutreachLog(outreach1);
        outreachLogService.createOutreachLog(outreach2);

        // Then - tenant1 should only see their data
        Page<OutreachLogEntity> tenant1Data = outreachLogService.getPatientOutreachHistory(
            tenant1, patient1, PageRequest.of(0, 10));
        assertThat(tenant1Data.getContent()).hasSize(1);
        assertThat(tenant1Data.getContent().get(0).getTenantId()).isEqualTo(tenant1);

        // Then - tenant2 should only see their data
        Page<OutreachLogEntity> tenant2Data = outreachLogService.getPatientOutreachHistory(
            tenant2, patient2, PageRequest.of(0, 10));
        assertThat(tenant2Data.getContent()).hasSize(1);
        assertThat(tenant2Data.getContent().get(0).getTenantId()).isEqualTo(tenant2);
    }

    @Test
    @DisplayName("OutreachLog: Should filter by outcome type")
    @Transactional
    void testOutreachLogFilterByOutcomeType() {
        // Given
        for (int i = 0; i < 3; i++) {
            outreachLogService.createOutreachLog(OutreachLogEntity.builder()
                .tenantId(tenant1)
                .patientId(patient1)
                .nurseId(nurse1)
                .contactMethod(OutreachLogEntity.ContactMethod.PHONE)
                .outcomeType(OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT)
                .reason(OutreachLogEntity.Reason.CARE_GAP)
                .contactedAt(Instant.now())
                .createdAt(Instant.now())
                .build());
        }

        for (int i = 0; i < 2; i++) {
            outreachLogService.createOutreachLog(OutreachLogEntity.builder()
                .tenantId(tenant1)
                .patientId(patient1)
                .nurseId(nurse1)
                .contactMethod(OutreachLogEntity.ContactMethod.EMAIL)
                .outcomeType(OutreachLogEntity.OutcomeType.NO_ANSWER)
                .reason(OutreachLogEntity.Reason.APPOINTMENT)
                .contactedAt(Instant.now())
                .createdAt(Instant.now())
                .build());
        }

        // When
        Page<OutreachLogEntity> successful = outreachLogService.getOutreachByOutcomeType(
            tenant1, OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT, PageRequest.of(0, 10));

        // Then
        assertThat(successful.getContent()).hasSize(3);
        assertThat(successful.getContent()).allMatch(
            o -> o.getOutcomeType() == OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT);
    }

    // ==================== Medication Reconciliation Integration Tests ====================

    @Test
    @DisplayName("MedicationReconciliation: Should persist and complete workflow")
    @Transactional
    void testMedicationReconciliationWorkflow() {
        // Given
        MedicationReconciliationEntity medRec = MedicationReconciliationEntity.builder()
            .tenantId(tenant1)
            .patientId(patient1)
            .reconcilerId(nurse1)
            .triggerType(MedicationReconciliationEntity.TriggerType.HOSPITAL_DISCHARGE)
            .medicationCount(5)
            .discrepancyCount(1)
            .patientEducationProvided(true)
            .patientUnderstanding(MedicationReconciliationEntity.PatientUnderstanding.GOOD)
            .startedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        // When - Start reconciliation
        MedicationReconciliationEntity created = medicationReconciliationService.startReconciliation(medRec);

        // Then
        assertThat(created.getStatus())
            .isEqualTo(MedicationReconciliationEntity.ReconciliationStatus.REQUESTED);

        // When - Complete reconciliation
        created.setStatus(MedicationReconciliationEntity.ReconciliationStatus.COMPLETED);
        MedicationReconciliationEntity completed = medicationReconciliationService.completeReconciliation(created);

        // Then
        assertThat(completed.getStatus())
            .isEqualTo(MedicationReconciliationEntity.ReconciliationStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("MedicationReconciliation: Should find pending reconciliations")
    @Transactional
    void testMedicationReconciliationFindPending() {
        // Given
        for (int i = 0; i < 3; i++) {
            medicationReconciliationService.startReconciliation(
                MedicationReconciliationEntity.builder()
                    .tenantId(tenant1)
                    .patientId(patient1)
                    .reconcilerId(nurse1)
                    .triggerType(MedicationReconciliationEntity.TriggerType.HOSPITAL_DISCHARGE)
                    .medicationCount(5)
                    .startedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build());
        }

        // When
        long pendingCount = medicationReconciliationService.countPendingReconciliations(tenant1);
        Page<MedicationReconciliationEntity> pending = medicationReconciliationService
            .getPendingReconciliations(tenant1, PageRequest.of(0, 10));

        // Then
        assertThat(pendingCount).isEqualTo(3);
        assertThat(pending.getContent()).hasSize(3);
        assertThat(pending.getContent()).allMatch(
            m -> m.getStatus() == MedicationReconciliationEntity.ReconciliationStatus.REQUESTED);
    }

    @Test
    @DisplayName("MedicationReconciliation: Should identify poor understanding sessions")
    @Transactional
    void testMedicationReconciliationPoorUnderstanding() {
        // Given
        MedicationReconciliationEntity poorUnderstanding = MedicationReconciliationEntity.builder()
            .tenantId(tenant1)
            .patientId(patient1)
            .reconcilerId(nurse1)
            .triggerType(MedicationReconciliationEntity.TriggerType.HOSPITAL_DISCHARGE)
            .patientUnderstanding(MedicationReconciliationEntity.PatientUnderstanding.POOR)
            .startedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        MedicationReconciliationEntity goodUnderstanding = MedicationReconciliationEntity.builder()
            .tenantId(tenant1)
            .patientId(patient2)
            .reconcilerId(nurse1)
            .triggerType(MedicationReconciliationEntity.TriggerType.HOSPITAL_ADMISSION)
            .patientUnderstanding(MedicationReconciliationEntity.PatientUnderstanding.GOOD)
            .startedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        medicationReconciliationService.startReconciliation(poorUnderstanding);
        medicationReconciliationService.startReconciliation(goodUnderstanding);

        // When
        List<MedicationReconciliationEntity> poor =
            medicationReconciliationService.findWithPoorUnderstanding(tenant1);

        // Then
        assertThat(poor).hasSize(1);
        assertThat(poor.get(0).getPatientUnderstanding())
            .isEqualTo(MedicationReconciliationEntity.PatientUnderstanding.POOR);
    }

    // ==================== Patient Education Integration Tests ====================

    @Test
    @DisplayName("PatientEducation: Should persist and retrieve education log")
    @Transactional
    void testPatientEducationPersistence() {
        // Given
        PatientEducationLogEntity education = PatientEducationLogEntity.builder()
            .tenantId(tenant1)
            .patientId(patient1)
            .educatorId(educator1)
            .materialType(PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT)
            .deliveryMethod(PatientEducationLogEntity.DeliveryMethod.IN_PERSON)
            .patientUnderstanding(PatientEducationLogEntity.PatientUnderstanding.GOOD)
            .deliveredAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        // When
        PatientEducationLogEntity created = patientEducationService.logEducationDelivery(education);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        Optional<PatientEducationLogEntity> retrieved =
            patientEducationService.getEducationLogById(created.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getMaterialType())
            .isEqualTo(PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT);
    }

    @Test
    @DisplayName("PatientEducation: Should track education by material type")
    @Transactional
    void testPatientEducationByMaterialType() {
        // Given
        for (int i = 0; i < 3; i++) {
            patientEducationService.logEducationDelivery(PatientEducationLogEntity.builder()
                .tenantId(tenant1)
                .patientId(patient1)
                .educatorId(educator1)
                .materialType(PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT)
                .deliveryMethod(PatientEducationLogEntity.DeliveryMethod.IN_PERSON)
                .patientUnderstanding(PatientEducationLogEntity.PatientUnderstanding.GOOD)
                .deliveredAt(Instant.now())
                .createdAt(Instant.now())
                .build());
        }

        // When
        List<PatientEducationLogEntity> diabetesEducation =
            patientEducationService.getPatientEducationByMaterialType(
                tenant1, patient1, PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT);

        // Then
        assertThat(diabetesEducation).hasSize(3);
        assertThat(diabetesEducation).allMatch(
            e -> e.getMaterialType() == PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT);
    }

    @Test
    @DisplayName("PatientEducation: Should generate patient metrics")
    @Transactional
    void testPatientEducationMetrics() {
        // Given
        for (int i = 0; i < 4; i++) {
            patientEducationService.logEducationDelivery(PatientEducationLogEntity.builder()
                .tenantId(tenant1)
                .patientId(patient1)
                .educatorId(educator1)
                .materialType(PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT)
                .deliveryMethod(PatientEducationLogEntity.DeliveryMethod.IN_PERSON)
                .patientUnderstanding(i < 2 ?
                    PatientEducationLogEntity.PatientUnderstanding.GOOD :
                    PatientEducationLogEntity.PatientUnderstanding.POOR)
                .deliveredAt(Instant.now())
                .createdAt(Instant.now())
                .build());
        }

        // When
        PatientEducationService.PatientEducationMetrics metrics =
            patientEducationService.getPatientEducationMetrics(tenant1, patient1);

        // Then
        assertThat(metrics.getTotalEducationSessions()).isEqualTo(4);
        assertThat(metrics.getSessionsWithPoorUnderstanding()).isEqualTo(2);
    }

    // ==================== Referral Coordination Integration Tests ====================

    @Test
    @DisplayName("ReferralCoordination: Should persist and complete referral workflow")
    @Transactional
    void testReferralCoordinationWorkflow() {
        // Given
        ReferralCoordinationEntity referral = ReferralCoordinationEntity.builder()
            .tenantId(tenant1)
            .patientId(patient1)
            .coordinatorId(coordinator1)
            .specialtyType("Cardiology")
            .priority(ReferralCoordinationEntity.Priority.ROUTINE)
            .authorizationStatus(ReferralCoordinationEntity.AuthorizationStatus.APPROVED)
            .requestedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        // When - Create referral
        ReferralCoordinationEntity created = referralCoordinationService.createReferral(referral);

        // Then
        assertThat(created.getStatus())
            .isEqualTo(ReferralCoordinationEntity.ReferralStatus.PENDING_AUTHORIZATION);

        // When - Update to authorized
        created.setStatus(ReferralCoordinationEntity.ReferralStatus.AUTHORIZED);
        ReferralCoordinationEntity updated = referralCoordinationService.updateReferral(created);

        // Then
        assertThat(updated.getStatus())
            .isEqualTo(ReferralCoordinationEntity.ReferralStatus.AUTHORIZED);
    }

    @Test
    @DisplayName("ReferralCoordination: Should identify urgent awaiting scheduling")
    @Transactional
    void testReferralCoordinationUrgentScheduling() {
        // Given
        ReferralCoordinationEntity urgent = ReferralCoordinationEntity.builder()
            .tenantId(tenant1)
            .patientId(patient1)
            .coordinatorId(coordinator1)
            .specialtyType("Cardiology")
            .priority(ReferralCoordinationEntity.Priority.URGENT)
            .status(ReferralCoordinationEntity.ReferralStatus.AUTHORIZED)
            .authorizationStatus(ReferralCoordinationEntity.AuthorizationStatus.APPROVED)
            .requestedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        ReferralCoordinationEntity routine = ReferralCoordinationEntity.builder()
            .tenantId(tenant1)
            .patientId(patient2)
            .coordinatorId(coordinator1)
            .specialtyType("Dermatology")
            .priority(ReferralCoordinationEntity.Priority.ROUTINE)
            .status(ReferralCoordinationEntity.ReferralStatus.AUTHORIZED)
            .authorizationStatus(ReferralCoordinationEntity.AuthorizationStatus.APPROVED)
            .requestedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        referralCoordinationService.createReferral(urgent);
        referralCoordinationService.createReferral(routine);

        // When
        List<ReferralCoordinationEntity> urgentList =
            referralCoordinationService.findUrgentAwaitingScheduling(tenant1);

        // Then
        assertThat(urgentList).hasSize(1);
        assertThat(urgentList.get(0).getPriority())
            .isEqualTo(ReferralCoordinationEntity.Priority.URGENT);
    }

    @Test
    @DisplayName("ReferralCoordination: Should generate metrics")
    @Transactional
    void testReferralCoordinationMetrics() {
        // Given
        for (int i = 0; i < 5; i++) {
            referralCoordinationService.createReferral(ReferralCoordinationEntity.builder()
                .tenantId(tenant1)
                .patientId(patient1)
                .coordinatorId(coordinator1)
                .specialtyType("Cardiology")
                .priority(ReferralCoordinationEntity.Priority.ROUTINE)
                .requestedAt(Instant.now())
                .createdAt(Instant.now())
                .build());
        }

        // When
        ReferralCoordinationService.ReferralMetrics metrics =
            referralCoordinationService.getMetrics(tenant1);

        // Then
        assertThat(metrics.getTotalReferrals()).isEqualTo(5);
        assertThat(metrics.getPendingReferrals()).isEqualTo(5); // All start as pending
    }

    // ==================== Cross-Service Integration Tests ====================

    @Test
    @DisplayName("Multi-Service: Complete patient workflow with all services")
    @Transactional
    void testCompletePatientNurseWorkflow() {
        // Given
        UUID testPatient = UUID.randomUUID();

        // When - Patient outreach
        OutreachLogEntity outreach = outreachLogService.createOutreachLog(
            OutreachLogEntity.builder()
                .tenantId(tenant1)
                .patientId(testPatient)
                .nurseId(nurse1)
                .contactMethod(OutreachLogEntity.ContactMethod.PHONE)
                .outcomeType(OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT)
                .reason(OutreachLogEntity.Reason.CARE_GAP)
                .contactedAt(Instant.now())
                .createdAt(Instant.now())
                .build());

        // When - Medication reconciliation
        MedicationReconciliationEntity medRec = medicationReconciliationService.startReconciliation(
            MedicationReconciliationEntity.builder()
                .tenantId(tenant1)
                .patientId(testPatient)
                .reconcilerId(nurse1)
                .triggerType(MedicationReconciliationEntity.TriggerType.HOSPITAL_DISCHARGE)
                .medicationCount(5)
                .patientEducationProvided(true)
                .patientUnderstanding(MedicationReconciliationEntity.PatientUnderstanding.GOOD)
                .startedAt(Instant.now())
                .createdAt(Instant.now())
                .build());

        // When - Patient education
        PatientEducationLogEntity education = patientEducationService.logEducationDelivery(
            PatientEducationLogEntity.builder()
                .tenantId(tenant1)
                .patientId(testPatient)
                .educatorId(educator1)
                .materialType(PatientEducationLogEntity.MaterialType.MEDICATION_ADHERENCE)
                .deliveryMethod(PatientEducationLogEntity.DeliveryMethod.IN_PERSON)
                .patientUnderstanding(PatientEducationLogEntity.PatientUnderstanding.GOOD)
                .deliveredAt(Instant.now())
                .createdAt(Instant.now())
                .build());

        // When - Referral coordination
        ReferralCoordinationEntity referral = referralCoordinationService.createReferral(
            ReferralCoordinationEntity.builder()
                .tenantId(tenant1)
                .patientId(testPatient)
                .coordinatorId(coordinator1)
                .specialtyType("Cardiology")
                .priority(ReferralCoordinationEntity.Priority.ROUTINE)
                .requestedAt(Instant.now())
                .createdAt(Instant.now())
                .build());

        // Then - All records created for same patient in same tenant
        assertThat(outreach).isNotNull();
        assertThat(medRec).isNotNull();
        assertThat(education).isNotNull();
        assertThat(referral).isNotNull();

        // And - All have correct tenant isolation
        assertThat(outreach.getTenantId()).isEqualTo(tenant1);
        assertThat(medRec.getTenantId()).isEqualTo(tenant1);
        assertThat(education.getTenantId()).isEqualTo(tenant1);
        assertThat(referral.getTenantId()).isEqualTo(tenant1);

        // And - All have same patient
        assertThat(outreach.getPatientId()).isEqualTo(testPatient);
        assertThat(medRec.getPatientId()).isEqualTo(testPatient);
        assertThat(education.getPatientId()).isEqualTo(testPatient);
        assertThat(referral.getPatientId()).isEqualTo(testPatient);
    }
}

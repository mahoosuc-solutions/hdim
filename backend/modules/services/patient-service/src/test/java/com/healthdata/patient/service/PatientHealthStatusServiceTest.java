package com.healthdata.patient.service;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PatientHealthStatusService.
 * Tests health status calculations, summaries, and alert identification.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Health Status Service Tests")
class PatientHealthStatusServiceTest {

    @Mock
    private PatientAggregationService aggregationService;

    @Mock
    private PatientTimelineService timelineService;

    private PatientHealthStatusService healthStatusService;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";

    @BeforeEach
    void setUp() {
        healthStatusService = new PatientHealthStatusService(aggregationService, timelineService);
    }

    @Nested
    @DisplayName("Get Health Status Summary Tests")
    class GetHealthStatusSummaryTests {

        @Test
        @DisplayName("Should build comprehensive health status summary")
        void shouldBuildHealthStatusSummary() {
            // Given
            setupMocksForHealthySummary();

            // When
            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.patientId()).isEqualTo(PATIENT_ID);
            assertThat(summary.assessmentDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Should count active conditions")
        void shouldCountActiveConditions() {
            // Given
            setupMocksWithActiveConditions(3);

            // When
            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.activeConditionsCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should count active medications")
        void shouldCountActiveMedications() {
            // Given
            setupMocksWithActiveMedications(5);

            // When
            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.activeMedicationsCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should count critical allergies")
        void shouldCountCriticalAllergies() {
            // Given
            setupMocksWithCriticalAllergies(2);

            // When
            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.criticalAllergiesCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should identify polypharmacy alert")
        void shouldIdentifyPolypharmacyAlert() {
            // Given
            setupMocksWithActiveMedications(7);

            // When
            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.healthAlerts())
                    .anyMatch(alert -> alert.contains("Polypharmacy"));
        }

        @Test
        @DisplayName("Should identify critical allergy alert")
        void shouldIdentifyCriticalAllergyAlert() {
            // Given
            setupMocksWithCriticalAllergies(2);

            // When
            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.healthAlerts())
                    .anyMatch(alert -> alert.contains("critical allerg"));
        }

        @Test
        @DisplayName("Should calculate immunization compliance")
        void shouldCalculateImmunizationCompliance() {
            // Given
            setupMocksWithCompletedImmunizations(12);

            // When
            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.immunizationComplianceStatus()).isEqualTo("compliant");
        }

        @Test
        @DisplayName("Should calculate medication adherence risk")
        void shouldCalculateMedicationAdherenceRisk() {
            // Given
            setupMocksWithActiveMedications(9);

            // When
            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.medicationAdherenceRisk()).isEqualTo("high");
        }
    }

    @Nested
    @DisplayName("Get Medication Summary Tests")
    class GetMedicationSummaryTests {

        @Test
        @DisplayName("Should generate medication summary")
        void shouldGenerateMedicationSummary() {
            // Given
            Bundle medications = createMedicationBundle(6, 3);
            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(medications);

            // When
            PatientHealthStatusService.MedicationSummary summary =
                    healthStatusService.getMedicationSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.activeCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should identify polypharmacy")
        void shouldIdentifyPolypharmacy() {
            // Given
            Bundle medications = createMedicationBundle(8, 8);
            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(medications);

            // When
            PatientHealthStatusService.MedicationSummary summary =
                    healthStatusService.getMedicationSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.polypharmacyFlag()).isTrue();
        }

        @Test
        @DisplayName("Should calculate adherence risk as high for many medications")
        void shouldCalculateHighAdherenceRisk() {
            // Given
            Bundle medications = createMedicationBundle(10, 10);
            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(medications);

            // When
            PatientHealthStatusService.MedicationSummary summary =
                    healthStatusService.getMedicationSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.adherenceRisk()).isEqualTo("high");
        }
    }

    @Nested
    @DisplayName("Get Allergy Summary Tests")
    class GetAllergySummaryTests {

        @Test
        @DisplayName("Should generate allergy summary")
        void shouldGenerateAllergySummary() {
            // Given
            Bundle allergies = createAllergyBundle(3, 1);
            when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(allergies);

            // When
            PatientHealthStatusService.AllergySummary summary =
                    healthStatusService.getAllergySummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.criticalCount()).isEqualTo(1);
            assertThat(summary.hasCriticalAllergies()).isTrue();
        }

        @Test
        @DisplayName("Should indicate no critical allergies")
        void shouldIndicateNoCriticalAllergies() {
            // Given
            Bundle allergies = createAllergyBundle(2, 0);
            when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(allergies);

            // When
            PatientHealthStatusService.AllergySummary summary =
                    healthStatusService.getAllergySummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.hasCriticalAllergies()).isFalse();
        }
    }

    @Nested
    @DisplayName("Get Condition Summary Tests")
    class GetConditionSummaryTests {

        @Test
        @DisplayName("Should generate condition summary")
        void shouldGenerateConditionSummary() {
            // Given
            Bundle conditions = createConditionBundle(5, 3);
            when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(conditions);

            // When
            PatientHealthStatusService.ConditionSummary summary =
                    healthStatusService.getConditionSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.activeCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should detect chronic conditions")
        void shouldDetectChronicConditions() {
            // Given
            Bundle conditions = createConditionBundle(4, 3);
            when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(conditions);

            // When
            PatientHealthStatusService.ConditionSummary summary =
                    healthStatusService.getConditionSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.hasChronicConditions()).isTrue();
        }

        @Test
        @DisplayName("Should calculate severity")
        void shouldCalculateSeverity() {
            // Given
            Bundle conditions = createConditionBundle(6, 6);
            when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(conditions);

            // When
            PatientHealthStatusService.ConditionSummary summary =
                    healthStatusService.getConditionSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.severity()).isEqualTo("high");
        }
    }

    @Nested
    @DisplayName("Get Immunization Summary Tests")
    class GetImmunizationSummaryTests {

        @Test
        @DisplayName("Should generate immunization summary")
        void shouldGenerateImmunizationSummary() {
            // Given
            Bundle immunizations = createImmunizationBundle(12, 10);
            when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(immunizations);

            // When
            PatientHealthStatusService.ImmunizationSummary summary =
                    healthStatusService.getImmunizationSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.completedCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should indicate compliant status")
        void shouldIndicateCompliantStatus() {
            // Given
            Bundle immunizations = createImmunizationBundle(12, 12);
            when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(immunizations);

            // When
            PatientHealthStatusService.ImmunizationSummary summary =
                    healthStatusService.getImmunizationSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.complianceStatus()).isEqualTo("compliant");
            assertThat(summary.isCompliant()).isTrue();
        }

        @Test
        @DisplayName("Should indicate non-compliant status")
        void shouldIndicateNonCompliantStatus() {
            // Given
            Bundle immunizations = createImmunizationBundle(4, 3);
            when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(immunizations);

            // When
            PatientHealthStatusService.ImmunizationSummary summary =
                    healthStatusService.getImmunizationSummary(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(summary.complianceStatus()).isEqualTo("non-compliant");
            assertThat(summary.isCompliant()).isFalse();
        }
    }

    // ==================== Setup Methods ====================

    private void setupMocksForHealthySummary() {
        when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createAllergyBundle(1, 0));
        when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createImmunizationBundle(10, 10));
        when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createMedicationBundle(3, 2));
        when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createConditionBundle(2, 1));
        when(aggregationService.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getEncounters(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEncounterBundle(3));
        when(aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(timelineService.getRecentActivityCount(eq(TENANT_ID), eq(PATIENT_ID), anyInt()))
                .thenReturn(5);
    }

    private void setupMocksWithActiveConditions(int count) {
        when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createConditionBundle(count, count));
        when(aggregationService.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getEncounters(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(timelineService.getRecentActivityCount(eq(TENANT_ID), eq(PATIENT_ID), anyInt()))
                .thenReturn(0);
    }

    private void setupMocksWithActiveMedications(int count) {
        when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createMedicationBundle(count, count));
        when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getEncounters(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(timelineService.getRecentActivityCount(eq(TENANT_ID), eq(PATIENT_ID), anyInt()))
                .thenReturn(0);
    }

    private void setupMocksWithCriticalAllergies(int count) {
        when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createAllergyBundle(count, count));
        when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getEncounters(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(timelineService.getRecentActivityCount(eq(TENANT_ID), eq(PATIENT_ID), anyInt()))
                .thenReturn(0);
    }

    private void setupMocksWithCompletedImmunizations(int count) {
        when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createImmunizationBundle(count, count));
        when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getEncounters(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, false))
                .thenReturn(createEmptyBundle());
        when(aggregationService.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(createEmptyBundle());
        when(timelineService.getRecentActivityCount(eq(TENANT_ID), eq(PATIENT_ID), anyInt()))
                .thenReturn(0);
    }

    // ==================== Bundle Creation Helpers ====================

    private Bundle createEmptyBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(0);
        return bundle;
    }

    private Bundle createAllergyBundle(int total, int critical) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(total);

        for (int i = 0; i < total; i++) {
            AllergyIntolerance allergy = new AllergyIntolerance();
            allergy.setId("allergy-" + i);
            if (i < critical) {
                allergy.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.HIGH);
            } else {
                allergy.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.LOW);
            }
            allergy.getCode().setText("Allergen " + i);
            bundle.addEntry().setResource(allergy);
        }

        return bundle;
    }

    private Bundle createImmunizationBundle(int total, int completed) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(total);

        for (int i = 0; i < total; i++) {
            Immunization immunization = new Immunization();
            immunization.setId("imm-" + i);
            if (i < completed) {
                immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
            } else {
                immunization.setStatus(Immunization.ImmunizationStatus.ENTEREDINERROR);
            }
            immunization.getVaccineCode().setText("Vaccine " + i);
            bundle.addEntry().setResource(immunization);
        }

        return bundle;
    }

    private Bundle createMedicationBundle(int total, int active) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(total);

        for (int i = 0; i < total; i++) {
            MedicationRequest med = new MedicationRequest();
            med.setId("med-" + i);
            if (i < active) {
                med.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
            } else {
                med.setStatus(MedicationRequest.MedicationRequestStatus.STOPPED);
            }
            med.getMedicationCodeableConcept().setText("Medication " + i);
            bundle.addEntry().setResource(med);
        }

        return bundle;
    }

    private Bundle createConditionBundle(int total, int active) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(total);

        for (int i = 0; i < total; i++) {
            Condition condition = new Condition();
            condition.setId("cond-" + i);
            if (i < active) {
                condition.getClinicalStatus().addCoding().setCode("active");
            } else {
                condition.getClinicalStatus().addCoding().setCode("resolved");
            }
            condition.getCode().setText("Condition " + i);
            bundle.addEntry().setResource(condition);
        }

        return bundle;
    }

    private Bundle createEncounterBundle(int count) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(count);

        for (int i = 0; i < count; i++) {
            Encounter encounter = new Encounter();
            encounter.setId("enc-" + i);
            encounter.setStatus(Encounter.EncounterStatus.FINISHED);
            Period period = new Period();
            period.setStart(Date.from(LocalDate.now().minusDays(i * 30).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            encounter.setPeriod(period);
            bundle.addEntry().setResource(encounter);
        }

        return bundle;
    }
}

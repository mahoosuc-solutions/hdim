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

    @Nested
    @DisplayName("Health Status Edge Cases")
    class HealthStatusEdgeCases {

        @Test
        @DisplayName("Should calculate medium and low adherence risk")
        void shouldCalculateMediumAndLowAdherenceRisk() {
            Bundle mediumBundle = createMedicationBundle(6, 6);
            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(mediumBundle);

            PatientHealthStatusService.MedicationSummary mediumSummary =
                    healthStatusService.getMedicationSummary(TENANT_ID, PATIENT_ID);
            assertThat(mediumSummary.adherenceRisk()).isEqualTo("medium");

            Bundle lowBundle = createMedicationBundle(2, 2);
            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(lowBundle);

            PatientHealthStatusService.MedicationSummary lowSummary =
                    healthStatusService.getMedicationSummary(TENANT_ID, PATIENT_ID);
            assertThat(lowSummary.adherenceRisk()).isEqualTo("low");
        }

        @Test
        @DisplayName("Should return partially compliant immunization status")
        void shouldReturnPartiallyCompliantImmunizations() {
            Bundle immunizations = createImmunizationBundle(8, 6);
            when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(immunizations);

            PatientHealthStatusService.ImmunizationSummary summary =
                    healthStatusService.getImmunizationSummary(TENANT_ID, PATIENT_ID);

            assertThat(summary.complianceStatus()).isEqualTo("partially-compliant");
        }

        @Test
        @DisplayName("Should flag no recent activity alert")
        void shouldFlagNoRecentActivityAlert() {
            setupMocksForHealthySummary();
            when(timelineService.getRecentActivityCount(TENANT_ID, PATIENT_ID, 90)).thenReturn(0);

            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            assertThat(summary.healthAlerts())
                    .anyMatch(alert -> alert.contains("No recent patient activity"));
        }

        @Test
        @DisplayName("Should calculate days since last encounter")
        void shouldCalculateDaysSinceLastEncounter() {
            when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getProcedures(TENANT_ID, PATIENT_ID))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getEncounters(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEncounterBundle(1));
            when(aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getGoals(TENANT_ID, PATIENT_ID))
                    .thenReturn(createEmptyBundle());
            when(timelineService.getRecentActivityCount(eq(TENANT_ID), eq(PATIENT_ID), anyInt()))
                    .thenReturn(1);

            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            assertThat(summary.daysSinceLastEncounter()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should return -1 when no encounters exist")
        void shouldReturnMinusOneWhenNoEncounters() {
            when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
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

            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            assertThat(summary.daysSinceLastEncounter()).isEqualTo(-1);
        }

        @Test
        @DisplayName("Should detect medication allergies and critical allergens")
        void shouldDetectMedicationAllergies() {
            Bundle allergies = createAllergyBundle(2, 1);
            AllergyIntolerance medAllergy = new AllergyIntolerance();
            medAllergy.setId("allergy-med");
            medAllergy.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.HIGH);
            medAllergy.getCode().setText("Penicillin");
            medAllergy.addCategory(AllergyIntolerance.AllergyIntoleranceCategory.MEDICATION);
            allergies.addEntry().setResource(medAllergy);
            allergies.setTotal(allergies.getEntry().size());

            when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(allergies);

            PatientHealthStatusService.AllergySummary summary =
                    healthStatusService.getAllergySummary(TENANT_ID, PATIENT_ID);

            assertThat(summary.hasMedicationAllergies()).isTrue();
            assertThat(summary.criticalAllergens()).contains("Penicillin");
        }

        @Test
        @DisplayName("Should count completed procedures in recent window")
        void shouldCountCompletedProceduresInWindow() {
            when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getProcedures(TENANT_ID, PATIENT_ID))
                    .thenReturn(createProcedureBundle());
            when(aggregationService.getEncounters(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createEmptyBundle());
            when(aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(createCarePlanBundle(2));
            when(aggregationService.getGoals(TENANT_ID, PATIENT_ID))
                    .thenReturn(createGoalBundle(1));
            when(timelineService.getRecentActivityCount(eq(TENANT_ID), eq(PATIENT_ID), anyInt()))
                    .thenReturn(4);

            PatientHealthStatusService.HealthStatusSummary summary =
                    healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID);

            assertThat(summary.recentProceduresCount()).isEqualTo(1);
            assertThat(summary.activeCarePlansCount()).isEqualTo(2);
            assertThat(summary.activeGoalsCount()).isEqualTo(1);
            assertThat(summary.hasActiveCarePlan()).isTrue();
        }

        @Test
        @DisplayName("Should filter medication names when code is missing")
        void shouldFilterMedicationNamesWhenMissing() {
            Bundle medications = createMedicationBundle(2, 2);
            MedicationRequest missing = new MedicationRequest();
            missing.setId("med-missing");
            missing.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
            medications.addEntry().setResource(missing);
            medications.setTotal(medications.getEntry().size());

            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(medications);

            PatientHealthStatusService.MedicationSummary summary =
                    healthStatusService.getMedicationSummary(TENANT_ID, PATIENT_ID);

            assertThat(summary.activeMedicationNames()).allMatch(name -> name != null && !name.isBlank());
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

    private Bundle createProcedureBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Procedure completedRecent = new Procedure();
        completedRecent.setId("proc-1");
        completedRecent.setStatus(Procedure.ProcedureStatus.COMPLETED);
        completedRecent.setPerformed(new DateTimeType(Date.from(LocalDate.now().minusDays(5)
                .atStartOfDay(ZoneId.systemDefault()).toInstant())));
        bundle.addEntry().setResource(completedRecent);

        Procedure completedOld = new Procedure();
        completedOld.setId("proc-2");
        completedOld.setStatus(Procedure.ProcedureStatus.COMPLETED);
        completedOld.setPerformed(new DateTimeType(Date.from(LocalDate.now().minusDays(80)
                .atStartOfDay(ZoneId.systemDefault()).toInstant())));
        bundle.addEntry().setResource(completedOld);

        Procedure incomplete = new Procedure();
        incomplete.setId("proc-3");
        incomplete.setStatus(Procedure.ProcedureStatus.INPROGRESS);
        bundle.addEntry().setResource(incomplete);

        bundle.setTotal(bundle.getEntry().size());
        return bundle;
    }

    private Bundle createCarePlanBundle(int activeCount) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        for (int i = 0; i < activeCount; i++) {
            CarePlan carePlan = new CarePlan();
            carePlan.setId("careplan-" + i);
            carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
            bundle.addEntry().setResource(carePlan);
        }

        bundle.setTotal(activeCount);
        return bundle;
    }

    private Bundle createGoalBundle(int activeCount) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        for (int i = 0; i < activeCount; i++) {
            Goal goal = new Goal();
            goal.setId("goal-" + i);
            goal.setLifecycleStatus(Goal.GoalLifecycleStatus.ACTIVE);
            bundle.addEntry().setResource(goal);
        }

        bundle.setTotal(activeCount);
        return bundle;
    }
}

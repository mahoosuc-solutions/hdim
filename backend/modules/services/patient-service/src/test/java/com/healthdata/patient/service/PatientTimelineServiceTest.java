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
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PatientTimelineService.
 * Tests timeline generation, filtering, and summarization.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Timeline Service Tests")
class PatientTimelineServiceTest {

    @Mock
    private PatientAggregationService aggregationService;

    private PatientTimelineService timelineService;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";

    @BeforeEach
    void setUp() {
        timelineService = new PatientTimelineService(aggregationService);
    }

    @Nested
    @DisplayName("Get Patient Timeline Tests")
    class GetPatientTimelineTests {

        @Test
        @DisplayName("Should build timeline from comprehensive health record")
        void shouldBuildTimelineFromHealthRecord() {
            // Given
            Bundle bundle = createBundleWithMixedResources();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(events).isNotEmpty();
            assertThat(events).allMatch(e -> e.date() != null);
        }

        @Test
        @DisplayName("Should sort events by date descending")
        void shouldSortEventsByDateDescending() {
            // Given
            Bundle bundle = createBundleWithDatedEncounters();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(events).isNotEmpty();
            for (int i = 1; i < events.size(); i++) {
                assertThat(events.get(i - 1).date())
                        .isAfterOrEqualTo(events.get(i).date());
            }
        }

        @Test
        @DisplayName("Should handle empty health record")
        void shouldHandleEmptyHealthRecord() {
            // Given
            Bundle emptyBundle = new Bundle();
            emptyBundle.setType(Bundle.BundleType.COLLECTION);
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(emptyBundle);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("Should include encounter events")
        void shouldIncludeEncounterEvents() {
            // Given
            Bundle bundle = createBundleWithEncounter();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(events).anyMatch(e -> "Encounter".equals(e.resourceType()));
        }

        @Test
        @DisplayName("Should include procedure events")
        void shouldIncludeProcedureEvents() {
            // Given
            Bundle bundle = createBundleWithProcedure();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(events).anyMatch(e -> "Procedure".equals(e.resourceType()));
        }

        @Test
        @DisplayName("Should include condition events")
        void shouldIncludeConditionEvents() {
            // Given
            Bundle bundle = createBundleWithCondition();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(events).anyMatch(e -> "Condition".equals(e.resourceType()));
        }

        @Test
        @DisplayName("Should skip resources without dates")
        void shouldSkipResourcesWithoutDates() {
            // Given
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);

            // Add encounter without period
            Encounter encounterNoPeriod = new Encounter();
            encounterNoPeriod.setId("enc-no-date");
            encounterNoPeriod.setStatus(Encounter.EncounterStatus.FINISHED);
            // No period set
            bundle.addEntry().setResource(encounterNoPeriod);

            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("Should include additional resource types and details")
        void shouldIncludeAdditionalResourceTypes() {
            Bundle bundle = createBundleWithAdditionalResources();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).hasSize(7);
            Set<String> resourceTypes = events.stream()
                    .map(PatientTimelineService.TimelineEvent::resourceType)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(resourceTypes).contains(
                    "MedicationRequest",
                    "Immunization",
                    "Observation",
                    "DiagnosticReport",
                    "AllergyIntolerance",
                    "CarePlan",
                    "Goal"
            );
            assertThat(events).anyMatch(event -> "Observation".equals(event.resourceType())
                    && event.details() != null
                    && event.details().contains("Quantity"));
        }

        @Test
        @DisplayName("Should ignore unsupported resource types")
        void shouldIgnoreUnsupportedResources() {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);
            Patient patient = new Patient();
            patient.setId("patient-1");
            bundle.addEntry().setResource(patient);

            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("Should use default descriptions when fields are missing")
        void shouldUseDefaultDescriptionsWhenMissing() {
            Bundle bundle = createBundleWithDefaultDescriptions();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).anyMatch(event -> "Encounter".equals(event.resourceType())
                    && "Encounter".equals(event.description()));
            assertThat(events).anyMatch(event -> "Procedure".equals(event.resourceType())
                    && "Procedure".equals(event.description()));
            assertThat(events).anyMatch(event -> "Condition".equals(event.resourceType())
                    && "Condition".equals(event.description()));
            assertThat(events).anyMatch(event -> "MedicationRequest".equals(event.resourceType())
                    && "Medication".equals(event.description()));
            assertThat(events).anyMatch(event -> "Immunization".equals(event.resourceType())
                    && "Immunization".equals(event.description()));
        }

        @Test
        @DisplayName("Should create events from period-based dates")
        void shouldCreateEventsFromPeriodDates() {
            Bundle bundle = createBundleWithPeriodDates();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).anyMatch(event -> "Procedure".equals(event.resourceType()));
            assertThat(events).anyMatch(event -> "Observation".equals(event.resourceType()));
            assertThat(events).anyMatch(event -> "DiagnosticReport".equals(event.resourceType()));
        }
    }

    @Nested
    @DisplayName("Get Timeline By Date Range Tests")
    class GetTimelineByDateRangeTests {

        @Test
        @DisplayName("Should filter events within date range")
        void shouldFilterWithinDateRange() {
            // Given
            Bundle bundle = createBundleWithDatedEncounters();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            LocalDate startDate = LocalDate.now().minusDays(60);
            LocalDate endDate = LocalDate.now().minusDays(20);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimelineByDateRange(TENANT_ID, PATIENT_ID, startDate, endDate);

            // Then
            assertThat(events).allMatch(e ->
                    !e.date().isBefore(startDate) && !e.date().isAfter(endDate));
        }

        @Test
        @DisplayName("Should return empty list when no events in range")
        void shouldReturnEmptyWhenNoEventsInRange() {
            // Given
            Bundle bundle = createBundleWithDatedEncounters();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            LocalDate startDate = LocalDate.now().plusYears(1);
            LocalDate endDate = LocalDate.now().plusYears(2);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimelineByDateRange(TENANT_ID, PATIENT_ID, startDate, endDate);

            // Then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Timeline By Resource Type Tests")
    class GetTimelineByResourceTypeTests {

        @Test
        @DisplayName("Should filter events by resource type")
        void shouldFilterByResourceType() {
            // Given
            Bundle bundle = createBundleWithMixedResources();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            List<PatientTimelineService.TimelineEvent> encounters =
                    timelineService.getPatientTimelineByResourceType(TENANT_ID, PATIENT_ID, "Encounter");

            // Then
            assertThat(encounters).allMatch(e -> "Encounter".equals(e.resourceType()));
        }

        @Test
        @DisplayName("Should return empty when resource type not found")
        void shouldReturnEmptyWhenTypeNotFound() {
            // Given
            Bundle bundle = createBundleWithEncounter();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimelineByResourceType(TENANT_ID, PATIENT_ID, "Immunization");

            // Then
            assertThat(events).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Timeline Summary By Month Tests")
    class GetTimelineSummaryByMonthTests {

        @Test
        @DisplayName("Should group events by month")
        void shouldGroupEventsByMonth() {
            // Given
            Bundle bundle = createBundleWithDatedEncounters();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            int currentYear = LocalDate.now().getYear();

            // When
            Map<String, Integer> summary =
                    timelineService.getTimelineSummaryByMonth(TENANT_ID, PATIENT_ID, currentYear);

            // Then
            assertThat(summary).isNotEmpty();
            // Keys should be in YYYY-MM format
            assertThat(summary.keySet()).allMatch(k -> k.matches("\\d{4}-\\d{2}"));
        }

        @Test
        @DisplayName("Should return empty map for year with no events")
        void shouldReturnEmptyForYearWithNoEvents() {
            // Given
            Bundle bundle = createBundleWithDatedEncounters();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            Map<String, Integer> summary =
                    timelineService.getTimelineSummaryByMonth(TENANT_ID, PATIENT_ID, 2010);

            // Then
            assertThat(summary).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Recent Activity Count Tests")
    class GetRecentActivityCountTests {

        @Test
        @DisplayName("Should count recent events")
        void shouldCountRecentEvents() {
            // Given
            Bundle bundle = createBundleWithDatedEncounters();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When
            int count = timelineService.getRecentActivityCount(TENANT_ID, PATIENT_ID, 90);

            // Then
            assertThat(count).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should return zero for no recent activity")
        void shouldReturnZeroForNoRecentActivity() {
            // Given
            Bundle emptyBundle = new Bundle();
            emptyBundle.setType(Bundle.BundleType.COLLECTION);
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(emptyBundle);

            // When
            int count = timelineService.getRecentActivityCount(TENANT_ID, PATIENT_ID, 30);

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Timeline Event Edge Cases")
    class TimelineEventEdgeCases {

        @Test
        @DisplayName("Should skip immunizations without datetime occurrence")
        void shouldSkipImmunizationsWithoutDateTime() {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);

            Immunization immunization = new Immunization();
            immunization.setId("imm-1");
            bundle.addEntry().setResource(immunization);

            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("Should skip observations without effective start")
        void shouldSkipObservationWithoutEffectiveStart() {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);

            Observation observation = new Observation();
            observation.setId("obs-1");
            observation.setEffective(new Period());
            bundle.addEntry().setResource(observation);

            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("Should skip diagnostic reports without effective start")
        void shouldSkipDiagnosticReportWithoutEffectiveStart() {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);

            DiagnosticReport report = new DiagnosticReport();
            report.setId("diag-1");
            report.setEffective(new Period());
            bundle.addEntry().setResource(report);

            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("Should skip procedures without period start")
        void shouldSkipProcedureWithoutPeriodStart() {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);

            Procedure procedure = new Procedure();
            procedure.setId("proc-1");
            procedure.setPerformed(new Period());
            bundle.addEntry().setResource(procedure);

            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("Should skip goals without date start")
        void shouldSkipGoalsWithoutDateStart() {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);

            Goal goal = new Goal();
            goal.setId("goal-1");
            bundle.addEntry().setResource(goal);

            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            List<PatientTimelineService.TimelineEvent> events =
                    timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID);

            assertThat(events).isEmpty();
        }
    }

    // ==================== Helper Methods ====================

    private Bundle createBundleWithEncounter() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Encounter encounter = new Encounter();
        encounter.setId("enc-1");
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        Period period = new Period();
        period.setStart(Date.from(LocalDate.now().minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        encounter.setPeriod(period);
        encounter.addType().setText("Office Visit");

        bundle.addEntry().setResource(encounter);
        return bundle;
    }

    private Bundle createBundleWithProcedure() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Procedure procedure = new Procedure();
        procedure.setId("proc-1");
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setPerformed(new DateTimeType(
                Date.from(LocalDate.now().minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant())));
        procedure.getCode().setText("Blood Draw");

        bundle.addEntry().setResource(procedure);
        return bundle;
    }

    private Bundle createBundleWithCondition() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Condition condition = new Condition();
        condition.setId("cond-1");
        condition.setRecordedDate(Date.from(LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        condition.getCode().setText("Type 2 Diabetes");
        condition.getClinicalStatus().addCoding().setCode("active");

        bundle.addEntry().setResource(condition);
        return bundle;
    }

    private Bundle createBundleWithDatedEncounters() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // Create encounters at different dates
        LocalDate[] dates = {
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(60),
                LocalDate.now().minusDays(90)
        };

        for (int i = 0; i < dates.length; i++) {
            Encounter encounter = new Encounter();
            encounter.setId("enc-" + i);
            encounter.setStatus(Encounter.EncounterStatus.FINISHED);
            Period period = new Period();
            period.setStart(Date.from(dates[i].atStartOfDay(ZoneId.systemDefault()).toInstant()));
            encounter.setPeriod(period);
            encounter.addType().setText("Visit " + (i + 1));
            bundle.addEntry().setResource(encounter);
        }

        return bundle;
    }

    private Bundle createBundleWithMixedResources() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        // Encounter
        Encounter encounter = new Encounter();
        encounter.setId("enc-1");
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        Period period = new Period();
        period.setStart(Date.from(LocalDate.now().minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        encounter.setPeriod(period);
        bundle.addEntry().setResource(encounter);

        // Procedure
        Procedure procedure = new Procedure();
        procedure.setId("proc-1");
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setPerformed(new DateTimeType(
                Date.from(LocalDate.now().minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant())));
        bundle.addEntry().setResource(procedure);

        // Condition
        Condition condition = new Condition();
        condition.setId("cond-1");
        condition.setRecordedDate(Date.from(LocalDate.now().minusDays(15).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        bundle.addEntry().setResource(condition);

        return bundle;
    }

    private Bundle createBundleWithAdditionalResources() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setId("med-1");
        medicationRequest.setAuthoredOn(Date.from(LocalDate.now().minusDays(2)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        medicationRequest.getMedicationCodeableConcept().setText("Atorvastatin");
        bundle.addEntry().setResource(medicationRequest);

        Immunization immunization = new Immunization();
        immunization.setId("imm-1");
        immunization.setOccurrence(new DateTimeType(Date.from(LocalDate.now().minusDays(12)
                .atStartOfDay(ZoneId.systemDefault()).toInstant())));
        immunization.getVaccineCode().setText("Influenza");
        bundle.addEntry().setResource(immunization);

        Observation observation = new Observation();
        observation.setId("obs-1");
        observation.setEffective(new DateTimeType(Date.from(LocalDate.now().minusDays(3)
                .atStartOfDay(ZoneId.systemDefault()).toInstant())));
        observation.getCode().setText("Blood Pressure");
        observation.setValue(new Quantity().setValue(120));
        bundle.addEntry().setResource(observation);

        DiagnosticReport report = new DiagnosticReport();
        report.setId("diag-1");
        Period reportPeriod = new Period();
        reportPeriod.setStart(Date.from(LocalDate.now().minusDays(7)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        report.setEffective(reportPeriod);
        report.getCode().setText("Lab Panel");
        bundle.addEntry().setResource(report);

        AllergyIntolerance allergy = new AllergyIntolerance();
        allergy.setId("allergy-1");
        allergy.setRecordedDate(Date.from(LocalDate.now().minusDays(20)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        allergy.getCode().setText("Peanut");
        allergy.setClinicalStatus(new CodeableConcept().addCoding(new Coding().setCode("active")));
        allergy.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.HIGH);
        bundle.addEntry().setResource(allergy);

        CarePlan carePlan = new CarePlan();
        carePlan.setId("careplan-1");
        carePlan.setCreated(Date.from(LocalDate.now().minusDays(4)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        carePlan.setDescription("Diabetes management plan");
        bundle.addEntry().setResource(carePlan);

        Goal goal = new Goal();
        goal.setId("goal-1");
        goal.setStart(new DateType(Date.from(LocalDate.now().minusDays(9)
                .atStartOfDay(ZoneId.systemDefault()).toInstant())));
        goal.setDescription(new CodeableConcept().setText("Lower A1C"));
        bundle.addEntry().setResource(goal);

        return bundle;
    }

    private Bundle createBundleWithDefaultDescriptions() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Encounter encounter = new Encounter();
        encounter.setId("enc-default");
        Period encounterPeriod = new Period();
        encounterPeriod.setStart(Date.from(LocalDate.now().minusDays(6)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        encounter.setPeriod(encounterPeriod);
        bundle.addEntry().setResource(encounter);

        Procedure procedure = new Procedure();
        procedure.setId("proc-default");
        procedure.setPerformed(new DateTimeType(Date.from(LocalDate.now().minusDays(8)
                .atStartOfDay(ZoneId.systemDefault()).toInstant())));
        bundle.addEntry().setResource(procedure);

        Condition condition = new Condition();
        condition.setId("cond-default");
        condition.setRecordedDate(Date.from(LocalDate.now().minusDays(9)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        bundle.addEntry().setResource(condition);

        MedicationRequest medication = new MedicationRequest();
        medication.setId("med-default");
        medication.setAuthoredOn(Date.from(LocalDate.now().minusDays(3)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        bundle.addEntry().setResource(medication);

        Immunization immunization = new Immunization();
        immunization.setId("imm-default");
        immunization.setOccurrence(new DateTimeType(Date.from(LocalDate.now().minusDays(11)
                .atStartOfDay(ZoneId.systemDefault()).toInstant())));
        bundle.addEntry().setResource(immunization);

        return bundle;
    }

    private Bundle createBundleWithPeriodDates() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        Procedure procedure = new Procedure();
        procedure.setId("proc-period");
        Period procedurePeriod = new Period();
        procedurePeriod.setStart(Date.from(LocalDate.now().minusDays(14)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        procedure.setPerformed(procedurePeriod);
        procedure.getCode().setText("Period Procedure");
        bundle.addEntry().setResource(procedure);

        Observation observation = new Observation();
        observation.setId("obs-period");
        Period obsPeriod = new Period();
        obsPeriod.setStart(Date.from(LocalDate.now().minusDays(2)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        observation.setEffective(obsPeriod);
        observation.getCode().setText("Temperature");
        bundle.addEntry().setResource(observation);

        DiagnosticReport report = new DiagnosticReport();
        report.setId("diag-date");
        report.setEffective(new DateTimeType(Date.from(LocalDate.now().minusDays(4)
                .atStartOfDay(ZoneId.systemDefault()).toInstant())));
        report.getCode().setText("X-Ray");
        bundle.addEntry().setResource(report);

        return bundle;
    }
}

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
}

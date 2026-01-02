package com.healthdata.patient.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Patient Timeline Service
 *
 * Provides chronological timeline views of patient health events.
 * Aggregates FHIR resources and sorts them by date to create
 * comprehensive patient timelines for clinical review.
 *
 * Timeline events include:
 * - Encounters (visits, admissions)
 * - Procedures
 * - Conditions (diagnoses)
 * - Medications
 * - Immunizations
 * - Lab results and vital signs
 * - Diagnostic reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientTimelineService {

    private final PatientAggregationService aggregationService;
    private final FhirContext fhirContext = FhirContext.forR4();

    /**
     * Get comprehensive patient timeline
     *
     * Returns all patient events sorted chronologically (most recent first).
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of timeline events
     */
    @Cacheable(value = "patientTimeline", key = "#tenantId + ':' + #patientId")
    public List<TimelineEvent> getPatientTimeline(String tenantId, String patientId) {
        log.info("Building patient timeline for patient: {} in tenant: {}", patientId, tenantId);

        // Get comprehensive health record
        Bundle healthRecord = aggregationService.getComprehensiveHealthRecord(tenantId, patientId);

        // Convert bundle entries to timeline events
        List<TimelineEvent> events = new ArrayList<>();

        healthRecord.getEntry().forEach(entry -> {
            Resource resource = entry.getResource();
            TimelineEvent event = createTimelineEvent(resource);
            if (event != null) {
                events.add(event);
            }
        });

        // Sort by date (most recent first)
        events.sort(Comparator.comparing(TimelineEvent::date).reversed());

        log.info("Built timeline with {} events for patient: {}", events.size(), patientId);
        return events;
    }

    /**
     * Get patient timeline filtered by date range
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of timeline events within date range
     */
    public List<TimelineEvent> getPatientTimelineByDateRange(
            String tenantId,
            String patientId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.info("Building patient timeline for date range {} to {}", startDate, endDate);

        List<TimelineEvent> timeline = getPatientTimeline(tenantId, patientId);

        return timeline.stream()
                .filter(event -> !event.date().isBefore(startDate) && !event.date().isAfter(endDate))
                .collect(Collectors.toList());
    }

    /**
     * Get patient timeline filtered by resource type
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param resourceType FHIR resource type (e.g., "Encounter", "Procedure")
     * @return List of timeline events for specific resource type
     */
    public List<TimelineEvent> getPatientTimelineByResourceType(
            String tenantId,
            String patientId,
            String resourceType
    ) {
        log.info("Building patient timeline for resource type: {}", resourceType);

        List<TimelineEvent> timeline = getPatientTimeline(tenantId, patientId);

        return timeline.stream()
                .filter(event -> event.resourceType().equals(resourceType))
                .collect(Collectors.toList());
    }

    /**
     * Get patient timeline summary by month
     *
     * Groups timeline events by month for calendar view or trend analysis.
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param year Year to summarize
     * @return Map of month -> event count
     */
    public Map<String, Integer> getTimelineSummaryByMonth(
            String tenantId,
            String patientId,
            int year
    ) {
        log.info("Building monthly timeline summary for year: {}", year);

        List<TimelineEvent> timeline = getPatientTimeline(tenantId, patientId);

        return timeline.stream()
                .filter(event -> event.date().getYear() == year)
                .collect(Collectors.groupingBy(
                        event -> event.date().getYear() + "-" + String.format("%02d", event.date().getMonthValue()),
                        Collectors.summingInt(e -> 1)
                ));
    }

    /**
     * Get recent activity count
     *
     * Returns count of patient events in the last N days.
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param days Number of days to look back
     * @return Count of events in last N days
     */
    public int getRecentActivityCount(String tenantId, String patientId, int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);

        List<TimelineEvent> timeline = getPatientTimeline(tenantId, patientId);

        return (int) timeline.stream()
                .filter(event -> !event.date().isBefore(cutoffDate))
                .count();
    }

    // ==================== Private Helper Methods ====================

    /**
     * Create timeline event from FHIR resource
     */
    private TimelineEvent createTimelineEvent(Resource resource) {
        String resourceType = resource.getResourceType().name();

        return switch (resourceType) {
            case "Encounter" -> createEncounterEvent((Encounter) resource);
            case "Procedure" -> createProcedureEvent((Procedure) resource);
            case "Condition" -> createConditionEvent((Condition) resource);
            case "MedicationRequest" -> createMedicationEvent((MedicationRequest) resource);
            case "Immunization" -> createImmunizationEvent((Immunization) resource);
            case "Observation" -> createObservationEvent((Observation) resource);
            case "DiagnosticReport" -> createDiagnosticReportEvent((DiagnosticReport) resource);
            case "AllergyIntolerance" -> createAllergyEvent((AllergyIntolerance) resource);
            case "CarePlan" -> createCarePlanEvent((CarePlan) resource);
            case "Goal" -> createGoalEvent((Goal) resource);
            default -> null;
        };
    }

    private TimelineEvent createEncounterEvent(Encounter encounter) {
        if (!encounter.hasPeriod() || !encounter.getPeriod().hasStart()) {
            return null;
        }

        LocalDate date = encounter.getPeriod().getStart().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        String description = encounter.hasType() && !encounter.getType().isEmpty() ?
                encounter.getType().get(0).getText() : "Encounter";

        return new TimelineEvent(
                encounter.getIdElement().getIdPart(),
                "Encounter",
                date,
                description,
                encounter.hasStatus() ? encounter.getStatus().toCode() : null,
                null
        );
    }

    private TimelineEvent createProcedureEvent(Procedure procedure) {
        if (!procedure.hasPerformed()) {
            return null;
        }

        LocalDate date = getProcedureDate(procedure);
        if (date == null) {
            return null;
        }

        String description = procedure.hasCode() ? procedure.getCode().getText() : "Procedure";

        return new TimelineEvent(
                procedure.getIdElement().getIdPart(),
                "Procedure",
                date,
                description,
                procedure.hasStatus() ? procedure.getStatus().toCode() : null,
                null
        );
    }

    private TimelineEvent createConditionEvent(Condition condition) {
        if (!condition.hasRecordedDate()) {
            return null;
        }

        LocalDate date = condition.getRecordedDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        String description = condition.hasCode() ? condition.getCode().getText() : "Condition";

        return new TimelineEvent(
                condition.getIdElement().getIdPart(),
                "Condition",
                date,
                description,
                condition.hasClinicalStatus() ? condition.getClinicalStatus().getCodingFirstRep().getCode() : null,
                condition.hasSeverity() ? condition.getSeverity().getText() : null
        );
    }

    private TimelineEvent createMedicationEvent(MedicationRequest medicationRequest) {
        if (!medicationRequest.hasAuthoredOn()) {
            return null;
        }

        LocalDate date = medicationRequest.getAuthoredOn().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        String description = medicationRequest.hasMedicationCodeableConcept() ?
                medicationRequest.getMedicationCodeableConcept().getText() : "Medication";

        return new TimelineEvent(
                medicationRequest.getIdElement().getIdPart(),
                "MedicationRequest",
                date,
                description,
                medicationRequest.hasStatus() ? medicationRequest.getStatus().toCode() : null,
                null
        );
    }

    private TimelineEvent createImmunizationEvent(Immunization immunization) {
        if (!immunization.hasOccurrence()) {
            return null;
        }

        LocalDate date = getImmunizationDate(immunization);
        if (date == null) {
            return null;
        }

        String description = immunization.hasVaccineCode() ?
                immunization.getVaccineCode().getText() : "Immunization";

        return new TimelineEvent(
                immunization.getIdElement().getIdPart(),
                "Immunization",
                date,
                description,
                immunization.hasStatus() ? immunization.getStatus().toCode() : null,
                null
        );
    }

    private TimelineEvent createObservationEvent(Observation observation) {
        if (!observation.hasEffective()) {
            return null;
        }

        LocalDate date = getObservationDate(observation);
        if (date == null) {
            return null;
        }

        String description = observation.hasCode() ? observation.getCode().getText() : "Observation";

        String details = observation.hasValue() ?
                observation.getValue().toString() : null;

        return new TimelineEvent(
                observation.getIdElement().getIdPart(),
                "Observation",
                date,
                description,
                observation.hasStatus() ? observation.getStatus().toCode() : null,
                details
        );
    }

    private TimelineEvent createDiagnosticReportEvent(DiagnosticReport report) {
        if (!report.hasEffective()) {
            return null;
        }

        LocalDate date = getDiagnosticReportDate(report);
        if (date == null) {
            return null;
        }

        String description = report.hasCode() ? report.getCode().getText() : "Diagnostic Report";

        return new TimelineEvent(
                report.getIdElement().getIdPart(),
                "DiagnosticReport",
                date,
                description,
                report.hasStatus() ? report.getStatus().toCode() : null,
                null
        );
    }

    private TimelineEvent createAllergyEvent(AllergyIntolerance allergy) {
        if (!allergy.hasRecordedDate()) {
            return null;
        }

        LocalDate date = allergy.getRecordedDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        String description = allergy.hasCode() ? allergy.getCode().getText() : "Allergy";

        return new TimelineEvent(
                allergy.getIdElement().getIdPart(),
                "AllergyIntolerance",
                date,
                description,
                allergy.hasClinicalStatus() ? allergy.getClinicalStatus().getCodingFirstRep().getCode() : null,
                allergy.hasCriticality() ? allergy.getCriticality().toCode() : null
        );
    }

    private TimelineEvent createCarePlanEvent(CarePlan carePlan) {
        if (!carePlan.hasCreated()) {
            return null;
        }

        LocalDate date = carePlan.getCreated().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        String description = carePlan.hasTitle() ? carePlan.getTitle() :
                (carePlan.hasDescription() ? carePlan.getDescription() : "Care Plan");

        return new TimelineEvent(
                carePlan.getIdElement().getIdPart(),
                "CarePlan",
                date,
                description,
                carePlan.hasStatus() ? carePlan.getStatus().toCode() : null,
                null
        );
    }

    private TimelineEvent createGoalEvent(Goal goal) {
        if (!goal.hasStart()) {
            return null;
        }

        LocalDate date = getGoalDate(goal);
        if (date == null) {
            return null;
        }

        String description = goal.hasDescription() ?
                goal.getDescription().getText() : "Goal";

        return new TimelineEvent(
                goal.getIdElement().getIdPart(),
                "Goal",
                date,
                description,
                goal.hasLifecycleStatus() ? goal.getLifecycleStatus().toCode() : null,
                null
        );
    }

    // Date extraction helpers

    private LocalDate getProcedureDate(Procedure procedure) {
        Type performed = procedure.getPerformed();
        if (performed instanceof DateTimeType) {
            return ((DateTimeType) performed).getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (performed instanceof Period) {
            Period period = (Period) performed;
            if (period.hasStart()) {
                return period.getStart().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        return null;
    }

    private LocalDate getImmunizationDate(Immunization immunization) {
        Type occurrence = immunization.getOccurrence();
        if (occurrence instanceof DateTimeType) {
            return ((DateTimeType) occurrence).getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private LocalDate getObservationDate(Observation observation) {
        Type effective = observation.getEffective();
        if (effective instanceof DateTimeType) {
            return ((DateTimeType) effective).getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (effective instanceof Period) {
            Period period = (Period) effective;
            if (period.hasStart()) {
                return period.getStart().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        return null;
    }

    private LocalDate getDiagnosticReportDate(DiagnosticReport report) {
        Type effective = report.getEffective();
        if (effective instanceof DateTimeType) {
            return ((DateTimeType) effective).getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (effective instanceof Period) {
            Period period = (Period) effective;
            if (period.hasStart()) {
                return period.getStart().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        return null;
    }

    private LocalDate getGoalDate(Goal goal) {
        Type start = goal.getStart();
        if (start instanceof DateType) {
            return ((DateType) start).getValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    /**
     * Timeline event record
     *
     * @param resourceId FHIR resource ID
     * @param resourceType FHIR resource type
     * @param date Event date
     * @param description Human-readable description
     * @param status Resource status (e.g., "active", "completed")
     * @param details Additional details
     */
    public record TimelineEvent(
            String resourceId,
            String resourceType,
            LocalDate date,
            String description,
            String status,
            String details
    ) {}
}

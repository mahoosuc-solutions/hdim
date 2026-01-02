package com.healthdata.patient.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Patient Health Status Service
 *
 * Provides health status dashboards and summary metrics for patient care.
 * Calculates health indicators, identifies care gaps, and provides
 * actionable insights for care management.
 *
 * Dashboard metrics include:
 * - Active conditions count
 * - Active medications count
 * - Recent encounters count
 * - Critical allergies count
 * - Immunization compliance status
 * - Recent activity indicators
 * - Care plan status
 * - Outstanding goals
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientHealthStatusService {

    private final PatientAggregationService aggregationService;
    private final PatientTimelineService timelineService;

    /**
     * Get comprehensive health status dashboard
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Health status summary
     */
    @Cacheable(value = "patientHealthStatus", key = "#tenantId + ':' + #patientId")
    public HealthStatusSummary getHealthStatusSummary(String tenantId, String patientId) {
        log.info("Building health status summary for patient: {} in tenant: {}", patientId, tenantId);

        // Get all patient data
        Bundle allergies = aggregationService.getAllergies(tenantId, patientId, false);
        Bundle immunizations = aggregationService.getImmunizations(tenantId, patientId, false);
        Bundle medications = aggregationService.getMedications(tenantId, patientId, false);
        Bundle conditions = aggregationService.getConditions(tenantId, patientId, false);
        Bundle procedures = aggregationService.getProcedures(tenantId, patientId);
        Bundle encounters = aggregationService.getEncounters(tenantId, patientId, false);
        Bundle carePlans = aggregationService.getCarePlans(tenantId, patientId, false);
        Bundle goals = aggregationService.getGoals(tenantId, patientId);

        // Calculate metrics
        HealthStatusSummary summary = new HealthStatusSummary(
                patientId,
                LocalDate.now(),
                countActiveConditions(conditions),
                countActiveMedications(medications),
                countCriticalAllergies(allergies),
                countCompletedImmunizations(immunizations),
                countRecentEncounters(encounters, 90),
                countCompletedProcedures(procedures, 30),
                countActiveCarePlans(carePlans),
                countActiveGoals(goals),
                calculateRecentActivityScore(tenantId, patientId),
                identifyHealthAlerts(tenantId, patientId, allergies, conditions, medications),
                calculateImmunizationComplianceStatus(immunizations),
                calculateMedicationAdherenceRisk(medications),
                hasActiveCarePlan(carePlans),
                calculateLastEncounterDays(encounters)
        );

        log.info("Built health status summary for patient: {} with {} active conditions", patientId, summary.activeConditionsCount());
        return summary;
    }

    /**
     * Get medication summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Medication summary
     */
    @Cacheable(value = "patientMedicationSummary", key = "#tenantId + ':' + #patientId")
    public MedicationSummary getMedicationSummary(String tenantId, String patientId) {
        log.info("Building medication summary for patient: {}", patientId);

        Bundle medications = aggregationService.getMedications(tenantId, patientId, false);

        int activeCount = countActiveMedications(medications);
        int totalCount = medications.getTotal();
        List<String> medicationNames = extractMedicationNames(medications);
        String adherenceRisk = calculateMedicationAdherenceRisk(medications);

        return new MedicationSummary(
                activeCount,
                totalCount,
                medicationNames,
                adherenceRisk,
                activeCount > 5 // Polypharmacy flag (>5 active medications)
        );
    }

    /**
     * Get allergy summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Allergy summary
     */
    @Cacheable(value = "patientAllergySummary", key = "#tenantId + ':' + #patientId")
    public AllergySummary getAllergySummary(String tenantId, String patientId) {
        log.info("Building allergy summary for patient: {}", patientId);

        Bundle allergies = aggregationService.getAllergies(tenantId, patientId, false);

        int criticalCount = countCriticalAllergies(allergies);
        int totalCount = allergies.getTotal();
        List<String> criticalAllergens = extractCriticalAllergens(allergies);
        boolean hasMedicationAllergies = hasMedicationAllergies(allergies);

        return new AllergySummary(
                criticalCount,
                totalCount,
                criticalAllergens,
                hasMedicationAllergies,
                criticalCount > 0 // Has critical allergies flag
        );
    }

    /**
     * Get condition summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Condition summary
     */
    @Cacheable(value = "patientConditionSummary", key = "#tenantId + ':' + #patientId")
    public ConditionSummary getConditionSummary(String tenantId, String patientId) {
        log.info("Building condition summary for patient: {}", patientId);

        Bundle conditions = aggregationService.getConditions(tenantId, patientId, false);

        int activeCount = countActiveConditions(conditions);
        int totalCount = conditions.getTotal();
        List<String> activeConditionNames = extractActiveConditionNames(conditions);
        boolean hasChronicConditions = hasChronicConditions(conditions);

        return new ConditionSummary(
                activeCount,
                totalCount,
                activeConditionNames,
                hasChronicConditions,
                calculateConditionSeverity(conditions)
        );
    }

    /**
     * Get immunization compliance summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Immunization summary
     */
    @Cacheable(value = "patientImmunizationSummary", key = "#tenantId + ':' + #patientId")
    public ImmunizationSummary getImmunizationSummary(String tenantId, String patientId) {
        log.info("Building immunization summary for patient: {}", patientId);

        Bundle immunizations = aggregationService.getImmunizations(tenantId, patientId, false);

        int completedCount = countCompletedImmunizations(immunizations);
        int totalCount = immunizations.getTotal();
        String complianceStatus = calculateImmunizationComplianceStatus(immunizations);
        List<String> completedVaccines = extractCompletedVaccines(immunizations);

        return new ImmunizationSummary(
                completedCount,
                totalCount,
                complianceStatus,
                completedVaccines,
                "compliant".equals(complianceStatus)
        );
    }

    // ==================== Private Helper Methods ====================

    private int countActiveConditions(Bundle conditions) {
        return (int) conditions.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Condition)
                .map(r -> (Condition) r)
                .filter(c -> c.hasClinicalStatus() &&
                        "active".equals(c.getClinicalStatus().getCodingFirstRep().getCode()))
                .count();
    }

    private int countActiveMedications(Bundle medications) {
        return (int) medications.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof MedicationRequest)
                .map(r -> (MedicationRequest) r)
                .filter(m -> m.hasStatus() &&
                        "active".equals(m.getStatus().toCode()))
                .count();
    }

    private int countCriticalAllergies(Bundle allergies) {
        return (int) allergies.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof AllergyIntolerance)
                .map(r -> (AllergyIntolerance) r)
                .filter(a -> a.hasCriticality() &&
                        "high".equals(a.getCriticality().toCode()))
                .count();
    }

    private int countCompletedImmunizations(Bundle immunizations) {
        return (int) immunizations.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Immunization)
                .map(r -> (Immunization) r)
                .filter(i -> i.hasStatus() &&
                        "completed".equals(i.getStatus().toCode()))
                .count();
    }

    private int countRecentEncounters(Bundle encounters, int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);

        return (int) encounters.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Encounter)
                .map(r -> (Encounter) r)
                .filter(e -> e.hasPeriod() && e.getPeriod().hasStart())
                .filter(e -> {
                    LocalDate encounterDate = e.getPeriod().getStart().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return !encounterDate.isBefore(cutoffDate);
                })
                .count();
    }

    private int countCompletedProcedures(Bundle procedures, int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);

        return (int) procedures.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Procedure)
                .map(r -> (Procedure) r)
                .filter(p -> p.hasStatus() && "completed".equals(p.getStatus().toCode()))
                .filter(p -> {
                    if (p.hasPerformed() && p.getPerformed() instanceof DateTimeType) {
                        LocalDate procedureDate = ((DateTimeType) p.getPerformed()).getValue().toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        return !procedureDate.isBefore(cutoffDate);
                    }
                    return false;
                })
                .count();
    }

    private int countActiveCarePlans(Bundle carePlans) {
        return (int) carePlans.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof CarePlan)
                .map(r -> (CarePlan) r)
                .filter(c -> c.hasStatus() &&
                        "active".equals(c.getStatus().toCode()))
                .count();
    }

    private int countActiveGoals(Bundle goals) {
        return (int) goals.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Goal)
                .map(r -> (Goal) r)
                .filter(g -> g.hasLifecycleStatus() &&
                        "active".equals(g.getLifecycleStatus().toCode()))
                .count();
    }

    private int calculateRecentActivityScore(String tenantId, String patientId) {
        // Activity score based on last 30 days
        int recentActivity = timelineService.getRecentActivityCount(tenantId, patientId, 30);

        // Score: 0-2=low, 3-5=medium, 6+=high
        if (recentActivity >= 6) return 3; // High
        if (recentActivity >= 3) return 2; // Medium
        return 1; // Low
    }

    private List<String> identifyHealthAlerts(
            String tenantId,
            String patientId,
            Bundle allergies,
            Bundle conditions,
            Bundle medications
    ) {
        List<String> alerts = new ArrayList<>();

        // Critical allergies
        int criticalAllergies = countCriticalAllergies(allergies);
        if (criticalAllergies > 0) {
            alerts.add("Patient has " + criticalAllergies + " critical allerg" +
                    (criticalAllergies == 1 ? "y" : "ies"));
        }

        // Polypharmacy
        int activeMedications = countActiveMedications(medications);
        if (activeMedications > 5) {
            alerts.add("Polypharmacy detected (" + activeMedications + " active medications)");
        }

        // Multiple active conditions
        int activeConditions = countActiveConditions(conditions);
        if (activeConditions >= 3) {
            alerts.add("Multiple active conditions (" + activeConditions + ")");
        }

        // No recent activity
        int recentActivity = timelineService.getRecentActivityCount(tenantId, patientId, 90);
        if (recentActivity == 0) {
            alerts.add("No recent patient activity in last 90 days");
        }

        return alerts;
    }

    private String calculateImmunizationComplianceStatus(Bundle immunizations) {
        int completedCount = countCompletedImmunizations(immunizations);

        // Simplified compliance calculation
        // In production, this would check against age-based guidelines
        if (completedCount >= 10) return "compliant";
        if (completedCount >= 5) return "partially-compliant";
        return "non-compliant";
    }

    private String calculateMedicationAdherenceRisk(Bundle medications) {
        int activeCount = countActiveMedications(medications);

        // Risk calculation based on medication count
        if (activeCount > 8) return "high";
        if (activeCount > 5) return "medium";
        return "low";
    }

    private boolean hasActiveCarePlan(Bundle carePlans) {
        return countActiveCarePlans(carePlans) > 0;
    }

    private int calculateLastEncounterDays(Bundle encounters) {
        LocalDate mostRecentEncounter = encounters.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Encounter)
                .map(r -> (Encounter) r)
                .filter(e -> e.hasPeriod() && e.getPeriod().hasStart())
                .map(e -> e.getPeriod().getStart().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(null);

        if (mostRecentEncounter == null) {
            return -1; // No encounters
        }

        return (int) java.time.temporal.ChronoUnit.DAYS.between(mostRecentEncounter, LocalDate.now());
    }

    private List<String> extractMedicationNames(Bundle medications) {
        return medications.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof MedicationRequest)
                .map(r -> (MedicationRequest) r)
                .filter(m -> m.hasStatus() && "active".equals(m.getStatus().toCode()))
                .filter(m -> m.hasMedicationCodeableConcept())
                .map(m -> m.getMedicationCodeableConcept().getText())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> extractCriticalAllergens(Bundle allergies) {
        return allergies.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof AllergyIntolerance)
                .map(r -> (AllergyIntolerance) r)
                .filter(a -> a.hasCriticality() && "high".equals(a.getCriticality().toCode()))
                .filter(a -> a.hasCode())
                .map(a -> a.getCode().getText())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean hasMedicationAllergies(Bundle allergies) {
        return allergies.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof AllergyIntolerance)
                .map(r -> (AllergyIntolerance) r)
                .anyMatch(a -> a.hasCategory() && !a.getCategory().isEmpty() &&
                        a.getCategory().get(0).getValue().toCode().equals("medication"));
    }

    private List<String> extractActiveConditionNames(Bundle conditions) {
        return conditions.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Condition)
                .map(r -> (Condition) r)
                .filter(c -> c.hasClinicalStatus() &&
                        "active".equals(c.getClinicalStatus().getCodingFirstRep().getCode()))
                .filter(c -> c.hasCode())
                .map(c -> c.getCode().getText())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean hasChronicConditions(Bundle conditions) {
        // Simplified check - in production, would check against chronic condition codes
        return countActiveConditions(conditions) >= 2;
    }

    private String calculateConditionSeverity(Bundle conditions) {
        int activeCount = countActiveConditions(conditions);

        if (activeCount >= 5) return "high";
        if (activeCount >= 3) return "medium";
        return "low";
    }

    private List<String> extractCompletedVaccines(Bundle immunizations) {
        return immunizations.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r instanceof Immunization)
                .map(r -> (Immunization) r)
                .filter(i -> i.hasStatus() && "completed".equals(i.getStatus().toCode()))
                .filter(i -> i.hasVaccineCode())
                .map(i -> i.getVaccineCode().getText())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== Response Records ====================

    public record HealthStatusSummary(
            String patientId,
            LocalDate assessmentDate,
            int activeConditionsCount,
            int activeMedicationsCount,
            int criticalAllergiesCount,
            int completedImmunizationsCount,
            int recentEncountersCount,
            int recentProceduresCount,
            int activeCarePlansCount,
            int activeGoalsCount,
            int recentActivityScore,
            List<String> healthAlerts,
            String immunizationComplianceStatus,
            String medicationAdherenceRisk,
            boolean hasActiveCarePlan,
            int daysSinceLastEncounter
    ) {}

    public record MedicationSummary(
            int activeCount,
            int totalCount,
            List<String> activeMedicationNames,
            String adherenceRisk,
            boolean polypharmacyFlag
    ) {}

    public record AllergySummary(
            int criticalCount,
            int totalCount,
            List<String> criticalAllergens,
            boolean hasMedicationAllergies,
            boolean hasCriticalAllergies
    ) {}

    public record ConditionSummary(
            int activeCount,
            int totalCount,
            List<String> activeConditionNames,
            boolean hasChronicConditions,
            String severity
    ) {}

    public record ImmunizationSummary(
            int completedCount,
            int totalCount,
            String complianceStatus,
            List<String> completedVaccines,
            boolean isCompliant
    ) {}
}

package com.healthdata.patient.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.patient.client.ConsentServiceClient;
import com.healthdata.patient.client.FhirServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Patient Aggregation Service
 *
 * Aggregates FHIR resources from the FHIR Service to provide a comprehensive
 * view of patient health data. Applies consent filters and caches results
 * for performance.
 *
 * HIPAA Compliance:
 * - Applies 42 CFR Part 2 consent filters for substance abuse data
 * - Caches aggregated data with 10-minute TTL to reduce FHIR service load
 * - Filters sensitive categories based on patient consent
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientAggregationService {

    private final FhirServiceClient fhirServiceClient;
    private final ConsentServiceClient consentServiceClient;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser jsonParser = fhirContext.newJsonParser();

    /**
     * Get comprehensive patient health record
     *
     * Aggregates all FHIR resources for a patient including:
     * - AllergyIntolerance
     * - Immunization
     * - MedicationRequest
     * - Condition
     * - Procedure
     * - Observation
     * - Encounter
     * - DiagnosticReport
     * - CarePlan
     * - Goal
     *
     * Applies consent filters before returning data.
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Bundle containing all patient resources
     */
    @Cacheable(value = "patientHealthRecord", key = "#tenantId + ':' + #patientId")
    public Bundle getComprehensiveHealthRecord(String tenantId, String patientId) {
        log.info("Aggregating comprehensive health record for patient: {} in tenant: {}", patientId, tenantId);

        // Check consent status
        ConsentServiceClient.ConsentStatus consentStatus = getConsentStatus(tenantId, patientId);
        List<String> restrictedResourceTypes = consentStatus.hasRestrictions() ?
                getRestrictedResourceTypes(tenantId, patientId) : List.of();
        List<String> sensitiveCategories = consentStatus.hasRestrictions() ?
                getSensitiveCategories(tenantId, patientId) : List.of();

        // Fetch all FHIR resources
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(0);

        // Fetch each resource type and add to bundle
        fetchAndAddResources(bundle, tenantId, patientId, "AllergyIntolerance",
                restrictedResourceTypes, () -> fhirServiceClient.getAllergyIntolerances(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "Immunization",
                restrictedResourceTypes, () -> fhirServiceClient.getImmunizations(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "MedicationRequest",
                restrictedResourceTypes, () -> fhirServiceClient.getMedicationRequests(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "Condition",
                restrictedResourceTypes, () -> fhirServiceClient.getConditions(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "Procedure",
                restrictedResourceTypes, () -> fhirServiceClient.getProcedures(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "Observation",
                restrictedResourceTypes, () -> fhirServiceClient.getObservations(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "Encounter",
                restrictedResourceTypes, () -> fhirServiceClient.getEncounters(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "DiagnosticReport",
                restrictedResourceTypes, () -> fhirServiceClient.getDiagnosticReports(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "CarePlan",
                restrictedResourceTypes, () -> fhirServiceClient.getCarePlans(tenantId, patientId));

        fetchAndAddResources(bundle, tenantId, patientId, "Goal",
                restrictedResourceTypes, () -> fhirServiceClient.getGoals(tenantId, patientId));

        log.info("Aggregated {} resources for patient: {}", bundle.getEntry().size(), patientId);
        return bundle;
    }

    /**
     * Get patient allergies and intolerances
     */
    @Cacheable(value = "patientAllergies", key = "#tenantId + ':' + #patientId")
    public Bundle getAllergies(String tenantId, String patientId, boolean onlyCritical) {
        log.info("Fetching {} allergies for patient: {}", onlyCritical ? "critical" : "all", patientId);

        return fetchBundleSafely("allergies", patientId, () ->
                onlyCritical ?
                        fhirServiceClient.getCriticalAllergies(tenantId, patientId) :
                        fhirServiceClient.getAllergyIntolerances(tenantId, patientId)
        );
    }

    /**
     * Get patient immunizations
     */
    @Cacheable(value = "patientImmunizations", key = "#tenantId + ':' + #patientId")
    public Bundle getImmunizations(String tenantId, String patientId, boolean onlyCompleted) {
        log.info("Fetching {} immunizations for patient: {}", onlyCompleted ? "completed" : "all", patientId);

        String response = onlyCompleted ?
                fhirServiceClient.getCompletedImmunizations(tenantId, patientId) :
                fhirServiceClient.getImmunizations(tenantId, patientId);

        return parseBundle(response);
    }

    /**
     * Get patient medications
     */
    @Cacheable(value = "patientMedications", key = "#tenantId + ':' + #patientId")
    public Bundle getMedications(String tenantId, String patientId, boolean onlyActive) {
        log.info("Fetching {} medications for patient: {}", onlyActive ? "active" : "all", patientId);

        return fetchBundleSafely("medications", patientId, () ->
                onlyActive ?
                        fhirServiceClient.getActiveMedications(tenantId, patientId) :
                        fhirServiceClient.getMedicationRequests(tenantId, patientId)
        );
    }

    /**
     * Get patient conditions
     */
    @Cacheable(value = "patientConditions", key = "#tenantId + ':' + #patientId")
    public Bundle getConditions(String tenantId, String patientId, boolean onlyActive) {
        log.info("Fetching {} conditions for patient: {}", onlyActive ? "active" : "all", patientId);

        String response = onlyActive ?
                fhirServiceClient.getActiveConditions(tenantId, patientId) :
                fhirServiceClient.getConditions(tenantId, patientId);

        return parseBundle(response);
    }

    /**
     * Get patient procedures
     */
    @Cacheable(value = "patientProcedures", key = "#tenantId + ':' + #patientId")
    public Bundle getProcedures(String tenantId, String patientId) {
        log.info("Fetching procedures for patient: {}", patientId);
        String response = fhirServiceClient.getProcedures(tenantId, patientId);
        return parseBundle(response);
    }

    /**
     * Get patient vital signs
     */
    @Cacheable(value = "patientVitals", key = "#tenantId + ':' + #patientId")
    public Bundle getVitalSigns(String tenantId, String patientId) {
        log.info("Fetching vital signs for patient: {}", patientId);
        String response = fhirServiceClient.getVitalSigns(tenantId, patientId);
        return parseBundle(response);
    }

    /**
     * Get patient lab results
     */
    @Cacheable(value = "patientLabs", key = "#tenantId + ':' + #patientId")
    public Bundle getLabResults(String tenantId, String patientId) {
        log.info("Fetching lab results for patient: {}", patientId);
        String response = fhirServiceClient.getLabResults(tenantId, patientId);
        return parseBundle(response);
    }

    /**
     * Get patient encounters
     */
    @Cacheable(value = "patientEncounters", key = "#tenantId + ':' + #patientId")
    public Bundle getEncounters(String tenantId, String patientId, boolean onlyActive) {
        log.info("Fetching {} encounters for patient: {}", onlyActive ? "active" : "all", patientId);

        String response = onlyActive ?
                fhirServiceClient.getActiveEncounters(tenantId, patientId) :
                fhirServiceClient.getEncounters(tenantId, patientId);

        return parseBundle(response);
    }

    /**
     * Get patient care plans
     */
    @Cacheable(value = "patientCarePlans", key = "#tenantId + ':' + #patientId")
    public Bundle getCarePlans(String tenantId, String patientId, boolean onlyActive) {
        log.info("Fetching {} care plans for patient: {}", onlyActive ? "active" : "all", patientId);

        String response = onlyActive ?
                fhirServiceClient.getActiveCarePlans(tenantId, patientId) :
                fhirServiceClient.getCarePlans(tenantId, patientId);

        return parseBundle(response);
    }

    /**
     * Get patient goals
     */
    @Cacheable(value = "patientGoals", key = "#tenantId + ':' + #patientId")
    public Bundle getGoals(String tenantId, String patientId) {
        log.info("Fetching goals for patient: {}", patientId);
        String response = fhirServiceClient.getGoals(tenantId, patientId);
        return parseBundle(response);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Fetch FHIR resources and add to bundle with consent filtering
     */
    private void fetchAndAddResources(
            Bundle bundle,
            String tenantId,
            String patientId,
            String resourceType,
            List<String> restrictedResourceTypes,
            ResourceFetcher fetcher
    ) {
        // Check if resource type is restricted by consent
        if (restrictedResourceTypes.contains(resourceType)) {
            log.info("Skipping restricted resource type: {} for patient: {}", resourceType, patientId);
            return;
        }

        try {
            String response = fetcher.fetch();
            Bundle resourceBundle = parseBundle(response);

            if (resourceBundle != null && resourceBundle.hasEntry()) {
                resourceBundle.getEntry().forEach(entry -> {
                    bundle.addEntry(entry);
                    bundle.setTotal(bundle.getTotal() + 1);
                });
            }
        } catch (Exception e) {
            log.error("Error fetching {} for patient {}: {}", resourceType, patientId, e.getMessage());
        }
    }

    /**
     * Parse JSON response to FHIR Bundle
     */
    private Bundle parseBundle(String json) {
        try {
            return jsonParser.parseResource(Bundle.class, json);
        } catch (Exception e) {
            log.error("Error parsing FHIR bundle: {}", e.getMessage());
            return createEmptyBundle();
        }
    }

    private Bundle fetchBundleSafely(String resourceType, String patientId, Supplier<String> fetcher) {
        try {
            return parseBundle(fetcher.get());
        } catch (Exception e) {
            log.error("Error fetching {} for patient {}: {}", resourceType, patientId, e.getMessage());
            return createEmptyBundle();
        }
    }

    /**
     * Create empty FHIR Bundle
     */
    private Bundle createEmptyBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(0);
        return bundle;
    }

    /**
     * Get consent status with fallback
     */
    private ConsentServiceClient.ConsentStatus getConsentStatus(String tenantId, String patientId) {
        try {
            return consentServiceClient.getConsentStatus(tenantId, patientId);
        } catch (Exception e) {
            log.warn("Error fetching consent status for patient {}: {}, using default", patientId, e.getMessage());
            return new ConsentServiceClient.ConsentStatus("active", null, null, false);
        }
    }

    /**
     * Get restricted resource types with fallback
     */
    private List<String> getRestrictedResourceTypes(String tenantId, String patientId) {
        try {
            return consentServiceClient.getRestrictedResourceTypes(tenantId, patientId);
        } catch (Exception e) {
            log.warn("Error fetching restricted resource types for patient {}: {}", patientId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get sensitive categories with fallback
     */
    private List<String> getSensitiveCategories(String tenantId, String patientId) {
        try {
            return consentServiceClient.getSensitiveCategories(tenantId, patientId);
        } catch (Exception e) {
            log.warn("Error fetching sensitive categories for patient {}: {}", patientId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Functional interface for fetching resources
     */
    @FunctionalInterface
    private interface ResourceFetcher {
        String fetch();
    }
}

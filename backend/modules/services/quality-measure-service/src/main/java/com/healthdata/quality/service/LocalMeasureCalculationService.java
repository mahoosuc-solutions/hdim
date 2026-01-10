package com.healthdata.quality.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.quality.measure.MeasureRegistry;
import com.healthdata.quality.model.MeasureResult;
import com.healthdata.quality.model.PatientData;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Local Measure Calculation Service
 *
 * Uses the local MeasureRegistry (DiabetesCareCalculator, etc.) to calculate
 * quality measures without requiring the CQL Engine service.
 *
 * This provides a fallback when CQL Engine is unavailable or for measures
 * that have local Java implementations.
 */
@Service
@Slf4j
public class LocalMeasureCalculationService {

    private final MeasureRegistry measureRegistry;
    private final RestTemplate restTemplate;
    private final FhirContext fhirContext;
    private final IParser jsonParser;

    @Value("${fhir.service.url:http://fhir-service:8085/fhir}")
    private String fhirServerUrl;

    @Value("${gateway.auth.signing-secret:dev-signing-secret-for-local-testing-only-minimum-32-chars}")
    private String gatewaySigningSecret;

    public LocalMeasureCalculationService(MeasureRegistry measureRegistry, RestTemplate restTemplate) {
        this.measureRegistry = measureRegistry;
        this.restTemplate = restTemplate;
        this.fhirContext = FhirContext.forR4();
        this.jsonParser = fhirContext.newJsonParser();
    }

    /**
     * Calculate a measure locally using MeasureRegistry
     *
     * @param tenantId Tenant ID for FHIR queries
     * @param patientId Patient UUID
     * @param measureId Measure ID (e.g., "CDC" for Comprehensive Diabetes Care)
     * @return MeasureResult with detailed sub-measures, care gaps, and recommendations
     */
    public MeasureResult calculateMeasureLocally(String tenantId, UUID patientId, String measureId) {
        log.info("Calculating measure {} locally for patient {} (tenant: {})", measureId, patientId, tenantId);

        // Check if measure is supported
        if (!measureRegistry.hasMeasure(measureId)) {
            throw new IllegalArgumentException("Measure not supported for local calculation: " + measureId +
                ". Available measures: " + measureRegistry.getMeasureIds());
        }

        // Fetch patient data from FHIR
        PatientData patientData = fetchPatientData(tenantId, patientId);

        // Calculate measure using local registry
        MeasureResult result = measureRegistry.calculateMeasure(measureId, patientData);

        log.info("Local calculation complete for measure {}. Eligible: {}, Care gaps: {}",
            measureId, result.isEligible(), result.getCareGaps().size());

        return result;
    }

    /**
     * Get list of measures available for local calculation
     */
    public List<MeasureRegistry.MeasureMetadata> getAvailableMeasures() {
        return measureRegistry.getMeasuresMetadata();
    }

    /**
     * Fetch all patient data needed for measure calculation
     */
    private PatientData fetchPatientData(String tenantId, UUID patientId) {
        HttpHeaders headers = createHeaders(tenantId);

        PatientData.PatientDataBuilder builder = PatientData.builder();

        // Fetch Patient resource
        Patient patient = fetchPatient(patientId, headers);
        builder.patient(patient);

        // Fetch Conditions
        List<Condition> conditions = fetchConditions(patientId, headers);
        builder.conditions(conditions);
        log.debug("Found {} conditions for patient {}", conditions.size(), patientId);

        // Fetch Observations
        List<Observation> observations = fetchObservations(patientId, headers);
        builder.observations(observations);
        log.debug("Found {} observations for patient {}", observations.size(), patientId);

        // Fetch Procedures
        List<Procedure> procedures = fetchProcedures(patientId, headers);
        builder.procedures(procedures);
        log.debug("Found {} procedures for patient {}", procedures.size(), patientId);

        // Fetch Encounters
        List<Encounter> encounters = fetchEncounters(patientId, headers);
        builder.encounters(encounters);
        log.debug("Found {} encounters for patient {}", encounters.size(), patientId);

        // Fetch MedicationStatements
        List<MedicationStatement> medications = fetchMedicationStatements(patientId, headers);
        builder.medicationStatements(medications);
        log.debug("Found {} medication statements for patient {}", medications.size(), patientId);

        return builder.build();
    }

    private HttpHeaders createHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Tenant-ID", tenantId);
        headers.set("X-Auth-User-Id", "00000000-0000-0000-0000-000000000001");
        headers.set("X-Auth-Username", "local-measure-service");
        headers.set("X-Auth-Tenant-Ids", tenantId);
        headers.set("X-Auth-Roles", "ADMIN,EVALUATOR");
        headers.set("X-Auth-Validated", "gateway-dev-mode");
        return headers;
    }

    private Patient fetchPatient(UUID patientId, HttpHeaders headers) {
        String url = fhirServerUrl + "/Patient/" + patientId;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return jsonParser.parseResource(Patient.class, response.getBody());
            }
        } catch (Exception e) {
            log.error("Error fetching patient {}: {}", patientId, e.getMessage());
        }

        // Return minimal patient object if fetch fails
        Patient patient = new Patient();
        patient.setId(patientId.toString());
        return patient;
    }

    private List<Condition> fetchConditions(UUID patientId, HttpHeaders headers) {
        String url = fhirServerUrl + "/Condition?patient=" + patientId;
        return fetchBundleResources(url, headers, Condition.class);
    }

    private List<Observation> fetchObservations(UUID patientId, HttpHeaders headers) {
        String url = fhirServerUrl + "/Observation?patient=" + patientId;
        return fetchBundleResources(url, headers, Observation.class);
    }

    private List<Procedure> fetchProcedures(UUID patientId, HttpHeaders headers) {
        String url = fhirServerUrl + "/Procedure?patient=" + patientId;
        return fetchBundleResources(url, headers, Procedure.class);
    }

    private List<Encounter> fetchEncounters(UUID patientId, HttpHeaders headers) {
        String url = fhirServerUrl + "/Encounter?patient=" + patientId;
        return fetchBundleResources(url, headers, Encounter.class);
    }

    private List<MedicationStatement> fetchMedicationStatements(UUID patientId, HttpHeaders headers) {
        String url = fhirServerUrl + "/MedicationStatement?patient=" + patientId;
        return fetchBundleResources(url, headers, MedicationStatement.class);
    }

    @SuppressWarnings("unchecked")
    private <T extends Resource> List<T> fetchBundleResources(String url, HttpHeaders headers, Class<T> resourceType) {
        List<T> resources = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Bundle bundle = jsonParser.parseResource(Bundle.class, response.getBody());

                if (bundle.hasEntry()) {
                    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                        if (entry.hasResource() && resourceType.isInstance(entry.getResource())) {
                            resources.add((T) entry.getResource());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching {} resources: {}", resourceType.getSimpleName(), e.getMessage());
        }

        return resources;
    }
}

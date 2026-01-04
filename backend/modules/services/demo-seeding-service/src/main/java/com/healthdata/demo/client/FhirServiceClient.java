package com.healthdata.demo.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client for persisting FHIR resources to the FHIR service.
 *
 * Handles batch persistence of generated demo data including:
 * - Patients
 * - Conditions
 * - Observations
 * - MedicationRequests
 * - Encounters
 * - Procedures
 */
@Component
public class FhirServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(FhirServiceClient.class);

    private final RestTemplate restTemplate;
    private final IParser fhirParser;
    private final String fhirServiceUrl;

    public FhirServiceClient(
            RestTemplate restTemplate,
            FhirContext fhirContext,
            @Value("${demo.services.fhir-service.url:http://fhir-service:8085/fhir}") String fhirServiceUrl) {
        this.restTemplate = restTemplate;
        this.fhirParser = fhirContext.newJsonParser().setPrettyPrint(false);
        this.fhirServiceUrl = fhirServiceUrl;
    }

    /**
     * Persist all resources in a FHIR Bundle to the FHIR service.
     *
     * @param bundle    The FHIR Bundle containing resources to persist
     * @param tenantId  The tenant ID for multi-tenant isolation
     * @return PersistenceResult with statistics
     */
    public PersistenceResult persistBundle(Bundle bundle, String tenantId) {
        logger.info("Persisting {} resources to FHIR service for tenant: {}",
            bundle.getEntry().size(), tenantId);

        PersistenceResult result = new PersistenceResult();
        result.setTenantId(tenantId);

        AtomicInteger patientCount = new AtomicInteger(0);
        AtomicInteger conditionCount = new AtomicInteger(0);
        AtomicInteger observationCount = new AtomicInteger(0);
        AtomicInteger medicationCount = new AtomicInteger(0);
        AtomicInteger encounterCount = new AtomicInteger(0);
        AtomicInteger procedureCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        List<String> errors = new ArrayList<>();

        // Process resources in order: Patients first, then dependent resources
        List<Bundle.BundleEntryComponent> patients = new ArrayList<>();
        List<Bundle.BundleEntryComponent> otherResources = new ArrayList<>();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Patient) {
                patients.add(entry);
            } else {
                otherResources.add(entry);
            }
        }

        // Persist patients first
        for (Bundle.BundleEntryComponent entry : patients) {
            try {
                Patient patient = (Patient) entry.getResource();
                persistResource("/Patient", patient, tenantId);
                patientCount.incrementAndGet();

                if (patientCount.get() % 100 == 0) {
                    logger.info("Persisted {} patients...", patientCount.get());
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
                errors.add("Patient: " + e.getMessage());
                logger.warn("Failed to persist Patient: {}", e.getMessage());
            }
        }

        // Persist other resources
        for (Bundle.BundleEntryComponent entry : otherResources) {
            Resource resource = entry.getResource();
            String resourceType = resource.fhirType();

            try {
                persistResource("/" + resourceType, resource, tenantId);

                switch (resourceType) {
                    case "Condition" -> conditionCount.incrementAndGet();
                    case "Observation" -> observationCount.incrementAndGet();
                    case "MedicationRequest" -> medicationCount.incrementAndGet();
                    case "Encounter" -> encounterCount.incrementAndGet();
                    case "Procedure" -> procedureCount.incrementAndGet();
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
                errors.add(resourceType + ": " + e.getMessage());
                if (errorCount.get() <= 10) {
                    logger.warn("Failed to persist {}: {}", resourceType, e.getMessage());
                }
            }
        }

        result.setPatientCount(patientCount.get());
        result.setConditionCount(conditionCount.get());
        result.setObservationCount(observationCount.get());
        result.setMedicationCount(medicationCount.get());
        result.setEncounterCount(encounterCount.get());
        result.setProcedureCount(procedureCount.get());
        result.setErrorCount(errorCount.get());
        result.setErrors(errors);
        result.setSuccess(errorCount.get() == 0 || patientCount.get() > 0);

        logger.info("Persistence complete: {} patients, {} conditions, {} observations, {} medications, {} encounters, {} procedures ({} errors)",
            patientCount.get(), conditionCount.get(), observationCount.get(),
            medicationCount.get(), encounterCount.get(), procedureCount.get(), errorCount.get());

        return result;
    }

    /**
     * Persist a single FHIR resource.
     */
    private void persistResource(String endpoint, Resource resource, String tenantId) {
        String url = fhirServiceUrl + endpoint;
        String body = fhirParser.encodeResourceToString(resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/fhir+json"));
        headers.set("X-Tenant-Id", tenantId);
        // Add demo mode header to bypass authentication
        headers.set("X-Demo-Mode", "true");

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("HTTP " + response.getStatusCode() + ": " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to POST to " + endpoint + ": " + e.getMessage(), e);
        }
    }

    /**
     * Check if the FHIR service is available.
     */
    public boolean isServiceAvailable() {
        try {
            String url = fhirServiceUrl.replace("/fhir", "") + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("FHIR service not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Result of bundle persistence operation.
     */
    public static class PersistenceResult {
        private String tenantId;
        private int patientCount;
        private int conditionCount;
        private int observationCount;
        private int medicationCount;
        private int encounterCount;
        private int procedureCount;
        private int errorCount;
        private List<String> errors = new ArrayList<>();
        private boolean success;

        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
        public int getConditionCount() { return conditionCount; }
        public void setConditionCount(int conditionCount) { this.conditionCount = conditionCount; }
        public int getObservationCount() { return observationCount; }
        public void setObservationCount(int observationCount) { this.observationCount = observationCount; }
        public int getMedicationCount() { return medicationCount; }
        public void setMedicationCount(int medicationCount) { this.medicationCount = medicationCount; }
        public int getEncounterCount() { return encounterCount; }
        public void setEncounterCount(int encounterCount) { this.encounterCount = encounterCount; }
        public int getProcedureCount() { return procedureCount; }
        public void setProcedureCount(int procedureCount) { this.procedureCount = procedureCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}

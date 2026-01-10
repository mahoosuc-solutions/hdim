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
    private final String fhirInternalUrl;
    private final String fhirExternalUrl;
    private final FhirTarget fhirTarget;

    public FhirServiceClient(
            RestTemplate restTemplate,
            FhirContext fhirContext,
            @Value("${demo.services.fhir.internal-url:http://fhir-service:8085/fhir}") String fhirInternalUrl,
            @Value("${demo.services.fhir.external-url:}") String fhirExternalUrl,
            @Value("${demo.services.fhir.target:internal}") String fhirTarget) {
        this.restTemplate = restTemplate;
        this.fhirParser = fhirContext.newJsonParser().setPrettyPrint(false);
        this.fhirInternalUrl = fhirInternalUrl;
        this.fhirExternalUrl = fhirExternalUrl;
        this.fhirTarget = FhirTarget.fromValue(fhirTarget);
    }

    /**
     * Persist all resources in a FHIR Bundle to the FHIR service.
     *
     * @param bundle    The FHIR Bundle containing resources to persist
     * @param tenantId  The tenant ID for multi-tenant isolation
     * @return PersistenceResult with statistics
     */
    public PersistenceResult persistBundle(Bundle bundle, String tenantId) {
        logger.info("Persisting {} resources to FHIR service for tenant: {} (target: {})",
            bundle.getEntry().size(), tenantId, fhirTarget);

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
        List<String> targets = resolveTargets(resource);
        RuntimeException lastError = null;

        for (String baseUrl : targets) {
            try {
                postResource(baseUrl, endpoint, resource, tenantId);
                return;
            } catch (RuntimeException e) {
                lastError = e;
                logger.warn("Failed to persist to {}{}: {}", baseUrl, endpoint, e.getMessage());
            }
        }

        if (lastError != null) {
            throw lastError;
        }
    }

    private void postResource(String baseUrl, String endpoint, Resource resource, String tenantId) {
        String url = baseUrl + endpoint;
        String body = fhirParser.encodeResourceToString(resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/fhir+json"));
        headers.set("X-Tenant-ID", tenantId);

        // Add gateway-trust headers for authentication (required by downstream services)
        // These simulate what the gateway would inject after JWT validation
        headers.set("X-Auth-User-Id", "demo-seeding-service");
        headers.set("X-Auth-Username", "demo-seeder");
        headers.set("X-Auth-Tenant-Ids", tenantId);
        headers.set("X-Auth-Roles", "ADMIN,SYSTEM");
        // Gateway validation signature - dev mode accepts any signature with "gateway-" prefix
        headers.set("X-Auth-Validated", "gateway-demo-seeding");

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
        boolean internalAvailable = isInternalAvailable();
        boolean externalAvailable = isExternalAvailable();

        return switch (fhirTarget) {
            case INTERNAL -> internalAvailable;
            case EXTERNAL -> externalAvailable;
            case BOTH -> internalAvailable && externalAvailable;
            case HYBRID -> internalAvailable && externalAvailable;
        };
    }

    private boolean isInternalAvailable() {
        return isHealthEndpointAvailable(fhirInternalUrl, true);
    }

    private boolean isExternalAvailable() {
        if (fhirExternalUrl == null || fhirExternalUrl.isBlank()) {
            if (fhirTarget == FhirTarget.EXTERNAL || fhirTarget == FhirTarget.BOTH || fhirTarget == FhirTarget.HYBRID) {
                logger.warn("FHIR external URL not configured for target: {}", fhirTarget);
            }
            return false;
        }
        return isHealthEndpointAvailable(fhirExternalUrl, false);
    }

    private boolean isHealthEndpointAvailable(String baseUrl, boolean isInternal) {
        try {
            String url;
            if (isInternal) {
                // For internal services, actuator is under the context path
                url = baseUrl + "/actuator/health";
            } else {
                url = baseUrl + "/metadata";
            }
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("FHIR service not available at {}: {}", baseUrl, e.getMessage());
            return false;
        }
    }
    
    private List<String> resolveTargets(Resource resource) {
        List<String> targets = new ArrayList<>();
        boolean isPatient = resource instanceof Patient;

        switch (fhirTarget) {
            case INTERNAL -> targets.add(fhirInternalUrl);
            case EXTERNAL -> {
                if (fhirExternalUrl != null && !fhirExternalUrl.isBlank()) {
                    targets.add(fhirExternalUrl);
                } else {
                    targets.add(fhirInternalUrl);
                }
            }
            case BOTH -> {
                targets.add(fhirInternalUrl);
                if (fhirExternalUrl != null && !fhirExternalUrl.isBlank()) {
                    targets.add(fhirExternalUrl);
                }
            }
            case HYBRID -> {
                if (isPatient) {
                    if (fhirExternalUrl != null && !fhirExternalUrl.isBlank()) {
                        targets.add(fhirExternalUrl);
                    } else {
                        targets.add(fhirInternalUrl);
                    }
                } else {
                    targets.add(fhirInternalUrl);
                }
            }
        }
        return targets;
    }

    private enum FhirTarget {
        INTERNAL,
        EXTERNAL,
        BOTH,
        HYBRID;

        static FhirTarget fromValue(String value) {
            if (value == null) {
                return INTERNAL;
            }
            try {
                return FhirTarget.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                logger.warn("Unknown FHIR target '{}', defaulting to INTERNAL", value);
                return INTERNAL;
            }
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

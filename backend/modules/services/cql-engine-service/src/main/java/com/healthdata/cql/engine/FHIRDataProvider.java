package com.healthdata.cql.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.audit.DataFlowTracker;
import com.healthdata.cql.client.FhirServiceClient;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FHIR Data Provider for Measure Evaluation
 *
 * Provides thread-safe, cached access to FHIR resources for measure evaluation.
 * This class handles all FHIR data retrieval and caching for performance.
 */
@Component
public class FHIRDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(FHIRDataProvider.class);
    private final FhirServiceClient fhirClient;
    private final ObjectMapper objectMapper;
    private final DataFlowTracker dataFlowTracker;

    // Thread-safe cache for patient data during evaluation batch
    private final ThreadLocal<Map<String, JsonNode>> patientCache = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public FHIRDataProvider(
            FhirServiceClient fhirClient,
            ObjectMapper objectMapper,
            DataFlowTracker dataFlowTracker) {
        this.fhirClient = fhirClient;
        this.objectMapper = objectMapper;
        this.dataFlowTracker = dataFlowTracker;
    }

    /**
     * Get complete patient context for measure evaluation
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return Patient context with all relevant resources
     */
    public PatientContext getPatientContext(String tenantId, String patientId) {
        logger.debug("Loading patient context for patient={}, tenant={}", patientId, tenantId);

        try {
            PatientContext context = new PatientContext();
            context.setPatientId(patientId);
            context.setTenantId(tenantId);

            // Load patient resource
            context.setPatient(getPatient(tenantId, patientId));

            // Load common resources needed for most measures
            context.setObservations(getObservations(tenantId, patientId, null, null));
            context.setConditions(getConditions(tenantId, patientId, null));
            context.setProcedures(getProcedures(tenantId, patientId, null, null));
            context.setMedicationRequests(getMedicationRequests(tenantId, patientId));
            context.setEncounters(getEncounters(tenantId, patientId, null, null));

            logger.debug("Loaded patient context: {} observations, {} conditions, {} procedures",
                    context.getObservations().size(),
                    context.getConditions().size(),
                    context.getProcedures().size());

            // Track patient context loading
            dataFlowTracker.recordStep(
                "Load Patient FHIR Context",
                "DATA_FETCH",
                List.of("Patient", "Observation", "Condition", "Procedure", "MedicationRequest", "Encounter"),
                String.format("patientId=%s", patientId),
                String.format("Patient context with %d observations, %d conditions, %d procedures, %d medications, %d encounters",
                    context.getObservations().size(),
                    context.getConditions().size(),
                    context.getProcedures().size(),
                    context.getMedicationRequests().size(),
                    context.getEncounters().size()),
                String.format("Loaded complete FHIR context for patient %s", patientId),
                "Retrieved all FHIR resources needed for measure evaluation"
            );

            return context;
        } catch (Exception e) {
            logger.error("Error loading patient context for patient={}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Failed to load patient context", e);
        }
    }

    /**
     * Get patient resource
     */
    public JsonNode getPatient(String tenantId, String patientId) {
        String cacheKey = "patient_" + patientId;
        return getCached(cacheKey, () -> {
            try {
                String patientJson = fhirClient.getPatient(tenantId, patientId);
                return objectMapper.readTree(patientJson);
            } catch (Exception e) {
                logger.error("Error fetching patient {}: {}", patientId, e.getMessage());
                return null;
            }
        });
    }

    /**
     * Get observations for a patient
     */
    public List<JsonNode> getObservations(String tenantId, String patientId, String code, String date) {
        String cacheKey = "observations_" + patientId + "_" + code + "_" + date;
        return getCached(cacheKey, () -> {
            try {
                String obsJson = fhirClient.searchObservations(tenantId, patientId, code, null, date, 1000);
                JsonNode bundle = objectMapper.readTree(obsJson);
                return extractEntries(bundle);
            } catch (Exception e) {
                logger.error("Error fetching observations for patient {}: {}", patientId, e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Get conditions for a patient
     */
    public List<JsonNode> getConditions(String tenantId, String patientId, String code) {
        String cacheKey = "conditions_" + patientId + "_" + code;
        return getCached(cacheKey, () -> {
            try {
                String conditionsJson = fhirClient.searchConditions(tenantId, patientId, code, "active");
                JsonNode bundle = objectMapper.readTree(conditionsJson);
                return extractEntries(bundle);
            } catch (Exception e) {
                logger.error("Error fetching conditions for patient {}: {}", patientId, e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Get procedures for a patient
     */
    public List<JsonNode> getProcedures(String tenantId, String patientId, String code, String date) {
        String cacheKey = "procedures_" + patientId + "_" + code + "_" + date;
        return getCached(cacheKey, () -> {
            try {
                String proceduresJson = fhirClient.searchProcedures(tenantId, patientId, code, date);
                JsonNode bundle = objectMapper.readTree(proceduresJson);
                return extractEntries(bundle);
            } catch (Exception e) {
                logger.error("Error fetching procedures for patient {}: {}", patientId, e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Get medication requests for a patient
     */
    public List<JsonNode> getMedicationRequests(String tenantId, String patientId) {
        String cacheKey = "medications_" + patientId;
        return getCached(cacheKey, () -> {
            try {
                String medsJson = fhirClient.searchMedicationRequests(tenantId, patientId, "active");
                JsonNode bundle = objectMapper.readTree(medsJson);
                return extractEntries(bundle);
            } catch (Exception e) {
                logger.error("Error fetching medications for patient {}: {}", patientId, e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Get encounters for a patient
     */
    public List<JsonNode> getEncounters(String tenantId, String patientId, String code, String date) {
        String cacheKey = "encounters_" + patientId + "_" + code + "_" + date;
        return getCached(cacheKey, () -> {
            try {
                String encountersJson = fhirClient.searchEncounters(tenantId, patientId, code, date);
                JsonNode bundle = objectMapper.readTree(encountersJson);
                return extractEntries(bundle);
            } catch (Exception e) {
                logger.error("Error fetching encounters for patient {}: {}", patientId, e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Extract entries from FHIR Bundle
     */
    private List<JsonNode> extractEntries(JsonNode bundle) {
        List<JsonNode> entries = new ArrayList<>();
        if (bundle != null && bundle.has("entry")) {
            bundle.get("entry").forEach(entry -> {
                if (entry.has("resource")) {
                    entries.add(entry.get("resource"));
                }
            });
        }
        return entries;
    }

    /**
     * Generic caching helper with supplier pattern
     */
    @SuppressWarnings("unchecked")
    private <T> T getCached(String key, java.util.function.Supplier<T> supplier) {
        Map<String, JsonNode> cache = patientCache.get();

        if (!cache.containsKey(key)) {
            T value = supplier.get();
            if (value instanceof JsonNode) {
                cache.put(key, (JsonNode) value);
            }
            return value;
        }

        // If cached value exists, try to cast it
        Object cached = cache.get(key);
        try {
            return (T) cached;
        } catch (ClassCastException e) {
            // If cast fails, re-fetch
            T value = supplier.get();
            if (value instanceof JsonNode) {
                cache.put(key, (JsonNode) value);
            }
            return value;
        }
    }

    /**
     * Clear the thread-local cache
     * Call this after completing a batch of evaluations
     */
    public void clearCache() {
        patientCache.remove();
    }

    /**
     * Patient Context - encapsulates all data needed for measure evaluation
     */
    public static class PatientContext {
        private String patientId;
        private String tenantId;
        private JsonNode patient;
        private List<JsonNode> observations = new ArrayList<>();
        private List<JsonNode> conditions = new ArrayList<>();
        private List<JsonNode> procedures = new ArrayList<>();
        private List<JsonNode> medicationRequests = new ArrayList<>();
        private List<JsonNode> encounters = new ArrayList<>();

        // Getters and Setters
        public String getPatientId() {
            return patientId;
        }

        public void setPatientId(String patientId) {
            this.patientId = patientId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public JsonNode getPatient() {
            return patient;
        }

        public void setPatient(JsonNode patient) {
            this.patient = patient;
        }

        public List<JsonNode> getObservations() {
            return observations;
        }

        public void setObservations(List<JsonNode> observations) {
            this.observations = observations;
        }

        public List<JsonNode> getConditions() {
            return conditions;
        }

        public void setConditions(List<JsonNode> conditions) {
            this.conditions = conditions;
        }

        public List<JsonNode> getProcedures() {
            return procedures;
        }

        public void setProcedures(List<JsonNode> procedures) {
            this.procedures = procedures;
        }

        public List<JsonNode> getMedicationRequests() {
            return medicationRequests;
        }

        public void setMedicationRequests(List<JsonNode> medicationRequests) {
            this.medicationRequests = medicationRequests;
        }

        public List<JsonNode> getEncounters() {
            return encounters;
        }

        public void setEncounters(List<JsonNode> encounters) {
            this.encounters = encounters;
        }
    }
}

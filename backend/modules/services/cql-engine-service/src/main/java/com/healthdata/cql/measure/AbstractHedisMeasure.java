package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.client.FhirServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for HEDIS measures
 *
 * Provides common functionality for FHIR data retrieval,
 * patient age calculation, and result building.
 */
public abstract class AbstractHedisMeasure implements HedisMeasure {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractHedisMeasure.class);

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected FhirServiceClient fhirClient;

    /**
     * Get patient data as JSON
     */
    protected JsonNode getPatientData(String tenantId, String patientId) {
        try {
            String patientJson = fhirClient.getPatient(tenantId, patientId);
            return objectMapper.readTree(patientJson);
        } catch (Exception e) {
            logger.error("Error fetching patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Get observations for a patient
     */
    protected JsonNode getObservations(String tenantId, String patientId, String code, String date) {
        try {
            String obsJson = fhirClient.searchObservations(tenantId, patientId, code, null, date, 1000);
            return objectMapper.readTree(obsJson);
        } catch (Exception e) {
            logger.error("Error fetching observations for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Get conditions for a patient
     */
    protected JsonNode getConditions(String tenantId, String patientId, String code) {
        try {
            String conditionsJson = fhirClient.searchConditions(tenantId, patientId, code, "active");
            return objectMapper.readTree(conditionsJson);
        } catch (Exception e) {
            logger.error("Error fetching conditions for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Get procedures for a patient
     */
    protected JsonNode getProcedures(String tenantId, String patientId, String code, String date) {
        try {
            String proceduresJson = fhirClient.searchProcedures(tenantId, patientId, code, date);
            return objectMapper.readTree(proceduresJson);
        } catch (Exception e) {
            logger.error("Error fetching procedures for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Get medication requests for a patient
     */
    protected JsonNode getMedicationRequests(String tenantId, String patientId) {
        try {
            String medsJson = fhirClient.searchMedicationRequests(tenantId, patientId, "active");
            return objectMapper.readTree(medsJson);
        } catch (Exception e) {
            logger.error("Error fetching medications for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Get medication requests for a patient with code and date filters
     */
    protected JsonNode getMedicationRequests(String tenantId, String patientId, String code, String date) {
        try {
            // Note: Simplified - real implementation would need to support medication code filtering via FHIR API
            String medsJson = fhirClient.searchMedicationRequests(tenantId, patientId, "active");
            return objectMapper.readTree(medsJson);
        } catch (Exception e) {
            logger.error("Error fetching medications for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Get encounters for a patient
     */
    protected JsonNode getEncounters(String tenantId, String patientId, String code, String date) {
        try {
            String encountersJson = fhirClient.searchEncounters(tenantId, patientId, code, date);
            return objectMapper.readTree(encountersJson);
        } catch (Exception e) {
            logger.error("Error fetching encounters for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate patient age from FHIR Patient resource
     */
    protected Integer getPatientAge(JsonNode patient) {
        try {
            if (patient != null && patient.has("birthDate")) {
                String birthDate = patient.get("birthDate").asText();
                LocalDate birth = LocalDate.parse(birthDate);
                return Period.between(birth, LocalDate.now()).getYears();
            }
        } catch (Exception e) {
            logger.warn("Could not calculate patient age: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract entries from FHIR Bundle
     */
    protected List<JsonNode> getEntries(JsonNode bundle) {
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
     * Check if a code matches any in a list of LOINC/SNOMED codes
     */
    protected boolean hasCode(JsonNode resource, List<String> targetCodes) {
        if (resource == null || !resource.has("code")) {
            return false;
        }

        JsonNode code = resource.get("code");
        if (code.has("coding")) {
            for (JsonNode coding : code.get("coding")) {
                String codeValue = coding.has("code") ? coding.get("code").asText() : null;
                if (codeValue != null && targetCodes.contains(codeValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a date is within the measurement period (last 12 months)
     */
    protected boolean isWithinMeasurementPeriod(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr.substring(0, 10)); // Handle datetime
            LocalDate cutoff = LocalDate.now().minusMonths(12);
            return date.isAfter(cutoff);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get effective date from an Observation or Procedure
     */
    protected String getEffectiveDate(JsonNode resource) {
        if (resource.has("effectiveDateTime")) {
            return resource.get("effectiveDateTime").asText();
        } else if (resource.has("performedDateTime")) {
            return resource.get("performedDateTime").asText();
        } else if (resource.has("recordedDate")) {
            return resource.get("recordedDate").asText();
        }
        return null;
    }
}

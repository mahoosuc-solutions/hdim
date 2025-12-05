package com.healthdata.quality.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for fetching and formatting patient names from FHIR Patient resources
 *
 * Features:
 * - Fetches FHIR Patient resource by ID
 * - Extracts name from Patient.name (handles multiple names, prefers "official" use)
 * - Formats as "FirstName LastName"
 * - Handles missing names gracefully (returns "Patient" as fallback)
 * - Caches patient names for performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientNameService {

    private final IGenericClient fhirClient;

    private static final String FALLBACK_NAME = "Patient";

    /**
     * Get formatted patient name from FHIR Patient resource
     *
     * @param patientId FHIR Patient ID
     * @return Formatted patient name as "FirstName LastName", or "Patient" as fallback
     */
    @Cacheable(value = "patientNames", key = "#patientId", unless = "#result == null || #result == 'Patient'")
    public String getPatientName(String patientId) {
        // Handle null or empty patient ID
        if (patientId == null || patientId.trim().isEmpty()) {
            log.debug("Patient ID is null or empty, returning fallback name");
            return FALLBACK_NAME;
        }

        try {
            log.debug("Fetching patient name for patient: {}", patientId);

            // Fetch patient from FHIR server
            Patient patient = fhirClient.read()
                .resource(Patient.class)
                .withId(patientId)
                .execute();

            // Extract and format name
            String formattedName = extractName(patient);

            if (formattedName != null && !formattedName.isEmpty()) {
                log.debug("Successfully fetched name for patient {}: {}", patientId, formattedName);
                return formattedName;
            } else {
                log.debug("No name found for patient {}, returning fallback", patientId);
                return FALLBACK_NAME;
            }

        } catch (Exception e) {
            log.warn("Failed to fetch patient name for patient {}: {}", patientId, e.getMessage());
            return FALLBACK_NAME;
        }
    }

    /**
     * Extract and format patient name from FHIR Patient resource
     * Prefers official name, falls back to usual name, then any available name
     *
     * @param patient FHIR Patient resource
     * @return Formatted name as "FirstName LastName"
     */
    private String extractName(Patient patient) {
        if (patient == null || patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }

        List<HumanName> names = patient.getName();

        // Try to find official name first
        HumanName officialName = names.stream()
            .filter(name -> name.getUse() == HumanName.NameUse.OFFICIAL)
            .findFirst()
            .orElse(null);

        if (officialName != null) {
            return formatName(officialName);
        }

        // Fall back to usual name
        HumanName usualName = names.stream()
            .filter(name -> name.getUse() == HumanName.NameUse.USUAL)
            .findFirst()
            .orElse(null);

        if (usualName != null) {
            return formatName(usualName);
        }

        // Fall back to first available name
        if (!names.isEmpty()) {
            return formatName(names.get(0));
        }

        return null;
    }

    /**
     * Format HumanName as "FirstName LastName"
     * Handles cases where only given name or only family name is present
     *
     * @param name FHIR HumanName
     * @return Formatted name string
     */
    private String formatName(HumanName name) {
        if (name == null) {
            return null;
        }

        StringBuilder formattedName = new StringBuilder();

        // Get first given name
        if (name.getGiven() != null && !name.getGiven().isEmpty()) {
            String givenName = name.getGiven().get(0).getValue();
            if (givenName != null && !givenName.trim().isEmpty()) {
                formattedName.append(givenName.trim());
            }
        }

        // Get family name
        if (name.getFamily() != null && !name.getFamily().trim().isEmpty()) {
            if (formattedName.length() > 0) {
                formattedName.append(" ");
            }
            formattedName.append(name.getFamily().trim());
        }

        String result = formattedName.toString().trim();
        return result.isEmpty() ? null : result;
    }
}

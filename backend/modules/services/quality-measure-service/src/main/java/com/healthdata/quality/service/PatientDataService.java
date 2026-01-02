package com.healthdata.quality.service;

import com.healthdata.quality.model.PatientData;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Service for fetching patient data from FHIR server.
 * Aggregates all FHIR resources needed for quality measure calculation.
 */
@Service
@Slf4j
public class PatientDataService {

    private final IGenericClient fhirClient;

    @Autowired
    public PatientDataService(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    /**
     * Fetch all data needed for quality measure calculation
     *
     * @param patientId Patient ID (UUID)
     * @return PatientData with all resources
     */
    public PatientData fetchPatientData(UUID patientId) {
        log.debug("Fetching patient data for patient: {}", patientId);

        try {
            // Fetch patient demographics
            Patient patient = fhirClient.read()
                .resource(Patient.class)
                .withId(patientId.toString())
                .execute();

            // Fetch all related resources
            List<Condition> conditions = fetchConditions(patientId);
            List<Observation> observations = fetchObservations(patientId);
            List<Procedure> procedures = fetchProcedures(patientId);
            List<Encounter> encounters = fetchEncounters(patientId);
            List<MedicationStatement> medicationStatements = fetchMedicationStatements(patientId);
            List<Immunization> immunizations = fetchImmunizations(patientId);

            log.debug("Fetched patient data: {} conditions, {} observations, {} procedures",
                conditions.size(), observations.size(), procedures.size());

            return PatientData.builder()
                .patient(patient)
                .conditions(conditions)
                .observations(observations)
                .procedures(procedures)
                .encounters(encounters)
                .medicationStatements(medicationStatements)
                .immunizations(immunizations)
                .build();

        } catch (Exception e) {
            log.error("Error fetching patient data for patient {}", patientId, e);
            throw new RuntimeException("Failed to fetch patient data", e);
        }
    }

    private List<Condition> fetchConditions(UUID patientId) {
        Bundle bundle = fhirClient.search()
            .forResource(Condition.class)
            .where(Condition.PATIENT.hasId(patientId.toString()))
            .returnBundle(Bundle.class)
            .execute();

        return bundle.getEntry().stream()
            .map(entry -> (Condition) entry.getResource())
            .collect(Collectors.toList());
    }

    private List<Observation> fetchObservations(UUID patientId) {
        Bundle bundle = fhirClient.search()
            .forResource(Observation.class)
            .where(Observation.PATIENT.hasId(patientId.toString()))
            .returnBundle(Bundle.class)
            .execute();

        return bundle.getEntry().stream()
            .map(entry -> (Observation) entry.getResource())
            .collect(Collectors.toList());
    }

    private List<Procedure> fetchProcedures(UUID patientId) {
        Bundle bundle = fhirClient.search()
            .forResource(Procedure.class)
            .where(Procedure.PATIENT.hasId(patientId.toString()))
            .returnBundle(Bundle.class)
            .execute();

        return bundle.getEntry().stream()
            .map(entry -> (Procedure) entry.getResource())
            .collect(Collectors.toList());
    }

    private List<Encounter> fetchEncounters(UUID patientId) {
        Bundle bundle = fhirClient.search()
            .forResource(Encounter.class)
            .where(Encounter.PATIENT.hasId(patientId.toString()))
            .returnBundle(Bundle.class)
            .execute();

        return bundle.getEntry().stream()
            .map(entry -> (Encounter) entry.getResource())
            .collect(Collectors.toList());
    }

    private List<MedicationStatement> fetchMedicationStatements(UUID patientId) {
        Bundle bundle = fhirClient.search()
            .forResource(MedicationStatement.class)
            .where(MedicationStatement.PATIENT.hasId(patientId.toString()))
            .returnBundle(Bundle.class)
            .execute();

        return bundle.getEntry().stream()
            .map(entry -> (MedicationStatement) entry.getResource())
            .collect(Collectors.toList());
    }

    private List<Immunization> fetchImmunizations(UUID patientId) {
        Bundle bundle = fhirClient.search()
            .forResource(Immunization.class)
            .where(Immunization.PATIENT.hasId(patientId.toString()))
            .returnBundle(Bundle.class)
            .execute();

        return bundle.getEntry().stream()
            .map(entry -> (Immunization) entry.getResource())
            .collect(Collectors.toList());
    }

    /**
     * Fetch patient observations for health score calculation
     * Supports tenant isolation if needed
     */
    public List<Observation> fetchPatientObservations(String tenantId, UUID patientId) {
        log.debug("Fetching observations for patient: {} (tenant: {})", patientId, tenantId);
        return fetchObservations(patientId);
    }

    /**
     * Fetch patient conditions for health score calculation
     * Supports tenant isolation if needed
     */
    public List<Condition> fetchPatientConditions(String tenantId, UUID patientId) {
        log.debug("Fetching conditions for patient: {} (tenant: {})", patientId, tenantId);
        return fetchConditions(patientId);
    }

    /**
     * Fetch patient procedures for health score calculation
     * Supports tenant isolation if needed
     */
    public List<Procedure> fetchPatientProcedures(String tenantId, UUID patientId) {
        log.debug("Fetching procedures for patient: {} (tenant: {})", patientId, tenantId);
        return fetchProcedures(patientId);
    }

    /**
     * Fetch patient demographics
     * Supports tenant isolation if needed
     */
    public Patient fetchPatient(String tenantId, UUID patientId) {
        log.debug("Fetching patient demographics: {} (tenant: {})", patientId, tenantId);
        try {
            return fhirClient.read()
                .resource(Patient.class)
                .withId(patientId.toString())
                .execute();
        } catch (Exception e) {
            log.error("Error fetching patient {}", patientId, e);
            throw new RuntimeException("Failed to fetch patient", e);
        }
    }

    /**
     * Fetch social history observations for SDOH score calculation
     *
     * Queries FHIR Observation resources with category = "social-history"
     * to identify Social Determinants of Health (SDOH) screenings and findings.
     *
     * Common SDOH observation codes include:
     * - Housing stability (LOINC 71802-3)
     * - Food insecurity (LOINC 88122-7)
     * - Transportation access (LOINC 93030-5)
     * - Employment status (LOINC 67875-5)
     * - Education level (LOINC 82589-3)
     * - Social isolation (LOINC 93159-2)
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return List of social history observations
     */
    public List<Observation> fetchSocialHistoryObservations(String tenantId, UUID patientId) {
        log.debug("Fetching social history observations for patient: {} (tenant: {})", patientId, tenantId);
        try {
            Bundle bundle = fhirClient.search()
                .forResource(Observation.class)
                .where(Observation.PATIENT.hasId(patientId.toString()))
                .where(Observation.CATEGORY.exactly().code("social-history"))
                .returnBundle(Bundle.class)
                .execute();

            List<Observation> observations = bundle.getEntry().stream()
                .map(entry -> (Observation) entry.getResource())
                .collect(Collectors.toList());

            log.debug("Found {} social history observations for patient {}", observations.size(), patientId);
            return observations;
        } catch (Exception e) {
            log.warn("Error fetching social history observations for patient {}: {}", patientId, e.getMessage());
            return List.of();
        }
    }
}

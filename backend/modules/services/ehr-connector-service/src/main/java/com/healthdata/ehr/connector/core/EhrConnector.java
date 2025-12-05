package com.healthdata.ehr.connector.core;

import org.hl7.fhir.r4.model.*;

import java.util.List;
import java.util.Optional;

/**
 * Core interface for EHR system connectors.
 * Provides standard FHIR R4 operations for retrieving patient data from external EHR systems.
 */
public interface EhrConnector {

    /**
     * Search for patients by Medical Record Number (MRN).
     *
     * @param mrn the Medical Record Number
     * @return list of matching patients
     */
    List<Patient> searchPatientByMrn(String mrn);

    /**
     * Search for patients by name and date of birth.
     *
     * @param familyName patient's family name
     * @param givenName patient's given name
     * @param birthDate patient's birth date (YYYY-MM-DD format)
     * @return list of matching patients
     */
    List<Patient> searchPatientByNameAndDob(String familyName, String givenName, String birthDate);

    /**
     * Retrieve a specific patient by ID.
     *
     * @param patientId the FHIR patient ID
     * @return the patient resource if found
     */
    Optional<Patient> getPatient(String patientId);

    /**
     * Retrieve encounters for a patient.
     *
     * @param patientId the FHIR patient ID
     * @return list of encounters
     */
    List<Encounter> getEncounters(String patientId);

    /**
     * Retrieve a specific encounter by ID.
     *
     * @param encounterId the FHIR encounter ID
     * @return the encounter resource if found
     */
    Optional<Encounter> getEncounter(String encounterId);

    /**
     * Retrieve observations/lab results for a patient.
     *
     * @param patientId the FHIR patient ID
     * @param category optional category filter (e.g., "laboratory", "vital-signs")
     * @return list of observations
     */
    List<Observation> getObservations(String patientId, String category);

    /**
     * Retrieve conditions for a patient.
     *
     * @param patientId the FHIR patient ID
     * @return list of conditions
     */
    List<Condition> getConditions(String patientId);

    /**
     * Retrieve medication requests for a patient.
     *
     * @param patientId the FHIR patient ID
     * @return list of medication requests
     */
    List<MedicationRequest> getMedicationRequests(String patientId);

    /**
     * Retrieve allergies for a patient.
     *
     * @param patientId the FHIR patient ID
     * @return list of allergy intolerances
     */
    List<AllergyIntolerance> getAllergies(String patientId);

    /**
     * Test the connection to the EHR system.
     *
     * @return true if connection is successful
     */
    boolean testConnection();

    /**
     * Get the name of the EHR system.
     *
     * @return EHR system name (e.g., "Epic", "Cerner")
     */
    String getSystemName();
}

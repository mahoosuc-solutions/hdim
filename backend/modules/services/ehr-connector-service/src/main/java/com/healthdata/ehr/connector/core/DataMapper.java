package com.healthdata.ehr.connector.core;

import org.hl7.fhir.r4.model.*;

/**
 * Interface for mapping EHR-specific FHIR extensions to normalized models.
 *
 * @param <T> The type of normalized model
 */
public interface DataMapper<T> {

    /**
     * Map a FHIR Patient resource to normalized model.
     *
     * @param patient FHIR Patient resource
     * @return normalized patient model
     */
    T mapPatient(Patient patient);

    /**
     * Map a FHIR Encounter resource to normalized model.
     *
     * @param encounter FHIR Encounter resource
     * @return normalized encounter model
     */
    T mapEncounter(Encounter encounter);

    /**
     * Map a FHIR Observation resource to normalized model.
     *
     * @param observation FHIR Observation resource
     * @return normalized observation model
     */
    T mapObservation(Observation observation);

    /**
     * Map a FHIR Condition resource to normalized model.
     *
     * @param condition FHIR Condition resource
     * @return normalized condition model
     */
    T mapCondition(Condition condition);

    /**
     * Extract EHR-specific extensions from a FHIR resource.
     *
     * @param resource FHIR resource with extensions
     * @return extracted extension data
     */
    T extractExtensions(Resource resource);
}

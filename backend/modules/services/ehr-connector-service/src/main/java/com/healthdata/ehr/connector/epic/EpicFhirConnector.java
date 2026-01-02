package com.healthdata.ehr.connector.epic;

import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.healthdata.ehr.connector.core.EhrConnector;
import com.healthdata.ehr.connector.core.EhrConnectionException;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Epic FHIR R4 API Connector.
 * Provides integration with Epic EHR systems using FHIR R4 standard.
 */
@Component
public class EpicFhirConnector implements EhrConnector {

    private static final Logger logger = LoggerFactory.getLogger(EpicFhirConnector.class);
    private static final String SYSTEM_NAME = "Epic";

    private final EpicConnectionConfig config;
    private final EpicAuthProvider authProvider;
    private final IGenericClient fhirClient;

    public EpicFhirConnector(
            EpicConnectionConfig config,
            EpicAuthProvider authProvider,
            IGenericClient fhirClient) {
        this.config = config;
        this.authProvider = authProvider;
        this.fhirClient = fhirClient;
    }

    @Override
    public List<Patient> searchPatientByMrn(String mrn) {
        validateParameter(mrn, "MRN");

        logger.info("Searching for patient with MRN: {}", mrn);

        return executeWithRetry(() -> {
            Bundle bundle = fhirClient.search()
                    .forResource(Patient.class)
                    .where(new TokenClientParam("identifier").exactly().systemAndCode(null, mrn))
                    .returnBundle(Bundle.class)
                    .execute();

            return extractResourcesFromBundle(bundle, Patient.class);
        });
    }

    @Override
    public List<Patient> searchPatientByNameAndDob(String familyName, String givenName, String birthDate) {
        validateParameter(familyName, "Family name");
        validateParameter(givenName, "Given name");
        validateParameter(birthDate, "Birth date");

        logger.info("Searching for patient: {} {}, DOB: {}", givenName, familyName, birthDate);

        return executeWithRetry(() -> {
            Bundle bundle = fhirClient.search()
                    .forResource(Patient.class)
                    .where(Patient.FAMILY.matches().value(familyName))
                    .and(Patient.GIVEN.matches().value(givenName))
                    .and(Patient.BIRTHDATE.exactly().day(birthDate))
                    .returnBundle(Bundle.class)
                    .execute();

            return extractResourcesFromBundle(bundle, Patient.class);
        });
    }

    @Override
    public Optional<Patient> getPatient(String patientId) {
        validateParameter(patientId, "Patient ID");

        logger.info("Retrieving patient: {}", patientId);

        try {
            Patient patient = fhirClient.read()
                    .resource(Patient.class)
                    .withId(patientId)
                    .execute();

            return Optional.of(patient);
        } catch (ResourceNotFoundException e) {
            logger.warn("Patient not found: {}", patientId);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error retrieving patient: {}", patientId, e);
            throw new EhrConnectionException(
                    "Failed to retrieve patient: " + e.getMessage(),
                    SYSTEM_NAME,
                    e
            );
        }
    }

    @Override
    public List<Encounter> getEncounters(String patientId) {
        validateParameter(patientId, "Patient ID");

        logger.info("Retrieving encounters for patient: {}", patientId);

        return executeWithRetry(() -> {
            Bundle bundle = fhirClient.search()
                    .forResource(Encounter.class)
                    .where(new ReferenceClientParam("patient").hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();

            return extractResourcesFromBundle(bundle, Encounter.class);
        });
    }

    @Override
    public Optional<Encounter> getEncounter(String encounterId) {
        validateParameter(encounterId, "Encounter ID");

        logger.info("Retrieving encounter: {}", encounterId);

        try {
            Encounter encounter = fhirClient.read()
                    .resource(Encounter.class)
                    .withId(encounterId)
                    .execute();

            return Optional.of(encounter);
        } catch (ResourceNotFoundException e) {
            logger.warn("Encounter not found: {}", encounterId);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error retrieving encounter: {}", encounterId, e);
            throw new EhrConnectionException(
                    "Failed to retrieve encounter: " + e.getMessage(),
                    SYSTEM_NAME,
                    e
            );
        }
    }

    @Override
    public List<Observation> getObservations(String patientId, String category) {
        validateParameter(patientId, "Patient ID");

        logger.info("Retrieving observations for patient: {}, category: {}", patientId, category);

        return executeWithRetry(() -> {
            var query = fhirClient.search()
                    .forResource(Observation.class)
                    .where(new ReferenceClientParam("patient").hasId(patientId));

            if (category != null && !category.isEmpty()) {
                query = query.and(new TokenClientParam("category").exactly().code(category));
            }

            Bundle bundle = query.returnBundle(Bundle.class).execute();

            return extractResourcesFromBundle(bundle, Observation.class);
        });
    }

    @Override
    public List<Condition> getConditions(String patientId) {
        validateParameter(patientId, "Patient ID");

        logger.info("Retrieving conditions for patient: {}", patientId);

        return executeWithRetry(() -> {
            Bundle bundle = fhirClient.search()
                    .forResource(Condition.class)
                    .where(new ReferenceClientParam("patient").hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();

            return extractResourcesFromBundle(bundle, Condition.class);
        });
    }

    @Override
    public List<MedicationRequest> getMedicationRequests(String patientId) {
        validateParameter(patientId, "Patient ID");

        logger.info("Retrieving medication requests for patient: {}", patientId);

        return executeWithRetry(() -> {
            Bundle bundle = fhirClient.search()
                    .forResource(MedicationRequest.class)
                    .where(new ReferenceClientParam("patient").hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();

            return extractResourcesFromBundle(bundle, MedicationRequest.class);
        });
    }

    @Override
    public List<AllergyIntolerance> getAllergies(String patientId) {
        validateParameter(patientId, "Patient ID");

        logger.info("Retrieving allergies for patient: {}", patientId);

        return executeWithRetry(() -> {
            Bundle bundle = fhirClient.search()
                    .forResource(AllergyIntolerance.class)
                    .where(new ReferenceClientParam("patient").hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();

            return extractResourcesFromBundle(bundle, AllergyIntolerance.class);
        });
    }

    @Override
    public boolean testConnection() {
        logger.info("Testing connection to Epic FHIR server");

        try {
            CapabilityStatement capabilityStatement = fhirClient.capabilities()
                    .ofType(CapabilityStatement.class)
                    .execute();

            logger.info("Successfully connected to Epic FHIR server");
            return capabilityStatement != null;
        } catch (Exception e) {
            logger.error("Failed to connect to Epic FHIR server", e);
            return false;
        }
    }

    @Override
    public String getSystemName() {
        return SYSTEM_NAME;
    }

    /**
     * Extract resources from a FHIR Bundle.
     *
     * @param bundle the FHIR bundle
     * @param resourceClass the resource class
     * @param <T> resource type
     * @return list of resources
     */
    private <T extends Resource> List<T> extractResourcesFromBundle(Bundle bundle, Class<T> resourceClass) {
        List<T> resources = new ArrayList<>();

        if (bundle == null || !bundle.hasEntry()) {
            return resources;
        }

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && resourceClass.isInstance(entry.getResource())) {
                resources.add(resourceClass.cast(entry.getResource()));
            }
        }

        logger.debug("Extracted {} resources of type {} from bundle", resources.size(), resourceClass.getSimpleName());

        return resources;
    }

    /**
     * Execute a FHIR operation with retry logic.
     *
     * @param operation the operation to execute
     * @param <T> return type
     * @return operation result
     */
    private <T> T executeWithRetry(SupplierWithException<T> operation) {
        int maxRetries = config.getMaxRetries();
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < maxRetries) {
                    logger.warn("Operation failed (attempt {}/{}), retrying...", attempt, maxRetries, e);
                    try {
                        Thread.sleep(1000L * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EhrConnectionException(
                                "Interrupted while waiting to retry",
                                SYSTEM_NAME,
                                ie
                        );
                    }
                } else {
                    logger.error("Operation failed after {} attempts", maxRetries, e);
                }
            }
        }

        throw new EhrConnectionException(
                "Failed after " + maxRetries + " attempts: " + lastException.getMessage(),
                SYSTEM_NAME,
                lastException
        );
    }

    /**
     * Validate a parameter is not null or empty.
     *
     * @param parameter the parameter value
     * @param parameterName the parameter name
     */
    private void validateParameter(String parameter, String parameterName) {
        if (parameter == null || parameter.trim().isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be null or empty");
        }
    }

    /**
     * Functional interface for operations that can throw exceptions.
     */
    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }
}

package com.healthdata.ehr.connector.cerner;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import com.healthdata.ehr.connector.cerner.config.CernerConnectionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CernerFhirConnector {

    private final IGenericClient fhirClient;
    private final CernerAuthProvider authProvider;
    private final CernerDataMapper dataMapper;
    private final CernerConnectionConfig config;

    public Patient getPatientById(String patientId) {
        try {
            log.info("Fetching patient by ID: {}", patientId);
            authProvider.getAuthorizationHeader();
            
            Patient patient = fhirClient.read()
                    .resource(Patient.class)
                    .withId(patientId)
                    .execute();
            
            return dataMapper.mapPatient(patient);
        } catch (Exception e) {
            log.error("Error fetching patient by ID: {}", patientId, e);
            throw new RuntimeException("Failed to fetch patient", e);
        }
    }

    public List<Patient> searchPatientsByIdentifier(String identifier) {
        try {
            log.info("Searching patients by identifier: {}", identifier);
            authProvider.getAuthorizationHeader();
            
            Bundle bundle = fhirClient.search()
                    .forResource(Patient.class)
                    .where(Patient.IDENTIFIER.exactly().code(identifier))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractResourcesFromBundle(bundle, Patient.class).stream()
                    .map(dataMapper::mapPatient)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching patients by identifier: {}", identifier, e);
            throw new RuntimeException("Failed to search patients", e);
        }
    }

    public List<Patient> searchPatientsByName(String familyName, String givenName) {
        try {
            log.info("Searching patients by name: {} {}", givenName, familyName);
            authProvider.getAuthorizationHeader();
            
            var query = fhirClient.search()
                    .forResource(Patient.class)
                    .where(Patient.FAMILY.matches().value(familyName));
            
            if (givenName != null) {
                query.where(Patient.GIVEN.matches().value(givenName));
            }

            Bundle bundle = query.returnBundle(Bundle.class).execute();
            
            return extractResourcesFromBundle(bundle, Patient.class).stream()
                    .map(dataMapper::mapPatient)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching patients by name", e);
            throw new RuntimeException("Failed to search patients by name", e);
        }
    }

    public Encounter getEncounterById(String encounterId) {
        try {
            log.info("Fetching encounter by ID: {}", encounterId);
            authProvider.getAuthorizationHeader();
            
            Encounter encounter = fhirClient.read()
                    .resource(Encounter.class)
                    .withId(encounterId)
                    .execute();
            
            return dataMapper.mapEncounter(encounter);
        } catch (Exception e) {
            log.error("Error fetching encounter by ID: {}", encounterId, e);
            throw new RuntimeException("Failed to fetch encounter", e);
        }
    }

    public List<Encounter> searchEncountersByPatient(String patientId) {
        try {
            log.info("Searching encounters for patient: {}", patientId);
            authProvider.getAuthorizationHeader();
            
            Bundle bundle = fhirClient.search()
                    .forResource(Encounter.class)
                    .where(Encounter.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractResourcesFromBundle(bundle, Encounter.class).stream()
                    .map(dataMapper::mapEncounter)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching encounters for patient: {}", patientId, e);
            throw new RuntimeException("Failed to search encounters", e);
        }
    }

    public List<Observation> searchObservationsByPatient(String patientId) {
        try {
            log.info("Searching observations for patient: {}", patientId);
            authProvider.getAuthorizationHeader();
            
            Bundle bundle = fhirClient.search()
                    .forResource(Observation.class)
                    .where(Observation.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractResourcesFromBundle(bundle, Observation.class).stream()
                    .map(dataMapper::mapObservation)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching observations for patient: {}", patientId, e);
            throw new RuntimeException("Failed to search observations", e);
        }
    }

    public List<Observation> searchObservationsByPatientAndCategory(String patientId, String category) {
        try {
            log.info("Searching observations for patient: {} with category: {}", patientId, category);
            authProvider.getAuthorizationHeader();
            
            Bundle bundle = fhirClient.search()
                    .forResource(Observation.class)
                    .where(Observation.PATIENT.hasId(patientId))
                    .where(Observation.CATEGORY.exactly().code(category))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractResourcesFromBundle(bundle, Observation.class).stream()
                    .map(dataMapper::mapObservation)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching observations by category", e);
            throw new RuntimeException("Failed to search observations by category", e);
        }
    }

    public List<Condition> searchConditionsByPatient(String patientId) {
        try {
            log.info("Searching conditions for patient: {}", patientId);
            authProvider.getAuthorizationHeader();
            
            Bundle bundle = fhirClient.search()
                    .forResource(Condition.class)
                    .where(Condition.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractResourcesFromBundle(bundle, Condition.class).stream()
                    .map(dataMapper::mapCondition)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching conditions for patient: {}", patientId, e);
            throw new RuntimeException("Failed to search conditions", e);
        }
    }

    public List<MedicationRequest> searchMedicationRequestsByPatient(String patientId) {
        try {
            log.info("Searching medication requests for patient: {}", patientId);
            authProvider.getAuthorizationHeader();
            
            Bundle bundle = fhirClient.search()
                    .forResource(MedicationRequest.class)
                    .where(MedicationRequest.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractResourcesFromBundle(bundle, MedicationRequest.class).stream()
                    .map(dataMapper::mapMedicationRequest)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching medication requests for patient: {}", patientId, e);
            throw new RuntimeException("Failed to search medication requests", e);
        }
    }

    public List<Immunization> searchImmunizationsByPatient(String patientId) {
        try {
            log.info("Searching immunizations for patient: {}", patientId);
            authProvider.getAuthorizationHeader();
            
            Bundle bundle = fhirClient.search()
                    .forResource(Immunization.class)
                    .where(Immunization.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractResourcesFromBundle(bundle, Immunization.class).stream()
                    .map(dataMapper::mapImmunization)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching immunizations for patient: {}", patientId, e);
            throw new RuntimeException("Failed to search immunizations", e);
        }
    }

    public List<DiagnosticReport> searchDiagnosticReportsByPatient(String patientId) {
        try {
            log.info("Searching diagnostic reports for patient: {}", patientId);
            authProvider.getAuthorizationHeader();
            
            Bundle bundle = fhirClient.search()
                    .forResource(DiagnosticReport.class)
                    .where(DiagnosticReport.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractResourcesFromBundle(bundle, DiagnosticReport.class).stream()
                    .map(dataMapper::mapDiagnosticReport)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching diagnostic reports for patient: {}", patientId, e);
            throw new RuntimeException("Failed to search diagnostic reports", e);
        }
    }

    public Bundle executeBatchRequest(Bundle requestBundle) {
        try {
            log.info("Executing batch request");
            authProvider.getAuthorizationHeader();
            
            return fhirClient.transaction()
                    .withBundle(requestBundle)
                    .execute();
        } catch (Exception e) {
            log.error("Error executing batch request", e);
            throw new RuntimeException("Failed to execute batch request", e);
        }
    }

    public Bundle executeTransactionRequest(Bundle requestBundle) {
        try {
            log.info("Executing transaction request");
            authProvider.getAuthorizationHeader();
            
            return fhirClient.transaction()
                    .withBundle(requestBundle)
                    .execute();
        } catch (Exception e) {
            log.error("Error executing transaction request", e);
            throw new RuntimeException("Failed to execute transaction request", e);
        }
    }

    public boolean testConnection() {
        try {
            log.info("Testing Cerner FHIR connection");
            authProvider.getAuthorizationHeader();
            
            fhirClient.capabilities()
                    .ofType(CapabilityStatement.class)
                    .execute();
            
            log.info("Cerner FHIR connection successful");
            return true;
        } catch (Exception e) {
            log.error("Cerner FHIR connection test failed", e);
            return false;
        }
    }

    public String getTenantId() {
        return config.getTenantId();
    }

    public boolean isSandboxMode() {
        return config.isSandboxMode();
    }

    public String getBaseUrl() {
        return config.getBaseUrl();
    }

    private <T extends Resource> List<T> extractResourcesFromBundle(Bundle bundle, Class<T> resourceType) {
        List<T> resources = new ArrayList<>();
        
        if (bundle != null && bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && resourceType.isInstance(entry.getResource())) {
                    resources.add(resourceType.cast(entry.getResource()));
                }
            }
        }
        
        return resources;
    }
}

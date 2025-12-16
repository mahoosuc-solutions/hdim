package com.healthdata.ehr.connector.epic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.healthdata.ehr.connector.core.EhrConnectionException;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test suite for EpicFhirConnector using WireMock.
 * Tests FHIR R4 API interactions with mocked Epic FHIR server responses.
 */
@DisplayName("Epic FHIR Connector Integration Tests")
class EpicFhirConnectorTest {

    private WireMockServer wireMockServer;
    private EpicFhirConnector connector;
    private FhirContext fhirContext;
    private IGenericClient fhirClient;
    private EpicConnectionConfig config;
    private EpicAuthProvider authProvider;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        configureFor("localhost", 8089);

        fhirContext = FhirContext.forR4();
        String baseUrl = "http://localhost:8089/fhir";

        // Create a real FHIR client pointing to WireMock
        // Disable server validation to avoid automatic metadata calls
        fhirContext.getRestfulClientFactory().setServerValidationMode(ca.uhn.fhir.rest.client.api.ServerValidationModeEnum.NEVER);
        fhirClient = fhirContext.newRestfulGenericClient(baseUrl);

        // Create mock config with minimal retries for faster tests
        config = new EpicConnectionConfig();
        config.setBaseUrl(baseUrl);
        config.setMaxRetries(1);

        // Create mock auth provider (not used in these tests since WireMock handles auth)
        authProvider = new EpicAuthProvider(config, null);

        connector = new EpicFhirConnector(config, authProvider, fhirClient);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Should fetch patient by ID successfully")
    void shouldFetchPatientById() {
        // Arrange
        String patientId = "epic-patient-123";
        stubFor(get(urlPathEqualTo("/fhir/Patient/" + patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getPatientJson(patientId, "Smith", "John"))));

        // Act
        Optional<Patient> result = connector.getPatient(patientId);

        // Assert
        assertTrue(result.isPresent());
        Patient patient = result.get();
        assertEquals(patientId, patient.getIdElement().getIdPart());
        assertEquals("Smith", patient.getNameFirstRep().getFamily());
        assertEquals("John", patient.getNameFirstRep().getGivenAsSingleString());

        verify(getRequestedFor(urlPathEqualTo("/fhir/Patient/" + patientId)));
    }

    @Test
    @DisplayName("Should return empty when patient not found")
    void shouldReturnEmptyWhenPatientNotFound() {
        // Arrange
        String patientId = "non-existent";
        stubFor(get(urlPathEqualTo("/fhir/Patient/" + patientId))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getOperationOutcomeJson("error", "Patient not found"))));

        // Act
        Optional<Patient> result = connector.getPatient(patientId);

        // Assert
        assertFalse(result.isPresent());
        verify(getRequestedFor(urlPathEqualTo("/fhir/Patient/" + patientId)));
    }

    @Test
    @DisplayName("Should search patients by MRN")
    void shouldSearchPatientsByMrn() {
        // Arrange
        String mrn = "E12345";
        stubFor(get(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("identifier", matching(".*E12345.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getPatientBundleJson("epic-patient-123", "Smith", "John"))));

        // Act
        List<Patient> results = connector.searchPatientByMrn(mrn);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        Patient patient = results.get(0);
        assertEquals("Smith", patient.getNameFirstRep().getFamily());

        verify(getRequestedFor(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("identifier", matching(".*E12345.*")));
    }

    @Test
    @DisplayName("Should search patients by name and DOB")
    void shouldSearchPatientsByNameAndDob() {
        // Arrange
        String familyName = "Smith";
        String givenName = "John";
        String birthDate = "1980-01-01";

        stubFor(get(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("family", equalTo(familyName))
            .withQueryParam("given", equalTo(givenName))
            .withQueryParam("birthdate", equalTo(birthDate))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getPatientBundleJson("epic-patient-456", familyName, givenName))));

        // Act
        List<Patient> results = connector.searchPatientByNameAndDob(familyName, givenName, birthDate);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        Patient patient = results.get(0);
        assertEquals(familyName, patient.getNameFirstRep().getFamily());
        assertEquals(givenName, patient.getNameFirstRep().getGivenAsSingleString());

        verify(getRequestedFor(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("family", equalTo(familyName))
            .withQueryParam("given", equalTo(givenName))
            .withQueryParam("birthdate", equalTo(birthDate)));
    }

    @Test
    @DisplayName("Should fetch encounter by ID")
    void shouldFetchEncounterById() {
        // Arrange
        String encounterId = "epic-encounter-789";
        stubFor(get(urlPathEqualTo("/fhir/Encounter/" + encounterId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getEncounterJson(encounterId, "finished", "IMP"))));

        // Act
        Optional<Encounter> result = connector.getEncounter(encounterId);

        // Assert
        assertTrue(result.isPresent());
        Encounter encounter = result.get();
        assertEquals(encounterId, encounter.getIdElement().getIdPart());
        assertEquals(Encounter.EncounterStatus.FINISHED, encounter.getStatus());

        verify(getRequestedFor(urlPathEqualTo("/fhir/Encounter/" + encounterId)));
    }

    @Test
    @DisplayName("Should search encounters by patient ID")
    void shouldSearchEncountersByPatient() {
        // Arrange
        String patientId = "epic-patient-123";
        stubFor(get(urlPathMatching("/fhir/Encounter.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getEncounterBundleJson("epic-encounter-001", "finished", "IMP"))));

        // Act
        List<Encounter> results = connector.getEncounters(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        Encounter encounter = results.get(0);
        assertEquals(Encounter.EncounterStatus.FINISHED, encounter.getStatus());

        verify(getRequestedFor(urlPathMatching("/fhir/Encounter.*"))
            .withQueryParam("patient", equalTo(patientId)));
    }

    @Test
    @DisplayName("Should search observations by patient ID")
    void shouldSearchObservationsByPatient() {
        // Arrange
        String patientId = "epic-patient-123";
        stubFor(get(urlPathMatching("/fhir/Observation.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getObservationBundleJson("epic-obs-001", "8867-4", "Heart rate", "72"))));

        // Act
        List<Observation> results = connector.getObservations(patientId, null);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        Observation observation = results.get(0);
        assertEquals(Observation.ObservationStatus.FINAL, observation.getStatus());

        verify(getRequestedFor(urlPathMatching("/fhir/Observation.*"))
            .withQueryParam("patient", equalTo(patientId)));
    }

    @Test
    @DisplayName("Should search observations by patient ID and category")
    void shouldSearchObservationsByPatientAndCategory() {
        // Arrange
        String patientId = "epic-patient-123";
        String category = "vital-signs";
        stubFor(get(urlPathMatching("/fhir/Observation.*"))
            .withQueryParam("patient", equalTo(patientId))
            .withQueryParam("category", equalTo(category))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getObservationBundleJson("epic-obs-002", "8867-4", "Heart rate", "75"))));

        // Act
        List<Observation> results = connector.getObservations(patientId, category);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        verify(getRequestedFor(urlPathMatching("/fhir/Observation.*"))
            .withQueryParam("patient", equalTo(patientId))
            .withQueryParam("category", equalTo(category)));
    }

    @Test
    @DisplayName("Should search conditions by patient ID")
    void shouldSearchConditionsByPatient() {
        // Arrange
        String patientId = "epic-patient-123";
        stubFor(get(urlPathMatching("/fhir/Condition.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getConditionBundleJson("epic-cond-001", "38341003", "Hypertension"))));

        // Act
        List<Condition> results = connector.getConditions(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        Condition condition = results.get(0);
        assertNotNull(condition.getCode());

        verify(getRequestedFor(urlPathMatching("/fhir/Condition.*"))
            .withQueryParam("patient", equalTo(patientId)));
    }

    @Test
    @DisplayName("Should search medication requests by patient ID")
    void shouldSearchMedicationRequestsByPatient() {
        // Arrange
        String patientId = "epic-patient-123";
        stubFor(get(urlPathMatching("/fhir/MedicationRequest.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getMedicationRequestBundleJson("epic-med-001", "Lisinopril 10mg"))));

        // Act
        List<MedicationRequest> results = connector.getMedicationRequests(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        verify(getRequestedFor(urlPathMatching("/fhir/MedicationRequest.*"))
            .withQueryParam("patient", equalTo(patientId)));
    }

    @Test
    @DisplayName("Should search allergies by patient ID")
    void shouldSearchAllergiesByPatient() {
        // Arrange
        String patientId = "epic-patient-123";
        stubFor(get(urlPathMatching("/fhir/AllergyIntolerance.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getAllergyBundleJson("epic-allergy-001", "Penicillin"))));

        // Act
        List<AllergyIntolerance> results = connector.getAllergies(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        verify(getRequestedFor(urlPathMatching("/fhir/AllergyIntolerance.*"))
            .withQueryParam("patient", equalTo(patientId)));
    }

    @Test
    @DisplayName("Should test connection successfully")
    void shouldTestConnectionSuccessfully() {
        // Arrange
        stubFor(get(urlPathEqualTo("/fhir/metadata"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getCapabilityStatementJson())));

        // Act
        boolean result = connector.testConnection();

        // Assert
        assertTrue(result);
        verify(getRequestedFor(urlPathEqualTo("/fhir/metadata")));
    }

    @Test
    @DisplayName("Should fail connection test on server error")
    void shouldFailConnectionTestOnServerError() {
        // Arrange
        stubFor(get(urlPathEqualTo("/fhir/metadata"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getOperationOutcomeJson("error", "Internal server error"))));

        // Act
        boolean result = connector.testConnection();

        // Assert
        assertFalse(result);
        verify(getRequestedFor(urlPathEqualTo("/fhir/metadata")));
    }

    @Test
    @DisplayName("Should throw exception for invalid patient ID")
    void shouldThrowExceptionForInvalidPatientId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> connector.getPatient(null));
        assertThrows(IllegalArgumentException.class,
            () -> connector.getPatient(""));
        assertThrows(IllegalArgumentException.class,
            () -> connector.getPatient("   "));
    }

    @Test
    @DisplayName("Should return system name")
    void shouldReturnSystemName() {
        // Act
        String systemName = connector.getSystemName();

        // Assert
        assertEquals("Epic", systemName);
    }

    @Test
    @DisplayName("Should handle server error with exception")
    void shouldHandleServerErrorWithException() {
        // Arrange
        String patientId = "epic-patient-error";
        stubFor(get(urlPathEqualTo("/fhir/Patient/" + patientId))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getOperationOutcomeJson("error", "Internal server error"))));

        // Act & Assert
        assertThrows(EhrConnectionException.class,
            () -> connector.getPatient(patientId));
    }

    @Test
    @DisplayName("Should return empty bundle when no results found")
    void shouldReturnEmptyBundleWhenNoResultsFound() {
        // Arrange
        String mrn = "NON-EXISTENT";
        stubFor(get(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("identifier", matching(".*NON-EXISTENT.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getEmptyBundleJson())));

        // Act
        List<Patient> results = connector.searchPatientByMrn(mrn);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // Helper methods to generate FHIR JSON responses

    private String getPatientJson(String id, String familyName, String givenName) {
        return String.format("""
            {
                "resourceType": "Patient",
                "id": "%s",
                "meta": {
                    "versionId": "1",
                    "lastUpdated": "2024-01-01T12:00:00Z"
                },
                "name": [{
                    "family": "%s",
                    "given": ["%s"]
                }],
                "gender": "male",
                "birthDate": "1980-01-01"
            }
            """, id, familyName, givenName);
    }

    private String getPatientBundleJson(String id, String familyName, String givenName) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "http://localhost:8089/fhir/Patient/%s",
                    "resource": {
                        "resourceType": "Patient",
                        "id": "%s",
                        "name": [{
                            "family": "%s",
                            "given": ["%s"]
                        }],
                        "gender": "male",
                        "birthDate": "1980-01-01"
                    }
                }]
            }
            """, id, id, familyName, givenName);
    }

    private String getEncounterJson(String id, String status, String encounterClass) {
        return String.format("""
            {
                "resourceType": "Encounter",
                "id": "%s",
                "status": "%s",
                "class": {
                    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
                    "code": "%s",
                    "display": "inpatient encounter"
                }
            }
            """, id, status, encounterClass);
    }

    private String getEncounterBundleJson(String id, String status, String encounterClass) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "http://localhost:8089/fhir/Encounter/%s",
                    "resource": {
                        "resourceType": "Encounter",
                        "id": "%s",
                        "status": "%s",
                        "class": {
                            "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
                            "code": "%s",
                            "display": "inpatient encounter"
                        }
                    }
                }]
            }
            """, id, id, status, encounterClass);
    }

    private String getObservationBundleJson(String id, String loincCode, String display, String value) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "http://localhost:8089/fhir/Observation/%s",
                    "resource": {
                        "resourceType": "Observation",
                        "id": "%s",
                        "status": "final",
                        "code": {
                            "coding": [{
                                "system": "http://loinc.org",
                                "code": "%s",
                                "display": "%s"
                            }]
                        },
                        "valueQuantity": {
                            "value": %s,
                            "unit": "beats/min"
                        }
                    }
                }]
            }
            """, id, id, loincCode, display, value);
    }

    private String getConditionBundleJson(String id, String snomedCode, String display) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "http://localhost:8089/fhir/Condition/%s",
                    "resource": {
                        "resourceType": "Condition",
                        "id": "%s",
                        "clinicalStatus": {
                            "coding": [{
                                "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
                                "code": "active"
                            }]
                        },
                        "code": {
                            "coding": [{
                                "system": "http://snomed.info/sct",
                                "code": "%s",
                                "display": "%s"
                            }]
                        }
                    }
                }]
            }
            """, id, id, snomedCode, display);
    }

    private String getMedicationRequestBundleJson(String id, String medication) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "http://localhost:8089/fhir/MedicationRequest/%s",
                    "resource": {
                        "resourceType": "MedicationRequest",
                        "id": "%s",
                        "status": "active",
                        "intent": "order",
                        "medicationCodeableConcept": {
                            "text": "%s"
                        }
                    }
                }]
            }
            """, id, id, medication);
    }

    private String getAllergyBundleJson(String id, String allergen) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "http://localhost:8089/fhir/AllergyIntolerance/%s",
                    "resource": {
                        "resourceType": "AllergyIntolerance",
                        "id": "%s",
                        "clinicalStatus": {
                            "coding": [{
                                "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",
                                "code": "active"
                            }]
                        },
                        "code": {
                            "text": "%s"
                        }
                    }
                }]
            }
            """, id, id, allergen);
    }

    private String getCapabilityStatementJson() {
        return """
            {
                "resourceType": "CapabilityStatement",
                "status": "active",
                "date": "2024-01-01",
                "kind": "instance",
                "fhirVersion": "4.0.1",
                "format": ["application/fhir+json"],
                "rest": [{
                    "mode": "server",
                    "resource": [{
                        "type": "Patient",
                        "interaction": [
                            {"code": "read"},
                            {"code": "search-type"}
                        ]
                    }]
                }]
            }
            """;
    }

    private String getOperationOutcomeJson(String severity, String diagnostics) {
        return String.format("""
            {
                "resourceType": "OperationOutcome",
                "issue": [{
                    "severity": "%s",
                    "code": "processing",
                    "diagnostics": "%s"
                }]
            }
            """, severity, diagnostics);
    }

    private String getEmptyBundleJson() {
        return """
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 0,
                "entry": []
            }
            """;
    }
}

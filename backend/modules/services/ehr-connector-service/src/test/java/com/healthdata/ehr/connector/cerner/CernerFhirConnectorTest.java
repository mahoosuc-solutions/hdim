package com.healthdata.ehr.connector.cerner;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.healthdata.ehr.connector.cerner.config.CernerConnectionConfig;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Integration test suite for CernerFhirConnector using WireMock.
 * Tests FHIR R4 API interactions with mocked Cerner FHIR server responses.
 */
@DisplayName("Cerner FHIR Connector Integration Tests")
class CernerFhirConnectorTest {

    private WireMockServer wireMockServer;
    private CernerFhirConnector connector;
    private FhirContext fhirContext;
    private IGenericClient fhirClient;
    private CernerConnectionConfig config;
    private CernerAuthProvider authProvider;
    private CernerDataMapper dataMapper;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        fhirContext = FhirContext.forR4();
        baseUrl = "http://localhost:" + wireMockServer.port() + "/fhir";

        // Create a real FHIR client pointing to WireMock
        // Disable server validation to avoid automatic metadata calls
        fhirContext.getRestfulClientFactory().setServerValidationMode(ca.uhn.fhir.rest.client.api.ServerValidationModeEnum.NEVER);
        fhirClient = fhirContext.newRestfulGenericClient(baseUrl);

        // Create mock config
        config = Mockito.mock(CernerConnectionConfig.class);
        when(config.getBaseUrl()).thenReturn(baseUrl);
        when(config.getTenantId()).thenReturn("test-tenant");
        when(config.isSandboxMode()).thenReturn(true);

        // Create mock auth provider (returns empty string since WireMock handles auth)
        authProvider = Mockito.mock(CernerAuthProvider.class);
        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");

        // Create real data mapper with FhirContext
        dataMapper = new CernerDataMapper(fhirContext);

        connector = new CernerFhirConnector(fhirClient, authProvider, dataMapper, config);
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
        String patientId = "cerner-patient-123";
        stubFor(get(urlPathEqualTo("/fhir/Patient/" + patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getPatientJson(patientId, "Johnson", "Mary"))));

        // Act
        Patient result = connector.getPatientById(patientId);

        // Assert
        assertNotNull(result);
        assertEquals(patientId, result.getIdElement().getIdPart());
        assertEquals("Johnson", result.getNameFirstRep().getFamily());
        assertEquals("Mary", result.getNameFirstRep().getGivenAsSingleString());

        verify(getRequestedFor(urlPathEqualTo("/fhir/Patient/" + patientId)));
    }

    @Test
    @DisplayName("Should throw exception when patient fetch fails")
    void shouldThrowExceptionWhenPatientFetchFails() {
        // Arrange
        String patientId = "non-existent";
        stubFor(get(urlPathEqualTo("/fhir/Patient/" + patientId))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getOperationOutcomeJson("error", "Patient not found"))));

        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> connector.getPatientById(patientId));
    }

    @Test
    @DisplayName("Should search patients by identifier")
    void shouldSearchPatientsByIdentifier() {
        // Arrange
        String identifier = "MRN-67890";
        stubFor(get(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("identifier", equalTo(identifier))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getPatientBundleJson("cerner-patient-456", "Williams", "Robert"))));

        // Act
        List<Patient> results = connector.searchPatientsByIdentifier(identifier);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        Patient patient = results.get(0);
        assertEquals("Williams", patient.getNameFirstRep().getFamily());

        verify(getRequestedFor(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("identifier", equalTo(identifier)));
    }

    @Test
    @DisplayName("Should search patients by name")
    void shouldSearchPatientsByName() {
        // Arrange
        String familyName = "Williams";
        String givenName = "Robert";

        stubFor(get(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("family", equalTo(familyName))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getPatientBundleJson("cerner-patient-789", familyName, givenName))));

        // Act
        List<Patient> results = connector.searchPatientsByName(familyName, givenName);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        Patient patient = results.get(0);
        assertEquals(familyName, patient.getNameFirstRep().getFamily());
        assertEquals(givenName, patient.getNameFirstRep().getGivenAsSingleString());

        verify(getRequestedFor(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("family", equalTo(familyName)));
    }

    @Test
    @DisplayName("Should search patients by name without given name")
    void shouldSearchPatientsByNameWithoutGivenName() {
        // Arrange
        String familyName = "Williams";

        stubFor(get(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("family", equalTo(familyName))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getPatientBundleJson("cerner-patient-999", familyName, "Jane"))));

        // Act
        List<Patient> results = connector.searchPatientsByName(familyName, null);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        verify(getRequestedFor(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("family", equalTo(familyName)));
    }

    @Test
    @DisplayName("Should fetch encounter by ID")
    void shouldFetchEncounterById() {
        // Arrange
        String encounterId = "cerner-encounter-123";
        stubFor(get(urlPathEqualTo("/fhir/Encounter/" + encounterId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getEncounterJson(encounterId, "in-progress", "AMB"))));

        // Act
        Encounter result = connector.getEncounterById(encounterId);

        // Assert
        assertNotNull(result);
        assertEquals(encounterId, result.getIdElement().getIdPart());
        assertEquals(Encounter.EncounterStatus.INPROGRESS, result.getStatus());

        verify(getRequestedFor(urlPathEqualTo("/fhir/Encounter/" + encounterId)));
    }

    @Test
    @DisplayName("Should search encounters by patient ID")
    void shouldSearchEncountersByPatient() {
        // Arrange
        String patientId = "cerner-patient-123";
        stubFor(get(urlPathMatching("/fhir/Encounter.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getEncounterBundleJson("cerner-encounter-456", "finished", "IMP"))));

        // Act
        List<Encounter> results = connector.searchEncountersByPatient(patientId);

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
        String patientId = "cerner-patient-123";
        stubFor(get(urlPathMatching("/fhir/Observation.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getObservationBundleJson("cerner-obs-001", "2339-0", "Glucose", "95"))));

        // Act
        List<Observation> results = connector.searchObservationsByPatient(patientId);

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
        String patientId = "cerner-patient-123";
        String category = "laboratory";
        stubFor(get(urlPathMatching("/fhir/Observation.*"))
            .withQueryParam("patient", equalTo(patientId))
            .withQueryParam("category", equalTo(category))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getObservationBundleJson("cerner-obs-002", "2339-0", "Glucose", "98"))));

        // Act
        List<Observation> results = connector.searchObservationsByPatientAndCategory(patientId, category);

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
        String patientId = "cerner-patient-123";
        stubFor(get(urlPathMatching("/fhir/Condition.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getConditionBundleJson("cerner-cond-001", "73211009", "Diabetes mellitus"))));

        // Act
        List<Condition> results = connector.searchConditionsByPatient(patientId);

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
        String patientId = "cerner-patient-123";
        stubFor(get(urlPathMatching("/fhir/MedicationRequest.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getMedicationRequestBundleJson("cerner-med-001", "Metformin 500mg"))));

        // Act
        List<MedicationRequest> results = connector.searchMedicationRequestsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        verify(getRequestedFor(urlPathMatching("/fhir/MedicationRequest.*"))
            .withQueryParam("patient", equalTo(patientId)));
    }

    @Test
    @DisplayName("Should search immunizations by patient ID")
    void shouldSearchImmunizationsByPatient() {
        // Arrange
        String patientId = "cerner-patient-123";
        stubFor(get(urlPathMatching("/fhir/Immunization.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getImmunizationBundleJson("cerner-imm-001", "Influenza vaccine"))));

        // Act
        List<Immunization> results = connector.searchImmunizationsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        verify(getRequestedFor(urlPathMatching("/fhir/Immunization.*"))
            .withQueryParam("patient", equalTo(patientId)));
    }

    @Test
    @DisplayName("Should search diagnostic reports by patient ID")
    void shouldSearchDiagnosticReportsByPatient() {
        // Arrange
        String patientId = "cerner-patient-123";
        stubFor(get(urlPathMatching("/fhir/DiagnosticReport.*"))
            .withQueryParam("patient", equalTo(patientId))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getDiagnosticReportBundleJson("cerner-report-001", "Complete Blood Count"))));

        // Act
        List<DiagnosticReport> results = connector.searchDiagnosticReportsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        verify(getRequestedFor(urlPathMatching("/fhir/DiagnosticReport.*"))
            .withQueryParam("patient", equalTo(patientId)));
    }

    @Test
    @DisplayName("Should execute batch request")
    void shouldExecuteBatchRequest() {
        // Arrange
        stubFor(post(urlPathEqualTo("/fhir"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getBatchResponseJson())));

        Bundle requestBundle = new Bundle();
        requestBundle.setType(Bundle.BundleType.BATCH);

        // Act
        Bundle result = connector.executeBatchRequest(requestBundle);

        // Assert
        assertNotNull(result);
        assertEquals(Bundle.BundleType.BATCHRESPONSE, result.getType());

        verify(postRequestedFor(urlPathEqualTo("/fhir")));
    }

    @Test
    @DisplayName("Should execute transaction request")
    void shouldExecuteTransactionRequest() {
        // Arrange
        stubFor(post(urlPathEqualTo("/fhir"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getTransactionResponseJson())));

        Bundle requestBundle = new Bundle();
        requestBundle.setType(Bundle.BundleType.TRANSACTION);

        // Act
        Bundle result = connector.executeTransactionRequest(requestBundle);

        // Assert
        assertNotNull(result);
        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, result.getType());

        verify(postRequestedFor(urlPathEqualTo("/fhir")));
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
    @DisplayName("Should fail connection test on error")
    void shouldFailConnectionTestOnError() {
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
    @DisplayName("Should return tenant ID")
    void shouldReturnTenantId() {
        // Act
        String tenantId = connector.getTenantId();

        // Assert
        assertEquals("test-tenant", tenantId);
    }

    @Test
    @DisplayName("Should return sandbox mode status")
    void shouldReturnSandboxModeStatus() {
        // Act
        boolean isSandbox = connector.isSandboxMode();

        // Assert
        assertTrue(isSandbox);
    }

    @Test
    @DisplayName("Should return base URL")
    void shouldReturnBaseUrl() {
        // Act
        String baseUrl = connector.getBaseUrl();

        // Assert
        assertEquals(this.baseUrl, baseUrl);
    }

    @Test
    @DisplayName("Should return empty list when no results found")
    void shouldReturnEmptyListWhenNoResultsFound() {
        // Arrange
        String identifier = "NON-EXISTENT";
        stubFor(get(urlPathMatching("/fhir/Patient.*"))
            .withQueryParam("identifier", equalTo(identifier))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(getEmptyBundleJson())));

        // Act
        List<Patient> results = connector.searchPatientsByIdentifier(identifier);

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
                "gender": "female",
                "birthDate": "1975-05-15"
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
                    "fullUrl": "%s/Patient/%s",
                    "resource": {
                        "resourceType": "Patient",
                        "id": "%s",
                        "name": [{
                            "family": "%s",
                            "given": ["%s"]
                        }],
                        "gender": "female",
                        "birthDate": "1975-05-15"
                    }
                }]
            }
            """, baseUrl, id, id, familyName, givenName);
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
                    "display": "ambulatory"
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
                    "fullUrl": "%s/Encounter/%s",
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
            """, baseUrl, id, id, status, encounterClass);
    }

    private String getObservationBundleJson(String id, String loincCode, String display, String value) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "%s/Observation/%s",
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
                            "unit": "mg/dL"
                        }
                    }
                }]
            }
            """, baseUrl, id, id, loincCode, display, value);
    }

    private String getConditionBundleJson(String id, String snomedCode, String display) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "%s/Condition/%s",
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
            """, baseUrl, id, id, snomedCode, display);
    }

    private String getMedicationRequestBundleJson(String id, String medication) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "%s/MedicationRequest/%s",
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
            """, baseUrl, id, id, medication);
    }

    private String getImmunizationBundleJson(String id, String vaccine) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "%s/Immunization/%s",
                    "resource": {
                        "resourceType": "Immunization",
                        "id": "%s",
                        "status": "completed",
                        "vaccineCode": {
                            "text": "%s"
                        },
                        "patient": {
                            "reference": "Patient/cerner-patient-123"
                        },
                        "occurrenceDateTime": "2024-01-01"
                    }
                }]
            }
            """, baseUrl, id, id, vaccine);
    }

    private String getDiagnosticReportBundleJson(String id, String reportName) {
        return String.format("""
            {
                "resourceType": "Bundle",
                "type": "searchset",
                "total": 1,
                "entry": [{
                    "fullUrl": "%s/DiagnosticReport/%s",
                    "resource": {
                        "resourceType": "DiagnosticReport",
                        "id": "%s",
                        "status": "final",
                        "code": {
                            "text": "%s"
                        }
                    }
                }]
            }
            """, baseUrl, id, id, reportName);
    }

    private String getBatchResponseJson() {
        return """
            {
                "resourceType": "Bundle",
                "type": "batch-response",
                "entry": [{
                    "response": {
                        "status": "200 OK"
                    }
                }]
            }
            """;
    }

    private String getTransactionResponseJson() {
        return """
            {
                "resourceType": "Bundle",
                "type": "transaction-response",
                "entry": [{
                    "response": {
                        "status": "201 Created",
                        "location": "Patient/new-patient-123/_history/1"
                    }
                }]
            }
            """;
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

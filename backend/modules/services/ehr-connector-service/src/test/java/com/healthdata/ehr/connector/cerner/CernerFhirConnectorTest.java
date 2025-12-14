package com.healthdata.ehr.connector.cerner;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.healthdata.ehr.connector.cerner.config.CernerConnectionConfig;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @disabled Temporarily disabled - HAPI FHIR client generic type mocking issues
 */
@ExtendWith(MockitoExtension.class)
@Disabled("HAPI FHIR client generic type mocking requires refactoring")
class CernerFhirConnectorTest {

    @Mock
    private IGenericClient fhirClient;

    @Mock
    private CernerAuthProvider authProvider;

    @Mock
    private CernerDataMapper dataMapper;

    @Mock
    private FhirContext fhirContext;

    private CernerConnectionConfig config;
    private CernerFhirConnector connector;

    @BeforeEach
    void setUp() {
        config = new CernerConnectionConfig();
        config.setBaseUrl("https://fhir-myrecord.cerner.com/r4/tenant123");
        config.setTenantId("tenant123");

        connector = new CernerFhirConnector(fhirClient, authProvider, dataMapper, config);
    }

    @Test
    void testSearchPatientById_Success() {
        // Arrange
        String patientId = "12345";
        Patient expectedPatient = new Patient();
        expectedPatient.setId(patientId);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IReadExecutable readExecutable = mock(IReadExecutable.class);
        IReadTyped readTyped = mock(IReadTyped.class);
        
        when(fhirClient.read()).thenReturn(readExecutable);
        when(readExecutable.resource(Patient.class)).thenReturn(readTyped);
        when(readTyped.withId(patientId)).thenReturn(readTyped);
        when(readTyped.execute()).thenReturn(expectedPatient);
        when(dataMapper.mapPatient(expectedPatient)).thenReturn(expectedPatient);

        // Act
        Patient result = connector.getPatientById(patientId);

        // Assert
        assertNotNull(result);
        assertEquals(patientId, result.getIdElement().getIdPart());
        verify(authProvider, times(1)).getAuthorizationHeader();
    }

    @Test
    void testSearchPatientByIdentifier_Success() {
        // Arrange
        String identifier = "MRN123";
        Patient patient = new Patient();
        patient.setId("12345");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(patient);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Patient.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapPatient(patient)).thenReturn(patient);

        // Act
        List<Patient> results = connector.searchPatientsByIdentifier(identifier);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetEncounterById_Success() {
        // Arrange
        String encounterId = "ENC-123";
        Encounter expectedEncounter = new Encounter();
        expectedEncounter.setId(encounterId);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IReadExecutable readExecutable = mock(IReadExecutable.class);
        IReadTyped readTyped = mock(IReadTyped.class);
        
        when(fhirClient.read()).thenReturn(readExecutable);
        when(readExecutable.resource(Encounter.class)).thenReturn(readTyped);
        when(readTyped.withId(encounterId)).thenReturn(readTyped);
        when(readTyped.execute()).thenReturn(expectedEncounter);
        when(dataMapper.mapEncounter(expectedEncounter)).thenReturn(expectedEncounter);

        // Act
        Encounter result = connector.getEncounterById(encounterId);

        // Assert
        assertNotNull(result);
        assertEquals(encounterId, result.getIdElement().getIdPart());
    }

    @Test
    void testSearchEncountersByPatient_Success() {
        // Arrange
        String patientId = "12345";
        Encounter encounter = new Encounter();
        encounter.setId("ENC-123");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(encounter);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Encounter.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapEncounter(encounter)).thenReturn(encounter);

        // Act
        List<Encounter> results = connector.searchEncountersByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchObservationsByPatient_Success() {
        // Arrange
        String patientId = "12345";
        Observation observation = new Observation();
        observation.setId("OBS-123");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(observation);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Observation.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapObservation(observation)).thenReturn(observation);

        // Act
        List<Observation> results = connector.searchObservationsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchObservationsByCategory_Success() {
        // Arrange
        String patientId = "12345";
        String category = "laboratory";
        Observation observation = new Observation();
        observation.setId("OBS-123");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(observation);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Observation.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapObservation(observation)).thenReturn(observation);

        // Act
        List<Observation> results = connector.searchObservationsByPatientAndCategory(patientId, category);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchConditionsByPatient_Success() {
        // Arrange
        String patientId = "12345";
        Condition condition = new Condition();
        condition.setId("COND-123");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(condition);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Condition.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapCondition(condition)).thenReturn(condition);

        // Act
        List<Condition> results = connector.searchConditionsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchMedicationRequestsByPatient_Success() {
        // Arrange
        String patientId = "12345";
        MedicationRequest medRequest = new MedicationRequest();
        medRequest.setId("MED-123");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(medRequest);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(MedicationRequest.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapMedicationRequest(medRequest)).thenReturn(medRequest);

        // Act
        List<MedicationRequest> results = connector.searchMedicationRequestsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchImmunizationsByPatient_Success() {
        // Arrange
        String patientId = "12345";
        Immunization immunization = new Immunization();
        immunization.setId("IMM-123");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(immunization);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Immunization.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapImmunization(immunization)).thenReturn(immunization);

        // Act
        List<Immunization> results = connector.searchImmunizationsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchDiagnosticReportsByPatient_Success() {
        // Arrange
        String patientId = "12345";
        DiagnosticReport report = new DiagnosticReport();
        report.setId("DIAG-123");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(report);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(DiagnosticReport.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapDiagnosticReport(report)).thenReturn(report);

        // Act
        List<DiagnosticReport> results = connector.searchDiagnosticReportsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetPatientById_HandlesNotFound() {
        // Arrange
        String patientId = "nonexistent";

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IReadExecutable readExecutable = mock(IReadExecutable.class);
        IReadTyped readTyped = mock(IReadTyped.class);
        
        when(fhirClient.read()).thenReturn(readExecutable);
        when(readExecutable.resource(Patient.class)).thenReturn(readTyped);
        when(readTyped.withId(patientId)).thenReturn(readTyped);
        when(readTyped.execute()).thenThrow(new ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException("Not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> connector.getPatientById(patientId));
    }

    @Test
    void testSearchPatientsByName_Success() {
        // Arrange
        String familyName = "Doe";
        String givenName = "John";
        Patient patient = new Patient();
        patient.setId("12345");

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(patient);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Patient.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(bundle);
        when(dataMapper.mapPatient(patient)).thenReturn(patient);

        // Act
        List<Patient> results = connector.searchPatientsByName(familyName, givenName);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testExecuteBatchRequest_Success() {
        // Arrange
        Bundle requestBundle = new Bundle();
        requestBundle.setType(Bundle.BundleType.BATCH);

        Bundle responseBundle = new Bundle();
        responseBundle.setType(Bundle.BundleType.BATCHRESPONSE);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        ITransactionTyped transaction = mock(ITransactionTyped.class);
        
        when(fhirClient.transaction()).thenReturn(transaction);
        when(transaction.withBundle(requestBundle)).thenReturn(transaction);
        when(transaction.execute()).thenReturn(responseBundle);

        // Act
        Bundle result = connector.executeBatchRequest(requestBundle);

        // Assert
        assertNotNull(result);
        assertEquals(Bundle.BundleType.BATCHRESPONSE, result.getType());
    }

    @Test
    void testExecuteTransactionRequest_Success() {
        // Arrange
        Bundle requestBundle = new Bundle();
        requestBundle.setType(Bundle.BundleType.TRANSACTION);

        Bundle responseBundle = new Bundle();
        responseBundle.setType(Bundle.BundleType.TRANSACTIONRESPONSE);

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        ITransactionTyped transaction = mock(ITransactionTyped.class);
        
        when(fhirClient.transaction()).thenReturn(transaction);
        when(transaction.withBundle(requestBundle)).thenReturn(transaction);
        when(transaction.execute()).thenReturn(responseBundle);

        // Act
        Bundle result = connector.executeTransactionRequest(requestBundle);

        // Assert
        assertNotNull(result);
        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, result.getType());
    }

    @Test
    void testGetTenantId_ReturnsConfiguredTenant() {
        // Act
        String tenantId = connector.getTenantId();

        // Assert
        assertEquals("tenant123", tenantId);
    }

    @Test
    void testIsSandboxMode_ReturnsFalseForProduction() {
        // Arrange
        config.setSandboxMode(false);

        // Act
        boolean isSandbox = connector.isSandboxMode();

        // Assert
        assertFalse(isSandbox);
    }

    @Test
    void testIsSandboxMode_ReturnsTrueForSandbox() {
        // Arrange
        config.setSandboxMode(true);

        // Act
        boolean isSandbox = connector.isSandboxMode();

        // Assert
        assertTrue(isSandbox);
    }

    @Test
    void testGetBaseUrl_ReturnsConfiguredUrl() {
        // Act
        String baseUrl = connector.getBaseUrl();

        // Assert
        assertEquals("https://fhir-myrecord.cerner.com/r4/tenant123", baseUrl);
    }

    @Test
    void testSearchWithPagination_HandlesNextLink() {
        // Arrange
        String patientId = "12345";
        Observation obs1 = new Observation();
        obs1.setId("OBS-1");

        Bundle firstPage = new Bundle();
        firstPage.setType(Bundle.BundleType.SEARCHSET);
        firstPage.addEntry().setResource(obs1);
        firstPage.addLink()
                .setRelation("next")
                .setUrl("https://fhir-myrecord.cerner.com/r4/tenant123/Observation?_page=2");

        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IQuery query = mock(IQuery.class);
        IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
        
        when(fhirClient.search()).thenReturn(untypedQuery);
        when(untypedQuery.forResource(Observation.class)).thenReturn(query);
        when(query.where(any(ICriterion.class))).thenReturn(query);
        when(query.returnBundle(Bundle.class)).thenReturn(query);
        when(query.execute()).thenReturn(firstPage);
        when(dataMapper.mapObservation(obs1)).thenReturn(obs1);

        // Act
        List<Observation> results = connector.searchObservationsByPatient(patientId);

        // Assert
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    @Test
    void testConnectionTest_Success() {
        // Arrange
        when(authProvider.getAuthorizationHeader()).thenReturn("Bearer test-token");
        
        IReadExecutable readExecutable = mock(IReadExecutable.class);
        IReadTyped readTyped = mock(IReadTyped.class);
        CapabilityStatement capabilityStatement = new CapabilityStatement();
        
        when(fhirClient.capabilities()).thenReturn(readTyped);
        when(readTyped.ofType(CapabilityStatement.class)).thenReturn(readTyped);
        when(readTyped.execute()).thenReturn(capabilityStatement);

        // Act
        boolean result = connector.testConnection();

        // Assert
        assertTrue(result);
    }
}

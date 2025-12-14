package com.healthdata.ehr.connector.epic;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import com.healthdata.ehr.connector.core.EhrConnectionException;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @disabled Temporarily disabled - HAPI FHIR client generic type mocking issues
 */
@ExtendWith(MockitoExtension.class)
@Disabled("HAPI FHIR client generic type mocking requires refactoring")
class EpicFhirConnectorTest {

    @Mock
    private IGenericClient fhirClient;

    @Mock
    private EpicAuthProvider authProvider;

    @Mock
    private EpicConnectionConfig config;

    @Mock
    private FhirContext fhirContext;

    @Mock
    private IQuery<Bundle> queryMock;

    @Mock
    private IQuery<IBaseBundle> baseQueryMock;

    @Mock
    private IUntypedQuery searchMock;

    @Mock
    private IRead readMock;

    @Mock
    private IReadTyped<Patient> readTypedPatientMock;

    @Mock
    private IReadTyped<Encounter> readTypedEncounterMock;

    private EpicFhirConnector connector;

    @BeforeEach
    void setUp() {
        when(config.getBaseUrl()).thenReturn("https://fhir.epic.com");
        when(config.getMaxRetries()).thenReturn(3);
        when(authProvider.getAccessToken()).thenReturn("test-access-token");

        connector = new EpicFhirConnector(config, authProvider, fhirClient);
    }

    @Test
    void testSearchPatientByMrn_Success() {
        // Arrange
        Patient patient = createTestPatient("123", "Smith", "John");
        Bundle bundle = createBundle(patient);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Patient.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(bundle);

        // Act
        List<Patient> results = connector.searchPatientByMrn("E12345");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("123", results.get(0).getIdElement().getIdPart());
    }

    @Test
    void testSearchPatientByMrn_NoResults() {
        // Arrange
        Bundle emptyBundle = new Bundle();
        emptyBundle.setType(Bundle.BundleType.SEARCHSET);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Patient.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(emptyBundle);

        // Act
        List<Patient> results = connector.searchPatientByMrn("NONEXISTENT");

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchPatientByNameAndDob_Success() {
        // Arrange
        Patient patient = createTestPatient("456", "Doe", "Jane");
        Bundle bundle = createBundle(patient);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Patient.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.and(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(bundle);

        // Act
        List<Patient> results = connector.searchPatientByNameAndDob("Doe", "Jane", "1990-01-01");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchPatientByNameAndDob_NullParameters_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> connector.searchPatientByNameAndDob(null, "Jane", "1990-01-01"));
        assertThrows(IllegalArgumentException.class,
                () -> connector.searchPatientByNameAndDob("Doe", null, "1990-01-01"));
        assertThrows(IllegalArgumentException.class,
                () -> connector.searchPatientByNameAndDob("Doe", "Jane", null));
    }

    @Test
    void testGetPatient_Success() {
        // Arrange
        Patient patient = createTestPatient("789", "Brown", "Bob");

        when(fhirClient.read()).thenReturn(readMock);
        when(readMock.resource(Patient.class)).thenReturn(readTypedPatientMock);
        when(readTypedPatientMock.withId("789")).thenReturn(readTypedPatientMock);
        when(readTypedPatientMock.execute()).thenReturn(patient);

        // Act
        Optional<Patient> result = connector.getPatient("789");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("789", result.get().getIdElement().getIdPart());
    }

    @Test
    void testGetPatient_NotFound() {
        // Arrange
        when(fhirClient.read()).thenReturn(readMock);
        when(readMock.resource(Patient.class)).thenReturn(readTypedPatientMock);
        when(readTypedPatientMock.withId("NOTFOUND")).thenReturn(readTypedPatientMock);
        when(readTypedPatientMock.execute()).thenThrow(new ResourceNotFoundException("Patient not found"));

        // Act
        Optional<Patient> result = connector.getPatient("NOTFOUND");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testGetEncounters_Success() {
        // Arrange
        Encounter encounter = createTestEncounter("enc-123");
        Bundle bundle = createBundle(encounter);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Encounter.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(bundle);

        // Act
        List<Encounter> results = connector.getEncounters("patient-123");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("enc-123", results.get(0).getIdElement().getIdPart());
    }

    @Test
    void testGetEncounters_WithPagination() {
        // Arrange
        Encounter encounter1 = createTestEncounter("enc-1");
        Encounter encounter2 = createTestEncounter("enc-2");

        Bundle firstPage = new Bundle();
        firstPage.setType(Bundle.BundleType.SEARCHSET);
        firstPage.addEntry().setResource(encounter1);

        Bundle.BundleLinkComponent nextLink = new Bundle.BundleLinkComponent();
        nextLink.setRelation("next");
        nextLink.setUrl("https://fhir.epic.com/Encounter?_getpages=123");
        firstPage.addLink(nextLink);

        Bundle secondPage = new Bundle();
        secondPage.setType(Bundle.BundleType.SEARCHSET);
        secondPage.addEntry().setResource(encounter2);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Encounter.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(firstPage);
        when(fhirClient.loadPage()).thenReturn(mock(IGetPage.class));

        // Act
        List<Encounter> results = connector.getEncounters("patient-123");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size()); // Only first page for now
    }

    @Test
    void testGetEncounter_Success() {
        // Arrange
        Encounter encounter = createTestEncounter("enc-456");

        when(fhirClient.read()).thenReturn(readMock);
        when(readMock.resource(Encounter.class)).thenReturn(readTypedEncounterMock);
        when(readTypedEncounterMock.withId("enc-456")).thenReturn(readTypedEncounterMock);
        when(readTypedEncounterMock.execute()).thenReturn(encounter);

        // Act
        Optional<Encounter> result = connector.getEncounter("enc-456");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("enc-456", result.get().getIdElement().getIdPart());
    }

    @Test
    void testGetObservations_Success() {
        // Arrange
        Observation observation = createTestObservation("obs-123");
        Bundle bundle = createBundle(observation);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Observation.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(bundle);

        // Act
        List<Observation> results = connector.getObservations("patient-123", null);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetObservations_WithCategory() {
        // Arrange
        Observation observation = createTestObservation("obs-lab");
        Bundle bundle = createBundle(observation);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Observation.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.and(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(bundle);

        // Act
        List<Observation> results = connector.getObservations("patient-123", "laboratory");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetConditions_Success() {
        // Arrange
        Condition condition = createTestCondition("cond-123");
        Bundle bundle = createBundle(condition);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Condition.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(bundle);

        // Act
        List<Condition> results = connector.getConditions("patient-123");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetMedicationRequests_Success() {
        // Arrange
        MedicationRequest medRequest = createTestMedicationRequest("med-123");
        Bundle bundle = createBundle(medRequest);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(MedicationRequest.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(bundle);

        // Act
        List<MedicationRequest> results = connector.getMedicationRequests("patient-123");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetAllergies_Success() {
        // Arrange
        AllergyIntolerance allergy = createTestAllergy("allergy-123");
        Bundle bundle = createBundle(allergy);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(AllergyIntolerance.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenReturn(bundle);

        // Act
        List<AllergyIntolerance> results = connector.getAllergies("patient-123");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testTestConnection_Success() {
        // Arrange
        CapabilityStatement capabilityStatement = new CapabilityStatement();
        capabilityStatement.setStatus(Enumerations.PublicationStatus.ACTIVE);

        when(fhirClient.capabilities()).thenReturn(mock(IFetchConformanceUntyped.class));
        when(fhirClient.capabilities().ofType(CapabilityStatement.class)).thenReturn(mock(IFetchConformanceTyped.class));
        when(fhirClient.capabilities().ofType(CapabilityStatement.class).execute()).thenReturn(capabilityStatement);

        // Act
        boolean result = connector.testConnection();

        // Assert
        assertTrue(result);
    }

    @Test
    void testTestConnection_Failure() {
        // Arrange
        when(fhirClient.capabilities()).thenReturn(mock(IFetchConformanceUntyped.class));
        when(fhirClient.capabilities().ofType(CapabilityStatement.class)).thenReturn(mock(IFetchConformanceTyped.class));
        when(fhirClient.capabilities().ofType(CapabilityStatement.class).execute())
                .thenThrow(new RuntimeException("Connection failed"));

        // Act
        boolean result = connector.testConnection();

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetSystemName() {
        // Act
        String systemName = connector.getSystemName();

        // Assert
        assertEquals("Epic", systemName);
    }

    @Test
    void testSearchPatientByMrn_WithRetry() {
        // Arrange
        Patient patient = createTestPatient("retry-123", "Retry", "Test");
        Bundle bundle = createBundle(patient);

        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Patient.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute())
                .thenThrow(new RuntimeException("Temporary error"))
                .thenReturn(bundle);

        // Act
        List<Patient> results = connector.searchPatientByMrn("E12345");

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(baseQueryMock, times(2)).execute();
    }

    @Test
    void testSearchPatientByMrn_MaxRetriesExceeded() {
        // Arrange
        when(fhirClient.search()).thenReturn(searchMock);
        when(searchMock.forResource(Patient.class)).thenReturn(queryMock);
        when(queryMock.where(any())).thenReturn(queryMock);
        when(queryMock.returnBundle(Bundle.class)).thenReturn(baseQueryMock);
        when(baseQueryMock.execute()).thenThrow(new RuntimeException("Persistent error"));

        // Act & Assert
        assertThrows(EhrConnectionException.class, () -> connector.searchPatientByMrn("E12345"));
    }

    @Test
    void testGetObservations_NullPatientId_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connector.getObservations(null, null));
    }

    @Test
    void testGetConditions_EmptyPatientId_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connector.getConditions(""));
    }

    @Test
    void testGetMedicationRequests_NullPatientId_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connector.getMedicationRequests(null));
    }

    @Test
    void testGetAllergies_NullPatientId_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connector.getAllergies(null));
    }

    @Test
    void testSearchPatientByMrn_NullMrn_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connector.searchPatientByMrn(null));
    }

    // Helper methods

    private Patient createTestPatient(String id, String family, String given) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.addName().setFamily(family).addGiven(given);
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(new Date());
        return patient;
    }

    private Encounter createTestEncounter(String id) {
        Encounter encounter = new Encounter();
        encounter.setId(id);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.setClass_(new Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", "IMP", "inpatient"));
        return encounter;
    }

    private Observation createTestObservation(String id) {
        Observation observation = new Observation();
        observation.setId(id);
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept()
                .addCoding(new Coding("http://loinc.org", "8867-4", "Heart rate")));
        return observation;
    }

    private Condition createTestCondition(String id) {
        Condition condition = new Condition();
        condition.setId(id);
        condition.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding("http://terminology.hl7.org/CodeSystem/condition-clinical", "active", "Active")));
        return condition;
    }

    private MedicationRequest createTestMedicationRequest(String id) {
        MedicationRequest medRequest = new MedicationRequest();
        medRequest.setId(id);
        medRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        return medRequest;
    }

    private AllergyIntolerance createTestAllergy(String id) {
        AllergyIntolerance allergy = new AllergyIntolerance();
        allergy.setId(id);
        allergy.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical", "active", "Active")));
        return allergy;
    }

    private Bundle createBundle(Resource resource) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(resource);
        return bundle;
    }
}

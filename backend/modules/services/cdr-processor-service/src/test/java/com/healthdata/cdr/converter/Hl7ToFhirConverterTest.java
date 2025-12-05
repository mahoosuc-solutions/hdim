package com.healthdata.cdr.converter;

import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.ORC;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.IN1;
import ca.uhn.hl7v2.model.v25.datatype.XPN;
import ca.uhn.hl7v2.model.v25.datatype.CX;
import ca.uhn.hl7v2.model.v25.datatype.TS;
import ca.uhn.hl7v2.model.v25.datatype.CE;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for HL7 to FHIR Converter
 * Tests conversion of HL7 v2 segments to FHIR resources
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HL7 to FHIR Converter Tests")
class Hl7ToFhirConverterTest {

    @InjectMocks
    private Hl7ToFhirConverter converter;

    @Mock
    private PID mockPid;

    @Mock
    private PV1 mockPv1;

    @Mock
    private OBX mockObx;

    @Mock
    private ORC mockOrc;

    @Mock
    private OBR mockObr;

    @Mock
    private IN1 mockIn1;

    @BeforeEach
    void setUp() {
        converter = new Hl7ToFhirConverter();
    }

    @Test
    @DisplayName("Should convert PID segment to Patient resource")
    void testConvertPidToPatient() throws Exception {
        // Given
        when(mockPid.getPatientIdentifierList(0).getIDNumber().getValue()).thenReturn("12345");
        when(mockPid.getPatientName(0).getFamilyName().getSurname().getValue()).thenReturn("DOE");
        when(mockPid.getPatientName(0).getGivenName().getValue()).thenReturn("JOHN");
        when(mockPid.getAdministrativeSex().getValue()).thenReturn("M");
        when(mockPid.getDateTimeOfBirth().getTime().getValue()).thenReturn("19800115");

        // When
        Patient patient = converter.convertPidToPatient(mockPid);

        // Then
        assertNotNull(patient);
        assertFalse(patient.getIdentifier().isEmpty());
        assertEquals("12345", patient.getIdentifier().get(0).getValue());
        assertFalse(patient.getName().isEmpty());
        assertEquals("DOE", patient.getName().get(0).getFamily());
        assertEquals("JOHN", patient.getName().get(0).getGivenAsSingleString());
        assertEquals(AdministrativeGender.MALE, patient.getGender());
        assertNotNull(patient.getBirthDate());
    }

    @Test
    @DisplayName("Should convert female gender correctly")
    void testConvertFemaleGender() throws Exception {
        // Given
        when(mockPid.getAdministrativeSex().getValue()).thenReturn("F");

        // When
        AdministrativeGender gender = converter.convertGender(mockPid);

        // Then
        assertEquals(AdministrativeGender.FEMALE, gender);
    }

    @Test
    @DisplayName("Should convert unknown gender correctly")
    void testConvertUnknownGender() throws Exception {
        // Given
        when(mockPid.getAdministrativeSex().getValue()).thenReturn("U");

        // When
        AdministrativeGender gender = converter.convertGender(mockPid);

        // Then
        assertEquals(AdministrativeGender.UNKNOWN, gender);
    }

    @Test
    @DisplayName("Should convert patient address from PID")
    void testConvertPatientAddress() throws Exception {
        // Given
        when(mockPid.getPatientAddress(0).getStreetAddress().getStreetOrMailingAddress().getValue())
            .thenReturn("123 MAIN ST");
        when(mockPid.getPatientAddress(0).getCity().getValue()).thenReturn("BOSTON");
        when(mockPid.getPatientAddress(0).getStateOrProvince().getValue()).thenReturn("MA");
        when(mockPid.getPatientAddress(0).getZipOrPostalCode().getValue()).thenReturn("02101");

        // When
        Address address = converter.convertAddress(mockPid);

        // Then
        assertNotNull(address);
        assertEquals("123 MAIN ST", address.getLine().get(0).getValue());
        assertEquals("BOSTON", address.getCity());
        assertEquals("MA", address.getState());
        assertEquals("02101", address.getPostalCode());
    }

    @Test
    @DisplayName("Should convert patient phone number from PID")
    void testConvertPatientPhoneNumber() throws Exception {
        // Given
        when(mockPid.getPhoneNumberHome(0).getTelephoneNumber().getValue())
            .thenReturn("(617)555-1234");

        // When
        ContactPoint phone = converter.convertPhoneNumber(mockPid);

        // Then
        assertNotNull(phone);
        assertEquals("(617)555-1234", phone.getValue());
        assertEquals(ContactPoint.ContactPointSystem.PHONE, phone.getSystem());
    }

    @Test
    @DisplayName("Should convert OBX segment to Observation resource")
    void testConvertObxToObservation() throws Exception {
        // Given
        when(mockObx.getValueType().getValue()).thenReturn("NM");
        when(mockObx.getObservationIdentifier().getIdentifier().getValue()).thenReturn("2345-7");
        when(mockObx.getObservationIdentifier().getText().getValue()).thenReturn("Glucose");
        when(mockObx.getObservationValue(0).getData().toString()).thenReturn("95");
        when(mockObx.getUnits().getText().getValue()).thenReturn("mg/dL");
        when(mockObx.getObservationResultStatus().getValue()).thenReturn("F");

        // When
        Observation observation = converter.convertObxToObservation(mockObx);

        // Then
        assertNotNull(observation);
        assertNotNull(observation.getCode());
        assertEquals("2345-7", observation.getCode().getCoding().get(0).getCode());
        assertEquals("Glucose", observation.getCode().getCoding().get(0).getDisplay());
        assertTrue(observation.getValue() instanceof Quantity);
        Quantity quantity = (Quantity) observation.getValue();
        assertEquals(95, quantity.getValue().doubleValue());
        assertEquals("mg/dL", quantity.getUnit());
        assertEquals(ObservationStatus.FINAL, observation.getStatus());
    }

    @Test
    @DisplayName("Should convert text observation value")
    void testConvertTextObservationValue() throws Exception {
        // Given
        when(mockObx.getValueType().getValue()).thenReturn("TX");
        when(mockObx.getObservationIdentifier().getIdentifier().getValue()).thenReturn("11502-2");
        when(mockObx.getObservationIdentifier().getText().getValue()).thenReturn("Organism");
        when(mockObx.getObservationValue(0).getData().toString()).thenReturn("Escherichia coli");

        // When
        Observation observation = converter.convertObxToObservation(mockObx);

        // Then
        assertNotNull(observation);
        assertTrue(observation.getValue() instanceof org.hl7.fhir.r4.model.StringType);
    }

    @Test
    @DisplayName("Should convert ORC segment to ServiceRequest resource")
    void testConvertOrcToServiceRequest() throws Exception {
        // Given
        when(mockOrc.getPlacerOrderNumber().getEntityIdentifier().getValue()).thenReturn("ORD123");
        when(mockOrc.getOrderControl().getValue()).thenReturn("NW");

        // When
        ServiceRequest serviceRequest = converter.convertOrcToServiceRequest(mockOrc);

        // Then
        assertNotNull(serviceRequest);
        assertFalse(serviceRequest.getIdentifier().isEmpty());
        assertEquals("ORD123", serviceRequest.getIdentifier().get(0).getValue());
    }

    @Test
    @DisplayName("Should handle date/time conversion from HL7 format")
    void testHandleDateTimeConversion() throws Exception {
        // Given
        String hl7DateTime = "20240115120000";

        // When
        Date date = converter.convertHl7DateTime(hl7DateTime);

        // Then
        assertNotNull(date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        assertEquals(hl7DateTime, sdf.format(date));
    }

    @Test
    @DisplayName("Should handle date conversion from HL7 format")
    void testHandleDateConversion() throws Exception {
        // Given
        String hl7Date = "20240115";

        // When
        Date date = converter.convertHl7Date(hl7Date);

        // Then
        assertNotNull(date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        assertEquals(hl7Date, sdf.format(date));
    }

    @Test
    @DisplayName("Should handle code system mapping for LOINC")
    void testHandleCodeSystemMappingLoinc() {
        // Given
        String loincCode = "2345-7";

        // When
        String codeSystem = converter.getCodeSystemForObservation(loincCode);

        // Then
        assertEquals("http://loinc.org", codeSystem);
    }

    @Test
    @DisplayName("Should handle code system mapping for custom codes")
    void testHandleCodeSystemMappingCustom() {
        // Given
        String customCode = "CUSTOM123";

        // When
        String codeSystem = converter.getCodeSystemForObservation(customCode);

        // Then
        assertNotNull(codeSystem);
    }

    @Test
    @DisplayName("Should convert PV1 to Encounter resource")
    void testConvertPv1ToEncounter() throws Exception {
        // Given
        when(mockPv1.getPatientClass().getValue()).thenReturn("I");
        when(mockPv1.getAssignedPatientLocation().getPointOfCare().getValue()).thenReturn("ICU");
        when(mockPv1.getAssignedPatientLocation().getRoom().getValue()).thenReturn("101");
        when(mockPv1.getAssignedPatientLocation().getBed().getValue()).thenReturn("A");
        when(mockPv1.getAdmissionType().getValue()).thenReturn("E");

        // When
        Encounter encounter = converter.convertPv1ToEncounter(mockPv1);

        // Then
        assertNotNull(encounter);
        assertEquals(Encounter.EncounterStatus.INPROGRESS, encounter.getStatus());
        assertNotNull(encounter.getClass_());
    }

    @Test
    @DisplayName("Should convert IN1 to Coverage resource")
    void testConvertIn1ToCoverage() throws Exception {
        // Given
        when(mockIn1.getInsurancePlanID().getIdentifier().getValue()).thenReturn("INS001");
        when(mockIn1.getInsuranceCompanyName(0).getOrganizationName().getValue()).thenReturn("BLUE_CROSS");

        // When
        Coverage coverage = converter.convertIn1ToCoverage(mockIn1);

        // Then
        assertNotNull(coverage);
        assertFalse(coverage.getIdentifier().isEmpty());
        assertEquals("INS001", coverage.getIdentifier().get(0).getValue());
    }

    @Test
    @DisplayName("Should convert OBR to DiagnosticReport")
    void testConvertObrToDiagnosticReport() throws Exception {
        // Given
        when(mockObr.getFillerOrderNumber().getEntityIdentifier().getValue()).thenReturn("RES123");
        when(mockObr.getUniversalServiceIdentifier().getIdentifier().getValue()).thenReturn("80048");
        when(mockObr.getUniversalServiceIdentifier().getText().getValue()).thenReturn("BMP");

        // When
        DiagnosticReport report = converter.convertObrToDiagnosticReport(mockObr);

        // Then
        assertNotNull(report);
        assertFalse(report.getIdentifier().isEmpty());
        assertEquals("RES123", report.getIdentifier().get(0).getValue());
        assertNotNull(report.getCode());
        assertEquals("80048", report.getCode().getCoding().get(0).getCode());
    }

    @Test
    @DisplayName("Should handle null values gracefully in PID conversion")
    void testHandleNullValuesInPidConversion() throws Exception {
        // Given
        when(mockPid.getPatientIdentifierList(0).getIDNumber().getValue()).thenReturn("12345");
        when(mockPid.getPatientName(0).getFamilyName().getSurname().getValue()).thenReturn(null);
        when(mockPid.getPatientName(0).getGivenName().getValue()).thenReturn(null);

        // When
        Patient patient = converter.convertPidToPatient(mockPid);

        // Then
        assertNotNull(patient);
        // Should handle nulls gracefully
    }

    @Test
    @DisplayName("Should handle empty date string")
    void testHandleEmptyDateString() {
        // Given
        String emptyDate = "";

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            converter.convertHl7Date(emptyDate);
        });
    }

    @Test
    @DisplayName("Should handle invalid date format")
    void testHandleInvalidDateFormat() {
        // Given
        String invalidDate = "INVALID";

        // When/Then
        assertThrows(DateConversionException.class, () -> {
            converter.convertHl7Date(invalidDate);
        });
    }

    @Test
    @DisplayName("Should convert observation status to FHIR")
    void testConvertObservationStatus() {
        // Test Final status
        assertEquals(ObservationStatus.FINAL, converter.convertObservationStatus("F"));

        // Test Preliminary status
        assertEquals(ObservationStatus.PRELIMINARY, converter.convertObservationStatus("P"));

        // Test Corrected status
        assertEquals(ObservationStatus.CORRECTED, converter.convertObservationStatus("C"));

        // Test Unknown status
        assertEquals(ObservationStatus.UNKNOWN, converter.convertObservationStatus("X"));
    }

    @Test
    @DisplayName("Should convert encounter class from PV1")
    void testConvertEncounterClass() throws Exception {
        // Inpatient
        when(mockPv1.getPatientClass().getValue()).thenReturn("I");
        Coding inpatient = converter.convertPatientClass(mockPv1);
        assertEquals("IMP", inpatient.getCode());

        // Outpatient
        when(mockPv1.getPatientClass().getValue()).thenReturn("O");
        Coding outpatient = converter.convertPatientClass(mockPv1);
        assertEquals("AMB", outpatient.getCode());

        // Emergency
        when(mockPv1.getPatientClass().getValue()).thenReturn("E");
        Coding emergency = converter.convertPatientClass(mockPv1);
        assertEquals("EMER", emergency.getCode());
    }

    @Test
    @DisplayName("Should extract patient identifier")
    void testExtractPatientIdentifier() throws Exception {
        // Given
        when(mockPid.getPatientIdentifierList(0).getIDNumber().getValue()).thenReturn("12345");

        // When
        String identifier = converter.extractPatientIdentifier(mockPid);

        // Then
        assertEquals("12345", identifier);
    }

    @Test
    @DisplayName("Should extract visit number")
    void testExtractVisitNumber() throws Exception {
        // Given
        when(mockPv1.getVisitNumber().getIDNumber().getValue()).thenReturn("V123456");

        // When
        String visitNumber = converter.extractVisitNumber(mockPv1);

        // Then
        assertEquals("V123456", visitNumber);
    }

    @Test
    @DisplayName("Should handle multiple patient identifiers")
    void testHandleMultiplePatientIdentifiers() throws Exception {
        // Given
        when(mockPid.getPatientIdentifierList(0).getIDNumber().getValue()).thenReturn("12345");
        when(mockPid.getPatientIdentifierList(1).getIDNumber().getValue()).thenReturn("SSN123");

        // When
        List<Identifier> identifiers = converter.convertPatientIdentifiers(mockPid);

        // Then
        assertNotNull(identifiers);
        assertTrue(identifiers.size() >= 1);
    }

    @Test
    @DisplayName("Should convert abnormal flag to interpretation")
    void testConvertAbnormalFlagToInterpretation() throws Exception {
        // High
        when(mockObx.getAbnormalFlags(0).getValue()).thenReturn("H");
        CodeableConcept high = converter.convertAbnormalFlag(mockObx);
        assertNotNull(high);

        // Low
        when(mockObx.getAbnormalFlags(0).getValue()).thenReturn("L");
        CodeableConcept low = converter.convertAbnormalFlag(mockObx);
        assertNotNull(low);

        // Normal
        when(mockObx.getAbnormalFlags(0).getValue()).thenReturn("N");
        CodeableConcept normal = converter.convertAbnormalFlag(mockObx);
        assertNotNull(normal);
    }

    @Test
    @DisplayName("Should handle time zone in date conversion")
    void testHandleTimeZoneInDateConversion() throws Exception {
        // Given
        String hl7DateTimeWithTz = "20240115120000-0500";

        // When
        Date date = converter.convertHl7DateTime(hl7DateTimeWithTz);

        // Then
        assertNotNull(date);
    }

    @Test
    @DisplayName("Should create FHIR Identifier from HL7 CX")
    void testCreateFhirIdentifierFromCx() throws Exception {
        // Given
        when(mockPid.getPatientIdentifierList(0).getIDNumber().getValue()).thenReturn("12345");
        when(mockPid.getPatientIdentifierList(0).getAssigningAuthority().getNamespaceID().getValue())
            .thenReturn("MRN");

        // When
        Identifier identifier = converter.createIdentifier(mockPid.getPatientIdentifierList(0));

        // Then
        assertNotNull(identifier);
        assertEquals("12345", identifier.getValue());
    }
}

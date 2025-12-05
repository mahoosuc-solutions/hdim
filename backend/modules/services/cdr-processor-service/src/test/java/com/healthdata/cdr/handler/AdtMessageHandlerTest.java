package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.model.v25.segment.IN1;
import com.healthdata.cdr.parser.Hl7v2ParserService;
import com.healthdata.cdr.converter.Hl7ToFhirConverter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for ADT Message Handler
 * Tests handling of ADT messages and conversion to FHIR resources
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ADT Message Handler Tests")
class AdtMessageHandlerTest {

    @InjectMocks
    private AdtMessageHandler adtMessageHandler;

    @Mock
    private Hl7v2ParserService parserService;

    @Mock
    private Hl7ToFhirConverter fhirConverter;

    private static final String ADT_A01_ADMIT =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115120000||ADT^A01|123456|P|2.5\r" +
        "EVN|A01|20240115120000\r" +
        "PID|1||12345^^^MRN||DOE^JOHN^A||19800115|M|||123 MAIN ST^^BOSTON^MA^02101\r" +
        "PV1|1|I|ICU^101^A|E|||1234^SMITH^JAMES|||||||||||||V123456\r" +
        "IN1|1|PPO|INS001|BLUE_CROSS|123 INS ST^^NYC^NY^10001||||GRP123";

    private static final String ADT_A02_TRANSFER =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115150000||ADT^A02|123457|P|2.5\r" +
        "EVN|A02|20240115150000\r" +
        "PID|1||12345^^^MRN||DOE^JOHN^A||19800115|M\r" +
        "PV1|1|I|WARD_B^202^A|E|||1234^SMITH^JAMES";

    private static final String ADT_A03_DISCHARGE =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240116100000||ADT^A03|123458|P|2.5\r" +
        "EVN|A03|20240116100000\r" +
        "PID|1||12345^^^MRN||DOE^JOHN^A||19800115|M\r" +
        "PV1|1|I|ICU^101^A|E|||1234^SMITH^JAMES||||||||||||20240116100000";

    private static final String ADT_A08_UPDATE =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115130000||ADT^A08|123459|P|2.5\r" +
        "EVN|A08|20240115130000\r" +
        "PID|1||12345^^^MRN||DOE^JOHN^ANTHONY||19800115|M|||456 OAK AVE^^CAMBRIDGE^MA^02139||(617)555-1234";

    private static final String ADT_A01_EMERGENCY =
        "MSH|^~\\&|ER_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115030000||ADT^A01|123460|P|2.5\r" +
        "EVN|A01|20240115030000\r" +
        "PID|1||12346^^^MRN||SMITH^JANE||19900520|F\r" +
        "PV1|1|E|ER^TRAUMA_1^A|E|||5678^JONES^ROBERT";

    @BeforeEach
    void setUp() {
        adtMessageHandler = new AdtMessageHandler(parserService, fhirConverter);
    }

    @Test
    @DisplayName("Should create Patient resource from ADT^A01")
    void testCreatePatientFromAdtA01() {
        // Given
        Message message = mock(ADT_A01.class);
        PID pid = mock(PID.class);
        Patient expectedPatient = new Patient();
        expectedPatient.setId("12345");

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.extractPidSegment(message)).thenReturn(pid);
        when(fhirConverter.convertPidToPatient(pid)).thenReturn(expectedPatient);

        // When
        Patient patient = adtMessageHandler.handleAdmit(ADT_A01_ADMIT);

        // Then
        assertNotNull(patient);
        assertEquals("12345", patient.getId());
        verify(parserService).parse(ADT_A01_ADMIT);
        verify(parserService).extractPidSegment(message);
        verify(fhirConverter).convertPidToPatient(pid);
    }

    @Test
    @DisplayName("Should create Encounter resource from ADT^A01")
    void testCreateEncounterFromAdtA01() {
        // Given
        Message message = mock(ADT_A01.class);
        PV1 pv1 = mock(PV1.class);
        Encounter expectedEncounter = new Encounter();
        expectedEncounter.setId("V123456");

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.extractPv1Segment(message)).thenReturn(pv1);
        when(fhirConverter.convertPv1ToEncounter(pv1)).thenReturn(expectedEncounter);

        // When
        Encounter encounter = adtMessageHandler.createEncounter(ADT_A01_ADMIT);

        // Then
        assertNotNull(encounter);
        assertEquals("V123456", encounter.getId());
        verify(fhirConverter).convertPv1ToEncounter(pv1);
    }

    @Test
    @DisplayName("Should update Patient from ADT^A08")
    void testUpdatePatientFromAdtA08() {
        // Given
        Message message = mock(ADT_A01.class);
        PID pid = mock(PID.class);
        Patient existingPatient = new Patient();
        existingPatient.setId("12345");
        Patient updatedPatient = new Patient();
        updatedPatient.setId("12345");

        when(parserService.parse(ADT_A08_UPDATE)).thenReturn(message);
        when(parserService.extractPidSegment(message)).thenReturn(pid);
        when(fhirConverter.updatePatientFromPid(existingPatient, pid)).thenReturn(updatedPatient);

        // When
        Patient result = adtMessageHandler.handleUpdate(ADT_A08_UPDATE, existingPatient);

        // Then
        assertNotNull(result);
        assertEquals("12345", result.getId());
        verify(fhirConverter).updatePatientFromPid(existingPatient, pid);
    }

    @Test
    @DisplayName("Should handle admit event")
    void testHandleAdmitEvent() {
        // Given
        Message message = mock(ADT_A01.class);
        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.getTriggerEvent(message)).thenReturn("A01");

        // When
        String eventType = adtMessageHandler.getEventType(ADT_A01_ADMIT);

        // Then
        assertEquals("ADMIT", eventType);
        verify(parserService).getTriggerEvent(message);
    }

    @Test
    @DisplayName("Should handle transfer event")
    void testHandleTransferEvent() {
        // Given
        Message message = mock(ADT_A01.class);
        PV1 pv1 = mock(PV1.class);
        Encounter encounter = new Encounter();

        when(parserService.parse(ADT_A02_TRANSFER)).thenReturn(message);
        when(parserService.getTriggerEvent(message)).thenReturn("A02");
        when(parserService.extractPv1Segment(message)).thenReturn(pv1);
        when(fhirConverter.updateEncounterLocation(any(Encounter.class), eq(pv1))).thenReturn(encounter);

        // When
        Encounter result = adtMessageHandler.handleTransfer(ADT_A02_TRANSFER, new Encounter());

        // Then
        assertNotNull(result);
        verify(fhirConverter).updateEncounterLocation(any(Encounter.class), eq(pv1));
    }

    @Test
    @DisplayName("Should handle discharge event")
    void testHandleDischargeEvent() {
        // Given
        Message message = mock(ADT_A01.class);
        PV1 pv1 = mock(PV1.class);
        Encounter encounter = new Encounter();
        encounter.setId("V123456");

        when(parserService.parse(ADT_A03_DISCHARGE)).thenReturn(message);
        when(parserService.getTriggerEvent(message)).thenReturn("A03");
        when(parserService.extractPv1Segment(message)).thenReturn(pv1);
        when(fhirConverter.closeEncounter(any(Encounter.class), eq(pv1))).thenReturn(encounter);

        // When
        Encounter result = adtMessageHandler.handleDischarge(ADT_A03_DISCHARGE, new Encounter());

        // Then
        assertNotNull(result);
        verify(fhirConverter).closeEncounter(any(Encounter.class), eq(pv1));
    }

    @Test
    @DisplayName("Should extract insurance from IN1 segment")
    void testExtractInsuranceFromIn1() {
        // Given
        Message message = mock(ADT_A01.class);
        IN1 in1 = mock(IN1.class);
        Coverage coverage = new Coverage();
        coverage.setId("INS001");

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.extractIn1Segment(message)).thenReturn(in1);
        when(fhirConverter.convertIn1ToCoverage(in1)).thenReturn(coverage);

        // When
        Coverage result = adtMessageHandler.extractInsurance(ADT_A01_ADMIT);

        // Then
        assertNotNull(result);
        assertEquals("INS001", result.getId());
        verify(fhirConverter).convertIn1ToCoverage(in1);
    }

    @Test
    @DisplayName("Should handle emergency admit")
    void testHandleEmergencyAdmit() {
        // Given
        Message message = mock(ADT_A01.class);
        PV1 pv1 = mock(PV1.class);
        Encounter encounter = new Encounter();

        when(parserService.parse(ADT_A01_EMERGENCY)).thenReturn(message);
        when(parserService.extractPv1Segment(message)).thenReturn(pv1);
        when(fhirConverter.convertPv1ToEncounter(pv1)).thenReturn(encounter);
        when(fhirConverter.isEmergencyAdmit(pv1)).thenReturn(true);

        // When
        boolean isEmergency = adtMessageHandler.isEmergencyAdmit(ADT_A01_EMERGENCY);

        // Then
        assertTrue(isEmergency);
        verify(fhirConverter).isEmergencyAdmit(pv1);
    }

    @Test
    @DisplayName("Should extract patient identifier from ADT message")
    void testExtractPatientIdentifier() {
        // Given
        Message message = mock(ADT_A01.class);
        PID pid = mock(PID.class);

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.extractPidSegment(message)).thenReturn(pid);
        when(fhirConverter.extractPatientIdentifier(pid)).thenReturn("12345");

        // When
        String patientId = adtMessageHandler.extractPatientId(ADT_A01_ADMIT);

        // Then
        assertEquals("12345", patientId);
        verify(fhirConverter).extractPatientIdentifier(pid);
    }

    @Test
    @DisplayName("Should extract visit number from ADT message")
    void testExtractVisitNumber() {
        // Given
        Message message = mock(ADT_A01.class);
        PV1 pv1 = mock(PV1.class);

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.extractPv1Segment(message)).thenReturn(pv1);
        when(fhirConverter.extractVisitNumber(pv1)).thenReturn("V123456");

        // When
        String visitNumber = adtMessageHandler.extractVisitNumber(ADT_A01_ADMIT);

        // Then
        assertEquals("V123456", visitNumber);
        verify(fhirConverter).extractVisitNumber(pv1);
    }

    @Test
    @DisplayName("Should handle null PV1 segment gracefully")
    void testHandleNullPv1Segment() {
        // Given
        Message message = mock(ADT_A01.class);

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.extractPv1Segment(message)).thenReturn(null);

        // When
        Encounter encounter = adtMessageHandler.createEncounter(ADT_A01_ADMIT);

        // Then
        assertNull(encounter);
    }

    @Test
    @DisplayName("Should handle null IN1 segment gracefully")
    void testHandleNullIn1Segment() {
        // Given
        Message message = mock(ADT_A01.class);

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.extractIn1Segment(message)).thenReturn(null);

        // When
        Coverage coverage = adtMessageHandler.extractInsurance(ADT_A01_ADMIT);

        // Then
        assertNull(coverage);
    }

    @Test
    @DisplayName("Should validate ADT message type")
    void testValidateAdtMessageType() {
        // Given
        Message message = mock(ADT_A01.class);

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.getMessageType(message)).thenReturn("ADT^A01");

        // When
        boolean isValid = adtMessageHandler.isValidAdtMessage(ADT_A01_ADMIT);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject non-ADT message")
    void testRejectNonAdtMessage() {
        // Given
        String oruMessage = "MSH|^~\\&|LAB|FAC|EMR|FAC|20240115120000||ORU^R01|789|P|2.5";
        Message message = mock(Message.class);

        when(parserService.parse(oruMessage)).thenReturn(message);
        when(parserService.getMessageType(message)).thenReturn("ORU^R01");

        // When
        boolean isValid = adtMessageHandler.isValidAdtMessage(oruMessage);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract admission timestamp")
    void testExtractAdmissionTimestamp() {
        // Given
        Message message = mock(ADT_A01.class);
        PV1 pv1 = mock(PV1.class);
        Date expectedDate = new Date();

        when(parserService.parse(ADT_A01_ADMIT)).thenReturn(message);
        when(parserService.extractPv1Segment(message)).thenReturn(pv1);
        when(fhirConverter.extractAdmitDateTime(pv1)).thenReturn(expectedDate);

        // When
        Date admitDate = adtMessageHandler.extractAdmitDateTime(ADT_A01_ADMIT);

        // Then
        assertNotNull(admitDate);
        assertEquals(expectedDate, admitDate);
    }

    @Test
    @DisplayName("Should extract discharge timestamp")
    void testExtractDischargeTimestamp() {
        // Given
        Message message = mock(ADT_A01.class);
        PV1 pv1 = mock(PV1.class);
        Date expectedDate = new Date();

        when(parserService.parse(ADT_A03_DISCHARGE)).thenReturn(message);
        when(parserService.extractPv1Segment(message)).thenReturn(pv1);
        when(fhirConverter.extractDischargeDateTime(pv1)).thenReturn(expectedDate);

        // When
        Date dischargeDate = adtMessageHandler.extractDischargeDateTime(ADT_A03_DISCHARGE);

        // Then
        assertNotNull(dischargeDate);
        assertEquals(expectedDate, dischargeDate);
    }
}

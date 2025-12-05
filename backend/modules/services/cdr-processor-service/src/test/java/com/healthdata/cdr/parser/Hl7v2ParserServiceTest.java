package com.healthdata.cdr.parser;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.message.ORM_O01;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for HL7 v2 Parser Service
 * Tests parsing of various HL7 v2 message types (ADT, ORU, ORM)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HL7 v2 Parser Service Tests")
class Hl7v2ParserServiceTest {

    @InjectMocks
    private Hl7v2ParserService parserService;

    @Mock
    private Parser hl7Parser;

    private static final String ADT_A01_MESSAGE =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115120000||ADT^A01|123456|P|2.5\r" +
        "EVN|A01|20240115120000\r" +
        "PID|1||12345^^^MRN||DOE^JOHN^A||19800115|M|||123 MAIN ST^^BOSTON^MA^02101\r" +
        "PV1|1|I|ICU^101^A|E|||1234^SMITH^JAMES|||||||||||||V123456";

    private static final String ADT_A02_MESSAGE =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115120000||ADT^A02|123457|P|2.5\r" +
        "EVN|A02|20240115120000\r" +
        "PID|1||12345^^^MRN||DOE^JOHN^A||19800115|M\r" +
        "PV1|1|I|WARD_B^202^A|E|||1234^SMITH^JAMES";

    private static final String ADT_A03_MESSAGE =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115180000||ADT^A03|123458|P|2.5\r" +
        "EVN|A03|20240115180000\r" +
        "PID|1||12345^^^MRN||DOE^JOHN^A||19800115|M\r" +
        "PV1|1|I|ICU^101^A|E|||1234^SMITH^JAMES||||||||||||20240115180000";

    private static final String ADT_A08_MESSAGE =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115120000||ADT^A08|123459|P|2.5\r" +
        "EVN|A08|20240115120000\r" +
        "PID|1||12345^^^MRN||DOE^JOHN^ANTHONY||19800115|M|||456 OAK AVE^^CAMBRIDGE^MA^02139||(617)555-1234";

    private static final String ORU_R01_MESSAGE =
        "MSH|^~\\&|LAB|FACILITY|EMR|FACILITY|20240115120000||ORU^R01|789012|P|2.5\r" +
        "PID|1||12345^^^MRN||DOE^JOHN\r" +
        "OBR|1|ORD123|RES123|80048^BMP|||20240115080000\r" +
        "OBX|1|NM|2345-7^Glucose||95|mg/dL|70-100|N|||F\r" +
        "OBX|2|NM|2160-0^Creatinine||1.1|mg/dL|0.7-1.3|N|||F";

    private static final String ORM_O01_MESSAGE =
        "MSH|^~\\&|ORDERING_APP|FACILITY|LAB|FACILITY|20240115120000||ORM^O01|456789|P|2.5\r" +
        "PID|1||12345^^^MRN||DOE^JOHN\r" +
        "ORC|NW|ORD456|||||^^^20240115120000\r" +
        "OBR|1|ORD456||80048^BMP^L|||20240115120000";

    private static final String MALFORMED_MESSAGE =
        "MSH|^~\\&|INCOMPLETE";

    private static final String MISSING_PID_MESSAGE =
        "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20240115120000||ADT^A01|123460|P|2.5\r" +
        "EVN|A01|20240115120000";

    @BeforeEach
    void setUp() {
        parserService = new Hl7v2ParserService();
    }

    @Test
    @DisplayName("Should parse ADT^A01 admit message successfully")
    void testParseAdtA01AdmitMessage() {
        // When
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // Then
        assertNotNull(message);
        assertTrue(message instanceof ADT_A01);

        ADT_A01 adtMessage = (ADT_A01) message;
        assertEquals("ADT", adtMessage.getMSH().getMessageType().getMessageCode().getValue());
        assertEquals("A01", adtMessage.getMSH().getMessageType().getTriggerEvent().getValue());
    }

    @Test
    @DisplayName("Should parse ADT^A02 transfer message successfully")
    void testParseAdtA02TransferMessage() {
        // When
        Message message = parserService.parse(ADT_A02_MESSAGE);

        // Then
        assertNotNull(message);
        assertTrue(message instanceof ADT_A01); // ADT_A02 uses same structure as A01

        ADT_A01 adtMessage = (ADT_A01) message;
        assertEquals("A02", adtMessage.getMSH().getMessageType().getTriggerEvent().getValue());
    }

    @Test
    @DisplayName("Should parse ADT^A03 discharge message successfully")
    void testParseAdtA03DischargeMessage() {
        // When
        Message message = parserService.parse(ADT_A03_MESSAGE);

        // Then
        assertNotNull(message);
        assertTrue(message instanceof ADT_A01);

        ADT_A01 adtMessage = (ADT_A01) message;
        assertEquals("A03", adtMessage.getMSH().getMessageType().getTriggerEvent().getValue());
    }

    @Test
    @DisplayName("Should parse ADT^A08 update patient message successfully")
    void testParseAdtA08UpdateMessage() {
        // When
        Message message = parserService.parse(ADT_A08_MESSAGE);

        // Then
        assertNotNull(message);
        assertTrue(message instanceof ADT_A01);

        ADT_A01 adtMessage = (ADT_A01) message;
        assertEquals("A08", adtMessage.getMSH().getMessageType().getTriggerEvent().getValue());
    }

    @Test
    @DisplayName("Should parse ORU^R01 lab result message successfully")
    void testParseOruR01LabResultMessage() {
        // When
        Message message = parserService.parse(ORU_R01_MESSAGE);

        // Then
        assertNotNull(message);
        assertTrue(message instanceof ORU_R01);

        ORU_R01 oruMessage = (ORU_R01) message;
        assertEquals("ORU", oruMessage.getMSH().getMessageType().getMessageCode().getValue());
        assertEquals("R01", oruMessage.getMSH().getMessageType().getTriggerEvent().getValue());
    }

    @Test
    @DisplayName("Should parse ORM^O01 order message successfully")
    void testParseOrmO01OrderMessage() {
        // When
        Message message = parserService.parse(ORM_O01_MESSAGE);

        // Then
        assertNotNull(message);
        assertTrue(message instanceof ORM_O01);

        ORM_O01 ormMessage = (ORM_O01) message;
        assertEquals("ORM", ormMessage.getMSH().getMessageType().getMessageCode().getValue());
        assertEquals("O01", ormMessage.getMSH().getMessageType().getTriggerEvent().getValue());
    }

    @Test
    @DisplayName("Should handle malformed message gracefully")
    void testHandleMalformedMessage() {
        // When/Then
        assertThrows(Hl7ParsingException.class, () -> {
            parserService.parse(MALFORMED_MESSAGE);
        });
    }

    @Test
    @DisplayName("Should handle null message input")
    void testHandleNullMessage() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.parse(null);
        });
    }

    @Test
    @DisplayName("Should handle empty message input")
    void testHandleEmptyMessage() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.parse("");
        });
    }

    @Test
    @DisplayName("Should extract patient demographics from PID segment")
    void testExtractPatientDemographicsFromPid() {
        // Given
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // When
        PID pid = parserService.extractPidSegment(message);

        // Then
        assertNotNull(pid);
        assertEquals("12345", pid.getPatientIdentifierList(0).getIDNumber().getValue());
        assertEquals("DOE", pid.getPatientName(0).getFamilyName().getSurname().getValue());
        assertEquals("JOHN", pid.getPatientName(0).getGivenName().getValue());
        assertEquals("M", pid.getAdministrativeSex().getValue());
        assertEquals("19800115", pid.getDateTimeOfBirth().getTime().getValue());
    }

    @Test
    @DisplayName("Should extract visit information from PV1 segment")
    void testExtractVisitInformationFromPv1() {
        // Given
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // When
        PV1 pv1 = parserService.extractPv1Segment(message);

        // Then
        assertNotNull(pv1);
        assertEquals("I", pv1.getPatientClass().getValue());
        assertEquals("ICU", pv1.getAssignedPatientLocation().getPointOfCare().getValue());
        assertEquals("101", pv1.getAssignedPatientLocation().getRoom().getValue());
        assertEquals("A", pv1.getAssignedPatientLocation().getBed().getValue());
        assertEquals("E", pv1.getAdmissionType().getValue());
    }

    @Test
    @DisplayName("Should handle missing optional PV1 segment")
    void testHandleMissingPv1Segment() {
        // Given
        Message message = parserService.parse(MISSING_PID_MESSAGE);

        // When
        PV1 pv1 = parserService.extractPv1Segment(message);

        // Then
        // Should return null or empty PV1 for missing segment
        // Implementation should handle this gracefully
        assertTrue(pv1 == null || pv1.isEmpty());
    }

    @Test
    @DisplayName("Should extract multiple OBX segments from ORU message")
    void testExtractMultipleObxSegments() {
        // Given
        Message message = parserService.parse(ORU_R01_MESSAGE);

        // When
        List<OBX> obxSegments = parserService.extractObxSegments(message);

        // Then
        assertNotNull(obxSegments);
        assertEquals(2, obxSegments.size());

        // First OBX
        assertEquals("NM", obxSegments.get(0).getValueType().getValue());
        assertEquals("2345-7", obxSegments.get(0).getObservationIdentifier().getIdentifier().getValue());
        assertEquals("Glucose", obxSegments.get(0).getObservationIdentifier().getText().getValue());
        assertEquals("95", obxSegments.get(0).getObservationValue(0).getData().toString());

        // Second OBX
        assertEquals("2160-0", obxSegments.get(1).getObservationIdentifier().getIdentifier().getValue());
        assertEquals("Creatinine", obxSegments.get(1).getObservationIdentifier().getText().getValue());
    }

    @Test
    @DisplayName("Should validate message structure")
    void testValidateMessageStructure() {
        // Given
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // When
        boolean isValid = parserService.validateMessage(message);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should get message type from MSH segment")
    void testGetMessageType() {
        // Given
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // When
        String messageType = parserService.getMessageType(message);

        // Then
        assertEquals("ADT^A01", messageType);
    }

    @Test
    @DisplayName("Should get trigger event from message")
    void testGetTriggerEvent() {
        // Given
        Message message = parserService.parse(ADT_A03_MESSAGE);

        // When
        String triggerEvent = parserService.getTriggerEvent(message);

        // Then
        assertEquals("A03", triggerEvent);
    }

    @Test
    @DisplayName("Should parse message with extended patient name")
    void testParseExtendedPatientName() {
        // Given
        Message message = parserService.parse(ADT_A08_MESSAGE);

        // When
        PID pid = parserService.extractPidSegment(message);

        // Then
        assertNotNull(pid);
        assertEquals("DOE", pid.getPatientName(0).getFamilyName().getSurname().getValue());
        assertEquals("JOHN", pid.getPatientName(0).getGivenName().getValue());
        assertEquals("ANTHONY", pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().getValue());
    }

    @Test
    @DisplayName("Should parse message with patient address")
    void testParsePatientAddress() {
        // Given
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // When
        PID pid = parserService.extractPidSegment(message);

        // Then
        assertNotNull(pid);
        assertEquals("123 MAIN ST", pid.getPatientAddress(0).getStreetAddress().getStreetOrMailingAddress().getValue());
        assertEquals("BOSTON", pid.getPatientAddress(0).getCity().getValue());
        assertEquals("MA", pid.getPatientAddress(0).getStateOrProvince().getValue());
        assertEquals("02101", pid.getPatientAddress(0).getZipOrPostalCode().getValue());
    }

    @Test
    @DisplayName("Should parse message with patient phone number")
    void testParsePatientPhoneNumber() {
        // Given
        Message message = parserService.parse(ADT_A08_MESSAGE);

        // When
        PID pid = parserService.extractPidSegment(message);

        // Then
        assertNotNull(pid);
        assertEquals("(617)555-1234", pid.getPhoneNumberHome(0).getTelephoneNumber().getValue());
    }

    @Test
    @DisplayName("Should handle message with missing optional segments")
    void testHandleMissingOptionalSegments() {
        // Given
        String minimalMessage =
            "MSH|^~\\&|APP|FAC|APP2|FAC2|20240115120000||ADT^A01|123|P|2.5\r" +
            "EVN|A01|20240115120000\r" +
            "PID|1||12345^^^MRN||DOE^JOHN";

        // When
        Message message = parserService.parse(minimalMessage);

        // Then
        assertNotNull(message);
        assertTrue(message instanceof ADT_A01);
    }

    @Test
    @DisplayName("Should parse HL7 v2.5 version correctly")
    void testParseHl7Version() {
        // Given
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // When
        String version = parserService.getVersion(message);

        // Then
        assertEquals("2.5", version);
    }

    @Test
    @DisplayName("Should extract message control ID")
    void testExtractMessageControlId() {
        // Given
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // When
        String controlId = parserService.getMessageControlId(message);

        // Then
        assertEquals("123456", controlId);
    }

    @Test
    @DisplayName("Should parse message timestamp")
    void testParseMessageTimestamp() {
        // Given
        Message message = parserService.parse(ADT_A01_MESSAGE);

        // When
        String timestamp = parserService.getMessageTimestamp(message);

        // Then
        assertEquals("20240115120000", timestamp);
    }
}

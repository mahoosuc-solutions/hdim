package com.healthdata.cdr.parser;
import org.junit.jupiter.api.Tag;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.parser.Parser;
import com.healthdata.cdr.dto.Hl7v2Message;
import com.healthdata.cdr.handler.*;
import com.healthdata.cdr.service.Hl7v2ParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Hl7v2ParserService.
 * Tests HL7 v2 message parsing and routing to handlers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HL7 v2 Parser Service Tests")
@Tag("unit")
class Hl7v2ParserServiceTest {

    @InjectMocks
    private Hl7v2ParserService parserService;

    @Mock
    private Parser hl7v2Parser;

    @Mock
    private AdtMessageHandler adtMessageHandler;

    @Mock
    private OruMessageHandler oruMessageHandler;

    @Mock
    private OrmMessageHandler ormMessageHandler;

    @Mock
    private RdeMessageHandler rdeMessageHandler;

    @Mock
    private RasMessageHandler rasMessageHandler;

    @Mock
    private VxuMessageHandler vxuMessageHandler;

    @Mock
    private MdmMessageHandler mdmMessageHandler;

    @Mock
    private SiuMessageHandler siuMessageHandler;

    @Mock
    private BarMessageHandler barMessageHandler;

    @Mock
    private DftMessageHandler dftMessageHandler;

    @Mock
    private PprMessageHandler pprMessageHandler;

    private static final String TENANT_ID = "test-tenant";

    @Nested
    @DisplayName("ADT Message Parsing")
    class AdtMessageParsingTests {

        @Test
        @DisplayName("Should parse ADT^A01 and route to ADT handler")
        void parseMessage_withAdtA01_routesToAdtHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|SENDING|FAC|RECV|FAC|20240115120000||ADT^A01|123|P|2.5\rPID|1||12345||DOE^JOHN";
            Message mockMessage = createMockMessage("ADT", "A01", "123", "2.5");
            Map<String, Object> handlerResult = new HashMap<>();
            handlerResult.put("patient", Map.of("patientId", "12345"));

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(adtMessageHandler.handle(mockMessage)).thenReturn(handlerResult);

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getStatus()).isEqualTo("PARSED");
            assertThat(result.getMessageType()).isEqualTo("ADT");
            assertThat(result.getTriggerEvent()).isEqualTo("A01");
            assertThat(result.getMessageControlId()).isEqualTo("123");
            assertThat(result.getVersion()).isEqualTo("2.5");
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            verify(adtMessageHandler).handle(mockMessage);
        }

        @Test
        @DisplayName("Should extract message code from ADT message")
        void parseMessage_withAdtA01_extractsMessageCode() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|SENDING|FAC|RECV|FAC|20240115120000||ADT^A01|456|P|2.5";
            Message mockMessage = createMockMessage("ADT", "A01", "456", "2.5");
            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(adtMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageCode()).isEqualTo("ADT^A01");
        }
    }

    @Nested
    @DisplayName("ORU Message Parsing")
    class OruMessageParsingTests {

        @Test
        @DisplayName("Should parse ORU^R01 and route to ORU handler")
        void parseMessage_withOruR01_routesToOruHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|LAB|FAC|EMR|FAC|20240115120000||ORU^R01|789|P|2.5";
            Message mockMessage = createMockMessage("ORU", "R01", "789", "2.5");
            Map<String, Object> handlerResult = new HashMap<>();
            handlerResult.put("observations", java.util.List.of());

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(oruMessageHandler.handle(mockMessage)).thenReturn(handlerResult);

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("ORU");
            assertThat(result.getTriggerEvent()).isEqualTo("R01");
            verify(oruMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("ORM Message Parsing")
    class OrmMessageParsingTests {

        @Test
        @DisplayName("Should parse ORM^O01 and route to ORM handler")
        void parseMessage_withOrmO01_routesToOrmHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|APP|FAC|LAB|FAC|20240115120000||ORM^O01|111|P|2.5";
            Message mockMessage = createMockMessage("ORM", "O01", "111", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(ormMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("ORM");
            verify(ormMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("RDE Message Parsing")
    class RdeMessageParsingTests {

        @Test
        @DisplayName("Should parse RDE^O11 and route to RDE handler")
        void parseMessage_withRdeO11_routesToRdeHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|RX|FAC|EMR|FAC|20240115120000||RDE^O11|222|P|2.5";
            Message mockMessage = createMockMessage("RDE", "O11", "222", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(rdeMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("RDE");
            verify(rdeMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("RAS Message Parsing")
    class RasMessageParsingTests {

        @Test
        @DisplayName("Should parse RAS^O17 and route to RAS handler")
        void parseMessage_withRasO17_routesToRasHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|RX|FAC|EMR|FAC|20240115120000||RAS^O17|333|P|2.5";
            Message mockMessage = createMockMessage("RAS", "O17", "333", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(rasMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("RAS");
            verify(rasMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("VXU Message Parsing")
    class VxuMessageParsingTests {

        @Test
        @DisplayName("Should parse VXU^V04 and route to VXU handler")
        void parseMessage_withVxuV04_routesToVxuHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|IMM|FAC|EMR|FAC|20240115120000||VXU^V04|444|P|2.5";
            Message mockMessage = createMockMessage("VXU", "V04", "444", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(vxuMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("VXU");
            verify(vxuMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("MDM Message Parsing")
    class MdmMessageParsingTests {

        @Test
        @DisplayName("Should parse MDM^T01 and route to MDM handler")
        void parseMessage_withMdmT01_routesToMdmHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|DOC|FAC|EMR|FAC|20240115120000||MDM^T01|555|P|2.5";
            Message mockMessage = createMockMessage("MDM", "T01", "555", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(mdmMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("MDM");
            verify(mdmMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("SIU Message Parsing")
    class SiuMessageParsingTests {

        @Test
        @DisplayName("Should parse SIU^S12 and route to SIU handler")
        void parseMessage_withSiuS12_routesToSiuHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|SCH|FAC|EMR|FAC|20240115120000||SIU^S12|666|P|2.5";
            Message mockMessage = createMockMessage("SIU", "S12", "666", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(siuMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("SIU");
            verify(siuMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("BAR Message Parsing")
    class BarMessageParsingTests {

        @Test
        @DisplayName("Should parse BAR^P01 and route to BAR handler")
        void parseMessage_withBarP01_routesToBarHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|FIN|FAC|EMR|FAC|20240115120000||BAR^P01|777|P|2.5";
            Message mockMessage = createMockMessage("BAR", "P01", "777", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(barMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("BAR");
            verify(barMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("DFT Message Parsing")
    class DftMessageParsingTests {

        @Test
        @DisplayName("Should parse DFT^P03 and route to DFT handler")
        void parseMessage_withDftP03_routesToDftHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|FIN|FAC|EMR|FAC|20240115120000||DFT^P03|888|P|2.5";
            Message mockMessage = createMockMessage("DFT", "P03", "888", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(dftMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("DFT");
            verify(dftMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("PPR Message Parsing")
    class PprMessageParsingTests {

        @Test
        @DisplayName("Should parse PPR^PC1 and route to PPR handler")
        void parseMessage_withPprPc1_routesToPprHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|APP|FAC|EMR|FAC|20240115120000||PPR^PC1|999|P|2.5";
            Message mockMessage = createMockMessage("PPR", "PC1", "999", "2.5");

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(pprMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("PPR");
            verify(pprMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return error status when parsing fails")
        void parseMessage_withInvalidMessage_returnsErrorStatus() throws HL7Exception {
            String rawMessage = "INVALID MESSAGE";
            when(hl7v2Parser.parse(rawMessage)).thenThrow(new HL7Exception("Parse error"));

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getStatus()).isEqualTo("ERROR");
            assertThat(result.getErrorMessage()).contains("Parse error");
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should handle unsupported message type")
        void parseMessage_withUnsupportedType_logsWarning() throws HL7Exception {
            // Test with a real but unsupported HL7 message type (QBP - Query by Parameter)
            // QBP is a valid HL7 message type but not currently supported by our handlers
            String rawMessage = "MSH|^~\\&|APP|FAC|EMR|FAC|20240115120000||QBP^Q11|000|P|2.5";
            Message mockMessage = createMockMessage("QBP", "Q11", "000", "2.5");
            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getStatus()).isEqualTo("PARSED");
            assertThat(result.getMessageType()).isEqualTo("QBP");
            assertThat(result.getParsedData()).containsKey("warning");
        }
    }

    @Nested
    @DisplayName("Message Validation")
    class MessageValidationTests {

        @Test
        @DisplayName("Should validate null message")
        void validateMessage_withNull_returnsFalse() {
            assertThat(parserService.validateMessage(null)).isFalse();
        }

        @Test
        @DisplayName("Should validate empty message")
        void validateMessage_withEmpty_returnsFalse() {
            assertThat(parserService.validateMessage("")).isFalse();
        }

        @Test
        @DisplayName("Should validate message without MSH")
        void validateMessage_withoutMsh_returnsFalse() {
            assertThat(parserService.validateMessage("PID|1||12345")).isFalse();
        }

        @Test
        @DisplayName("Should validate valid message")
        void validateMessage_withValidMessage_returnsTrue() {
            String validMessage = "MSH|^~\\&|APP|FAC|EMR|FAC|20240115120000||ADT^A01|123|P|2.5";
            assertThat(parserService.validateMessage(validMessage)).isTrue();
        }
    }

    // Helper method to create mock HL7 message
    private Message createMockMessage(String messageType, String triggerEvent, String controlId, String version) throws HL7Exception {
        Message message = mock(Message.class);
        MSH msh = mock(MSH.class);

        // Message type
        ca.uhn.hl7v2.model.v25.datatype.MSG msgType = mock(ca.uhn.hl7v2.model.v25.datatype.MSG.class);
        ca.uhn.hl7v2.model.v25.datatype.ID msgCode = mock(ca.uhn.hl7v2.model.v25.datatype.ID.class);
        ca.uhn.hl7v2.model.v25.datatype.ID trigger = mock(ca.uhn.hl7v2.model.v25.datatype.ID.class);
        when(msgCode.getValue()).thenReturn(messageType);
        when(trigger.getValue()).thenReturn(triggerEvent);
        when(msgType.getMessageCode()).thenReturn(msgCode);
        when(msgType.getTriggerEvent()).thenReturn(trigger);
        when(msh.getMessageType()).thenReturn(msgType);

        // Control ID
        ca.uhn.hl7v2.model.v25.datatype.ST controlIdSt = mock(ca.uhn.hl7v2.model.v25.datatype.ST.class);
        when(controlIdSt.getValue()).thenReturn(controlId);
        when(msh.getMessageControlID()).thenReturn(controlIdSt);

        // Sending/Receiving apps
        ca.uhn.hl7v2.model.v25.datatype.HD sendingApp = mock(ca.uhn.hl7v2.model.v25.datatype.HD.class);
        ca.uhn.hl7v2.model.v25.datatype.IS sendingAppName = mock(ca.uhn.hl7v2.model.v25.datatype.IS.class);
        when(sendingAppName.getValue()).thenReturn("SENDING_APP");
        when(sendingApp.getNamespaceID()).thenReturn(sendingAppName);
        when(msh.getSendingApplication()).thenReturn(sendingApp);

        ca.uhn.hl7v2.model.v25.datatype.HD sendingFac = mock(ca.uhn.hl7v2.model.v25.datatype.HD.class);
        ca.uhn.hl7v2.model.v25.datatype.IS sendingFacName = mock(ca.uhn.hl7v2.model.v25.datatype.IS.class);
        when(sendingFacName.getValue()).thenReturn("SENDING_FAC");
        when(sendingFac.getNamespaceID()).thenReturn(sendingFacName);
        when(msh.getSendingFacility()).thenReturn(sendingFac);

        ca.uhn.hl7v2.model.v25.datatype.HD recvApp = mock(ca.uhn.hl7v2.model.v25.datatype.HD.class);
        ca.uhn.hl7v2.model.v25.datatype.IS recvAppName = mock(ca.uhn.hl7v2.model.v25.datatype.IS.class);
        when(recvAppName.getValue()).thenReturn("RECV_APP");
        when(recvApp.getNamespaceID()).thenReturn(recvAppName);
        when(msh.getReceivingApplication()).thenReturn(recvApp);

        ca.uhn.hl7v2.model.v25.datatype.HD recvFac = mock(ca.uhn.hl7v2.model.v25.datatype.HD.class);
        ca.uhn.hl7v2.model.v25.datatype.IS recvFacName = mock(ca.uhn.hl7v2.model.v25.datatype.IS.class);
        when(recvFacName.getValue()).thenReturn("RECV_FAC");
        when(recvFac.getNamespaceID()).thenReturn(recvFacName);
        when(msh.getReceivingFacility()).thenReturn(recvFac);

        // Version
        ca.uhn.hl7v2.model.v25.datatype.VID versionId = mock(ca.uhn.hl7v2.model.v25.datatype.VID.class);
        ca.uhn.hl7v2.model.v25.datatype.ID versionIdVal = mock(ca.uhn.hl7v2.model.v25.datatype.ID.class);
        when(versionIdVal.getValue()).thenReturn(version);
        when(versionId.getVersionID()).thenReturn(versionIdVal);
        when(msh.getVersionID()).thenReturn(versionId);

        // Date/Time
        ca.uhn.hl7v2.model.v25.datatype.TS dateTime = mock(ca.uhn.hl7v2.model.v25.datatype.TS.class);
        ca.uhn.hl7v2.model.v25.datatype.DTM time = mock(ca.uhn.hl7v2.model.v25.datatype.DTM.class);
        when(time.getValue()).thenReturn("20240115120000");
        when(dateTime.getTime()).thenReturn(time);
        when(msh.getDateTimeOfMessage()).thenReturn(dateTime);

        when(message.get("MSH")).thenReturn(msh);

        return message;
    }
}

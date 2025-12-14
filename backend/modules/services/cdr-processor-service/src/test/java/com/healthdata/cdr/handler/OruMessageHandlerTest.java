package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.PID;
import com.healthdata.cdr.service.Hl7v2ParserService;
import com.healthdata.cdr.converter.Hl7ToFhirConverter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for ORU Message Handler
 * Tests handling of ORU (lab result) messages and conversion to FHIR resources
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ORU Message Handler Tests")
class OruMessageHandlerTest {

    @InjectMocks
    private OruMessageHandler oruMessageHandler;

    @Mock
    private Hl7v2ParserService parserService;

    @Mock
    private Hl7ToFhirConverter fhirConverter;

    private static final String ORU_R01_SINGLE_OBX =
        "MSH|^~\\&|LAB|FACILITY|EMR|FACILITY|20240115120000||ORU^R01|789012|P|2.5\r" +
        "PID|1||12345^^^MRN||DOE^JOHN\r" +
        "OBR|1|ORD123|RES123|80048^BMP|||20240115080000\r" +
        "OBX|1|NM|2345-7^Glucose||95|mg/dL|70-100|N|||F";

    private static final String ORU_R01_MULTIPLE_OBX =
        "MSH|^~\\&|LAB|FACILITY|EMR|FACILITY|20240115120000||ORU^R01|789012|P|2.5\r" +
        "PID|1||12345^^^MRN||DOE^JOHN\r" +
        "OBR|1|ORD123|RES123|80048^BMP|||20240115080000\r" +
        "OBX|1|NM|2345-7^Glucose||95|mg/dL|70-100|N|||F\r" +
        "OBX|2|NM|2160-0^Creatinine||1.1|mg/dL|0.7-1.3|N|||F\r" +
        "OBX|3|NM|2951-2^Sodium||140|mmol/L|136-145|N|||F\r" +
        "OBX|4|NM|2823-3^Potassium||4.0|mmol/L|3.5-5.1|N|||F";

    private static final String ORU_R01_TEXT_RESULT =
        "MSH|^~\\&|LAB|FACILITY|EMR|FACILITY|20240115120000||ORU^R01|789013|P|2.5\r" +
        "PID|1||12345^^^MRN||DOE^JOHN\r" +
        "OBR|1|ORD124|RES124|11502-2^Culture|||20240115080000\r" +
        "OBX|1|TX|11502-2^Organism||Escherichia coli||||F|||F";

    private static final String ORU_R01_ABNORMAL_RESULT =
        "MSH|^~\\&|LAB|FACILITY|EMR|FACILITY|20240115120000||ORU^R01|789014|P|2.5\r" +
        "PID|1||12345^^^MRN||DOE^JOHN\r" +
        "OBR|1|ORD125|RES125|80048^BMP|||20240115080000\r" +
        "OBX|1|NM|2345-7^Glucose||180|mg/dL|70-100|H|||F";

    private static final String ORU_R01_PRELIMINARY =
        "MSH|^~\\&|LAB|FACILITY|EMR|FACILITY|20240115120000||ORU^R01|789015|P|2.5\r" +
        "PID|1||12345^^^MRN||DOE^JOHN\r" +
        "OBR|1|ORD126|RES126|80048^BMP|||20240115080000\r" +
        "OBX|1|NM|2345-7^Glucose||95|mg/dL|70-100|N|||P";

    private static final String ORU_R01_CORRECTED =
        "MSH|^~\\&|LAB|FACILITY|EMR|FACILITY|20240115120000||ORU^R01|789016|P|2.5\r" +
        "PID|1||12345^^^MRN||DOE^JOHN\r" +
        "OBR|1|ORD127|RES127|80048^BMP|||20240115080000\r" +
        "OBX|1|NM|2345-7^Glucose||98|mg/dL|70-100|N|||C";

    @BeforeEach
    void setUp() {
        oruMessageHandler = new OruMessageHandler(parserService, fhirConverter);
    }

    @Test
    @DisplayName("Should create Observation resource from single OBX segment")
    void testCreateObservationFromSingleObx() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);
        Observation observation = new Observation();
        observation.setId("OBS-001");

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.convertObxToObservation(obx)).thenReturn(observation);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_SINGLE_OBX);

        // Then
        assertNotNull(observations);
        assertEquals(1, observations.size());
        assertEquals("OBS-001", observations.get(0).getId());
        verify(fhirConverter).convertObxToObservation(obx);
    }

    @Test
    @DisplayName("Should handle multiple OBX segments")
    void testHandleMultipleObxSegments() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx1 = mock(OBX.class);
        OBX obx2 = mock(OBX.class);
        OBX obx3 = mock(OBX.class);
        OBX obx4 = mock(OBX.class);

        Observation obs1 = new Observation();
        obs1.setId("OBS-001");
        Observation obs2 = new Observation();
        obs2.setId("OBS-002");
        Observation obs3 = new Observation();
        obs3.setId("OBS-003");
        Observation obs4 = new Observation();
        obs4.setId("OBS-004");

        when(parserService.parse(ORU_R01_MULTIPLE_OBX)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx1, obx2, obx3, obx4));
        when(fhirConverter.convertObxToObservation(obx1)).thenReturn(obs1);
        when(fhirConverter.convertObxToObservation(obx2)).thenReturn(obs2);
        when(fhirConverter.convertObxToObservation(obx3)).thenReturn(obs3);
        when(fhirConverter.convertObxToObservation(obx4)).thenReturn(obs4);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_MULTIPLE_OBX);

        // Then
        assertNotNull(observations);
        assertEquals(4, observations.size());
        verify(fhirConverter, times(4)).convertObxToObservation(any(OBX.class));
    }

    @Test
    @DisplayName("Should parse numeric result correctly")
    void testParseNumericResult() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);
        Observation observation = new Observation();
        Quantity quantity = new Quantity();
        quantity.setValue(95);
        quantity.setUnit("mg/dL");
        observation.setValue(quantity);

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.convertObxToObservation(obx)).thenReturn(observation);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_SINGLE_OBX);

        // Then
        assertNotNull(observations);
        assertEquals(1, observations.size());
        assertTrue(observations.get(0).getValue() instanceof Quantity);
        Quantity result = (Quantity) observations.get(0).getValue();
        assertEquals(95, result.getValue().doubleValue());
        assertEquals("mg/dL", result.getUnit());
    }

    @Test
    @DisplayName("Should parse text result correctly")
    void testParseTextResult() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);
        Observation observation = new Observation();
        observation.setValue(new StringType("Escherichia coli"));

        when(parserService.parse(ORU_R01_TEXT_RESULT)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.convertObxToObservation(obx)).thenReturn(observation);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_TEXT_RESULT);

        // Then
        assertNotNull(observations);
        assertEquals(1, observations.size());
        assertTrue(observations.get(0).getValue() instanceof StringType);
        StringType result = (StringType) observations.get(0).getValue();
        assertEquals("Escherichia coli", result.getValue());
    }

    @Test
    @DisplayName("Should handle result status code - Final")
    void testHandleResultStatusFinal() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);
        Observation observation = new Observation();
        observation.setStatus(ObservationStatus.FINAL);

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.convertObxToObservation(obx)).thenReturn(observation);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_SINGLE_OBX);

        // Then
        assertEquals(ObservationStatus.FINAL, observations.get(0).getStatus());
    }

    @Test
    @DisplayName("Should handle result status code - Preliminary")
    void testHandleResultStatusPreliminary() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);
        Observation observation = new Observation();
        observation.setStatus(ObservationStatus.PRELIMINARY);

        when(parserService.parse(ORU_R01_PRELIMINARY)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.convertObxToObservation(obx)).thenReturn(observation);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_PRELIMINARY);

        // Then
        assertEquals(ObservationStatus.PRELIMINARY, observations.get(0).getStatus());
    }

    @Test
    @DisplayName("Should handle result status code - Corrected")
    void testHandleResultStatusCorrected() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);
        Observation observation = new Observation();
        observation.setStatus(ObservationStatus.CORRECTED);

        when(parserService.parse(ORU_R01_CORRECTED)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.convertObxToObservation(obx)).thenReturn(observation);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_CORRECTED);

        // Then
        assertEquals(ObservationStatus.CORRECTED, observations.get(0).getStatus());
    }

    @Test
    @DisplayName("Should create DiagnosticReport from ORU message")
    void testCreateDiagnosticReport() {
        // Given
        Message message = mock(ORU_R01.class);
        OBR obr = mock(OBR.class);
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId("DR-001");

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractObrSegment(message)).thenReturn(obr);
        when(fhirConverter.convertObrToDiagnosticReport(obr)).thenReturn(diagnosticReport);

        // When
        DiagnosticReport result = oruMessageHandler.createDiagnosticReport(ORU_R01_SINGLE_OBX);

        // Then
        assertNotNull(result);
        assertEquals("DR-001", result.getId());
        verify(fhirConverter).convertObrToDiagnosticReport(obr);
    }

    @Test
    @DisplayName("Should link Observations to DiagnosticReport")
    void testLinkObservationsToDiagnosticReport() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx1 = mock(OBX.class);
        OBX obx2 = mock(OBX.class);
        OBR obr = mock(OBR.class);

        Observation obs1 = new Observation();
        obs1.setId("OBS-001");
        Observation obs2 = new Observation();
        obs2.setId("OBS-002");

        DiagnosticReport report = new DiagnosticReport();
        report.setId("DR-001");

        when(parserService.parse(ORU_R01_MULTIPLE_OBX)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx1, obx2));
        when(parserService.extractObrSegment(message)).thenReturn(obr);
        when(fhirConverter.convertObxToObservation(obx1)).thenReturn(obs1);
        when(fhirConverter.convertObxToObservation(obx2)).thenReturn(obs2);
        when(fhirConverter.convertObrToDiagnosticReport(obr)).thenReturn(report);

        // When
        DiagnosticReport result = oruMessageHandler.processCompleteLabOrder(ORU_R01_MULTIPLE_OBX);

        // Then
        assertNotNull(result);
        verify(fhirConverter).linkObservationsToDiagnosticReport(eq(report), anyList());
    }

    @Test
    @DisplayName("Should handle abnormal result flag")
    void testHandleAbnormalResultFlag() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);
        Observation observation = new Observation();
        CodeableConcept interpretation = new CodeableConcept();
        interpretation.setText("High");
        observation.addInterpretation(interpretation);

        when(parserService.parse(ORU_R01_ABNORMAL_RESULT)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.convertObxToObservation(obx)).thenReturn(observation);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_ABNORMAL_RESULT);

        // Then
        assertNotNull(observations);
        assertFalse(observations.get(0).getInterpretation().isEmpty());
    }

    @Test
    @DisplayName("Should extract observation identifier")
    void testExtractObservationIdentifier() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.extractObservationCode(obx)).thenReturn("2345-7");

        // When
        String code = oruMessageHandler.extractFirstObservationCode(ORU_R01_SINGLE_OBX);

        // Then
        assertEquals("2345-7", code);
    }

    @Test
    @DisplayName("Should validate ORU message type")
    void testValidateOruMessageType() {
        // Given
        Message message = mock(ORU_R01.class);

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.getMessageType(message)).thenReturn("ORU^R01");

        // When
        boolean isValid = oruMessageHandler.isValidOruMessage(ORU_R01_SINGLE_OBX);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject non-ORU message")
    void testRejectNonOruMessage() {
        // Given
        String adtMessage = "MSH|^~\\&|APP|FAC|APP2|FAC2|20240115120000||ADT^A01|123|P|2.5";
        Message message = mock(Message.class);

        when(parserService.parse(adtMessage)).thenReturn(message);
        when(parserService.getMessageType(message)).thenReturn("ADT^A01");

        // When
        boolean isValid = oruMessageHandler.isValidOruMessage(adtMessage);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty OBX segments")
    void testHandleEmptyObxSegments() {
        // Given
        Message message = mock(ORU_R01.class);

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList());

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_SINGLE_OBX);

        // Then
        assertNotNull(observations);
        assertTrue(observations.isEmpty());
    }

    @Test
    @DisplayName("Should extract patient reference from ORU message")
    void testExtractPatientReference() {
        // Given
        Message message = mock(ORU_R01.class);
        PID pid = mock(PID.class);

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractPidSegment(message)).thenReturn(pid);
        when(fhirConverter.extractPatientIdentifier(pid)).thenReturn("12345");

        // When
        String patientId = oruMessageHandler.extractPatientId(ORU_R01_SINGLE_OBX);

        // Then
        assertEquals("12345", patientId);
    }

    @Test
    @DisplayName("Should extract order number from OBR")
    void testExtractOrderNumber() {
        // Given
        Message message = mock(ORU_R01.class);
        OBR obr = mock(OBR.class);

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractObrSegment(message)).thenReturn(obr);
        when(fhirConverter.extractOrderNumber(obr)).thenReturn("ORD123");

        // When
        String orderNumber = oruMessageHandler.extractOrderNumber(ORU_R01_SINGLE_OBX);

        // Then
        assertEquals("ORD123", orderNumber);
    }

    @Test
    @DisplayName("Should handle observation with reference range")
    void testHandleObservationWithReferenceRange() {
        // Given
        Message message = mock(ORU_R01.class);
        OBX obx = mock(OBX.class);
        Observation observation = new Observation();
        Observation.ObservationReferenceRangeComponent referenceRange =
            new Observation.ObservationReferenceRangeComponent();
        referenceRange.setText("70-100");
        observation.addReferenceRange(referenceRange);

        when(parserService.parse(ORU_R01_SINGLE_OBX)).thenReturn(message);
        when(parserService.extractObxSegments(message)).thenReturn(Arrays.asList(obx));
        when(fhirConverter.convertObxToObservation(obx)).thenReturn(observation);

        // When
        List<Observation> observations = oruMessageHandler.processLabResults(ORU_R01_SINGLE_OBX);

        // Then
        assertNotNull(observations);
        assertFalse(observations.get(0).getReferenceRange().isEmpty());
        assertEquals("70-100", observations.get(0).getReferenceRange().get(0).getText());
    }
}

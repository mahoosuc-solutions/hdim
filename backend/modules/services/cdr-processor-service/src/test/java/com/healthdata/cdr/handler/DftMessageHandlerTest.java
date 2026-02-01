package com.healthdata.cdr.handler;
import org.junit.jupiter.api.Tag;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.DFT_P03;
import ca.uhn.hl7v2.model.v25.message.DFT_P11;
import ca.uhn.hl7v2.model.v25.segment.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DftMessageHandler.
 * Tests DFT (Detailed Financial Transaction) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DFT Message Handler Tests")
@Tag("unit")
class DftMessageHandlerTest {

    @InjectMocks
    private DftMessageHandler handler;

    @Nested
    @DisplayName("DFT^P03 - Post Detail Financial Transaction")
    class DftP03Tests {

        @Test
        @DisplayName("Should extract trigger event P03 and event description")
        void handle_withDftP03_extractsTriggerEvent() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(null);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getFINANCIALReps()).thenReturn(0);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsEntry("messageType", "DFT");
            assertThat(result).containsEntry("triggerEvent", "P03");
            assertThat(result).containsEntry("eventDescription", "Post detail financial transaction");
        }

        @Test
        @DisplayName("Should extract patient data from PID segment")
        void handle_withDftP03_extractsPatientData() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            PID pid = createMockPidWithBasicData();

            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(pid);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getFINANCIALReps()).thenReturn(0);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
            assertThat(patient).containsEntry("familyName", "Smith");
        }

        @Test
        @DisplayName("Should extract visit data from PV1 segment")
        void handle_withDftP03_extractsVisitData() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            PV1 pv1 = createMockPv1WithBasicData();

            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(null);
            when(dft.getPV1()).thenReturn(pv1);
            when(dft.getFINANCIALReps()).thenReturn(0);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("visit");
            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit).containsEntry("visitNumber", "V001");
            assertThat(visit).containsEntry("patientClass", "I");
        }

        @Test
        @DisplayName("Should extract event data from EVN segment")
        void handle_withDftP03_extractsEventData() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            EVN evn = createMockEvn("P03", "202401151200");

            when(dft.getEVN()).thenReturn(evn);
            when(dft.getPID()).thenReturn(null);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getFINANCIALReps()).thenReturn(0);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("event");
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) result.get("event");
            assertThat(event).containsEntry("eventTypeCode", "P03");
        }
    }

    @Nested
    @DisplayName("DFT^P11 - Post Detail Financial Transaction (New Format)")
    class DftP11Tests {

        @Test
        @DisplayName("Should extract trigger event P11 and event description")
        void handle_withDftP11_extractsTriggerEvent() throws HL7Exception {
            DFT_P11 dft = mock(DFT_P11.class);
            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(null);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);
            when(dft.getFINANCIALReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsEntry("messageType", "DFT");
            assertThat(result).containsEntry("triggerEvent", "P11");
            assertThat(result).containsEntry("eventDescription", "Post detail financial transaction (new format)");
        }

        @Test
        @DisplayName("Should extract patient data from PID segment")
        void handle_withDftP11_extractsPatientData() throws HL7Exception {
            DFT_P11 dft = mock(DFT_P11.class);
            PID pid = createMockPidWithBasicData();

            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(pid);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);
            when(dft.getFINANCIALReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
        }
    }

    @Nested
    @DisplayName("Unsupported Message Types")
    class UnsupportedMessageTests {

        @Test
        @DisplayName("Should handle unsupported DFT message type gracefully")
        void handle_withUnsupportedType_returnsBasicData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).containsEntry("messageType", "DFT");
            assertThat(result).doesNotContainKey("triggerEvent");
        }
    }

    // Helper methods
    private PID createMockPidWithBasicData() throws HL7Exception {
        PID pid = mock(PID.class);

        CX patientId = mock(CX.class);
        ST idNumber = mock(ST.class);
        when(idNumber.getValue()).thenReturn("12345");
        when(patientId.getIDNumber()).thenReturn(idNumber);

        ID idType = mock(ID.class);
        when(idType.getValue()).thenReturn("MR");
        when(patientId.getIdentifierTypeCode()).thenReturn(idType);
        when(pid.getPatientIdentifierList(0)).thenReturn(patientId);

        XPN patientName = mock(XPN.class);
        FN familyName = mock(FN.class);
        ST surname = mock(ST.class);
        when(surname.getValue()).thenReturn("Smith");
        when(familyName.getSurname()).thenReturn(surname);
        when(patientName.getFamilyName()).thenReturn(familyName);

        ST givenName = mock(ST.class);
        when(givenName.getValue()).thenReturn("John");
        when(patientName.getGivenName()).thenReturn(givenName);
        when(pid.getPatientName(0)).thenReturn(patientName);

        return pid;
    }

    private PV1 createMockPv1WithBasicData() throws HL7Exception {
        PV1 pv1 = mock(PV1.class);

        CX visitNumber = mock(CX.class);
        ST visitId = mock(ST.class);
        when(visitId.getValue()).thenReturn("V001");
        when(visitNumber.getIDNumber()).thenReturn(visitId);
        when(pv1.getVisitNumber()).thenReturn(visitNumber);

        IS patientClass = mock(IS.class);
        when(patientClass.getValue()).thenReturn("I");
        when(pv1.getPatientClass()).thenReturn(patientClass);

        IS hospitalService = mock(IS.class);
        when(hospitalService.getValue()).thenReturn("MED");
        when(pv1.getHospitalService()).thenReturn(hospitalService);

        return pv1;
    }

    private EVN createMockEvn(String eventType, String dateTime) throws HL7Exception {
        EVN evn = mock(EVN.class);

        ID eventTypeCode = mock(ID.class);
        when(eventTypeCode.getValue()).thenReturn(eventType);
        when(evn.getEventTypeCode()).thenReturn(eventTypeCode);

        TS recordedDateTime = mock(TS.class);
        DTM time = mock(DTM.class);
        when(time.getValue()).thenReturn(dateTime);
        when(recordedDateTime.getTime()).thenReturn(time);
        when(evn.getRecordedDateTime()).thenReturn(recordedDateTime);

        return evn;
    }
}

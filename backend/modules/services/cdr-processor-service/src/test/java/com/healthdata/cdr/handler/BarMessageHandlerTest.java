package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.BAR_P01;
import ca.uhn.hl7v2.model.v25.message.BAR_P02;
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
 * Unit tests for BarMessageHandler.
 * Tests BAR (Billing Account Record) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BAR Message Handler Tests")
class BarMessageHandlerTest {

    @InjectMocks
    private BarMessageHandler handler;

    @Nested
    @DisplayName("BAR^P01 - Add Patient Account")
    class BarP01Tests {

        @Test
        @DisplayName("Should extract trigger event P01 and event description")
        void handle_withBarP01_extractsTriggerEvent() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            when(bar.getEVN()).thenReturn(null);
            when(bar.getPID()).thenReturn(null);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsEntry("messageType", "BAR");
            assertThat(result).containsEntry("triggerEvent", "P01");
            assertThat(result).containsEntry("eventDescription", "Add patient account");
        }

        @Test
        @DisplayName("Should extract patient ID and name from PID segment")
        void handle_withBarP01_extractsPatientData() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            PID pid = createMockPidWithBasicData();

            when(bar.getEVN()).thenReturn(null);
            when(bar.getPID()).thenReturn(pid);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
            assertThat(patient).containsEntry("familyName", "Smith");
            assertThat(patient).containsEntry("givenName", "John");
        }

        @Test
        @DisplayName("Should extract account number from PID segment")
        void handle_withBarP01_extractsAccountNumber() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            PID pid = createMockPidWithAccountNumber();

            when(bar.getEVN()).thenReturn(null);
            when(bar.getPID()).thenReturn(pid);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("accountNumber", "ACC001");
        }

        @Test
        @DisplayName("Should extract SSN from PID segment")
        void handle_withBarP01_extractsSsn() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            PID pid = createMockPidWithSsn();

            when(bar.getEVN()).thenReturn(null);
            when(bar.getPID()).thenReturn(pid);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("ssn", "123-45-6789");
        }

        @Test
        @DisplayName("Should extract event data from EVN segment")
        void handle_withBarP01_extractsEventData() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            EVN evn = createMockEvn("P01", "202401151200");

            when(bar.getEVN()).thenReturn(evn);
            when(bar.getPID()).thenReturn(null);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("event");
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) result.get("event");
            assertThat(event).containsEntry("eventTypeCode", "P01");
            assertThat(event).containsEntry("recordedDateTime", "202401151200");
        }
    }

    @Nested
    @DisplayName("BAR^P02 - Purge Patient Accounts")
    class BarP02Tests {

        @Test
        @DisplayName("Should extract trigger event P02 and event description")
        void handle_withBarP02_extractsTriggerEvent() throws HL7Exception {
            BAR_P02 bar = mock(BAR_P02.class);
            when(bar.getEVN()).thenReturn(null);
            when(bar.getPATIENTReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsEntry("messageType", "BAR");
            assertThat(result).containsEntry("triggerEvent", "P02");
            assertThat(result).containsEntry("eventDescription", "Purge patient accounts");
        }

        @Test
        @DisplayName("Should extract event data from EVN segment")
        void handle_withBarP02_extractsEventData() throws HL7Exception {
            BAR_P02 bar = mock(BAR_P02.class);
            EVN evn = createMockEvn("P02", "202401151300");

            when(bar.getEVN()).thenReturn(evn);
            when(bar.getPATIENTReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("event");
        }
    }

    @Nested
    @DisplayName("Unsupported Message Types")
    class UnsupportedMessageTests {

        @Test
        @DisplayName("Should handle unsupported BAR message type gracefully")
        void handle_withUnsupportedType_returnsBasicData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).containsEntry("messageType", "BAR");
            assertThat(result).doesNotContainKey("triggerEvent");
        }
    }

    // Helper methods for creating mock objects
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

        TS dob = mock(TS.class);
        DTM dobTime = mock(DTM.class);
        when(dobTime.getValue()).thenReturn("19800101");
        when(dob.getTime()).thenReturn(dobTime);
        when(pid.getDateTimeOfBirth()).thenReturn(dob);

        return pid;
    }

    private PID createMockPidWithAccountNumber() throws HL7Exception {
        PID pid = createMockPidWithBasicData();

        CX accountNumber = mock(CX.class);
        ST accNum = mock(ST.class);
        when(accNum.getValue()).thenReturn("ACC001");
        when(accountNumber.getIDNumber()).thenReturn(accNum);
        when(pid.getPatientAccountNumber()).thenReturn(accountNumber);

        return pid;
    }

    private PID createMockPidWithSsn() throws HL7Exception {
        PID pid = createMockPidWithBasicData();

        ST ssn = mock(ST.class);
        when(ssn.getValue()).thenReturn("123-45-6789");
        when(pid.getSSNNumberPatient()).thenReturn(ssn);

        return pid;
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

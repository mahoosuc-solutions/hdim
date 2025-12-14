package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.SIU_S12;
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
 * Unit tests for SiuMessageHandler.
 * Tests SIU (Scheduling Information Unsolicited) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SIU Message Handler Tests")
class SiuMessageHandlerTest {

    @InjectMocks
    private SiuMessageHandler handler;

    @Nested
    @DisplayName("Trigger Event Tests")
    class TriggerEventTests {

        @Test
        @DisplayName("Should return SIU message type for non-SIU_S12 message")
        void handle_withNonSiuMessage_returnsBasicData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).containsEntry("messageType", "SIU");
            assertThat(result).doesNotContainKey("triggerEvent");
        }

        @Test
        @DisplayName("Should extract S12 trigger event for new appointment booking")
        void determineTriggerEvent_withS12_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S12");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("messageType", "SIU");
            assertThat(result).containsEntry("triggerEvent", "S12");
            assertThat(result).containsEntry("eventDescription", "Notification of new appointment booking");
        }

        @Test
        @DisplayName("Should extract S13 trigger event for appointment rescheduling")
        void determineTriggerEvent_withS13_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S13");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S13");
            assertThat(result).containsEntry("eventDescription", "Notification of appointment rescheduling");
        }

        @Test
        @DisplayName("Should extract S14 trigger event for appointment modification")
        void determineTriggerEvent_withS14_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S14");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S14");
            assertThat(result).containsEntry("eventDescription", "Notification of appointment modification");
        }

        @Test
        @DisplayName("Should extract S15 trigger event for appointment cancellation")
        void determineTriggerEvent_withS15_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S15");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S15");
            assertThat(result).containsEntry("eventDescription", "Notification of appointment cancellation");
        }

        @Test
        @DisplayName("Should extract S17 trigger event for appointment deletion")
        void determineTriggerEvent_withS17_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S17");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S17");
            assertThat(result).containsEntry("eventDescription", "Notification of appointment deletion");
        }

        @Test
        @DisplayName("Should extract S26 trigger event for patient no-show")
        void determineTriggerEvent_withS26_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S26");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S26");
            assertThat(result).containsEntry("eventDescription", "Notification of patient no-show");
        }

        @Test
        @DisplayName("Should handle unknown trigger event")
        void determineTriggerEvent_withUnknown_returnsGenericDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S99");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S99");
            assertThat(result).containsEntry("eventDescription", "Scheduling notification");
        }
    }

    @Nested
    @DisplayName("Scheduling Data Extraction")
    class SchedulingDataTests {

        @Test
        @DisplayName("Should extract placer and filler appointment IDs from SCH segment")
        void extractSchedulingData_withSchSegment_extractsAppointmentIds() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S12");
            SCH sch = setupSchWithAppointmentIds(siu);

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsKey("schedule");
            @SuppressWarnings("unchecked")
            Map<String, Object> schedule = (Map<String, Object>) result.get("schedule");
            assertThat(schedule).containsEntry("placerAppointmentId", "PLACER001");
            assertThat(schedule).containsEntry("fillerAppointmentId", "FILLER001");
        }

        @Test
        @DisplayName("Should extract appointment duration from SCH segment")
        void extractSchedulingData_withSchSegment_extractsDuration() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S12");
            setupSchWithDuration(siu);

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsKey("schedule");
            @SuppressWarnings("unchecked")
            Map<String, Object> schedule = (Map<String, Object>) result.get("schedule");
            assertThat(schedule).containsEntry("appointmentDuration", "30");
        }
    }

    @Nested
    @DisplayName("Patient Data Extraction")
    class PatientDataTests {

        @Test
        @DisplayName("Should extract patient ID and name from nested PID segment")
        void extractPatientData_withPidSegment_extractsPatientInfo() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupSiuWithPatientData(siu, "S12", "12345", "Smith", "John");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
            assertThat(patient).containsEntry("familyName", "Smith");
            assertThat(patient).containsEntry("givenName", "John");
        }
    }

    // Helper methods
    private void setupMinimalSiu(SIU_S12 siu, String triggerEvent) throws HL7Exception {
        // Setup MSH for trigger event
        MSH msh = mock(MSH.class);
        MSG messageType = mock(MSG.class);
        ID trigger = mock(ID.class);
        when(trigger.getValue()).thenReturn(triggerEvent);
        when(messageType.getTriggerEvent()).thenReturn(trigger);
        when(msh.getMessageType()).thenReturn(messageType);
        when(siu.getMSH()).thenReturn(msh);

        // Null SCH
        when(siu.getSCH()).thenReturn(null);

        // Null patient - using deep stubs handles this
        when(siu.getPATIENT().getPID()).thenReturn(null);
        when(siu.getPATIENT().getPV1()).thenReturn(null);

        // No resources
        when(siu.getRESOURCESReps()).thenReturn(0);
    }

    private SCH setupSchWithAppointmentIds(SIU_S12 siu) throws HL7Exception {
        SCH sch = mock(SCH.class);
        when(siu.getSCH()).thenReturn(sch);

        // Placer Appointment ID
        EI placerAppointmentId = mock(EI.class);
        ST placerEntity = mock(ST.class);
        when(placerEntity.getValue()).thenReturn("PLACER001");
        when(placerAppointmentId.getEntityIdentifier()).thenReturn(placerEntity);
        when(sch.getPlacerAppointmentID()).thenReturn(placerAppointmentId);

        // Filler Appointment ID
        EI fillerAppointmentId = mock(EI.class);
        ST fillerEntity = mock(ST.class);
        when(fillerEntity.getValue()).thenReturn("FILLER001");
        when(fillerAppointmentId.getEntityIdentifier()).thenReturn(fillerEntity);
        when(sch.getFillerAppointmentID()).thenReturn(fillerAppointmentId);

        // Null other fields
        when(sch.getOccurrenceNumber()).thenReturn(null);
        when(sch.getPlacerGroupNumber()).thenReturn(null);
        when(sch.getScheduleID()).thenReturn(null);
        when(sch.getEventReason()).thenReturn(null);
        when(sch.getAppointmentReason()).thenReturn(null);
        when(sch.getAppointmentType()).thenReturn(null);
        when(sch.getAppointmentDuration()).thenReturn(null);
        when(sch.getAppointmentDurationUnits()).thenReturn(null);
        when(sch.getAppointmentTimingQuantity(0)).thenReturn(null);
        when(sch.getEnteredByPerson(0)).thenReturn(null);
        when(sch.getEnteredByPhoneNumber(0)).thenReturn(null);
        when(sch.getFillerStatusCode()).thenReturn(null);

        return sch;
    }

    private void setupSchWithDuration(SIU_S12 siu) throws HL7Exception {
        SCH sch = mock(SCH.class);
        when(siu.getSCH()).thenReturn(sch);

        // Duration
        NM duration = mock(NM.class);
        when(duration.getValue()).thenReturn("30");
        when(sch.getAppointmentDuration()).thenReturn(duration);

        // Null other fields
        when(sch.getPlacerAppointmentID()).thenReturn(null);
        when(sch.getFillerAppointmentID()).thenReturn(null);
        when(sch.getOccurrenceNumber()).thenReturn(null);
        when(sch.getPlacerGroupNumber()).thenReturn(null);
        when(sch.getScheduleID()).thenReturn(null);
        when(sch.getEventReason()).thenReturn(null);
        when(sch.getAppointmentReason()).thenReturn(null);
        when(sch.getAppointmentType()).thenReturn(null);
        when(sch.getAppointmentDurationUnits()).thenReturn(null);
        when(sch.getAppointmentTimingQuantity(0)).thenReturn(null);
        when(sch.getEnteredByPerson(0)).thenReturn(null);
        when(sch.getEnteredByPhoneNumber(0)).thenReturn(null);
        when(sch.getFillerStatusCode()).thenReturn(null);
    }

    private void setupSiuWithPatientData(SIU_S12 siu, String triggerEvent, String patientId, String familyName, String givenName) throws HL7Exception {
        setupMinimalSiu(siu, triggerEvent);

        // Create PID mock
        PID pid = mock(PID.class);

        // Patient ID
        CX patientIdCx = mock(CX.class);
        ST idNumber = mock(ST.class);
        when(idNumber.getValue()).thenReturn(patientId);
        when(patientIdCx.getIDNumber()).thenReturn(idNumber);
        ID idType = mock(ID.class);
        when(idType.getValue()).thenReturn("MR");
        when(patientIdCx.getIdentifierTypeCode()).thenReturn(idType);
        when(pid.getPatientIdentifierList(0)).thenReturn(patientIdCx);

        // Patient Name
        XPN patientName = mock(XPN.class);
        FN fn = mock(FN.class);
        ST surname = mock(ST.class);
        when(surname.getValue()).thenReturn(familyName);
        when(fn.getSurname()).thenReturn(surname);
        when(patientName.getFamilyName()).thenReturn(fn);

        ST given = mock(ST.class);
        when(given.getValue()).thenReturn(givenName);
        when(patientName.getGivenName()).thenReturn(given);
        when(pid.getPatientName(0)).thenReturn(patientName);

        // DOB
        when(pid.getDateTimeOfBirth()).thenReturn(null);

        // Gender
        when(pid.getAdministrativeSex()).thenReturn(null);

        // Phone
        when(pid.getPhoneNumberHome(0)).thenReturn(null);

        // Wire up the PID through the deep stubs
        when(siu.getPATIENT().getPID()).thenReturn(pid);
    }
}

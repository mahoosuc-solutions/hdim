package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.SIU_S12;
import ca.uhn.hl7v2.model.v25.segment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SiuMessageHandler.
 * Tests extraction of scheduling data from HL7 v2 SIU messages.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SIU Message Handler Tests")
class SiuMessageHandlerTest {

    @InjectMocks
    private SiuMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SiuMessageHandler();
    }

    @Nested
    @DisplayName("SIU^S12 - New Appointment Booking")
    class SiuS12Tests {

        @Test
        @DisplayName("Should extract message type from SIU message")
        void handle_withValidSiuS12_extractsMessageType() throws HL7Exception {
            // Given
            SIU_S12 siu = mock(SIU_S12.class);
            SCH sch = mock(SCH.class);
            MSH msh = mock(MSH.class);
            MSG msgType = mock(MSG.class);
            ID triggerEvent = mock(ID.class);

            SIU_S12.PATIENT patient = mock(SIU_S12.PATIENT.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);

            when(siu.getSCH()).thenReturn(sch);
            when(siu.getMSH()).thenReturn(msh);
            when(msh.getMessageType()).thenReturn(msgType);
            when(msgType.getTriggerEvent()).thenReturn(triggerEvent);
            when(triggerEvent.getValue()).thenReturn("S12");
            when(siu.getPATIENT()).thenReturn(patient);
            when(patient.getPID()).thenReturn(pid);
            when(patient.getPV1()).thenReturn(pv1);
            when(siu.getRESOURCESReps()).thenReturn(0);

            mockMinimalSch(sch);
            mockMinimalPid(pid);
            mockMinimalPv1(pv1);

            // When
            Map<String, Object> result = handler.handle(siu);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("SIU");
            assertThat(result.get("triggerEvent")).isEqualTo("S12");
            assertThat(result.get("eventDescription")).isEqualTo("Notification of new appointment booking");
        }

        @Test
        @DisplayName("Should extract scheduling data from SCH segment")
        void handle_withValidSiuS12_extractsSchedulingData() throws HL7Exception {
            // Given
            SIU_S12 siu = mock(SIU_S12.class);
            SCH sch = mock(SCH.class);
            MSH msh = mock(MSH.class);
            MSG msgType = mock(MSG.class);
            ID triggerEvent = mock(ID.class);

            SIU_S12.PATIENT patient = mock(SIU_S12.PATIENT.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);

            when(siu.getSCH()).thenReturn(sch);
            when(siu.getMSH()).thenReturn(msh);
            when(msh.getMessageType()).thenReturn(msgType);
            when(msgType.getTriggerEvent()).thenReturn(triggerEvent);
            when(triggerEvent.getValue()).thenReturn("S12");
            when(siu.getPATIENT()).thenReturn(patient);
            when(patient.getPID()).thenReturn(pid);
            when(patient.getPV1()).thenReturn(pv1);
            when(siu.getRESOURCESReps()).thenReturn(0);

            mockMinimalPid(pid);
            mockMinimalPv1(pv1);

            // Mock placer appointment ID
            EI placerAppointmentId = mock(EI.class);
            ST placerEntityId = mock(ST.class);
            when(sch.getPlacerAppointmentID()).thenReturn(placerAppointmentId);
            when(placerAppointmentId.getEntityIdentifier()).thenReturn(placerEntityId);
            when(placerEntityId.getValue()).thenReturn("PLACER-001");

            // Mock filler appointment ID
            EI fillerAppointmentId = mock(EI.class);
            ST fillerEntityId = mock(ST.class);
            when(sch.getFillerAppointmentID()).thenReturn(fillerAppointmentId);
            when(fillerAppointmentId.getEntityIdentifier()).thenReturn(fillerEntityId);
            when(fillerEntityId.getValue()).thenReturn("FILLER-001");

            // Mock occurrence number
            NM occurrenceNumber = mock(NM.class);
            when(sch.getOccurrenceNumber()).thenReturn(occurrenceNumber);
            when(occurrenceNumber.getValue()).thenReturn("1");

            // Mock appointment duration
            NM duration = mock(NM.class);
            when(sch.getAppointmentDuration()).thenReturn(duration);
            when(duration.getValue()).thenReturn("30");

            // Mock duration units
            CE durationUnits = mock(CE.class);
            ST durationUnitsId = mock(ST.class);
            when(sch.getAppointmentDurationUnits()).thenReturn(durationUnits);
            when(durationUnits.getIdentifier()).thenReturn(durationUnitsId);
            when(durationUnitsId.getValue()).thenReturn("MIN");

            // Mock remaining fields as null
            when(sch.getPlacerGroupNumber()).thenReturn(null);
            when(sch.getScheduleID()).thenReturn(null);
            when(sch.getEventReason()).thenReturn(null);
            when(sch.getAppointmentReason()).thenReturn(null);
            when(sch.getAppointmentType()).thenReturn(null);
            when(sch.getAppointmentTimingQuantity(0)).thenReturn(null);
            when(sch.getEnteredByPerson(0)).thenReturn(null);
            when(sch.getEnteredByPhoneNumber(0)).thenReturn(null);
            when(sch.getFillerStatusCode()).thenReturn(null);

            // When
            Map<String, Object> result = handler.handle(siu);

            // Then
            assertThat(result).containsKey("schedule");
            @SuppressWarnings("unchecked")
            Map<String, Object> schedule = (Map<String, Object>) result.get("schedule");
            assertThat(schedule.get("placerAppointmentId")).isEqualTo("PLACER-001");
            assertThat(schedule.get("fillerAppointmentId")).isEqualTo("FILLER-001");
            assertThat(schedule.get("occurrenceNumber")).isEqualTo("1");
            assertThat(schedule.get("appointmentDuration")).isEqualTo("30");
            assertThat(schedule.get("appointmentDurationUnits")).isEqualTo("MIN");
        }

        @Test
        @DisplayName("Should extract appointment timing from SCH segment")
        void handle_withValidSiuS12_extractsAppointmentTiming() throws HL7Exception {
            // Given
            SIU_S12 siu = mock(SIU_S12.class);
            SCH sch = mock(SCH.class);
            MSH msh = mock(MSH.class);
            MSG msgType = mock(MSG.class);
            ID triggerEvent = mock(ID.class);

            SIU_S12.PATIENT patient = mock(SIU_S12.PATIENT.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);

            when(siu.getSCH()).thenReturn(sch);
            when(siu.getMSH()).thenReturn(msh);
            when(msh.getMessageType()).thenReturn(msgType);
            when(msgType.getTriggerEvent()).thenReturn(triggerEvent);
            when(triggerEvent.getValue()).thenReturn("S12");
            when(siu.getPATIENT()).thenReturn(patient);
            when(patient.getPID()).thenReturn(pid);
            when(patient.getPV1()).thenReturn(pv1);
            when(siu.getRESOURCESReps()).thenReturn(0);

            mockMinimalPid(pid);
            mockMinimalPv1(pv1);

            // Mock timing
            TQ appointmentTiming = mock(TQ.class);
            TS startDateTime = mock(TS.class);
            DTM startTime = mock(DTM.class);
            TS endDateTime = mock(TS.class);
            DTM endTime = mock(DTM.class);

            when(sch.getAppointmentTimingQuantity(0)).thenReturn(appointmentTiming);
            when(appointmentTiming.getStartDateTime()).thenReturn(startDateTime);
            when(startDateTime.getTime()).thenReturn(startTime);
            when(startTime.getValue()).thenReturn("20240115090000");
            when(appointmentTiming.getEndDateTime()).thenReturn(endDateTime);
            when(endDateTime.getTime()).thenReturn(endTime);
            when(endTime.getValue()).thenReturn("20240115093000");

            // Mock remaining fields as null
            when(sch.getPlacerAppointmentID()).thenReturn(null);
            when(sch.getFillerAppointmentID()).thenReturn(null);
            when(sch.getOccurrenceNumber()).thenReturn(null);
            when(sch.getPlacerGroupNumber()).thenReturn(null);
            when(sch.getScheduleID()).thenReturn(null);
            when(sch.getEventReason()).thenReturn(null);
            when(sch.getAppointmentReason()).thenReturn(null);
            when(sch.getAppointmentType()).thenReturn(null);
            when(sch.getAppointmentDuration()).thenReturn(null);
            when(sch.getAppointmentDurationUnits()).thenReturn(null);
            when(sch.getEnteredByPerson(0)).thenReturn(null);
            when(sch.getEnteredByPhoneNumber(0)).thenReturn(null);
            when(sch.getFillerStatusCode()).thenReturn(null);

            // When
            Map<String, Object> result = handler.handle(siu);

            // Then
            assertThat(result).containsKey("schedule");
            @SuppressWarnings("unchecked")
            Map<String, Object> schedule = (Map<String, Object>) result.get("schedule");
            assertThat(schedule).containsKey("timing");
            @SuppressWarnings("unchecked")
            Map<String, Object> timing = (Map<String, Object>) schedule.get("timing");
            assertThat(timing.get("startDateTime")).isEqualTo("20240115090000");
            assertThat(timing.get("endDateTime")).isEqualTo("20240115093000");
        }
    }

    @Nested
    @DisplayName("Event Description Tests")
    class EventDescriptionTests {

        @Test
        @DisplayName("Should return correct description for S13 - Rescheduling")
        void handle_withS13Event_returnsReschedulingDescription() throws HL7Exception {
            // Given
            SIU_S12 siu = createMockSiuWithTriggerEvent("S13");

            // When
            Map<String, Object> result = handler.handle(siu);

            // Then
            assertThat(result.get("eventDescription")).isEqualTo("Notification of appointment rescheduling");
        }

        @Test
        @DisplayName("Should return correct description for S14 - Modification")
        void handle_withS14Event_returnsModificationDescription() throws HL7Exception {
            // Given
            SIU_S12 siu = createMockSiuWithTriggerEvent("S14");

            // When
            Map<String, Object> result = handler.handle(siu);

            // Then
            assertThat(result.get("eventDescription")).isEqualTo("Notification of appointment modification");
        }

        @Test
        @DisplayName("Should return correct description for S15 - Cancellation")
        void handle_withS15Event_returnsCancellationDescription() throws HL7Exception {
            // Given
            SIU_S12 siu = createMockSiuWithTriggerEvent("S15");

            // When
            Map<String, Object> result = handler.handle(siu);

            // Then
            assertThat(result.get("eventDescription")).isEqualTo("Notification of appointment cancellation");
        }

        @Test
        @DisplayName("Should return correct description for S26 - No-show")
        void handle_withS26Event_returnsNoShowDescription() throws HL7Exception {
            // Given
            SIU_S12 siu = createMockSiuWithTriggerEvent("S26");

            // When
            Map<String, Object> result = handler.handle(siu);

            // Then
            assertThat(result.get("eventDescription")).isEqualTo("Notification of patient no-show");
        }
    }

    @Nested
    @DisplayName("Patient Data Extraction")
    class PatientDataTests {

        @Test
        @DisplayName("Should extract patient data from PID segment")
        void handle_withValidSiu_extractsPatientData() throws HL7Exception {
            // Given
            SIU_S12 siu = mock(SIU_S12.class);
            SCH sch = mock(SCH.class);
            MSH msh = mock(MSH.class);
            MSG msgType = mock(MSG.class);
            ID triggerEvent = mock(ID.class);

            SIU_S12.PATIENT patient = mock(SIU_S12.PATIENT.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);

            when(siu.getSCH()).thenReturn(sch);
            when(siu.getMSH()).thenReturn(msh);
            when(msh.getMessageType()).thenReturn(msgType);
            when(msgType.getTriggerEvent()).thenReturn(triggerEvent);
            when(triggerEvent.getValue()).thenReturn("S12");
            when(siu.getPATIENT()).thenReturn(patient);
            when(patient.getPID()).thenReturn(pid);
            when(patient.getPV1()).thenReturn(pv1);
            when(siu.getRESOURCESReps()).thenReturn(0);

            mockMinimalSch(sch);
            mockMinimalPv1(pv1);

            // Mock patient data
            CX patientId = mock(CX.class);
            ST idNumber = mock(ST.class);
            ID idTypeCode = mock(ID.class);
            when(pid.getPatientIdentifierList(0)).thenReturn(patientId);
            when(patientId.getIDNumber()).thenReturn(idNumber);
            when(idNumber.getValue()).thenReturn("PAT-12345");
            when(patientId.getIdentifierTypeCode()).thenReturn(idTypeCode);
            when(idTypeCode.getValue()).thenReturn("MRN");

            XPN patientName = mock(XPN.class);
            FN familyName = mock(FN.class);
            ST surname = mock(ST.class);
            ST givenName = mock(ST.class);
            when(pid.getPatientName(0)).thenReturn(patientName);
            when(patientName.getFamilyName()).thenReturn(familyName);
            when(familyName.getSurname()).thenReturn(surname);
            when(surname.getValue()).thenReturn("SMITH");
            when(patientName.getGivenName()).thenReturn(givenName);
            when(givenName.getValue()).thenReturn("JANE");

            TS dateOfBirth = mock(TS.class);
            DTM dobTime = mock(DTM.class);
            when(pid.getDateTimeOfBirth()).thenReturn(dateOfBirth);
            when(dateOfBirth.getTime()).thenReturn(dobTime);
            when(dobTime.getValue()).thenReturn("19900520");

            IS gender = mock(IS.class);
            when(pid.getAdministrativeSex()).thenReturn(gender);
            when(gender.getValue()).thenReturn("F");

            XTN phoneHome = mock(XTN.class);
            ST phoneNumber = mock(ST.class);
            when(pid.getPhoneNumberHome(0)).thenReturn(phoneHome);
            when(phoneHome.getTelephoneNumber()).thenReturn(phoneNumber);
            when(phoneNumber.getValue()).thenReturn("617-555-1234");

            // When
            Map<String, Object> result = handler.handle(siu);

            // Then
            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patientData = (Map<String, Object>) result.get("patient");
            assertThat(patientData.get("patientId")).isEqualTo("PAT-12345");
            assertThat(patientData.get("patientIdType")).isEqualTo("MRN");
            assertThat(patientData.get("familyName")).isEqualTo("SMITH");
            assertThat(patientData.get("givenName")).isEqualTo("JANE");
            assertThat(patientData.get("fullName")).isEqualTo("JANE SMITH");
            assertThat(patientData.get("dateOfBirth")).isEqualTo("19900520");
            assertThat(patientData.get("gender")).isEqualTo("F");
            assertThat(patientData.get("phoneHome")).isEqualTo("617-555-1234");
        }
    }

    @Nested
    @DisplayName("Unsupported Message Types")
    class UnsupportedMessageTests {

        @Test
        @DisplayName("Should handle non-SIU message gracefully")
        void handle_withNonSiuMessage_returnsBasicData() throws HL7Exception {
            // Given
            Message unsupportedMessage = mock(Message.class);

            // When
            Map<String, Object> result = handler.handle(unsupportedMessage);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("SIU");
            assertThat(result).doesNotContainKey("triggerEvent");
        }
    }

    // Helper methods

    private SIU_S12 createMockSiuWithTriggerEvent(String event) throws HL7Exception {
        SIU_S12 siu = mock(SIU_S12.class);
        SCH sch = mock(SCH.class);
        MSH msh = mock(MSH.class);
        MSG msgType = mock(MSG.class);
        ID triggerEvent = mock(ID.class);

        SIU_S12.PATIENT patient = mock(SIU_S12.PATIENT.class);
        PID pid = mock(PID.class);
        PV1 pv1 = mock(PV1.class);

        when(siu.getSCH()).thenReturn(sch);
        when(siu.getMSH()).thenReturn(msh);
        when(msh.getMessageType()).thenReturn(msgType);
        when(msgType.getTriggerEvent()).thenReturn(triggerEvent);
        when(triggerEvent.getValue()).thenReturn(event);
        when(siu.getPATIENT()).thenReturn(patient);
        when(patient.getPID()).thenReturn(pid);
        when(patient.getPV1()).thenReturn(pv1);
        when(siu.getRESOURCESReps()).thenReturn(0);

        mockMinimalSch(sch);
        mockMinimalPid(pid);
        mockMinimalPv1(pv1);

        return siu;
    }

    private void mockMinimalSch(SCH sch) {
        when(sch.getPlacerAppointmentID()).thenReturn(null);
        when(sch.getFillerAppointmentID()).thenReturn(null);
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
    }

    private void mockMinimalPid(PID pid) {
        when(pid.getPatientIdentifierList(0)).thenReturn(null);
        when(pid.getPatientName(0)).thenReturn(null);
        when(pid.getDateTimeOfBirth()).thenReturn(null);
        when(pid.getAdministrativeSex()).thenReturn(null);
        when(pid.getPhoneNumberHome(0)).thenReturn(null);
    }

    private void mockMinimalPv1(PV1 pv1) {
        when(pv1.getVisitNumber()).thenReturn(null);
        when(pv1.getPatientClass()).thenReturn(null);
        when(pv1.getAssignedPatientLocation()).thenReturn(null);
        when(pv1.getAttendingDoctor(0)).thenReturn(null);
    }
}

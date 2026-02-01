package com.healthdata.cdr.handler;
import org.junit.jupiter.api.Tag;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ADT_A02;
import ca.uhn.hl7v2.model.v25.message.ADT_A03;
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
 * Unit tests for AdtMessageHandler.
 * Tests ADT (Admit/Discharge/Transfer) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ADT Message Handler Tests")
@Tag("unit")
class AdtMessageHandlerTest {

    @InjectMocks
    private AdtMessageHandler handler;

    @Nested
    @DisplayName("Message Type Tests")
    class MessageTypeTests {

        @Test
        @DisplayName("Should return empty data for non-ADT message")
        void handle_withNonAdtMessage_returnsEmptyData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            // Should not throw, just return empty patient/visit data
            assertThat(result).doesNotContainKey("patient");
            assertThat(result).doesNotContainKey("visit");
        }

        @Test
        @DisplayName("Should process ADT_A01 message successfully")
        void handle_withAdtA01Message_processesMessage() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupMinimalAdt(adt);

            Map<String, Object> result = handler.handle(adt);

            assertThat(result).containsKey("patient");
        }
    }

    @Nested
    @DisplayName("Patient Data Tests")
    class PatientDataTests {

        @Test
        @DisplayName("Should extract patient identifier from PID segment")
        void extractPatient_withPidSegment_extractsPatientId() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithPatient(adt, "12345", "Smith", "John");

            Map<String, Object> result = handler.handle(adt);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientIdentifier", "12345");
        }

        @Test
        @DisplayName("Should extract patient name from PID segment")
        void extractPatient_withPidSegment_extractsPatientName() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithPatient(adt, "12345", "Smith", "John");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("familyName", "Smith");
            assertThat(patient).containsEntry("givenName", "John");
        }

        @Test
        @DisplayName("Should extract date of birth from PID segment")
        void extractPatient_withPidSegment_extractsDateOfBirth() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithDateOfBirth(adt, "19800115");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("dateOfBirth", "19800115");
        }

        @Test
        @DisplayName("Should extract gender from PID segment")
        void extractPatient_withPidSegment_extractsGender() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithGender(adt, "M");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("gender", "M");
        }
    }

    @Nested
    @DisplayName("Visit Data Tests")
    class VisitDataTests {

        @Test
        @DisplayName("Should extract patient class from PV1 segment")
        void extractVisit_withPv1Segment_extractsPatientClass() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithVisit(adt, "I", "V123456");

            Map<String, Object> result = handler.handle(adt);

            assertThat(result).containsKey("visit");
            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit).containsEntry("patientClass", "I");
        }

        @Test
        @DisplayName("Should extract visit number from PV1 segment")
        void extractVisit_withPv1Segment_extractsVisitNumber() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithVisit(adt, "I", "V123456");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit).containsEntry("visitNumber", "V123456");
        }

        @Test
        @DisplayName("Should extract admit date/time from PV1 segment")
        void extractVisit_withPv1Segment_extractsAdmitDateTime() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithAdmitDateTime(adt, "20240115120000");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit).containsEntry("admitDateTime", "20240115120000");
        }
    }

    @Nested
    @DisplayName("Event Data Tests")
    class EventDataTests {

        @Test
        @DisplayName("Should extract event type code from EVN segment")
        void extractEvent_withEvnSegment_extractsEventTypeCode() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithEvent(adt, "A01");

            Map<String, Object> result = handler.handle(adt);

            assertThat(result).containsKey("event");
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) result.get("event");
            assertThat(event).containsEntry("eventTypeCode", "A01");
        }

        @Test
        @DisplayName("Should map A01 event to in-progress FHIR status")
        void extractEvent_withA01Event_mapsToInProgress() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithEvent(adt, "A01");

            Map<String, Object> result = handler.handle(adt);

            assertThat(result).containsEntry("fhirEncounterStatus", "in-progress");
        }

        @Test
        @DisplayName("Should map A03 event to finished FHIR status")
        void extractEvent_withA03Event_mapsToFinished() throws HL7Exception {
            ADT_A03 adt = mock(ADT_A03.class, RETURNS_DEEP_STUBS);
            setupAdtA03WithEvent(adt, "A03");

            Map<String, Object> result = handler.handle(adt);

            assertThat(result).containsEntry("fhirEncounterStatus", "finished");
        }
    }

    @Nested
    @DisplayName("Event Type Mapping Tests")
    class EventTypeMappingTests {

        @Test
        @DisplayName("Should map A01 to in-progress status")
        void mapEventToEncounterStatus_withA01_returnsInProgress() {
            String status = handler.mapEventToEncounterStatus("A01");
            assertThat(status).isEqualTo("in-progress");
        }

        @Test
        @DisplayName("Should map A03 to finished status")
        void mapEventToEncounterStatus_withA03_returnsFinished() {
            String status = handler.mapEventToEncounterStatus("A03");
            assertThat(status).isEqualTo("finished");
        }

        @Test
        @DisplayName("Should map A05 to planned status")
        void mapEventToEncounterStatus_withA05_returnsPlanned() {
            String status = handler.mapEventToEncounterStatus("A05");
            assertThat(status).isEqualTo("planned");
        }

        @Test
        @DisplayName("Should map A11 to cancelled status")
        void mapEventToEncounterStatus_withA11_returnsCancelled() {
            String status = handler.mapEventToEncounterStatus("A11");
            assertThat(status).isEqualTo("cancelled");
        }

        @Test
        @DisplayName("Should identify A06 as patient class change event")
        void isPatientClassChangeEvent_withA06_returnsTrue() {
            boolean isChange = handler.isPatientClassChangeEvent("A06");
            assertThat(isChange).isTrue();
        }

        @Test
        @DisplayName("Should identify A07 as patient class change event")
        void isPatientClassChangeEvent_withA07_returnsTrue() {
            boolean isChange = handler.isPatientClassChangeEvent("A07");
            assertThat(isChange).isTrue();
        }

        @Test
        @DisplayName("Should not identify A01 as patient class change event")
        void isPatientClassChangeEvent_withA01_returnsFalse() {
            boolean isChange = handler.isPatientClassChangeEvent("A01");
            assertThat(isChange).isFalse();
        }

        @Test
        @DisplayName("Should identify A11 as cancellation event")
        void isCancellationEvent_withA11_returnsTrue() {
            boolean isCancellation = handler.isCancellationEvent("A11");
            assertThat(isCancellation).isTrue();
        }

        @Test
        @DisplayName("Should return I for A06 target patient class")
        void getTargetPatientClass_withA06_returnsI() {
            String targetClass = handler.getTargetPatientClass("A06");
            assertThat(targetClass).isEqualTo("I");
        }

        @Test
        @DisplayName("Should return O for A07 target patient class")
        void getTargetPatientClass_withA07_returnsO() {
            String targetClass = handler.getTargetPatientClass("A07");
            assertThat(targetClass).isEqualTo("O");
        }
    }

    // Helper methods
    private void setupMinimalAdt(ADT_A01 adt) throws HL7Exception {
        // PID segment
        PID pid = mock(PID.class);
        when(adt.getPID()).thenReturn(pid);
        when(pid.getPatientID()).thenReturn(null);
        when(pid.getPatientIdentifierList(0)).thenReturn(null);
        when(pid.getPatientName(0)).thenReturn(null);
        when(pid.getDateTimeOfBirth()).thenReturn(null);
        when(pid.getAdministrativeSex()).thenReturn(null);
        when(pid.getPatientAddress(0)).thenReturn(null);
        when(pid.getPhoneNumberHome(0)).thenReturn(null);
        when(pid.getSSNNumberPatient()).thenReturn(null);

        // PV1 segment
        when(adt.getPV1()).thenReturn(null);

        // EVN segment
        when(adt.getEVN()).thenReturn(null);
    }

    private void setupAdtWithPatient(ADT_A01 adt, String patientId, String familyName, String givenName) throws HL7Exception {
        // PID segment
        PID pid = mock(PID.class);
        when(adt.getPID()).thenReturn(pid);

        // Patient ID
        when(pid.getPatientID()).thenReturn(null);

        CX patientIdCx = mock(CX.class);
        ST idNumber = mock(ST.class);
        when(idNumber.getValue()).thenReturn(patientId);
        when(patientIdCx.getIDNumber()).thenReturn(idNumber);
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

        ST middle = mock(ST.class);
        when(middle.getValue()).thenReturn(null);
        when(patientName.getSecondAndFurtherGivenNamesOrInitialsThereof()).thenReturn(middle);

        when(pid.getPatientName(0)).thenReturn(patientName);

        // Null other fields
        when(pid.getDateTimeOfBirth()).thenReturn(null);
        when(pid.getAdministrativeSex()).thenReturn(null);
        when(pid.getPatientAddress(0)).thenReturn(null);
        when(pid.getPhoneNumberHome(0)).thenReturn(null);
        when(pid.getSSNNumberPatient()).thenReturn(null);

        // No PV1 or EVN
        when(adt.getPV1()).thenReturn(null);
        when(adt.getEVN()).thenReturn(null);
    }

    private void setupAdtWithDateOfBirth(ADT_A01 adt, String dob) throws HL7Exception {
        setupAdtWithPatient(adt, "12345", "Smith", "John");

        PID pid = adt.getPID();

        TS dateOfBirth = mock(TS.class);
        DTM dtm = mock(DTM.class);
        when(dtm.getValue()).thenReturn(dob);
        when(dateOfBirth.getTime()).thenReturn(dtm);
        when(pid.getDateTimeOfBirth()).thenReturn(dateOfBirth);
    }

    private void setupAdtWithGender(ADT_A01 adt, String gender) throws HL7Exception {
        setupAdtWithPatient(adt, "12345", "Smith", "John");

        PID pid = adt.getPID();

        IS adminSex = mock(IS.class);
        when(adminSex.getValue()).thenReturn(gender);
        when(pid.getAdministrativeSex()).thenReturn(adminSex);
    }

    private void setupAdtWithVisit(ADT_A01 adt, String patientClass, String visitNumber) throws HL7Exception {
        setupMinimalAdt(adt);

        PV1 pv1 = mock(PV1.class);
        when(adt.getPV1()).thenReturn(pv1);

        // Patient class
        IS patientClassIs = mock(IS.class);
        when(patientClassIs.getValue()).thenReturn(patientClass);
        when(pv1.getPatientClass()).thenReturn(patientClassIs);

        // Visit number
        CX visitNum = mock(CX.class);
        ST visitId = mock(ST.class);
        when(visitId.getValue()).thenReturn(visitNumber);
        when(visitNum.getIDNumber()).thenReturn(visitId);
        when(pv1.getVisitNumber()).thenReturn(visitNum);

        // Null other fields
        when(pv1.getAssignedPatientLocation()).thenReturn(null);
        when(pv1.getAttendingDoctor(0)).thenReturn(null);
        when(pv1.getAdmissionType()).thenReturn(null);
        when(pv1.getAdmitDateTime()).thenReturn(null);
        when(pv1.getDischargeDateTime(0)).thenReturn(null);
    }

    private void setupAdtWithAdmitDateTime(ADT_A01 adt, String admitDateTime) throws HL7Exception {
        setupAdtWithVisit(adt, "I", "V123456");

        PV1 pv1 = adt.getPV1();

        TS admitTs = mock(TS.class);
        DTM admitDtm = mock(DTM.class);
        when(admitDtm.getValue()).thenReturn(admitDateTime);
        when(admitTs.getTime()).thenReturn(admitDtm);
        when(pv1.getAdmitDateTime()).thenReturn(admitTs);
    }

    private void setupAdtWithEvent(ADT_A01 adt, String eventTypeCode) throws HL7Exception {
        setupMinimalAdt(adt);

        EVN evn = mock(EVN.class);
        when(adt.getEVN()).thenReturn(evn);

        ID eventType = mock(ID.class);
        when(eventType.getValue()).thenReturn(eventTypeCode);
        when(evn.getEventTypeCode()).thenReturn(eventType);

        when(evn.getRecordedDateTime()).thenReturn(null);
        when(evn.getEventOccurred()).thenReturn(null);
    }

    private void setupAdtA03WithEvent(ADT_A03 adt, String eventTypeCode) throws HL7Exception {
        // PID segment
        when(adt.getPID()).thenReturn(null);

        // PV1 segment
        when(adt.getPV1()).thenReturn(null);

        // EVN segment
        EVN evn = mock(EVN.class);
        when(adt.getEVN()).thenReturn(evn);

        ID eventType = mock(ID.class);
        when(eventType.getValue()).thenReturn(eventTypeCode);
        when(evn.getEventTypeCode()).thenReturn(eventType);

        when(evn.getRecordedDateTime()).thenReturn(null);
        when(evn.getEventOccurred()).thenReturn(null);
    }
}

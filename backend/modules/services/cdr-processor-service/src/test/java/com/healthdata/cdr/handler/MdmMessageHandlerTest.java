package com.healthdata.cdr.handler;
import org.junit.jupiter.api.Tag;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.MDM_T01;
import ca.uhn.hl7v2.model.v25.message.MDM_T02;
import ca.uhn.hl7v2.model.v25.segment.EVN;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.model.v25.segment.TXA;
import org.junit.jupiter.api.BeforeEach;
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
 * Unit tests for MdmMessageHandler.
 * Tests extraction of medical document management data from HL7 v2 MDM messages.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MDM Message Handler Tests")
@Tag("unit")
class MdmMessageHandlerTest {

    @InjectMocks
    private MdmMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MdmMessageHandler();
    }

    @Nested
    @DisplayName("MDM^T01 - Original Document Notification")
    class MdmT01Tests {

        @Test
        @DisplayName("Should extract message type and trigger event from MDM^T01")
        void handle_withValidMdmT01_extractsMessageTypeAndTriggerEvent() throws HL7Exception {
            // Given
            MDM_T01 mdm = mock(MDM_T01.class);
            EVN evn = mock(EVN.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);
            TXA txa = mock(TXA.class);

            when(mdm.getEVN()).thenReturn(evn);
            when(mdm.getPID()).thenReturn(pid);
            when(mdm.getPV1()).thenReturn(pv1);
            when(mdm.getTXA()).thenReturn(txa);

            // Mock minimal required fields
            mockMinimalEvn(evn);
            mockMinimalPid(pid);
            mockMinimalPv1(pv1);
            mockMinimalTxa(txa);

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("MDM");
            assertThat(result.get("triggerEvent")).isEqualTo("T01");
            assertThat(result.get("eventDescription")).isEqualTo("Original document notification");
        }

        @Test
        @DisplayName("Should extract patient data from MDM^T01")
        void handle_withValidMdmT01_extractsPatientData() throws HL7Exception {
            // Given
            MDM_T01 mdm = mock(MDM_T01.class);
            PID pid = mock(PID.class);
            EVN evn = mock(EVN.class);
            PV1 pv1 = mock(PV1.class);
            TXA txa = mock(TXA.class);

            when(mdm.getEVN()).thenReturn(evn);
            when(mdm.getPID()).thenReturn(pid);
            when(mdm.getPV1()).thenReturn(pv1);
            when(mdm.getTXA()).thenReturn(txa);

            mockMinimalEvn(evn);
            mockMinimalPv1(pv1);
            mockMinimalTxa(txa);

            // Mock patient data
            CX patientId = mock(CX.class);
            ST idNumber = mock(ST.class);
            ID idTypeCode = mock(ID.class);
            when(pid.getPatientIdentifierList(0)).thenReturn(patientId);
            when(patientId.getIDNumber()).thenReturn(idNumber);
            when(idNumber.getValue()).thenReturn("12345");
            when(patientId.getIdentifierTypeCode()).thenReturn(idTypeCode);
            when(idTypeCode.getValue()).thenReturn("MRN");

            XPN patientName = mock(XPN.class);
            FN familyName = mock(FN.class);
            ST surname = mock(ST.class);
            ST givenName = mock(ST.class);
            when(pid.getPatientName(0)).thenReturn(patientName);
            when(patientName.getFamilyName()).thenReturn(familyName);
            when(familyName.getSurname()).thenReturn(surname);
            when(surname.getValue()).thenReturn("DOE");
            when(patientName.getGivenName()).thenReturn(givenName);
            when(givenName.getValue()).thenReturn("JOHN");

            TS dateOfBirth = mock(TS.class);
            DTM dobTime = mock(DTM.class);
            when(pid.getDateTimeOfBirth()).thenReturn(dateOfBirth);
            when(dateOfBirth.getTime()).thenReturn(dobTime);
            when(dobTime.getValue()).thenReturn("19800115");

            IS gender = mock(IS.class);
            when(pid.getAdministrativeSex()).thenReturn(gender);
            when(gender.getValue()).thenReturn("M");

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient.get("patientId")).isEqualTo("12345");
            assertThat(patient.get("patientIdType")).isEqualTo("MRN");
            assertThat(patient.get("familyName")).isEqualTo("DOE");
            assertThat(patient.get("givenName")).isEqualTo("JOHN");
            assertThat(patient.get("fullName")).isEqualTo("JOHN DOE");
            assertThat(patient.get("dateOfBirth")).isEqualTo("19800115");
            assertThat(patient.get("gender")).isEqualTo("M");
        }

        @Test
        @DisplayName("Should extract document data from TXA segment")
        void handle_withValidMdmT01_extractsDocumentData() throws HL7Exception {
            // Given
            MDM_T01 mdm = mock(MDM_T01.class);
            EVN evn = mock(EVN.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);
            TXA txa = mock(TXA.class);

            when(mdm.getEVN()).thenReturn(evn);
            when(mdm.getPID()).thenReturn(pid);
            when(mdm.getPV1()).thenReturn(pv1);
            when(mdm.getTXA()).thenReturn(txa);

            mockMinimalEvn(evn);
            mockMinimalPid(pid);
            mockMinimalPv1(pv1);

            // Mock TXA document data
            SI setId = mock(SI.class);
            when(txa.getSetIDTXA()).thenReturn(setId);
            when(setId.getValue()).thenReturn("1");

            IS docType = mock(IS.class);
            when(txa.getDocumentType()).thenReturn(docType);
            when(docType.getValue()).thenReturn("DISCHARGE_SUMMARY");

            ID contentPresentation = mock(ID.class);
            when(txa.getDocumentContentPresentation()).thenReturn(contentPresentation);
            when(contentPresentation.getValue()).thenReturn("TX");

            TS activityDateTime = mock(TS.class);
            DTM activityTime = mock(DTM.class);
            when(txa.getActivityDateTime()).thenReturn(activityDateTime);
            when(activityDateTime.getTime()).thenReturn(activityTime);
            when(activityTime.getValue()).thenReturn("20240115120000");

            ID completionStatus = mock(ID.class);
            when(txa.getDocumentCompletionStatus()).thenReturn(completionStatus);
            when(completionStatus.getValue()).thenReturn("AU");

            ID confidentialityStatus = mock(ID.class);
            when(txa.getDocumentConfidentialityStatus()).thenReturn(confidentialityStatus);
            when(confidentialityStatus.getValue()).thenReturn("R");

            ID availabilityStatus = mock(ID.class);
            when(txa.getDocumentAvailabilityStatus()).thenReturn(availabilityStatus);
            when(availabilityStatus.getValue()).thenReturn("AV");

            // Mock unique document number
            EI uniqueDocNum = mock(EI.class);
            ST entityIdentifier = mock(ST.class);
            when(txa.getUniqueDocumentNumber()).thenReturn(uniqueDocNum);
            when(uniqueDocNum.getEntityIdentifier()).thenReturn(entityIdentifier);
            when(entityIdentifier.getValue()).thenReturn("DOC-12345");

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).containsKey("document");
            @SuppressWarnings("unchecked")
            Map<String, Object> document = (Map<String, Object>) result.get("document");
            assertThat(document.get("setId")).isEqualTo("1");
            assertThat(document.get("documentType")).isEqualTo("DISCHARGE_SUMMARY");
            assertThat(document.get("contentPresentation")).isEqualTo("TX");
            assertThat(document.get("activityDateTime")).isEqualTo("20240115120000");
            assertThat(document.get("completionStatus")).isEqualTo("AU");
            assertThat(document.get("confidentialityStatus")).isEqualTo("R");
            assertThat(document.get("availabilityStatus")).isEqualTo("AV");
            assertThat(document.get("uniqueDocumentNumber")).isEqualTo("DOC-12345");
        }

        @Test
        @DisplayName("Should handle missing EVN segment gracefully")
        void handle_withNullEvnSegment_handlesGracefully() throws HL7Exception {
            // Given
            MDM_T01 mdm = mock(MDM_T01.class);
            when(mdm.getEVN()).thenReturn(null);
            when(mdm.getPID()).thenReturn(null);
            when(mdm.getPV1()).thenReturn(null);
            when(mdm.getTXA()).thenReturn(null);

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("MDM");
            assertThat(result.get("triggerEvent")).isEqualTo("T01");
        }
    }

    @Nested
    @DisplayName("MDM^T02 - Original Document Notification and Content")
    class MdmT02Tests {

        @Test
        @DisplayName("Should extract message type and trigger event from MDM^T02")
        void handle_withValidMdmT02_extractsMessageTypeAndTriggerEvent() throws HL7Exception {
            // Given
            MDM_T02 mdm = mock(MDM_T02.class);
            EVN evn = mock(EVN.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);
            TXA txa = mock(TXA.class);

            when(mdm.getEVN()).thenReturn(evn);
            when(mdm.getPID()).thenReturn(pid);
            when(mdm.getPV1()).thenReturn(pv1);
            when(mdm.getTXA()).thenReturn(txa);

            mockMinimalEvn(evn);
            mockMinimalPid(pid);
            mockMinimalPv1(pv1);
            mockMinimalTxa(txa);

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("MDM");
            assertThat(result.get("triggerEvent")).isEqualTo("T02");
            assertThat(result.get("eventDescription")).isEqualTo("Original document notification and content");
        }

        @Test
        @DisplayName("Should extract document content placeholder from MDM^T02")
        void handle_withValidMdmT02_extractsDocumentContent() throws HL7Exception {
            // Given
            MDM_T02 mdm = mock(MDM_T02.class);
            EVN evn = mock(EVN.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);
            TXA txa = mock(TXA.class);

            when(mdm.getEVN()).thenReturn(evn);
            when(mdm.getPID()).thenReturn(pid);
            when(mdm.getPV1()).thenReturn(pv1);
            when(mdm.getTXA()).thenReturn(txa);

            mockMinimalEvn(evn);
            mockMinimalPid(pid);
            mockMinimalPv1(pv1);
            mockMinimalTxa(txa);

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).containsKey("documentContent");
            assertThat(result.get("documentContent")).isEqualTo("See attached document");
        }
    }

    @Nested
    @DisplayName("Unsupported Message Types")
    class UnsupportedMessageTests {

        @Test
        @DisplayName("Should handle unsupported MDM message type gracefully")
        void handle_withUnsupportedMessageType_logsWarning() throws HL7Exception {
            // Given
            Message unsupportedMessage = mock(Message.class);

            // When
            Map<String, Object> result = handler.handle(unsupportedMessage);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("MDM");
            // Should not contain trigger event or description for unsupported types
            assertThat(result).doesNotContainKey("triggerEvent");
        }
    }

    @Nested
    @DisplayName("Visit Data Extraction")
    class VisitDataTests {

        @Test
        @DisplayName("Should extract visit data from PV1 segment")
        void handle_withValidMdmT01_extractsVisitData() throws HL7Exception {
            // Given
            MDM_T01 mdm = mock(MDM_T01.class);
            EVN evn = mock(EVN.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);
            TXA txa = mock(TXA.class);

            when(mdm.getEVN()).thenReturn(evn);
            when(mdm.getPID()).thenReturn(pid);
            when(mdm.getPV1()).thenReturn(pv1);
            when(mdm.getTXA()).thenReturn(txa);

            mockMinimalEvn(evn);
            mockMinimalPid(pid);
            mockMinimalTxa(txa);

            // Mock visit data
            CX visitNumber = mock(CX.class);
            ST visitIdNumber = mock(ST.class);
            when(pv1.getVisitNumber()).thenReturn(visitNumber);
            when(visitNumber.getIDNumber()).thenReturn(visitIdNumber);
            when(visitIdNumber.getValue()).thenReturn("V123456");

            IS patientClass = mock(IS.class);
            when(pv1.getPatientClass()).thenReturn(patientClass);
            when(patientClass.getValue()).thenReturn("I");

            // Mock attending doctor
            XCN attendingDoctor = mock(XCN.class);
            ST doctorId = mock(ST.class);
            FN doctorFamilyName = mock(FN.class);
            ST doctorSurname = mock(ST.class);
            ST doctorGivenName = mock(ST.class);
            when(pv1.getAttendingDoctor(0)).thenReturn(attendingDoctor);
            when(attendingDoctor.getIDNumber()).thenReturn(doctorId);
            when(doctorId.getValue()).thenReturn("DR001");
            when(attendingDoctor.getFamilyName()).thenReturn(doctorFamilyName);
            when(doctorFamilyName.getSurname()).thenReturn(doctorSurname);
            when(doctorSurname.getValue()).thenReturn("SMITH");
            when(attendingDoctor.getGivenName()).thenReturn(doctorGivenName);
            when(doctorGivenName.getValue()).thenReturn("JAMES");

            // Mock location
            PL location = mock(PL.class);
            IS pointOfCare = mock(IS.class);
            IS room = mock(IS.class);
            IS bed = mock(IS.class);
            HD facility = mock(HD.class);
            IS facilityNamespace = mock(IS.class);
            when(pv1.getAssignedPatientLocation()).thenReturn(location);
            when(location.getPointOfCare()).thenReturn(pointOfCare);
            when(pointOfCare.getValue()).thenReturn("ICU");
            when(location.getRoom()).thenReturn(room);
            when(room.getValue()).thenReturn("101");
            when(location.getBed()).thenReturn(bed);
            when(bed.getValue()).thenReturn("A");
            when(location.getFacility()).thenReturn(facility);
            when(facility.getNamespaceID()).thenReturn(facilityNamespace);
            when(facilityNamespace.getValue()).thenReturn("MAIN_HOSPITAL");

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).containsKey("visit");
            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit.get("visitNumber")).isEqualTo("V123456");
            assertThat(visit.get("patientClass")).isEqualTo("I");

            @SuppressWarnings("unchecked")
            Map<String, String> doctor = (Map<String, String>) visit.get("attendingDoctor");
            assertThat(doctor.get("id")).isEqualTo("DR001");
            assertThat(doctor.get("familyName")).isEqualTo("SMITH");
            assertThat(doctor.get("givenName")).isEqualTo("JAMES");

            @SuppressWarnings("unchecked")
            Map<String, String> loc = (Map<String, String>) visit.get("location");
            assertThat(loc.get("pointOfCare")).isEqualTo("ICU");
            assertThat(loc.get("room")).isEqualTo("101");
            assertThat(loc.get("bed")).isEqualTo("A");
            assertThat(loc.get("facility")).isEqualTo("MAIN_HOSPITAL");
        }
    }

    @Nested
    @DisplayName("Event Data Extraction")
    class EventDataTests {

        @Test
        @DisplayName("Should extract event data from EVN segment")
        void handle_withValidMdmT01_extractsEventData() throws HL7Exception {
            // Given
            MDM_T01 mdm = mock(MDM_T01.class);
            EVN evn = mock(EVN.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);
            TXA txa = mock(TXA.class);

            when(mdm.getEVN()).thenReturn(evn);
            when(mdm.getPID()).thenReturn(pid);
            when(mdm.getPV1()).thenReturn(pv1);
            when(mdm.getTXA()).thenReturn(txa);

            mockMinimalPid(pid);
            mockMinimalPv1(pv1);
            mockMinimalTxa(txa);

            // Mock event data
            ID eventTypeCode = mock(ID.class);
            when(evn.getEventTypeCode()).thenReturn(eventTypeCode);
            when(eventTypeCode.getValue()).thenReturn("T01");

            TS recordedDateTime = mock(TS.class);
            DTM recordedTime = mock(DTM.class);
            when(evn.getRecordedDateTime()).thenReturn(recordedDateTime);
            when(recordedDateTime.getTime()).thenReturn(recordedTime);
            when(recordedTime.getValue()).thenReturn("20240115120000");

            IS eventReasonCode = mock(IS.class);
            when(evn.getEventReasonCode()).thenReturn(eventReasonCode);
            when(eventReasonCode.getValue()).thenReturn("01");

            XCN operatorId = mock(XCN.class);
            ST operatorIdNumber = mock(ST.class);
            when(evn.getOperatorID(0)).thenReturn(operatorId);
            when(operatorId.getIDNumber()).thenReturn(operatorIdNumber);
            when(operatorIdNumber.getValue()).thenReturn("OP001");

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).containsKey("event");
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) result.get("event");
            assertThat(event.get("eventTypeCode")).isEqualTo("T01");
            assertThat(event.get("recordedDateTime")).isEqualTo("20240115120000");
            assertThat(event.get("eventReasonCode")).isEqualTo("01");
            assertThat(event.get("operatorId")).isEqualTo("OP001");
        }
    }

    // Helper methods for minimal mocking

    private void mockMinimalEvn(EVN evn) {
        when(evn.getEventTypeCode()).thenReturn(null);
        when(evn.getRecordedDateTime()).thenReturn(null);
        when(evn.getEventReasonCode()).thenReturn(null);
    }

    private void mockMinimalPid(PID pid) {
        when(pid.getPatientIdentifierList(0)).thenReturn(null);
        when(pid.getPatientName(0)).thenReturn(null);
        when(pid.getDateTimeOfBirth()).thenReturn(null);
        when(pid.getAdministrativeSex()).thenReturn(null);
    }

    private void mockMinimalPv1(PV1 pv1) {
        when(pv1.getVisitNumber()).thenReturn(null);
        when(pv1.getPatientClass()).thenReturn(null);
        when(pv1.getAttendingDoctor(0)).thenReturn(null);
        when(pv1.getAssignedPatientLocation()).thenReturn(null);
    }

    private void mockMinimalTxa(TXA txa) {
        when(txa.getSetIDTXA()).thenReturn(null);
        when(txa.getDocumentType()).thenReturn(null);
        when(txa.getDocumentContentPresentation()).thenReturn(null);
        when(txa.getActivityDateTime()).thenReturn(null);
        when(txa.getPrimaryActivityProviderCodeName(0)).thenReturn(null);
        when(txa.getOriginationDateTime()).thenReturn(null);
        when(txa.getTranscriptionDateTime()).thenReturn(null);
        when(txa.getEditDateTime(0)).thenReturn(null);
        when(txa.getOriginatorCodeName(0)).thenReturn(null);
        when(txa.getUniqueDocumentNumber()).thenReturn(null);
        when(txa.getParentDocumentNumber()).thenReturn(null);
        when(txa.getDocumentCompletionStatus()).thenReturn(null);
        when(txa.getDocumentConfidentialityStatus()).thenReturn(null);
        when(txa.getDocumentAvailabilityStatus()).thenReturn(null);
        when(txa.getDocumentStorageStatus()).thenReturn(null);
    }
}

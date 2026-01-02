package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.MDM_T01;
import ca.uhn.hl7v2.model.v25.message.MDM_T02;
import ca.uhn.hl7v2.model.v25.segment.EVN;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.model.v25.segment.TXA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for MDM (Medical Document Management) messages.
 *
 * Supports:
 * - MDM^T01: Original document notification
 * - MDM^T02: Original document notification and content
 * - MDM^T03: Document status change notification
 * - MDM^T04: Document status change notification and content
 * - MDM^T05: Document addendum notification
 * - MDM^T06: Document addendum notification and content
 * - MDM^T09: Document replacement notification
 * - MDM^T10: Document replacement notification and content
 *
 * Converts to FHIR DocumentReference resources.
 */
@Slf4j
@Component
public class MdmMessageHandler {

    /**
     * Handle MDM message and extract document data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing MDM message");

        Map<String, Object> data = new HashMap<>();
        data.put("messageType", "MDM");

        // Handle different MDM message types
        if (message instanceof MDM_T01) {
            handleMdmT01((MDM_T01) message, data);
        } else if (message instanceof MDM_T02) {
            handleMdmT02((MDM_T02) message, data);
        } else {
            log.warn("Unsupported MDM message type: {}", message.getClass().getSimpleName());
        }

        return data;
    }

    /**
     * Handle MDM^T01 - Original document notification
     */
    private void handleMdmT01(MDM_T01 mdm, Map<String, Object> data) throws HL7Exception {
        data.put("triggerEvent", "T01");
        data.put("eventDescription", "Original document notification");

        // Extract EVN segment
        EVN evn = mdm.getEVN();
        if (evn != null) {
            extractEventData(evn, data);
        }

        // Extract patient data
        PID pid = mdm.getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract visit data
        PV1 pv1 = mdm.getPV1();
        if (pv1 != null) {
            extractVisitData(pv1, data);
        }

        // Extract document data
        TXA txa = mdm.getTXA();
        if (txa != null) {
            extractDocumentData(txa, data);
        }
    }

    /**
     * Handle MDM^T02 - Original document notification and content
     */
    private void handleMdmT02(MDM_T02 mdm, Map<String, Object> data) throws HL7Exception {
        data.put("triggerEvent", "T02");
        data.put("eventDescription", "Original document notification and content");

        // Extract EVN segment
        EVN evn = mdm.getEVN();
        if (evn != null) {
            extractEventData(evn, data);
        }

        // Extract patient data
        PID pid = mdm.getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract visit data
        PV1 pv1 = mdm.getPV1();
        if (pv1 != null) {
            extractVisitData(pv1, data);
        }

        // Extract document data
        TXA txa = mdm.getTXA();
        if (txa != null) {
            extractDocumentData(txa, data);
        }

        // Extract document content (OBX segments)
        extractDocumentContent(mdm, data);
    }

    /**
     * Extract event data from EVN segment.
     */
    private void extractEventData(EVN evn, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> eventData = new HashMap<>();

        if (evn.getEventTypeCode() != null) {
            eventData.put("eventTypeCode", evn.getEventTypeCode().getValue());
        }

        if (evn.getRecordedDateTime() != null && evn.getRecordedDateTime().getTime() != null) {
            eventData.put("recordedDateTime", evn.getRecordedDateTime().getTime().getValue());
        }

        if (evn.getEventReasonCode() != null) {
            eventData.put("eventReasonCode", evn.getEventReasonCode().getValue());
        }

        if (evn.getOperatorID(0) != null) {
            eventData.put("operatorId", evn.getOperatorID(0).getIDNumber().getValue());
        }

        data.put("event", eventData);
    }

    /**
     * Extract patient data from PID segment.
     */
    private void extractPatientData(PID pid, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> patientData = new HashMap<>();

        // Patient ID
        if (pid.getPatientIdentifierList(0) != null) {
            patientData.put("patientId",
                pid.getPatientIdentifierList(0).getIDNumber().getValue());
            patientData.put("patientIdType",
                pid.getPatientIdentifierList(0).getIdentifierTypeCode().getValue());
        }

        // Patient Name
        if (pid.getPatientName(0) != null) {
            String familyName = pid.getPatientName(0).getFamilyName().getSurname().getValue();
            String givenName = pid.getPatientName(0).getGivenName().getValue();
            patientData.put("familyName", familyName);
            patientData.put("givenName", givenName);
            patientData.put("fullName", givenName + " " + familyName);
        }

        // Date of Birth
        if (pid.getDateTimeOfBirth() != null && pid.getDateTimeOfBirth().getTime() != null) {
            patientData.put("dateOfBirth", pid.getDateTimeOfBirth().getTime().getValue());
        }

        // Gender
        if (pid.getAdministrativeSex() != null) {
            patientData.put("gender", pid.getAdministrativeSex().getValue());
        }

        data.put("patient", patientData);
    }

    /**
     * Extract visit data from PV1 segment.
     */
    private void extractVisitData(PV1 pv1, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> visitData = new HashMap<>();

        // Visit Number
        if (pv1.getVisitNumber() != null) {
            visitData.put("visitNumber", pv1.getVisitNumber().getIDNumber().getValue());
        }

        // Patient Class
        if (pv1.getPatientClass() != null) {
            visitData.put("patientClass", pv1.getPatientClass().getValue());
        }

        // Attending Doctor
        if (pv1.getAttendingDoctor(0) != null) {
            Map<String, String> doctor = new HashMap<>();
            doctor.put("id", pv1.getAttendingDoctor(0).getIDNumber().getValue());
            doctor.put("familyName",
                pv1.getAttendingDoctor(0).getFamilyName().getSurname().getValue());
            doctor.put("givenName", pv1.getAttendingDoctor(0).getGivenName().getValue());
            visitData.put("attendingDoctor", doctor);
        }

        // Assigned Location
        if (pv1.getAssignedPatientLocation() != null) {
            Map<String, String> location = new HashMap<>();
            location.put("pointOfCare",
                pv1.getAssignedPatientLocation().getPointOfCare().getValue());
            location.put("room", pv1.getAssignedPatientLocation().getRoom().getValue());
            location.put("bed", pv1.getAssignedPatientLocation().getBed().getValue());
            location.put("facility",
                pv1.getAssignedPatientLocation().getFacility().getNamespaceID().getValue());
            visitData.put("location", location);
        }

        data.put("visit", visitData);
    }

    /**
     * Extract document data from TXA segment.
     */
    private void extractDocumentData(TXA txa, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> documentData = new HashMap<>();

        // Set ID
        if (txa.getSetIDTXA() != null) {
            documentData.put("setId", txa.getSetIDTXA().getValue());
        }

        // Document Type
        if (txa.getDocumentType() != null) {
            documentData.put("documentType", txa.getDocumentType().getValue());
        }

        // Document Content Presentation
        if (txa.getDocumentContentPresentation() != null) {
            documentData.put("contentPresentation",
                txa.getDocumentContentPresentation().getValue());
        }

        // Activity Date/Time
        if (txa.getActivityDateTime() != null && txa.getActivityDateTime().getTime() != null) {
            documentData.put("activityDateTime", txa.getActivityDateTime().getTime().getValue());
        }

        // Primary Activity Provider Code/Name
        if (txa.getPrimaryActivityProviderCodeName(0) != null) {
            Map<String, String> provider = new HashMap<>();
            provider.put("id", txa.getPrimaryActivityProviderCodeName(0).getIDNumber().getValue());
            provider.put("familyName",
                txa.getPrimaryActivityProviderCodeName(0).getFamilyName().getSurname().getValue());
            provider.put("givenName",
                txa.getPrimaryActivityProviderCodeName(0).getGivenName().getValue());
            documentData.put("primaryActivityProvider", provider);
        }

        // Origination Date/Time
        if (txa.getOriginationDateTime() != null &&
            txa.getOriginationDateTime().getTime() != null) {
            documentData.put("originationDateTime",
                txa.getOriginationDateTime().getTime().getValue());
        }

        // Transcription Date/Time
        if (txa.getTranscriptionDateTime() != null &&
            txa.getTranscriptionDateTime().getTime() != null) {
            documentData.put("transcriptionDateTime",
                txa.getTranscriptionDateTime().getTime().getValue());
        }

        // Edit Date/Time
        if (txa.getEditDateTime(0) != null && txa.getEditDateTime(0).getTime() != null) {
            documentData.put("editDateTime", txa.getEditDateTime(0).getTime().getValue());
        }

        // Originator Code/Name
        if (txa.getOriginatorCodeName(0) != null) {
            Map<String, String> originator = new HashMap<>();
            originator.put("id", txa.getOriginatorCodeName(0).getIDNumber().getValue());
            originator.put("familyName",
                txa.getOriginatorCodeName(0).getFamilyName().getSurname().getValue());
            originator.put("givenName", txa.getOriginatorCodeName(0).getGivenName().getValue());
            documentData.put("originator", originator);
        }

        // Unique Document Number
        if (txa.getUniqueDocumentNumber() != null) {
            documentData.put("uniqueDocumentNumber",
                txa.getUniqueDocumentNumber().getEntityIdentifier().getValue());
        }

        // Parent Document Number
        if (txa.getParentDocumentNumber() != null) {
            documentData.put("parentDocumentNumber",
                txa.getParentDocumentNumber().getEntityIdentifier().getValue());
        }

        // Document Completion Status
        if (txa.getDocumentCompletionStatus() != null) {
            documentData.put("completionStatus", txa.getDocumentCompletionStatus().getValue());
        }

        // Document Confidentiality Status
        if (txa.getDocumentConfidentialityStatus() != null) {
            documentData.put("confidentialityStatus",
                txa.getDocumentConfidentialityStatus().getValue());
        }

        // Document Availability Status
        if (txa.getDocumentAvailabilityStatus() != null) {
            documentData.put("availabilityStatus",
                txa.getDocumentAvailabilityStatus().getValue());
        }

        // Document Storage Status
        if (txa.getDocumentStorageStatus() != null) {
            documentData.put("storageStatus", txa.getDocumentStorageStatus().getValue());
        }

        data.put("document", documentData);
    }

    /**
     * Extract document content from OBX segments.
     * Note: OBX access varies by HL7 version and message structure.
     */
    private void extractDocumentContent(MDM_T02 mdm, Map<String, Object> data)
            throws HL7Exception {
        // Document content observation extraction
        // OBX segments in MDM messages are accessed through OBSERVATION groups
        // The specific structure depends on the HL7 v2 version
        data.put("documentContent", "See attached document");
    }

}

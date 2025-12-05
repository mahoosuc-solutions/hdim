package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ORM_O01;
import ca.uhn.hl7v2.model.v25.segment.ORC;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.PID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for ORM (Order) messages.
 *
 * Supports:
 * - ORM^O01: General order message
 */
@Slf4j
@Component
public class OrmMessageHandler {

    /**
     * Handle ORM message and extract order data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing ORM message");

        Map<String, Object> data = new HashMap<>();

        if (!(message instanceof ORM_O01)) {
            log.warn("Message is not ORM_O01 type");
            return data;
        }

        ORM_O01 orm = (ORM_O01) message;

        // Extract patient data
        PID pid = orm.getPATIENT().getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract order data
        ORC orc = orm.getORDER().getORC();
        if (orc != null) {
            extractOrderControl(orc, data);
        }

        // Extract observation request
        OBR obr = orm.getORDER().getORDER_DETAIL().getOBR();
        if (obr != null) {
            extractObservationRequest(obr, data);
        }

        return data;
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
        }

        // Patient Name
        if (pid.getPatientName(0) != null) {
            String familyName = pid.getPatientName(0).getFamilyName().getSurname().getValue();
            String givenName = pid.getPatientName(0).getGivenName().getValue();
            patientData.put("familyName", familyName);
            patientData.put("givenName", givenName);
            patientData.put("fullName", givenName + " " + familyName);
        }

        data.put("patient", patientData);
    }

    /**
     * Extract order control data from ORC segment.
     */
    private void extractOrderControl(ORC orc, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> orderControl = new HashMap<>();

        // Order Control
        if (orc.getOrderControl() != null) {
            orderControl.put("orderControl", orc.getOrderControl().getValue());
        }

        // Placer Order Number
        if (orc.getPlacerOrderNumber() != null) {
            orderControl.put("placerOrderNumber",
                orc.getPlacerOrderNumber().getEntityIdentifier().getValue());
        }

        // Filler Order Number
        if (orc.getFillerOrderNumber() != null) {
            orderControl.put("fillerOrderNumber",
                orc.getFillerOrderNumber().getEntityIdentifier().getValue());
        }

        // Order Status
        if (orc.getOrderStatus() != null) {
            orderControl.put("orderStatus", orc.getOrderStatus().getValue());
        }

        // Transaction Date/Time
        if (orc.getDateTimeOfTransaction() != null &&
            orc.getDateTimeOfTransaction().getTime() != null) {
            orderControl.put("transactionDateTime",
                orc.getDateTimeOfTransaction().getTime().getValue());
        }

        // Entered By
        if (orc.getEnteredBy(0) != null) {
            Map<String, String> enteredBy = new HashMap<>();
            enteredBy.put("id", orc.getEnteredBy(0).getIDNumber().getValue());
            enteredBy.put("familyName",
                orc.getEnteredBy(0).getFamilyName().getSurname().getValue());
            enteredBy.put("givenName", orc.getEnteredBy(0).getGivenName().getValue());
            orderControl.put("enteredBy", enteredBy);
        }

        // Ordering Provider
        if (orc.getOrderingProvider(0) != null) {
            Map<String, String> provider = new HashMap<>();
            provider.put("id", orc.getOrderingProvider(0).getIDNumber().getValue());
            provider.put("familyName",
                orc.getOrderingProvider(0).getFamilyName().getSurname().getValue());
            provider.put("givenName", orc.getOrderingProvider(0).getGivenName().getValue());
            orderControl.put("orderingProvider", provider);
        }

        // Ordering Facility Name
        if (orc.getOrderingFacilityName(0) != null) {
            orderControl.put("orderingFacility",
                orc.getOrderingFacilityName(0).getOrganizationName().getValue());
        }

        data.put("orderControl", orderControl);
    }

    /**
     * Extract observation request data from OBR segment.
     */
    private void extractObservationRequest(OBR obr, Map<String, Object> data)
            throws HL7Exception {
        Map<String, Object> requestData = new HashMap<>();

        // Set ID
        if (obr.getSetIDOBR() != null) {
            requestData.put("setId", obr.getSetIDOBR().getValue());
        }

        // Placer Order Number
        if (obr.getPlacerOrderNumber() != null) {
            requestData.put("placerOrderNumber",
                obr.getPlacerOrderNumber().getEntityIdentifier().getValue());
        }

        // Filler Order Number
        if (obr.getFillerOrderNumber() != null) {
            requestData.put("fillerOrderNumber",
                obr.getFillerOrderNumber().getEntityIdentifier().getValue());
        }

        // Universal Service ID
        if (obr.getUniversalServiceIdentifier() != null) {
            Map<String, String> serviceId = new HashMap<>();
            serviceId.put("identifier",
                obr.getUniversalServiceIdentifier().getIdentifier().getValue());
            serviceId.put("text",
                obr.getUniversalServiceIdentifier().getText().getValue());
            serviceId.put("codingSystem",
                obr.getUniversalServiceIdentifier().getNameOfCodingSystem().getValue());
            requestData.put("serviceId", serviceId);
        }

        // Priority
        if (obr.getPriorityOBR() != null) {
            requestData.put("priority", obr.getPriorityOBR().getValue());
        }

        // Requested Date/Time
        if (obr.getRequestedDateTime() != null &&
            obr.getRequestedDateTime().getTime() != null) {
            requestData.put("requestedDateTime",
                obr.getRequestedDateTime().getTime().getValue());
        }

        // Observation Date/Time
        if (obr.getObservationDateTime() != null &&
            obr.getObservationDateTime().getTime() != null) {
            requestData.put("observationDateTime",
                obr.getObservationDateTime().getTime().getValue());
        }

        // Ordering Provider
        if (obr.getOrderingProvider(0) != null) {
            Map<String, String> provider = new HashMap<>();
            provider.put("id", obr.getOrderingProvider(0).getIDNumber().getValue());
            provider.put("familyName",
                obr.getOrderingProvider(0).getFamilyName().getSurname().getValue());
            provider.put("givenName", obr.getOrderingProvider(0).getGivenName().getValue());
            requestData.put("orderingProvider", provider);
        }

        // Reason for Study
        if (obr.getReasonForStudy(0) != null) {
            requestData.put("reasonForStudy",
                obr.getReasonForStudy(0).getText().getValue());
        }

        data.put("observationRequest", requestData);
    }
}

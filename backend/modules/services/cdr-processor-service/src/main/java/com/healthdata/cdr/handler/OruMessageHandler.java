package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.PID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for ORU (Observation Result / Lab Results) messages.
 *
 * Supports:
 * - ORU^R01: Unsolicited transmission of observation results
 */
@Slf4j
@Component
public class OruMessageHandler {

    /**
     * Handle ORU message and extract lab results.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing ORU message");

        Map<String, Object> data = new HashMap<>();

        if (!(message instanceof ORU_R01)) {
            log.warn("Message is not ORU_R01 type");
            return data;
        }

        ORU_R01 oru = (ORU_R01) message;

        // Extract patient data
        PID pid = oru.getPATIENT_RESULT().getPATIENT().getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract order observations
        List<Map<String, Object>> orderObservations = new ArrayList<>();
        int orderCount = oru.getPATIENT_RESULTReps();

        for (int i = 0; i < orderCount; i++) {
            ORU_R01_ORDER_OBSERVATION orderObs =
                oru.getPATIENT_RESULT(i).getORDER_OBSERVATION();

            if (orderObs != null) {
                Map<String, Object> orderData = extractOrderObservation(orderObs);
                orderObservations.add(orderData);
            }
        }

        data.put("orderObservations", orderObservations);
        data.put("totalOrders", orderObservations.size());

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
     * Extract order observation data.
     */
    private Map<String, Object> extractOrderObservation(ORU_R01_ORDER_OBSERVATION orderObs)
            throws HL7Exception {
        Map<String, Object> orderData = new HashMap<>();

        // Extract OBR (Observation Request) segment
        OBR obr = orderObs.getOBR();
        if (obr != null) {
            extractObservationRequest(obr, orderData);
        }

        // Extract OBX (Observation Result) segments
        List<Map<String, Object>> observations = new ArrayList<>();
        int obsCount = orderObs.getOBSERVATIONReps();

        for (int i = 0; i < obsCount; i++) {
            ORU_R01_OBSERVATION obs = orderObs.getOBSERVATION(i);
            OBX obx = obs.getOBX();

            if (obx != null) {
                Map<String, Object> observationData = extractObservationResult(obx);
                observations.add(observationData);
            }
        }

        orderData.put("observations", observations);
        orderData.put("totalObservations", observations.size());

        return orderData;
    }

    /**
     * Extract observation request data from OBR segment.
     */
    private void extractObservationRequest(OBR obr, Map<String, Object> orderData)
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

        // Observation Date/Time
        if (obr.getObservationDateTime() != null && obr.getObservationDateTime().getTime() != null) {
            requestData.put("observationDateTime",
                obr.getObservationDateTime().getTime().getValue());
        }

        // Result Status
        if (obr.getResultStatus() != null) {
            requestData.put("resultStatus", obr.getResultStatus().getValue());
        }

        orderData.put("request", requestData);
    }

    /**
     * Extract observation result data from OBX segment.
     */
    private Map<String, Object> extractObservationResult(OBX obx) throws HL7Exception {
        Map<String, Object> resultData = new HashMap<>();

        // Set ID
        if (obx.getSetIDOBX() != null) {
            resultData.put("setId", obx.getSetIDOBX().getValue());
        }

        // Value Type
        if (obx.getValueType() != null) {
            resultData.put("valueType", obx.getValueType().getValue());
        }

        // Observation Identifier
        if (obx.getObservationIdentifier() != null) {
            Map<String, String> identifier = new HashMap<>();
            identifier.put("code",
                obx.getObservationIdentifier().getIdentifier().getValue());
            identifier.put("text",
                obx.getObservationIdentifier().getText().getValue());
            identifier.put("codingSystem",
                obx.getObservationIdentifier().getNameOfCodingSystem().getValue());
            resultData.put("identifier", identifier);
        }

        // Observation Sub-ID
        if (obx.getObservationSubID() != null) {
            resultData.put("subId", obx.getObservationSubID().getValue());
        }

        // Observation Value
        if (obx.getObservationValue(0) != null) {
            resultData.put("value", obx.getObservationValue(0).getData().toString());
        }

        // Units
        if (obx.getUnits() != null) {
            Map<String, String> units = new HashMap<>();
            units.put("identifier", obx.getUnits().getIdentifier().getValue());
            units.put("text", obx.getUnits().getText().getValue());
            resultData.put("units", units);
        }

        // Reference Range
        if (obx.getReferencesRange() != null) {
            resultData.put("referenceRange", obx.getReferencesRange().getValue());
        }

        // Abnormal Flags
        if (obx.getAbnormalFlags(0) != null) {
            resultData.put("abnormalFlag", obx.getAbnormalFlags(0).getValue());
        }

        // Observation Result Status
        if (obx.getObservationResultStatus() != null) {
            resultData.put("resultStatus", obx.getObservationResultStatus().getValue());
        }

        // Date/Time of Observation
        if (obx.getDateTimeOfTheObservation() != null &&
            obx.getDateTimeOfTheObservation().getTime() != null) {
            resultData.put("observationDateTime",
                obx.getDateTimeOfTheObservation().getTime().getValue());
        }

        return resultData;
    }
}

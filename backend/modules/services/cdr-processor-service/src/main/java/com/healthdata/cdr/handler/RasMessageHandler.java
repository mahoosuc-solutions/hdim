package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.RAS_O17;
import ca.uhn.hl7v2.model.v25.segment.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for RAS^O17 (Pharmacy/Treatment Administration) messages.
 *
 * Extracts:
 * - Patient information (PID)
 * - Order control (ORC)
 * - Pharmacy/Treatment Administration (RXA)
 * - Route (RXR)
 *
 * Maps to FHIR MedicationAdministration resource.
 */
@Slf4j
@Component
public class RasMessageHandler {

    /**
     * Handle RAS^O17 message and extract relevant data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing RAS^O17 message");

        Map<String, Object> data = new HashMap<>();

        if (!(message instanceof RAS_O17)) {
            log.warn("Message is not RAS_O17 type");
            return data;
        }

        RAS_O17 ras = (RAS_O17) message;

        // Extract patient data
        try {
            PID pid = ras.getPATIENT().getPID();
            if (pid != null) {
                extractPatientData(pid, data);
            }
        } catch (Exception e) {
            log.debug("No patient segment found: {}", e.getMessage());
        }

        // Extract administration data
        extractAdministrationData(ras, data);

        return data;
    }

    private void extractAdministrationData(RAS_O17 ras, Map<String, Object> data) throws HL7Exception {
        List<Map<String, Object>> administrations = new ArrayList<>();

        int orderCount = ras.getORDERReps();
        for (int i = 0; i < orderCount; i++) {
            Map<String, Object> adminData = new HashMap<>();

            // ORC - Order Control
            ORC orc = ras.getORDER(i).getORC();
            if (orc != null) {
                extractOrderControl(orc, adminData);
            }

            // RXA - Pharmacy/Treatment Administration (can have multiple)
            try {
                int adminCount = ras.getORDER(i).getADMINISTRATIONReps();
                List<Map<String, Object>> rxaList = new ArrayList<>();

                for (int j = 0; j < adminCount; j++) {
                    RXA rxa = ras.getORDER(i).getADMINISTRATION(j).getRXA();
                    if (rxa != null) {
                        rxaList.add(extractAdministrationSegment(rxa));
                    }

                    // RXR for this administration
                    RXR rxr = ras.getORDER(i).getADMINISTRATION(j).getRXR();
                    if (rxr != null && !rxaList.isEmpty()) {
                        Map<String, Object> lastAdmin = rxaList.get(rxaList.size() - 1);
                        lastAdmin.put("route", extractRoute(rxr));
                    }
                }
                adminData.put("administrations", rxaList);
            } catch (Exception e) {
                log.debug("Error extracting administrations: {}", e.getMessage());
            }

            administrations.add(adminData);
        }

        data.put("medicationAdministrations", administrations);
    }

    private void extractOrderControl(ORC orc, Map<String, Object> adminData) throws HL7Exception {
        // Order Control Code
        if (orc.getOrderControl() != null) {
            adminData.put("orderControlCode", orc.getOrderControl().getValue());
        }

        // Placer Order Number
        if (orc.getPlacerOrderNumber() != null && orc.getPlacerOrderNumber().getEntityIdentifier() != null) {
            adminData.put("placerOrderNumber", orc.getPlacerOrderNumber().getEntityIdentifier().getValue());
        }

        // Filler Order Number
        if (orc.getFillerOrderNumber() != null && orc.getFillerOrderNumber().getEntityIdentifier() != null) {
            adminData.put("fillerOrderNumber", orc.getFillerOrderNumber().getEntityIdentifier().getValue());
        }
    }

    private Map<String, Object> extractAdministrationSegment(RXA rxa) throws HL7Exception {
        Map<String, Object> admin = new HashMap<>();

        // Give Sub-ID Counter
        if (rxa.getGiveSubIDCounter() != null) {
            admin.put("giveSubIdCounter", rxa.getGiveSubIDCounter().getValue());
        }

        // Administration Sub-ID Counter
        if (rxa.getAdministrationSubIDCounter() != null) {
            admin.put("administrationSubIdCounter", rxa.getAdministrationSubIDCounter().getValue());
        }

        // Date/Time Start of Administration
        if (rxa.getDateTimeStartOfAdministration() != null &&
            rxa.getDateTimeStartOfAdministration().getTime() != null) {
            admin.put("startDateTime", rxa.getDateTimeStartOfAdministration().getTime().getValue());
        }

        // Date/Time End of Administration
        if (rxa.getDateTimeEndOfAdministration() != null &&
            rxa.getDateTimeEndOfAdministration().getTime() != null) {
            admin.put("endDateTime", rxa.getDateTimeEndOfAdministration().getTime().getValue());
        }

        // Administered Code
        if (rxa.getAdministeredCode() != null) {
            Map<String, String> drugCode = new HashMap<>();
            drugCode.put("code", rxa.getAdministeredCode().getIdentifier().getValue());
            drugCode.put("display", rxa.getAdministeredCode().getText().getValue());
            drugCode.put("system", rxa.getAdministeredCode().getNameOfCodingSystem().getValue());
            admin.put("administeredCode", drugCode);
        }

        // Administered Amount
        if (rxa.getAdministeredAmount() != null) {
            admin.put("administeredAmount", rxa.getAdministeredAmount().getValue());
        }

        // Administered Units
        if (rxa.getAdministeredUnits() != null) {
            admin.put("administeredUnits", rxa.getAdministeredUnits().getIdentifier().getValue());
            admin.put("administeredUnitsDisplay", rxa.getAdministeredUnits().getText().getValue());
        }

        // Administered Dosage Form
        if (rxa.getAdministeredDosageForm() != null) {
            admin.put("administeredDosageForm", rxa.getAdministeredDosageForm().getIdentifier().getValue());
        }

        // Administration Notes
        if (rxa.getAdministrationNotes(0) != null) {
            admin.put("administrationNotes", rxa.getAdministrationNotes(0).getText().getValue());
        }

        // Administering Provider
        if (rxa.getAdministeringProvider(0) != null) {
            Map<String, String> provider = new HashMap<>();
            provider.put("id", rxa.getAdministeringProvider(0).getIDNumber().getValue());
            provider.put("familyName", rxa.getAdministeringProvider(0).getFamilyName().getSurname().getValue());
            provider.put("givenName", rxa.getAdministeringProvider(0).getGivenName().getValue());
            admin.put("administeringProvider", provider);
        }

        // Administered-at Location
        if (rxa.getAdministeredAtLocation() != null) {
            Map<String, String> location = new HashMap<>();
            location.put("pointOfCare", rxa.getAdministeredAtLocation().getPointOfCare().getValue());
            location.put("room", rxa.getAdministeredAtLocation().getRoom().getValue());
            location.put("bed", rxa.getAdministeredAtLocation().getBed().getValue());
            location.put("facility", rxa.getAdministeredAtLocation().getFacility().getNamespaceID().getValue());
            admin.put("location", location);
        }

        // Administered Per (Time Unit)
        if (rxa.getAdministeredPerTimeUnit() != null) {
            admin.put("administeredPerTimeUnit", rxa.getAdministeredPerTimeUnit().getValue());
        }

        // Administered Strength
        if (rxa.getAdministeredStrength() != null) {
            admin.put("administeredStrength", rxa.getAdministeredStrength().getValue());
        }

        // Administered Strength Units
        if (rxa.getAdministeredStrengthUnits() != null) {
            admin.put("administeredStrengthUnits", rxa.getAdministeredStrengthUnits().getText().getValue());
        }

        // Substance Lot Number
        if (rxa.getSubstanceLotNumber(0) != null) {
            admin.put("lotNumber", rxa.getSubstanceLotNumber(0).getValue());
        }

        // Substance Expiration Date
        if (rxa.getSubstanceExpirationDate(0) != null &&
            rxa.getSubstanceExpirationDate(0).getTime() != null) {
            admin.put("expirationDate", rxa.getSubstanceExpirationDate(0).getTime().getValue());
        }

        // Substance Manufacturer Name
        if (rxa.getSubstanceManufacturerName(0) != null) {
            admin.put("manufacturer", rxa.getSubstanceManufacturerName(0).getText().getValue());
        }

        // Substance/Treatment Refusal Reason
        if (rxa.getSubstanceTreatmentRefusalReason(0) != null) {
            admin.put("refusalReason", rxa.getSubstanceTreatmentRefusalReason(0).getText().getValue());
        }

        // Indication
        if (rxa.getIndication(0) != null) {
            Map<String, String> indication = new HashMap<>();
            indication.put("code", rxa.getIndication(0).getIdentifier().getValue());
            indication.put("display", rxa.getIndication(0).getText().getValue());
            admin.put("indication", indication);
        }

        // Completion Status
        if (rxa.getCompletionStatus() != null) {
            String status = rxa.getCompletionStatus().getValue();
            admin.put("completionStatus", status);
            admin.put("fhirStatus", mapCompletionStatus(status));
        }

        // Action Code - RXA
        if (rxa.getActionCodeRXA() != null) {
            admin.put("actionCode", rxa.getActionCodeRXA().getValue());
        }

        // System Entry Date/Time
        if (rxa.getSystemEntryDateTime() != null && rxa.getSystemEntryDateTime().getTime() != null) {
            admin.put("systemEntryDateTime", rxa.getSystemEntryDateTime().getTime().getValue());
        }

        return admin;
    }

    private Map<String, String> extractRoute(RXR rxr) throws HL7Exception {
        Map<String, String> route = new HashMap<>();

        // Route
        if (rxr.getRoute() != null) {
            route.put("code", rxr.getRoute().getIdentifier().getValue());
            route.put("display", rxr.getRoute().getText().getValue());
            route.put("system", rxr.getRoute().getNameOfCodingSystem().getValue());
        }

        // Administration Site
        if (rxr.getAdministrationSite() != null) {
            route.put("siteCode", rxr.getAdministrationSite().getIdentifier().getValue());
            route.put("siteDisplay", rxr.getAdministrationSite().getText().getValue());
        }

        // Administration Method
        if (rxr.getAdministrationMethod() != null) {
            route.put("method", rxr.getAdministrationMethod().getIdentifier().getValue());
        }

        return route;
    }

    private String mapCompletionStatus(String hl7Status) {
        if (hl7Status == null) return "completed";

        return switch (hl7Status) {
            case "CP" -> "completed";      // Complete
            case "RE" -> "not-done";       // Refused
            case "NA" -> "not-done";       // Not Administered
            case "PA" -> "stopped";        // Partially Administered
            default -> "completed";
        };
    }

    private void extractPatientData(PID pid, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> patientData = new HashMap<>();

        // Patient ID
        if (pid.getPatientID() != null && pid.getPatientID().getIDNumber() != null) {
            patientData.put("patientId", pid.getPatientID().getIDNumber().getValue());
        }

        // Patient Identifier List
        if (pid.getPatientIdentifierList(0) != null) {
            patientData.put("patientIdentifier",
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
}

package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.VXU_V04;
import ca.uhn.hl7v2.model.v25.segment.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for VXU^V04 (Unsolicited Vaccination Record Update) messages.
 *
 * Extracts:
 * - Patient information (PID)
 * - Patient visit (PV1)
 * - Next of Kin (NK1) for guardian info
 * - Administered vaccine (RXA)
 * - Route (RXR)
 * - Observations (OBX) for funding source, eligibility, VIS info
 *
 * Maps to FHIR Immunization resource.
 */
@Slf4j
@Component
public class VxuMessageHandler {

    /**
     * Handle VXU^V04 message and extract relevant data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing VXU^V04 message");

        Map<String, Object> data = new HashMap<>();

        if (!(message instanceof VXU_V04)) {
            log.warn("Message is not VXU_V04 type");
            return data;
        }

        VXU_V04 vxu = (VXU_V04) message;

        // Extract patient data
        PID pid = vxu.getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract next of kin (for pediatric patients)
        extractNextOfKin(vxu, data);

        // Extract vaccination data
        extractVaccinationData(vxu, data);

        return data;
    }

    private void extractVaccinationData(VXU_V04 vxu, Map<String, Object> data) throws HL7Exception {
        List<Map<String, Object>> immunizations = new ArrayList<>();

        int orderCount = vxu.getORDERReps();
        for (int i = 0; i < orderCount; i++) {
            Map<String, Object> immunization = new HashMap<>();

            // RXA - Administered vaccine
            RXA rxa = vxu.getORDER(i).getRXA();
            if (rxa != null) {
                extractVaccineAdministration(rxa, immunization);
            }

            // RXR - Route
            RXR rxr = vxu.getORDER(i).getRXR();
            if (rxr != null) {
                extractRoute(rxr, immunization);
            }

            // OBX - Observations (funding, eligibility, VIS)
            try {
                int obxCount = vxu.getORDER(i).getOBSERVATIONReps();
                List<Map<String, Object>> observations = new ArrayList<>();
                for (int j = 0; j < obxCount; j++) {
                    OBX obx = vxu.getORDER(i).getOBSERVATION(j).getOBX();
                    if (obx != null) {
                        observations.add(extractVaccineObservation(obx));
                    }
                }
                immunization.put("observations", observations);

                // Extract specific observations
                processVaccineObservations(observations, immunization);
            } catch (Exception e) {
                log.debug("Error extracting observations: {}", e.getMessage());
            }

            immunizations.add(immunization);
        }

        data.put("immunizations", immunizations);
    }

    private void extractVaccineAdministration(RXA rxa, Map<String, Object> immunization) throws HL7Exception {
        // Give Sub-ID Counter
        if (rxa.getGiveSubIDCounter() != null) {
            immunization.put("giveSubIdCounter", rxa.getGiveSubIDCounter().getValue());
        }

        // Administration Sub-ID Counter
        if (rxa.getAdministrationSubIDCounter() != null) {
            immunization.put("administrationSubIdCounter", rxa.getAdministrationSubIDCounter().getValue());
        }

        // Date/Time Start of Administration
        if (rxa.getDateTimeStartOfAdministration() != null &&
            rxa.getDateTimeStartOfAdministration().getTime() != null) {
            immunization.put("administrationDate", rxa.getDateTimeStartOfAdministration().getTime().getValue());
        }

        // Date/Time End of Administration
        if (rxa.getDateTimeEndOfAdministration() != null &&
            rxa.getDateTimeEndOfAdministration().getTime() != null) {
            immunization.put("administrationEndDate", rxa.getDateTimeEndOfAdministration().getTime().getValue());
        }

        // Administered Code (CVX code)
        if (rxa.getAdministeredCode() != null) {
            Map<String, String> vaccineCode = new HashMap<>();
            vaccineCode.put("code", rxa.getAdministeredCode().getIdentifier().getValue());
            vaccineCode.put("display", rxa.getAdministeredCode().getText().getValue());
            vaccineCode.put("system", rxa.getAdministeredCode().getNameOfCodingSystem().getValue());
            immunization.put("vaccineCode", vaccineCode);

            // Map CVX system
            String codingSystem = rxa.getAdministeredCode().getNameOfCodingSystem().getValue();
            if ("CVX".equals(codingSystem)) {
                immunization.put("vaccineCodeSystemUri", "http://hl7.org/fhir/sid/cvx");
            }
        }

        // Administered Amount
        if (rxa.getAdministeredAmount() != null) {
            immunization.put("doseQuantity", rxa.getAdministeredAmount().getValue());
        }

        // Administered Units
        if (rxa.getAdministeredUnits() != null) {
            immunization.put("doseUnit", rxa.getAdministeredUnits().getIdentifier().getValue());
        }

        // Administered Dosage Form
        if (rxa.getAdministeredDosageForm() != null) {
            immunization.put("dosageForm", rxa.getAdministeredDosageForm().getText().getValue());
        }

        // Substance Lot Number
        if (rxa.getSubstanceLotNumber(0) != null) {
            immunization.put("lotNumber", rxa.getSubstanceLotNumber(0).getValue());
        }

        // Substance Expiration Date
        if (rxa.getSubstanceExpirationDate(0) != null &&
            rxa.getSubstanceExpirationDate(0).getTime() != null) {
            immunization.put("expirationDate", rxa.getSubstanceExpirationDate(0).getTime().getValue());
        }

        // Substance Manufacturer Name (MVX code)
        if (rxa.getSubstanceManufacturerName(0) != null) {
            immunization.put("manufacturer", rxa.getSubstanceManufacturerName(0).getText().getValue());
            immunization.put("manufacturerCode",
                rxa.getSubstanceManufacturerName(0).getIdentifier().getValue());
        }

        // Administering Provider
        if (rxa.getAdministeringProvider(0) != null) {
            Map<String, String> provider = new HashMap<>();
            provider.put("id", rxa.getAdministeringProvider(0).getIDNumber().getValue());
            provider.put("familyName", rxa.getAdministeringProvider(0).getFamilyName().getSurname().getValue());
            provider.put("givenName", rxa.getAdministeringProvider(0).getGivenName().getValue());
            immunization.put("performer", provider);
        }

        // Administered-at Location
        if (rxa.getAdministeredAtLocation() != null) {
            Map<String, String> location = new HashMap<>();
            location.put("facility", rxa.getAdministeredAtLocation().getFacility().getNamespaceID().getValue());
            location.put("pointOfCare", rxa.getAdministeredAtLocation().getPointOfCare().getValue());
            immunization.put("location", location);
        }

        // Note: Ordering Provider is typically in ORC segment (ORC-12), not RXA
        // If ORC segment data is available, it should be passed separately

        // Completion Status
        if (rxa.getCompletionStatus() != null) {
            String status = rxa.getCompletionStatus().getValue();
            immunization.put("completionStatus", status);
            immunization.put("status", mapCompletionStatus(status));
        }

        // Action Code
        if (rxa.getActionCodeRXA() != null) {
            immunization.put("actionCode", rxa.getActionCodeRXA().getValue());
        }

        // Substance/Treatment Refusal Reason
        if (rxa.getSubstanceTreatmentRefusalReason(0) != null) {
            Map<String, String> refusalReason = new HashMap<>();
            refusalReason.put("code", rxa.getSubstanceTreatmentRefusalReason(0).getIdentifier().getValue());
            refusalReason.put("display", rxa.getSubstanceTreatmentRefusalReason(0).getText().getValue());
            immunization.put("refusalReason", refusalReason);
        }

        // Indication
        if (rxa.getIndication(0) != null) {
            Map<String, String> indication = new HashMap<>();
            indication.put("code", rxa.getIndication(0).getIdentifier().getValue());
            indication.put("display", rxa.getIndication(0).getText().getValue());
            immunization.put("indication", indication);
        }

        // Administered Drug Strength Volume
        if (rxa.getAdministeredDrugStrengthVolume() != null) {
            immunization.put("drugStrengthVolume", rxa.getAdministeredDrugStrengthVolume().getValue());
        }

        // Administered Drug Strength Volume Units
        if (rxa.getAdministeredDrugStrengthVolumeUnits() != null) {
            immunization.put("drugStrengthVolumeUnits",
                rxa.getAdministeredDrugStrengthVolumeUnits().getIdentifier().getValue());
        }
    }

    private void extractRoute(RXR rxr, Map<String, Object> immunization) throws HL7Exception {
        // Route
        if (rxr.getRoute() != null) {
            Map<String, String> route = new HashMap<>();
            route.put("code", rxr.getRoute().getIdentifier().getValue());
            route.put("display", rxr.getRoute().getText().getValue());
            route.put("system", rxr.getRoute().getNameOfCodingSystem().getValue());
            immunization.put("route", route);
        }

        // Administration Site
        if (rxr.getAdministrationSite() != null) {
            Map<String, String> site = new HashMap<>();
            site.put("code", rxr.getAdministrationSite().getIdentifier().getValue());
            site.put("display", rxr.getAdministrationSite().getText().getValue());
            immunization.put("site", site);
        }

        // Administration Method
        if (rxr.getAdministrationMethod() != null) {
            immunization.put("administrationMethod", rxr.getAdministrationMethod().getText().getValue());
        }
    }

    private Map<String, Object> extractVaccineObservation(OBX obx) throws HL7Exception {
        Map<String, Object> observation = new HashMap<>();

        // Observation Identifier
        if (obx.getObservationIdentifier() != null) {
            Map<String, String> code = new HashMap<>();
            code.put("code", obx.getObservationIdentifier().getIdentifier().getValue());
            code.put("display", obx.getObservationIdentifier().getText().getValue());
            code.put("system", obx.getObservationIdentifier().getNameOfCodingSystem().getValue());
            observation.put("code", code);
        }

        // Value Type
        if (obx.getValueType() != null) {
            observation.put("valueType", obx.getValueType().getValue());
        }

        // Observation Value
        if (obx.getObservationValue(0) != null) {
            observation.put("value", obx.getObservationValue(0).getData().toString());
        }

        // Observation Date/Time
        if (obx.getDateTimeOfTheObservation() != null &&
            obx.getDateTimeOfTheObservation().getTime() != null) {
            observation.put("dateTime", obx.getDateTimeOfTheObservation().getTime().getValue());
        }

        // Observation Result Status
        if (obx.getObservationResultStatus() != null) {
            observation.put("status", obx.getObservationResultStatus().getValue());
        }

        return observation;
    }

    private void processVaccineObservations(List<Map<String, Object>> observations, Map<String, Object> immunization) {
        for (Map<String, Object> obs : observations) {
            @SuppressWarnings("unchecked")
            Map<String, String> code = (Map<String, String>) obs.get("code");
            if (code == null) continue;

            String obsCode = code.get("code");
            if (obsCode == null) continue;

            // Process specific observation types
            switch (obsCode) {
                case "64994-7" -> // Vaccine Funding Source
                    immunization.put("fundingSource", obs.get("value"));
                case "30956-7" -> // Vaccine Type (from barcode)
                    immunization.put("barcodeVaccineType", obs.get("value"));
                case "30963-3" -> // Vaccine Funding Program Eligibility
                    immunization.put("fundingProgramEligibility", obs.get("value"));
                case "69764-9" -> // Document Type (VIS)
                    immunization.put("visDocumentType", obs.get("value"));
                case "29768-9" -> // Date VIS Presented
                    immunization.put("visPresentedDate", obs.get("value"));
                case "29769-7" -> // VIS Publication Date
                    immunization.put("visPublicationDate", obs.get("value"));
            }
        }
    }

    private String mapCompletionStatus(String hl7Status) {
        if (hl7Status == null) return "completed";

        return switch (hl7Status) {
            case "CP" -> "completed";      // Complete
            case "RE" -> "not-done";       // Refused
            case "NA" -> "not-done";       // Not Administered
            case "PA" -> "not-done";       // Partially Administered
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

        // Date of Birth
        if (pid.getDateTimeOfBirth() != null && pid.getDateTimeOfBirth().getTime() != null) {
            patientData.put("dateOfBirth", pid.getDateTimeOfBirth().getTime().getValue());
        }

        // Gender
        if (pid.getAdministrativeSex() != null) {
            patientData.put("gender", pid.getAdministrativeSex().getValue());
        }

        // Mother's Identifier (for newborns)
        if (pid.getMotherSIdentifier(0) != null) {
            patientData.put("motherIdentifier", pid.getMotherSIdentifier(0).getIDNumber().getValue());
        }

        // Birth Order
        if (pid.getBirthOrder() != null) {
            patientData.put("birthOrder", pid.getBirthOrder().getValue());
        }

        // Multiple Birth Indicator
        if (pid.getMultipleBirthIndicator() != null) {
            patientData.put("multipleBirth", "Y".equals(pid.getMultipleBirthIndicator().getValue()));
        }

        data.put("patient", patientData);
    }

    private void extractNextOfKin(VXU_V04 vxu, Map<String, Object> data) throws HL7Exception {
        try {
            int nk1Count = vxu.getNK1Reps();
            if (nk1Count > 0) {
                List<Map<String, Object>> nextOfKins = new ArrayList<>();

                for (int i = 0; i < nk1Count; i++) {
                    NK1 nk1 = vxu.getNK1(i);
                    Map<String, Object> nk = new HashMap<>();

                    // Name
                    if (nk1.getNKName(0) != null) {
                        nk.put("familyName", nk1.getNKName(0).getFamilyName().getSurname().getValue());
                        nk.put("givenName", nk1.getNKName(0).getGivenName().getValue());
                    }

                    // Relationship
                    if (nk1.getRelationship() != null) {
                        nk.put("relationship", nk1.getRelationship().getIdentifier().getValue());
                        nk.put("relationshipDisplay", nk1.getRelationship().getText().getValue());
                    }

                    // Address
                    if (nk1.getAddress(0) != null) {
                        Map<String, String> address = new HashMap<>();
                        address.put("street", nk1.getAddress(0).getStreetAddress().getStreetOrMailingAddress().getValue());
                        address.put("city", nk1.getAddress(0).getCity().getValue());
                        address.put("state", nk1.getAddress(0).getStateOrProvince().getValue());
                        address.put("zip", nk1.getAddress(0).getZipOrPostalCode().getValue());
                        nk.put("address", address);
                    }

                    // Phone
                    if (nk1.getPhoneNumber(0) != null) {
                        nk.put("phone", nk1.getPhoneNumber(0).getTelephoneNumber().getValue());
                    }

                    nextOfKins.add(nk);
                }

                data.put("nextOfKin", nextOfKins);
            }
        } catch (Exception e) {
            log.debug("Error extracting next of kin: {}", e.getMessage());
        }
    }
}

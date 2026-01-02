package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.RDE_O11;
import ca.uhn.hl7v2.model.v25.segment.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for RDE^O11 (Pharmacy/Treatment Encoded Order) messages.
 *
 * Extracts:
 * - Patient information (PID)
 * - Patient visit (PV1)
 * - Order control (ORC)
 * - Pharmacy/Treatment order (RXE)
 * - Route (RXR)
 *
 * Maps to FHIR MedicationRequest resource.
 */
@Slf4j
@Component
public class RdeMessageHandler {

    /**
     * Handle RDE^O11 message and extract relevant data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing RDE^O11 message");

        Map<String, Object> data = new HashMap<>();

        if (!(message instanceof RDE_O11)) {
            log.warn("Message is not RDE_O11 type");
            return data;
        }

        RDE_O11 rde = (RDE_O11) message;

        // Extract patient data
        try {
            PID pid = rde.getPATIENT().getPID();
            if (pid != null) {
                extractPatientData(pid, data);
            }
        } catch (Exception e) {
            log.debug("No patient segment found: {}", e.getMessage());
        }

        // Extract visit data
        try {
            PV1 pv1 = rde.getPATIENT().getPATIENT_VISIT().getPV1();
            if (pv1 != null) {
                extractVisitData(pv1, data);
            }
        } catch (Exception e) {
            log.debug("No visit segment found: {}", e.getMessage());
        }

        // Extract order data
        extractOrderData(rde, data);

        return data;
    }

    private void extractOrderData(RDE_O11 rde, Map<String, Object> data) throws HL7Exception {
        List<Map<String, Object>> orders = new ArrayList<>();

        int orderCount = rde.getORDERReps();
        for (int i = 0; i < orderCount; i++) {
            Map<String, Object> orderData = new HashMap<>();

            // ORC - Order Control
            ORC orc = rde.getORDER(i).getORC();
            if (orc != null) {
                extractOrderControl(orc, orderData);
            }

            // RXE - Pharmacy/Treatment Encoded Order
            RXE rxe = rde.getORDER(i).getRXE();
            if (rxe != null) {
                extractPharmacyOrder(rxe, orderData);
            }

            // RXR - Route
            int rxrCount = rde.getORDER(i).getRXRReps();
            List<Map<String, String>> routes = new ArrayList<>();
            for (int j = 0; j < rxrCount; j++) {
                RXR rxr = rde.getORDER(i).getRXR(j);
                if (rxr != null) {
                    routes.add(extractRoute(rxr));
                }
            }
            orderData.put("routes", routes);

            orders.add(orderData);
        }

        data.put("orders", orders);
    }

    private void extractOrderControl(ORC orc, Map<String, Object> orderData) throws HL7Exception {
        // Order Control Code
        if (orc.getOrderControl() != null) {
            orderData.put("orderControlCode", orc.getOrderControl().getValue());
        }

        // Placer Order Number
        if (orc.getPlacerOrderNumber() != null && orc.getPlacerOrderNumber().getEntityIdentifier() != null) {
            orderData.put("placerOrderNumber", orc.getPlacerOrderNumber().getEntityIdentifier().getValue());
        }

        // Filler Order Number
        if (orc.getFillerOrderNumber() != null && orc.getFillerOrderNumber().getEntityIdentifier() != null) {
            orderData.put("fillerOrderNumber", orc.getFillerOrderNumber().getEntityIdentifier().getValue());
        }

        // Order Status
        if (orc.getOrderStatus() != null) {
            orderData.put("orderStatus", orc.getOrderStatus().getValue());
        }

        // Date/Time of Transaction
        if (orc.getDateTimeOfTransaction() != null && orc.getDateTimeOfTransaction().getTime() != null) {
            orderData.put("orderDateTime", orc.getDateTimeOfTransaction().getTime().getValue());
        }

        // Ordering Provider
        if (orc.getOrderingProvider(0) != null) {
            Map<String, String> provider = new HashMap<>();
            provider.put("id", orc.getOrderingProvider(0).getIDNumber().getValue());
            provider.put("familyName", orc.getOrderingProvider(0).getFamilyName().getSurname().getValue());
            provider.put("givenName", orc.getOrderingProvider(0).getGivenName().getValue());
            orderData.put("orderingProvider", provider);
        }

        // Ordering Facility
        if (orc.getOrderingFacilityName(0) != null) {
            orderData.put("orderingFacility", orc.getOrderingFacilityName(0).getOrganizationName().getValue());
        }
    }

    private void extractPharmacyOrder(RXE rxe, Map<String, Object> orderData) throws HL7Exception {
        Map<String, Object> medication = new HashMap<>();

        // Give Code - Drug identifier
        if (rxe.getGiveCode() != null) {
            Map<String, String> drugCode = new HashMap<>();
            drugCode.put("code", rxe.getGiveCode().getIdentifier().getValue());
            drugCode.put("display", rxe.getGiveCode().getText().getValue());
            drugCode.put("system", rxe.getGiveCode().getNameOfCodingSystem().getValue());
            medication.put("drugCode", drugCode);
        }

        // Give Amount - Minimum
        if (rxe.getGiveAmountMinimum() != null) {
            medication.put("giveAmountMin", rxe.getGiveAmountMinimum().getValue());
        }

        // Give Amount - Maximum
        if (rxe.getGiveAmountMaximum() != null) {
            medication.put("giveAmountMax", rxe.getGiveAmountMaximum().getValue());
        }

        // Give Units
        if (rxe.getGiveUnits() != null) {
            medication.put("giveUnits", rxe.getGiveUnits().getIdentifier().getValue());
            medication.put("giveUnitsDisplay", rxe.getGiveUnits().getText().getValue());
        }

        // Give Dosage Form
        if (rxe.getGiveDosageForm() != null) {
            medication.put("dosageForm", rxe.getGiveDosageForm().getIdentifier().getValue());
        }

        // Provider's Administration Instructions
        if (rxe.getProviderSAdministrationInstructions(0) != null) {
            medication.put("administrationInstructions",
                rxe.getProviderSAdministrationInstructions(0).getText().getValue());
        }

        // Dispense Amount
        if (rxe.getDispenseAmount() != null) {
            medication.put("dispenseAmount", rxe.getDispenseAmount().getValue());
        }

        // Dispense Units
        if (rxe.getDispenseUnits() != null) {
            medication.put("dispenseUnits", rxe.getDispenseUnits().getIdentifier().getValue());
        }

        // Number of Refills
        if (rxe.getNumberOfRefills() != null) {
            medication.put("numberOfRefills", rxe.getNumberOfRefills().getValue());
        }

        // Ordering Provider's DEA Number
        if (rxe.getOrderingProviderSDEANumber(0) != null) {
            medication.put("orderingProviderDEA", rxe.getOrderingProviderSDEANumber(0).getIDNumber().getValue());
        }

        // Pharmacist/Treatment Supplier's Verifier ID
        if (rxe.getPharmacistTreatmentSupplierSVerifierID(0) != null) {
            medication.put("verifierId",
                rxe.getPharmacistTreatmentSupplierSVerifierID(0).getIDNumber().getValue());
        }

        // Prescription Number
        if (rxe.getPrescriptionNumber() != null) {
            medication.put("prescriptionNumber", rxe.getPrescriptionNumber().getValue());
        }

        // Total Daily Dose
        if (rxe.getTotalDailyDose() != null) {
            Map<String, String> dailyDose = new HashMap<>();
            dailyDose.put("value", rxe.getTotalDailyDose().getQuantity().getValue());
            dailyDose.put("unit", rxe.getTotalDailyDose().getUnits().getIdentifier().getValue());
            medication.put("totalDailyDose", dailyDose);
        }

        // Needs Human Review
        if (rxe.getNeedsHumanReview() != null) {
            medication.put("needsHumanReview", "Y".equals(rxe.getNeedsHumanReview().getValue()));
        }

        // Pharmacy/Treatment Instructions
        if (rxe.getPharmacyTreatmentSupplierSSpecialDispensingInstructions(0) != null) {
            medication.put("specialInstructions",
                rxe.getPharmacyTreatmentSupplierSSpecialDispensingInstructions(0).getText().getValue());
        }

        // Give Per (Time Unit)
        if (rxe.getGivePerTimeUnit() != null) {
            medication.put("givePerTimeUnit", rxe.getGivePerTimeUnit().getValue());
        }

        // Give Rate Amount
        if (rxe.getGiveRateAmount() != null) {
            medication.put("giveRateAmount", rxe.getGiveRateAmount().getValue());
        }

        // Give Rate Units
        if (rxe.getGiveRateUnits() != null) {
            medication.put("giveRateUnits", rxe.getGiveRateUnits().getIdentifier().getValue());
        }

        orderData.put("medication", medication);
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

        // Administration Device
        if (rxr.getAdministrationDevice() != null) {
            route.put("device", rxr.getAdministrationDevice().getIdentifier().getValue());
        }

        // Administration Method
        if (rxr.getAdministrationMethod() != null) {
            route.put("method", rxr.getAdministrationMethod().getIdentifier().getValue());
        }

        return route;
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

        data.put("patient", patientData);
    }

    private void extractVisitData(PV1 pv1, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> visitData = new HashMap<>();

        // Patient Class
        if (pv1.getPatientClass() != null) {
            visitData.put("patientClass", pv1.getPatientClass().getValue());
        }

        // Visit Number
        if (pv1.getVisitNumber() != null && pv1.getVisitNumber().getIDNumber() != null) {
            visitData.put("visitNumber", pv1.getVisitNumber().getIDNumber().getValue());
        }

        data.put("visit", visitData);
    }
}

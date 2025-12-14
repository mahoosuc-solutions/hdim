package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.DFT_P03;
import ca.uhn.hl7v2.model.v25.message.DFT_P11;
import ca.uhn.hl7v2.model.v25.segment.DG1;
import ca.uhn.hl7v2.model.v25.segment.EVN;
import ca.uhn.hl7v2.model.v25.segment.FT1;
import ca.uhn.hl7v2.model.v25.segment.GT1;
import ca.uhn.hl7v2.model.v25.segment.IN1;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PR1;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for DFT (Detailed Financial Transaction) messages.
 *
 * Supports:
 * - DFT^P03: Post detail financial transaction
 * - DFT^P11: Post detail financial transaction (new format)
 *
 * Converts to FHIR Claim, ChargeItem, and related financial resources.
 */
@Slf4j
@Component
public class DftMessageHandler {

    /**
     * Handle DFT message and extract financial transaction data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing DFT message");

        Map<String, Object> data = new HashMap<>();
        data.put("messageType", "DFT");

        if (message instanceof DFT_P03) {
            handleDftP03((DFT_P03) message, data);
        } else if (message instanceof DFT_P11) {
            handleDftP11((DFT_P11) message, data);
        } else {
            log.warn("Unsupported DFT message type: {}", message.getClass().getSimpleName());
        }

        return data;
    }

    /**
     * Handle DFT^P03 - Post detail financial transaction.
     */
    private void handleDftP03(DFT_P03 dft, Map<String, Object> data) throws HL7Exception {
        data.put("triggerEvent", "P03");
        data.put("eventDescription", "Post detail financial transaction");

        // Extract EVN segment
        EVN evn = dft.getEVN();
        if (evn != null) {
            extractEventData(evn, data);
        }

        // Extract patient data
        PID pid = dft.getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract visit data
        PV1 pv1 = dft.getPV1();
        if (pv1 != null) {
            extractVisitData(pv1, data);
        }

        // Extract financial transactions
        int ftCount = dft.getFINANCIALReps();
        if (ftCount > 0) {
            List<Map<String, Object>> transactions = new ArrayList<>();
            for (int i = 0; i < ftCount; i++) {
                Map<String, Object> transaction = new HashMap<>();

                // FT1 segment - Financial Transaction
                FT1 ft1 = dft.getFINANCIAL(i).getFT1();
                if (ft1 != null) {
                    extractFinancialTransaction(ft1, transaction);
                }

                // Procedures for this transaction
                int pr1Count = dft.getFINANCIAL(i).getFINANCIAL_PROCEDUREReps();
                if (pr1Count > 0) {
                    List<Map<String, Object>> procedures = new ArrayList<>();
                    for (int j = 0; j < pr1Count; j++) {
                        PR1 pr1 = dft.getFINANCIAL(i).getFINANCIAL_PROCEDURE(j).getPR1();
                        if (pr1 != null) {
                            procedures.add(extractProcedure(pr1));
                        }
                    }
                    transaction.put("procedures", procedures);
                }

                // Common Order for this transaction
                int orderCount = dft.getFINANCIAL(i).getFINANCIAL_COMMON_ORDERReps();
                if (orderCount > 0) {
                    List<Map<String, Object>> orders = new ArrayList<>();
                    for (int j = 0; j < orderCount; j++) {
                        Map<String, Object> order = new HashMap<>();

                        // OBR segment
                        OBR obr = dft.getFINANCIAL(i).getFINANCIAL_COMMON_ORDER(j)
                            .getFINANCIAL_ORDER().getOBR();
                        if (obr != null) {
                            extractObservationRequest(obr, order);
                        }

                        // Note: Financial observations (OBX) structure varies by HL7 version
                        // Additional observation extraction can be added based on specific profile

                        orders.add(order);
                    }
                    transaction.put("orders", orders);
                }

                transactions.add(transaction);
            }
            data.put("financialTransactions", transactions);
        }

        // Extract diagnoses
        int dg1Count = dft.getDG1Reps();
        if (dg1Count > 0) {
            List<Map<String, Object>> diagnoses = new ArrayList<>();
            for (int i = 0; i < dg1Count; i++) {
                DG1 dg1 = dft.getDG1(i);
                diagnoses.add(extractDiagnosis(dg1));
            }
            data.put("diagnoses", diagnoses);
        }

        // Extract guarantor information
        int gt1Count = dft.getGT1Reps();
        if (gt1Count > 0) {
            List<Map<String, Object>> guarantors = new ArrayList<>();
            for (int i = 0; i < gt1Count; i++) {
                GT1 gt1 = dft.getGT1(i);
                guarantors.add(extractGuarantor(gt1));
            }
            data.put("guarantors", guarantors);
        }

        // Extract insurance information
        int inCount = dft.getINSURANCEReps();
        if (inCount > 0) {
            List<Map<String, Object>> insurances = new ArrayList<>();
            for (int i = 0; i < inCount; i++) {
                IN1 in1 = dft.getINSURANCE(i).getIN1();
                if (in1 != null) {
                    insurances.add(extractInsurance(in1));
                }
            }
            data.put("insurances", insurances);
        }
    }

    /**
     * Handle DFT^P11 - Post detail financial transaction (new format).
     */
    private void handleDftP11(DFT_P11 dft, Map<String, Object> data) throws HL7Exception {
        data.put("triggerEvent", "P11");
        data.put("eventDescription", "Post detail financial transaction (new format)");

        // Extract EVN segment
        EVN evn = dft.getEVN();
        if (evn != null) {
            extractEventData(evn, data);
        }

        // Extract patient data
        PID pid = dft.getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract visit data
        PV1 pv1 = dft.getPV1();
        if (pv1 != null) {
            extractVisitData(pv1, data);
        }

        // Extract diagnoses
        int dg1Count = dft.getDG1Reps();
        if (dg1Count > 0) {
            List<Map<String, Object>> diagnoses = new ArrayList<>();
            for (int i = 0; i < dg1Count; i++) {
                DG1 dg1 = dft.getDG1(i);
                diagnoses.add(extractDiagnosis(dg1));
            }
            data.put("diagnoses", diagnoses);
        }

        // Extract guarantor information
        int gt1Count = dft.getGT1Reps();
        if (gt1Count > 0) {
            List<Map<String, Object>> guarantors = new ArrayList<>();
            for (int i = 0; i < gt1Count; i++) {
                GT1 gt1 = dft.getGT1(i);
                guarantors.add(extractGuarantor(gt1));
            }
            data.put("guarantors", guarantors);
        }

        // Extract insurance information
        int inCount = dft.getINSURANCEReps();
        if (inCount > 0) {
            List<Map<String, Object>> insurances = new ArrayList<>();
            for (int i = 0; i < inCount; i++) {
                IN1 in1 = dft.getINSURANCE(i).getIN1();
                if (in1 != null) {
                    insurances.add(extractInsurance(in1));
                }
            }
            data.put("insurances", insurances);
        }

        // Extract financial (FT1) transactions - direct in P11
        int ftCount = dft.getFINANCIALReps();
        if (ftCount > 0) {
            List<Map<String, Object>> transactions = new ArrayList<>();
            for (int i = 0; i < ftCount; i++) {
                Map<String, Object> transaction = new HashMap<>();
                FT1 ft1 = dft.getFINANCIAL(i).getFT1();
                if (ft1 != null) {
                    extractFinancialTransaction(ft1, transaction);
                }
                transactions.add(transaction);
            }
            data.put("financialTransactions", transactions);
        }
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

        // Patient Account Number
        if (pid.getPatientAccountNumber() != null) {
            patientData.put("accountNumber",
                pid.getPatientAccountNumber().getIDNumber().getValue());
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

        // Hospital Service
        if (pv1.getHospitalService() != null) {
            visitData.put("hospitalService", pv1.getHospitalService().getValue());
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

        // Attending Doctor
        if (pv1.getAttendingDoctor(0) != null) {
            Map<String, String> doctor = new HashMap<>();
            doctor.put("id", pv1.getAttendingDoctor(0).getIDNumber().getValue());
            doctor.put("familyName",
                pv1.getAttendingDoctor(0).getFamilyName().getSurname().getValue());
            doctor.put("givenName", pv1.getAttendingDoctor(0).getGivenName().getValue());
            visitData.put("attendingDoctor", doctor);
        }

        // Admit Date/Time
        if (pv1.getAdmitDateTime() != null && pv1.getAdmitDateTime().getTime() != null) {
            visitData.put("admitDateTime", pv1.getAdmitDateTime().getTime().getValue());
        }

        // Financial Class
        if (pv1.getFinancialClass(0) != null) {
            visitData.put("financialClass", pv1.getFinancialClass(0).getFinancialClassCode().getValue());
        }

        data.put("visit", visitData);
    }

    /**
     * Extract financial transaction from FT1 segment.
     */
    private void extractFinancialTransaction(FT1 ft1, Map<String, Object> data) throws HL7Exception {
        // Set ID
        if (ft1.getSetIDFT1() != null) {
            data.put("setId", ft1.getSetIDFT1().getValue());
        }

        // Transaction ID
        if (ft1.getTransactionID() != null) {
            data.put("transactionId", ft1.getTransactionID().getValue());
        }

        // Transaction Batch ID
        if (ft1.getTransactionBatchID() != null) {
            data.put("batchId", ft1.getTransactionBatchID().getValue());
        }

        // Transaction Date
        if (ft1.getTransactionDate() != null && ft1.getTransactionDate().getRangeStartDateTime() != null) {
            data.put("transactionDate",
                ft1.getTransactionDate().getRangeStartDateTime().getTime().getValue());
        }

        // Transaction Posting Date
        if (ft1.getTransactionPostingDate() != null && ft1.getTransactionPostingDate().getTime() != null) {
            data.put("postingDate", ft1.getTransactionPostingDate().getTime().getValue());
        }

        // Transaction Type
        if (ft1.getTransactionType() != null) {
            data.put("transactionType", ft1.getTransactionType().getValue());
        }

        // Transaction Code
        if (ft1.getTransactionCode() != null) {
            Map<String, String> code = new HashMap<>();
            code.put("identifier", ft1.getTransactionCode().getIdentifier().getValue());
            code.put("text", ft1.getTransactionCode().getText().getValue());
            code.put("codingSystem", ft1.getTransactionCode().getNameOfCodingSystem().getValue());
            data.put("transactionCode", code);
        }

        // Transaction Description
        if (ft1.getTransactionDescription() != null) {
            data.put("description", ft1.getTransactionDescription().getValue());
        }

        // Transaction Quantity
        if (ft1.getTransactionQuantity() != null) {
            data.put("quantity", ft1.getTransactionQuantity().getValue());
        }

        // Extended Transaction Amount
        if (ft1.getTransactionAmountExtended() != null) {
            Map<String, Object> amount = new HashMap<>();
            if (ft1.getTransactionAmountExtended().getPrice() != null) {
                amount.put("value", ft1.getTransactionAmountExtended().getPrice().getQuantity().getValue());
            }
            data.put("extendedAmount", amount);
        }

        // Unit Transaction Amount
        if (ft1.getTransactionAmountUnit() != null) {
            Map<String, Object> unitAmount = new HashMap<>();
            if (ft1.getTransactionAmountUnit().getPrice() != null) {
                unitAmount.put("value", ft1.getTransactionAmountUnit().getPrice().getQuantity().getValue());
            }
            data.put("unitAmount", unitAmount);
        }

        // Department Code
        if (ft1.getDepartmentCode() != null) {
            Map<String, String> dept = new HashMap<>();
            dept.put("identifier", ft1.getDepartmentCode().getIdentifier().getValue());
            dept.put("text", ft1.getDepartmentCode().getText().getValue());
            data.put("departmentCode", dept);
        }

        // Insurance Plan ID
        if (ft1.getInsurancePlanID() != null) {
            Map<String, String> plan = new HashMap<>();
            plan.put("identifier", ft1.getInsurancePlanID().getIdentifier().getValue());
            plan.put("text", ft1.getInsurancePlanID().getText().getValue());
            data.put("insurancePlanId", plan);
        }

        // Fee Schedule
        if (ft1.getFeeSchedule() != null) {
            data.put("feeSchedule", ft1.getFeeSchedule().getValue());
        }

        // Diagnosis Code - FT1
        if (ft1.getDiagnosisCodeFT1(0) != null) {
            Map<String, String> diagCode = new HashMap<>();
            diagCode.put("identifier", ft1.getDiagnosisCodeFT1(0).getIdentifier().getValue());
            diagCode.put("text", ft1.getDiagnosisCodeFT1(0).getText().getValue());
            diagCode.put("codingSystem", ft1.getDiagnosisCodeFT1(0).getNameOfCodingSystem().getValue());
            data.put("diagnosisCode", diagCode);
        }

        // Performed By Code
        if (ft1.getPerformedByCode(0) != null) {
            Map<String, String> performer = new HashMap<>();
            performer.put("id", ft1.getPerformedByCode(0).getIDNumber().getValue());
            performer.put("familyName", ft1.getPerformedByCode(0).getFamilyName().getSurname().getValue());
            performer.put("givenName", ft1.getPerformedByCode(0).getGivenName().getValue());
            data.put("performedBy", performer);
        }

        // Ordering Provider
        if (ft1.getOrderedByCode(0) != null) {
            Map<String, String> orderer = new HashMap<>();
            orderer.put("id", ft1.getOrderedByCode(0).getIDNumber().getValue());
            orderer.put("familyName", ft1.getOrderedByCode(0).getFamilyName().getSurname().getValue());
            orderer.put("givenName", ft1.getOrderedByCode(0).getGivenName().getValue());
            data.put("orderedBy", orderer);
        }

        // Procedure Code
        if (ft1.getProcedureCode() != null) {
            Map<String, String> procCode = new HashMap<>();
            procCode.put("identifier", ft1.getProcedureCode().getIdentifier().getValue());
            procCode.put("text", ft1.getProcedureCode().getText().getValue());
            procCode.put("codingSystem", ft1.getProcedureCode().getNameOfCodingSystem().getValue());
            data.put("procedureCode", procCode);
        }

        // Procedure Code Modifier
        if (ft1.getProcedureCodeModifier(0) != null) {
            data.put("procedureCodeModifier",
                ft1.getProcedureCodeModifier(0).getIdentifier().getValue());
        }

        // NDC Code (National Drug Code)
        if (ft1.getNDCCode() != null) {
            data.put("ndcCode", ft1.getNDCCode().getIdentifier().getValue());
        }

        // Payment Reference ID
        if (ft1.getPaymentReferenceID() != null) {
            data.put("paymentReferenceId", ft1.getPaymentReferenceID().getIDNumber().getValue());
        }
    }

    /**
     * Extract procedure from PR1 segment.
     */
    private Map<String, Object> extractProcedure(PR1 pr1) throws HL7Exception {
        Map<String, Object> procedure = new HashMap<>();

        if (pr1.getSetIDPR1() != null) {
            procedure.put("setId", pr1.getSetIDPR1().getValue());
        }

        if (pr1.getProcedureCode() != null) {
            Map<String, String> code = new HashMap<>();
            code.put("identifier", pr1.getProcedureCode().getIdentifier().getValue());
            code.put("text", pr1.getProcedureCode().getText().getValue());
            code.put("codingSystem", pr1.getProcedureCode().getNameOfCodingSystem().getValue());
            procedure.put("code", code);
        }

        if (pr1.getProcedureDateTime() != null && pr1.getProcedureDateTime().getTime() != null) {
            procedure.put("dateTime", pr1.getProcedureDateTime().getTime().getValue());
        }

        if (pr1.getSurgeon(0) != null) {
            Map<String, String> surgeon = new HashMap<>();
            surgeon.put("id", pr1.getSurgeon(0).getIDNumber().getValue());
            surgeon.put("familyName", pr1.getSurgeon(0).getFamilyName().getSurname().getValue());
            surgeon.put("givenName", pr1.getSurgeon(0).getGivenName().getValue());
            procedure.put("surgeon", surgeon);
        }

        return procedure;
    }

    /**
     * Extract observation request from OBR segment.
     */
    private void extractObservationRequest(OBR obr, Map<String, Object> data) throws HL7Exception {
        if (obr.getSetIDOBR() != null) {
            data.put("setId", obr.getSetIDOBR().getValue());
        }

        if (obr.getUniversalServiceIdentifier() != null) {
            Map<String, String> serviceId = new HashMap<>();
            serviceId.put("identifier", obr.getUniversalServiceIdentifier().getIdentifier().getValue());
            serviceId.put("text", obr.getUniversalServiceIdentifier().getText().getValue());
            data.put("serviceId", serviceId);
        }

        if (obr.getObservationDateTime() != null && obr.getObservationDateTime().getTime() != null) {
            data.put("observationDateTime", obr.getObservationDateTime().getTime().getValue());
        }
    }

    /**
     * Extract observation from OBX segment.
     */
    private Map<String, Object> extractObservation(OBX obx) throws HL7Exception {
        Map<String, Object> obs = new HashMap<>();

        if (obx.getSetIDOBX() != null) {
            obs.put("setId", obx.getSetIDOBX().getValue());
        }

        if (obx.getValueType() != null) {
            obs.put("valueType", obx.getValueType().getValue());
        }

        if (obx.getObservationIdentifier() != null) {
            Map<String, String> id = new HashMap<>();
            id.put("identifier", obx.getObservationIdentifier().getIdentifier().getValue());
            id.put("text", obx.getObservationIdentifier().getText().getValue());
            obs.put("observationId", id);
        }

        if (obx.getObservationValue(0) != null) {
            obs.put("value", obx.getObservationValue(0).getData().toString());
        }

        return obs;
    }

    /**
     * Extract diagnosis from DG1 segment.
     */
    private Map<String, Object> extractDiagnosis(DG1 dg1) throws HL7Exception {
        Map<String, Object> diagnosis = new HashMap<>();

        if (dg1.getSetIDDG1() != null) {
            diagnosis.put("setId", dg1.getSetIDDG1().getValue());
        }

        if (dg1.getDiagnosisCodeDG1() != null) {
            Map<String, String> code = new HashMap<>();
            code.put("identifier", dg1.getDiagnosisCodeDG1().getIdentifier().getValue());
            code.put("text", dg1.getDiagnosisCodeDG1().getText().getValue());
            code.put("codingSystem", dg1.getDiagnosisCodeDG1().getNameOfCodingSystem().getValue());
            diagnosis.put("code", code);
        }

        if (dg1.getDiagnosisDateTime() != null && dg1.getDiagnosisDateTime().getTime() != null) {
            diagnosis.put("dateTime", dg1.getDiagnosisDateTime().getTime().getValue());
        }

        if (dg1.getDiagnosisType() != null) {
            diagnosis.put("type", dg1.getDiagnosisType().getValue());
        }

        return diagnosis;
    }

    /**
     * Extract guarantor from GT1 segment.
     */
    private Map<String, Object> extractGuarantor(GT1 gt1) throws HL7Exception {
        Map<String, Object> guarantor = new HashMap<>();

        if (gt1.getSetIDGT1() != null) {
            guarantor.put("setId", gt1.getSetIDGT1().getValue());
        }

        if (gt1.getGuarantorNumber(0) != null) {
            guarantor.put("guarantorNumber", gt1.getGuarantorNumber(0).getIDNumber().getValue());
        }

        if (gt1.getGuarantorName(0) != null) {
            String familyName = gt1.getGuarantorName(0).getFamilyName().getSurname().getValue();
            String givenName = gt1.getGuarantorName(0).getGivenName().getValue();
            guarantor.put("familyName", familyName);
            guarantor.put("givenName", givenName);
            guarantor.put("fullName", givenName + " " + familyName);
        }

        if (gt1.getGuarantorRelationship() != null) {
            guarantor.put("relationship", gt1.getGuarantorRelationship().getIdentifier().getValue());
        }

        return guarantor;
    }

    /**
     * Extract insurance from IN1 segment.
     */
    private Map<String, Object> extractInsurance(IN1 in1) throws HL7Exception {
        Map<String, Object> insurance = new HashMap<>();

        if (in1.getSetIDIN1() != null) {
            insurance.put("setId", in1.getSetIDIN1().getValue());
        }

        if (in1.getInsurancePlanID() != null) {
            Map<String, String> planId = new HashMap<>();
            planId.put("identifier", in1.getInsurancePlanID().getIdentifier().getValue());
            planId.put("text", in1.getInsurancePlanID().getText().getValue());
            insurance.put("planId", planId);
        }

        if (in1.getInsuranceCompanyName(0) != null) {
            insurance.put("companyName",
                in1.getInsuranceCompanyName(0).getOrganizationName().getValue());
        }

        if (in1.getGroupNumber() != null) {
            insurance.put("groupNumber", in1.getGroupNumber().getValue());
        }

        if (in1.getPlanEffectiveDate() != null) {
            insurance.put("effectiveDate", in1.getPlanEffectiveDate().getValue());
        }

        if (in1.getPlanExpirationDate() != null) {
            insurance.put("expirationDate", in1.getPlanExpirationDate().getValue());
        }

        return insurance;
    }
}

package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.BAR_P01;
import ca.uhn.hl7v2.model.v25.message.BAR_P02;
import ca.uhn.hl7v2.model.v25.segment.ACC;
import ca.uhn.hl7v2.model.v25.segment.DG1;
import ca.uhn.hl7v2.model.v25.segment.EVN;
import ca.uhn.hl7v2.model.v25.segment.GT1;
import ca.uhn.hl7v2.model.v25.segment.IN1;
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
 * Handler for BAR (Billing Account Record) messages.
 *
 * Supports:
 * - BAR^P01: Add patient account
 * - BAR^P02: Purge patient accounts
 * - BAR^P05: Update account
 * - BAR^P06: End account
 * - BAR^P10: Transmit ambulatory payment classification (APC) groups
 * - BAR^P12: Update diagnosis/procedure
 *
 * Converts to FHIR Account, Claim, and Coverage resources.
 */
@Slf4j
@Component
public class BarMessageHandler {

    /**
     * Handle BAR message and extract billing account data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing BAR message");

        Map<String, Object> data = new HashMap<>();
        data.put("messageType", "BAR");

        if (message instanceof BAR_P01) {
            handleBarP01((BAR_P01) message, data);
        } else if (message instanceof BAR_P02) {
            handleBarP02((BAR_P02) message, data);
        } else {
            log.warn("Unsupported BAR message type: {}", message.getClass().getSimpleName());
        }

        return data;
    }

    /**
     * Handle BAR^P01 - Add patient account.
     */
    private void handleBarP01(BAR_P01 bar, Map<String, Object> data) throws HL7Exception {
        data.put("triggerEvent", "P01");
        data.put("eventDescription", "Add patient account");

        // Extract EVN segment
        EVN evn = bar.getEVN();
        if (evn != null) {
            extractEventData(evn, data);
        }

        // Extract patient data
        PID pid = bar.getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract visit data
        int visitCount = bar.getVISITReps();
        if (visitCount > 0) {
            List<Map<String, Object>> visits = new ArrayList<>();
            for (int i = 0; i < visitCount; i++) {
                Map<String, Object> visitData = new HashMap<>();

                // PV1 segment
                PV1 pv1 = bar.getVISIT(i).getPV1();
                if (pv1 != null) {
                    extractVisitData(pv1, visitData);
                }

                // Diagnoses
                int dg1Count = bar.getVISIT(i).getDG1Reps();
                if (dg1Count > 0) {
                    List<Map<String, Object>> diagnoses = new ArrayList<>();
                    for (int j = 0; j < dg1Count; j++) {
                        DG1 dg1 = bar.getVISIT(i).getDG1(j);
                        diagnoses.add(extractDiagnosis(dg1));
                    }
                    visitData.put("diagnoses", diagnoses);
                }

                // Procedures
                int pr1Count = bar.getVISIT(i).getPROCEDUREReps();
                if (pr1Count > 0) {
                    List<Map<String, Object>> procedures = new ArrayList<>();
                    for (int j = 0; j < pr1Count; j++) {
                        PR1 pr1 = bar.getVISIT(i).getPROCEDURE(j).getPR1();
                        procedures.add(extractProcedure(pr1));
                    }
                    visitData.put("procedures", procedures);
                }

                // Insurance
                int in1Count = bar.getVISIT(i).getINSURANCEReps();
                if (in1Count > 0) {
                    List<Map<String, Object>> insurances = new ArrayList<>();
                    for (int j = 0; j < in1Count; j++) {
                        IN1 in1 = bar.getVISIT(i).getINSURANCE(j).getIN1();
                        insurances.add(extractInsurance(in1));
                    }
                    visitData.put("insurances", insurances);
                }

                // Accident (ACC segment)
                ACC acc = bar.getVISIT(i).getACC();
                if (acc != null) {
                    visitData.put("accident", extractAccident(acc));
                }

                visits.add(visitData);
            }
            data.put("visits", visits);
        }

        // Extract guarantor information
        int gt1Count = bar.getGT1Reps();
        if (gt1Count > 0) {
            List<Map<String, Object>> guarantors = new ArrayList<>();
            for (int i = 0; i < gt1Count; i++) {
                GT1 gt1 = bar.getGT1(i);
                guarantors.add(extractGuarantor(gt1));
            }
            data.put("guarantors", guarantors);
        }
    }

    /**
     * Handle BAR^P02 - Purge patient accounts.
     */
    private void handleBarP02(BAR_P02 bar, Map<String, Object> data) throws HL7Exception {
        data.put("triggerEvent", "P02");
        data.put("eventDescription", "Purge patient accounts");

        // Extract EVN segment
        EVN evn = bar.getEVN();
        if (evn != null) {
            extractEventData(evn, data);
        }

        // Extract patient data for purge
        int patientCount = bar.getPATIENTReps();
        List<Map<String, Object>> patients = new ArrayList<>();
        for (int i = 0; i < patientCount; i++) {
            Map<String, Object> patientData = new HashMap<>();
            PID pid = bar.getPATIENT(i).getPID();
            if (pid != null) {
                extractPatientData(pid, patientData);
            }
            PV1 pv1 = bar.getPATIENT(i).getPV1();
            if (pv1 != null) {
                extractVisitData(pv1, patientData);
            }
            patients.add(patientData);
        }
        data.put("patients", patients);
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
            eventData.put("eventReasonCode", evn.getEventReasonCode().getIdentifier().getValue());
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

        // SSN
        if (pid.getSSNNumberPatient() != null) {
            patientData.put("ssn", pid.getSSNNumberPatient().getValue());
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

        // Patient Type
        if (pv1.getPatientType() != null) {
            visitData.put("patientType", pv1.getPatientType().getValue());
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

        // Discharge Date/Time
        if (pv1.getDischargeDateTime(0) != null && pv1.getDischargeDateTime(0).getTime() != null) {
            visitData.put("dischargeDateTime", pv1.getDischargeDateTime(0).getTime().getValue());
        }

        // Financial Class
        if (pv1.getFinancialClass(0) != null) {
            visitData.put("financialClass", pv1.getFinancialClass(0).getFinancialClassCode().getValue());
        }

        // Discharge Disposition
        if (pv1.getDischargeDisposition() != null) {
            visitData.put("dischargeDisposition", pv1.getDischargeDisposition().getValue());
        }

        // Discharged to Location
        if (pv1.getDischargedToLocation() != null) {
            visitData.put("dischargedToLocation",
                pv1.getDischargedToLocation().getDischargeLocation().getValue());
        }

        // Account Status
        if (pv1.getAccountStatus() != null) {
            visitData.put("accountStatus", pv1.getAccountStatus().getValue());
        }

        data.put("visit", visitData);
    }

    /**
     * Extract diagnosis from DG1 segment.
     */
    private Map<String, Object> extractDiagnosis(DG1 dg1) throws HL7Exception {
        Map<String, Object> diagnosis = new HashMap<>();

        if (dg1.getSetIDDG1() != null) {
            diagnosis.put("setId", dg1.getSetIDDG1().getValue());
        }

        if (dg1.getDiagnosisCodingMethod() != null) {
            diagnosis.put("codingMethod", dg1.getDiagnosisCodingMethod().getValue());
        }

        if (dg1.getDiagnosisCodeDG1() != null) {
            Map<String, String> code = new HashMap<>();
            code.put("identifier", dg1.getDiagnosisCodeDG1().getIdentifier().getValue());
            code.put("text", dg1.getDiagnosisCodeDG1().getText().getValue());
            code.put("codingSystem", dg1.getDiagnosisCodeDG1().getNameOfCodingSystem().getValue());
            diagnosis.put("code", code);
        }

        if (dg1.getDiagnosisDescription() != null) {
            diagnosis.put("description", dg1.getDiagnosisDescription().getValue());
        }

        if (dg1.getDiagnosisDateTime() != null && dg1.getDiagnosisDateTime().getTime() != null) {
            diagnosis.put("dateTime", dg1.getDiagnosisDateTime().getTime().getValue());
        }

        if (dg1.getDiagnosisType() != null) {
            diagnosis.put("type", dg1.getDiagnosisType().getValue());
        }

        if (dg1.getDiagnosisPriority() != null) {
            diagnosis.put("priority", dg1.getDiagnosisPriority().getValue());
        }

        if (dg1.getDiagnosingClinician(0) != null) {
            Map<String, String> clinician = new HashMap<>();
            clinician.put("id", dg1.getDiagnosingClinician(0).getIDNumber().getValue());
            clinician.put("familyName",
                dg1.getDiagnosingClinician(0).getFamilyName().getSurname().getValue());
            clinician.put("givenName", dg1.getDiagnosingClinician(0).getGivenName().getValue());
            diagnosis.put("diagnosingClinician", clinician);
        }

        return diagnosis;
    }

    /**
     * Extract procedure from PR1 segment.
     */
    private Map<String, Object> extractProcedure(PR1 pr1) throws HL7Exception {
        Map<String, Object> procedure = new HashMap<>();

        if (pr1.getSetIDPR1() != null) {
            procedure.put("setId", pr1.getSetIDPR1().getValue());
        }

        if (pr1.getProcedureCodingMethod() != null) {
            procedure.put("codingMethod", pr1.getProcedureCodingMethod().getValue());
        }

        if (pr1.getProcedureCode() != null) {
            Map<String, String> code = new HashMap<>();
            code.put("identifier", pr1.getProcedureCode().getIdentifier().getValue());
            code.put("text", pr1.getProcedureCode().getText().getValue());
            code.put("codingSystem", pr1.getProcedureCode().getNameOfCodingSystem().getValue());
            procedure.put("code", code);
        }

        if (pr1.getProcedureDescription() != null) {
            procedure.put("description", pr1.getProcedureDescription().getValue());
        }

        if (pr1.getProcedureDateTime() != null && pr1.getProcedureDateTime().getTime() != null) {
            procedure.put("dateTime", pr1.getProcedureDateTime().getTime().getValue());
        }

        if (pr1.getProcedureFunctionalType() != null) {
            procedure.put("functionalType", pr1.getProcedureFunctionalType().getValue());
        }

        if (pr1.getProcedurePriority() != null) {
            procedure.put("priority", pr1.getProcedurePriority().getValue());
        }

        if (pr1.getProcedureMinutes() != null) {
            procedure.put("minutes", pr1.getProcedureMinutes().getValue());
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

        if (in1.getInsuranceCompanyID(0) != null) {
            insurance.put("companyId", in1.getInsuranceCompanyID(0).getIDNumber().getValue());
        }

        if (in1.getInsuranceCompanyName(0) != null) {
            insurance.put("companyName",
                in1.getInsuranceCompanyName(0).getOrganizationName().getValue());
        }

        if (in1.getGroupNumber() != null) {
            insurance.put("groupNumber", in1.getGroupNumber().getValue());
        }

        if (in1.getGroupName(0) != null) {
            insurance.put("groupName", in1.getGroupName(0).getOrganizationName().getValue());
        }

        if (in1.getInsuredSGroupEmpID(0) != null) {
            insurance.put("subscriberId", in1.getInsuredSGroupEmpID(0).getIDNumber().getValue());
        }

        if (in1.getPlanEffectiveDate() != null) {
            insurance.put("effectiveDate", in1.getPlanEffectiveDate().getValue());
        }

        if (in1.getPlanExpirationDate() != null) {
            insurance.put("expirationDate", in1.getPlanExpirationDate().getValue());
        }

        if (in1.getInsuranceCoPhoneNumber(0) != null) {
            insurance.put("companyPhone",
                in1.getInsuranceCoPhoneNumber(0).getTelephoneNumber().getValue());
        }

        if (in1.getCoordinationOfBenefits() != null) {
            insurance.put("coordinationOfBenefits", in1.getCoordinationOfBenefits().getValue());
        }

        if (in1.getInsuredSRelationshipToPatient() != null) {
            insurance.put("relationshipToPatient",
                in1.getInsuredSRelationshipToPatient().getIdentifier().getValue());
        }

        return insurance;
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

        if (gt1.getGuarantorSpouseName(0) != null) {
            guarantor.put("spouseName",
                gt1.getGuarantorSpouseName(0).getFamilyName().getSurname().getValue());
        }

        if (gt1.getGuarantorAddress(0) != null) {
            Map<String, String> address = new HashMap<>();
            address.put("street", gt1.getGuarantorAddress(0).getStreetAddress().getStreetOrMailingAddress().getValue());
            address.put("city", gt1.getGuarantorAddress(0).getCity().getValue());
            address.put("state", gt1.getGuarantorAddress(0).getStateOrProvince().getValue());
            address.put("postalCode", gt1.getGuarantorAddress(0).getZipOrPostalCode().getValue());
            guarantor.put("address", address);
        }

        if (gt1.getGuarantorPhNumHome(0) != null) {
            guarantor.put("phoneHome", gt1.getGuarantorPhNumHome(0).getTelephoneNumber().getValue());
        }

        if (gt1.getGuarantorPhNumBusiness(0) != null) {
            guarantor.put("phoneBusiness",
                gt1.getGuarantorPhNumBusiness(0).getTelephoneNumber().getValue());
        }

        if (gt1.getGuarantorDateTimeOfBirth() != null &&
            gt1.getGuarantorDateTimeOfBirth().getTime() != null) {
            guarantor.put("dateOfBirth", gt1.getGuarantorDateTimeOfBirth().getTime().getValue());
        }

        if (gt1.getGuarantorAdministrativeSex() != null) {
            guarantor.put("gender", gt1.getGuarantorAdministrativeSex().getValue());
        }

        if (gt1.getGuarantorType() != null) {
            guarantor.put("type", gt1.getGuarantorType().getValue());
        }

        if (gt1.getGuarantorRelationship() != null) {
            guarantor.put("relationship", gt1.getGuarantorRelationship().getIdentifier().getValue());
        }

        if (gt1.getGuarantorSSN() != null) {
            guarantor.put("ssn", gt1.getGuarantorSSN().getValue());
        }

        if (gt1.getGuarantorEmployerName(0) != null) {
            // XPN type - extract family name as employer name
            guarantor.put("employerName",
                gt1.getGuarantorEmployerName(0).getFamilyName().getSurname().getValue());
        }

        return guarantor;
    }

    /**
     * Extract accident information from ACC segment.
     */
    private Map<String, Object> extractAccident(ACC acc) throws HL7Exception {
        Map<String, Object> accident = new HashMap<>();

        if (acc.getAccidentDateTime() != null && acc.getAccidentDateTime().getTime() != null) {
            accident.put("dateTime", acc.getAccidentDateTime().getTime().getValue());
        }

        if (acc.getAccidentCode() != null) {
            Map<String, String> code = new HashMap<>();
            code.put("identifier", acc.getAccidentCode().getIdentifier().getValue());
            code.put("text", acc.getAccidentCode().getText().getValue());
            accident.put("code", code);
        }

        if (acc.getAccidentLocation() != null) {
            accident.put("location", acc.getAccidentLocation().getValue());
        }

        if (acc.getAutoAccidentState() != null) {
            accident.put("autoAccidentState", acc.getAutoAccidentState().getIdentifier().getValue());
        }

        if (acc.getAccidentJobRelatedIndicator() != null) {
            accident.put("jobRelated", acc.getAccidentJobRelatedIndicator().getValue());
        }

        if (acc.getAccidentDeathIndicator() != null) {
            accident.put("deathIndicator", acc.getAccidentDeathIndicator().getValue());
        }

        return accident;
    }
}

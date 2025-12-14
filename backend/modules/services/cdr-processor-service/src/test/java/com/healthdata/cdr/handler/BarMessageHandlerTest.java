package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.BAR_P01;
import ca.uhn.hl7v2.model.v25.message.BAR_P02;
import ca.uhn.hl7v2.model.v25.segment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BarMessageHandler.
 * Tests BAR (Billing Account Record) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BAR Message Handler Tests")
class BarMessageHandlerTest {

    @InjectMocks
    private BarMessageHandler handler;

    @Nested
    @DisplayName("BAR^P01 - Add Patient Account")
    class BarP01Tests {

        @Test
        @DisplayName("Should extract trigger event P01")
        void handle_withBarP01_extractsTriggerEvent() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            EVN evn = mock(EVN.class);
            when(bar.getEVN()).thenReturn(evn);
            when(bar.getPID()).thenReturn(null);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsEntry("messageType", "BAR");
            assertThat(result).containsEntry("triggerEvent", "P01");
            assertThat(result).containsEntry("eventDescription", "Add patient account");
        }

        @Test
        @DisplayName("Should extract patient data with account number")
        void handle_withBarP01_extractsPatientData() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            PID pid = createMockPid();

            when(bar.getEVN()).thenReturn(null);
            when(bar.getPID()).thenReturn(pid);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
            assertThat(patient).containsEntry("familyName", "Smith");
            assertThat(patient).containsEntry("givenName", "John");
            assertThat(patient).containsEntry("accountNumber", "ACC001");
        }

        @Test
        @DisplayName("Should extract visit data with diagnoses")
        void handle_withBarP01_extractsVisitDataWithDiagnoses() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            setupBarP01WithVisit(bar);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("visits");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> visits = (List<Map<String, Object>>) result.get("visits");
            assertThat(visits).hasSize(1);
            assertThat(visits.get(0)).containsKey("diagnoses");
        }

        @Test
        @DisplayName("Should extract insurance information")
        void handle_withBarP01_extractsInsurance() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            setupBarP01WithInsurance(bar);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("visits");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> visits = (List<Map<String, Object>>) result.get("visits");
            assertThat(visits).hasSize(1);
            assertThat(visits.get(0)).containsKey("insurances");
        }
    }

    @Nested
    @DisplayName("BAR^P02 - Purge Patient Accounts")
    class BarP02Tests {

        @Test
        @DisplayName("Should extract trigger event P02")
        void handle_withBarP02_extractsTriggerEvent() throws HL7Exception {
            BAR_P02 bar = mock(BAR_P02.class);
            when(bar.getEVN()).thenReturn(null);
            when(bar.getPATIENTReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsEntry("messageType", "BAR");
            assertThat(result).containsEntry("triggerEvent", "P02");
            assertThat(result).containsEntry("eventDescription", "Purge patient accounts");
        }

        @Test
        @DisplayName("Should extract multiple patients for purge")
        void handle_withBarP02_extractsMultiplePatients() throws HL7Exception {
            BAR_P02 bar = mock(BAR_P02.class);
            setupBarP02WithPatients(bar);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("patients");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> patients = (List<Map<String, Object>>) result.get("patients");
            assertThat(patients).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Diagnosis Extraction")
    class DiagnosisTests {

        @Test
        @DisplayName("Should extract diagnosis code and description")
        void extractDiagnosis_withValidData_extractsCode() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            setupBarP01WithDiagnosis(bar, "E11.9", "Type 2 diabetes", "ICD-10");

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("visits");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> visits = (List<Map<String, Object>>) result.get("visits");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> diagnoses = (List<Map<String, Object>>) visits.get(0).get("diagnoses");

            assertThat(diagnoses).hasSize(1);
            @SuppressWarnings("unchecked")
            Map<String, String> code = (Map<String, String>) diagnoses.get(0).get("code");
            assertThat(code).containsEntry("identifier", "E11.9");
            assertThat(code).containsEntry("text", "Type 2 diabetes");
        }

        @Test
        @DisplayName("Should extract diagnosis type and priority")
        void extractDiagnosis_withTypeAndPriority_extractsAll() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            setupBarP01WithDiagnosisDetails(bar);

            Map<String, Object> result = handler.handle(bar);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> visits = (List<Map<String, Object>>) result.get("visits");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> diagnoses = (List<Map<String, Object>>) visits.get(0).get("diagnoses");

            assertThat(diagnoses.get(0)).containsEntry("type", "A");
            assertThat(diagnoses.get(0)).containsEntry("priority", "1");
        }
    }

    @Nested
    @DisplayName("Insurance Extraction")
    class InsuranceTests {

        @Test
        @DisplayName("Should extract insurance plan details")
        void extractInsurance_withValidData_extractsPlan() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            setupBarP01WithInsuranceDetails(bar);

            Map<String, Object> result = handler.handle(bar);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> visits = (List<Map<String, Object>>) result.get("visits");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> insurances = (List<Map<String, Object>>) visits.get(0).get("insurances");

            assertThat(insurances).hasSize(1);
            assertThat(insurances.get(0)).containsEntry("groupNumber", "GRP123");
            assertThat(insurances.get(0)).containsEntry("companyName", "Blue Cross");
        }

        @Test
        @DisplayName("Should extract coordination of benefits")
        void extractInsurance_withCOB_extractsCoordination() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            setupBarP01WithCOB(bar);

            Map<String, Object> result = handler.handle(bar);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> visits = (List<Map<String, Object>>) result.get("visits");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> insurances = (List<Map<String, Object>>) visits.get(0).get("insurances");

            assertThat(insurances.get(0)).containsEntry("coordinationOfBenefits", "1");
        }
    }

    @Nested
    @DisplayName("Unsupported Message Types")
    class UnsupportedMessageTests {

        @Test
        @DisplayName("Should handle unsupported BAR message type")
        void handle_withUnsupportedType_returnsBasicData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).containsEntry("messageType", "BAR");
            assertThat(result).doesNotContainKey("triggerEvent");
        }
    }

    // Helper methods for creating mock objects
    private PID createMockPid() throws HL7Exception {
        PID pid = mock(PID.class);

        CX patientId = mock(CX.class);
        ST idNumber = mock(ST.class);
        when(idNumber.getValue()).thenReturn("12345");
        when(patientId.getIDNumber()).thenReturn(idNumber);

        ID idType = mock(ID.class);
        when(idType.getValue()).thenReturn("MR");
        when(patientId.getIdentifierTypeCode()).thenReturn(idType);
        when(pid.getPatientIdentifierList(0)).thenReturn(patientId);

        CX accountNumber = mock(CX.class);
        ST accNum = mock(ST.class);
        when(accNum.getValue()).thenReturn("ACC001");
        when(accountNumber.getIDNumber()).thenReturn(accNum);
        when(pid.getPatientAccountNumber()).thenReturn(accountNumber);

        XPN patientName = mock(XPN.class);
        FN familyName = mock(FN.class);
        ST surname = mock(ST.class);
        when(surname.getValue()).thenReturn("Smith");
        when(familyName.getSurname()).thenReturn(surname);
        when(patientName.getFamilyName()).thenReturn(familyName);

        ST givenName = mock(ST.class);
        when(givenName.getValue()).thenReturn("John");
        when(patientName.getGivenName()).thenReturn(givenName);
        when(pid.getPatientName(0)).thenReturn(patientName);

        TS dob = mock(TS.class);
        DTM dobTime = mock(DTM.class);
        when(dobTime.getValue()).thenReturn("19800101");
        when(dob.getTime()).thenReturn(dobTime);
        when(pid.getDateTimeOfBirth()).thenReturn(dob);

        ST ssn = mock(ST.class);
        when(ssn.getValue()).thenReturn("123-45-6789");
        when(pid.getSSNNumberPatient()).thenReturn(ssn);

        return pid;
    }

    private void setupBarP01WithVisit(BAR_P01 bar) throws HL7Exception {
        when(bar.getEVN()).thenReturn(null);
        when(bar.getPID()).thenReturn(null);
        when(bar.getVISITReps()).thenReturn(1);

        BAR_P01.VISIT visit = mock(BAR_P01.VISIT.class);
        when(bar.getVISIT(0)).thenReturn(visit);

        PV1 pv1 = createMockPv1();
        when(visit.getPV1()).thenReturn(pv1);
        when(visit.getDG1Reps()).thenReturn(1);

        DG1 dg1 = createMockDg1("J06.9", "Acute upper respiratory infection");
        when(visit.getDG1(0)).thenReturn(dg1);

        when(visit.getPROCEDUREReps()).thenReturn(0);
        when(visit.getINSURANCEReps()).thenReturn(0);

        ACC acc = mock(ACC.class);
        when(visit.getACC()).thenReturn(acc);
    }

    private void setupBarP01WithInsurance(BAR_P01 bar) throws HL7Exception {
        when(bar.getEVN()).thenReturn(null);
        when(bar.getPID()).thenReturn(null);
        when(bar.getVISITReps()).thenReturn(1);

        BAR_P01.VISIT visit = mock(BAR_P01.VISIT.class);
        when(bar.getVISIT(0)).thenReturn(visit);
        when(visit.getPV1()).thenReturn(null);
        when(visit.getDG1Reps()).thenReturn(0);
        when(visit.getPROCEDUREReps()).thenReturn(0);
        when(visit.getINSURANCEReps()).thenReturn(1);

        BAR_P01.VISIT.INSURANCE insurance = mock(BAR_P01.VISIT.INSURANCE.class);
        when(visit.getINSURANCE(0)).thenReturn(insurance);

        IN1 in1 = createMockIn1();
        when(insurance.getIN1()).thenReturn(in1);

        when(visit.getACC()).thenReturn(null);
    }

    private void setupBarP01WithDiagnosis(BAR_P01 bar, String code, String text, String system) throws HL7Exception {
        when(bar.getEVN()).thenReturn(null);
        when(bar.getPID()).thenReturn(null);
        when(bar.getVISITReps()).thenReturn(1);

        BAR_P01.VISIT visit = mock(BAR_P01.VISIT.class);
        when(bar.getVISIT(0)).thenReturn(visit);
        when(visit.getPV1()).thenReturn(null);
        when(visit.getDG1Reps()).thenReturn(1);

        DG1 dg1 = createMockDg1(code, text);
        when(visit.getDG1(0)).thenReturn(dg1);

        when(visit.getPROCEDUREReps()).thenReturn(0);
        when(visit.getINSURANCEReps()).thenReturn(0);
        when(visit.getACC()).thenReturn(null);
    }

    private void setupBarP01WithDiagnosisDetails(BAR_P01 bar) throws HL7Exception {
        when(bar.getEVN()).thenReturn(null);
        when(bar.getPID()).thenReturn(null);
        when(bar.getVISITReps()).thenReturn(1);

        BAR_P01.VISIT visit = mock(BAR_P01.VISIT.class);
        when(bar.getVISIT(0)).thenReturn(visit);
        when(visit.getPV1()).thenReturn(null);
        when(visit.getDG1Reps()).thenReturn(1);

        DG1 dg1 = createMockDg1WithDetails();
        when(visit.getDG1(0)).thenReturn(dg1);

        when(visit.getPROCEDUREReps()).thenReturn(0);
        when(visit.getINSURANCEReps()).thenReturn(0);
        when(visit.getACC()).thenReturn(null);
    }

    private void setupBarP01WithInsuranceDetails(BAR_P01 bar) throws HL7Exception {
        when(bar.getEVN()).thenReturn(null);
        when(bar.getPID()).thenReturn(null);
        when(bar.getVISITReps()).thenReturn(1);

        BAR_P01.VISIT visit = mock(BAR_P01.VISIT.class);
        when(bar.getVISIT(0)).thenReturn(visit);
        when(visit.getPV1()).thenReturn(null);
        when(visit.getDG1Reps()).thenReturn(0);
        when(visit.getPROCEDUREReps()).thenReturn(0);
        when(visit.getINSURANCEReps()).thenReturn(1);

        BAR_P01.VISIT.INSURANCE insurance = mock(BAR_P01.VISIT.INSURANCE.class);
        when(visit.getINSURANCE(0)).thenReturn(insurance);

        IN1 in1 = createMockIn1WithDetails();
        when(insurance.getIN1()).thenReturn(in1);

        when(visit.getACC()).thenReturn(null);
    }

    private void setupBarP01WithCOB(BAR_P01 bar) throws HL7Exception {
        when(bar.getEVN()).thenReturn(null);
        when(bar.getPID()).thenReturn(null);
        when(bar.getVISITReps()).thenReturn(1);

        BAR_P01.VISIT visit = mock(BAR_P01.VISIT.class);
        when(bar.getVISIT(0)).thenReturn(visit);
        when(visit.getPV1()).thenReturn(null);
        when(visit.getDG1Reps()).thenReturn(0);
        when(visit.getPROCEDUREReps()).thenReturn(0);
        when(visit.getINSURANCEReps()).thenReturn(1);

        BAR_P01.VISIT.INSURANCE insurance = mock(BAR_P01.VISIT.INSURANCE.class);
        when(visit.getINSURANCE(0)).thenReturn(insurance);

        IN1 in1 = createMockIn1WithCOB();
        when(insurance.getIN1()).thenReturn(in1);

        when(visit.getACC()).thenReturn(null);
    }

    private void setupBarP02WithPatients(BAR_P02 bar) throws HL7Exception {
        when(bar.getEVN()).thenReturn(null);
        when(bar.getPATIENTReps()).thenReturn(2);

        for (int i = 0; i < 2; i++) {
            BAR_P02.PATIENT patient = mock(BAR_P02.PATIENT.class);
            when(bar.getPATIENT(i)).thenReturn(patient);
            when(patient.getPID()).thenReturn(null);
            when(patient.getPV1()).thenReturn(null);
        }
    }

    private PV1 createMockPv1() throws HL7Exception {
        PV1 pv1 = mock(PV1.class);

        CX visitNumber = mock(CX.class);
        ST visitId = mock(ST.class);
        when(visitId.getValue()).thenReturn("V001");
        when(visitNumber.getIDNumber()).thenReturn(visitId);
        when(pv1.getVisitNumber()).thenReturn(visitNumber);

        IS patientClass = mock(IS.class);
        when(patientClass.getValue()).thenReturn("I");
        when(pv1.getPatientClass()).thenReturn(patientClass);

        return pv1;
    }

    private DG1 createMockDg1(String code, String text) throws HL7Exception {
        DG1 dg1 = mock(DG1.class);

        SI setId = mock(SI.class);
        when(setId.getValue()).thenReturn("1");
        when(dg1.getSetIDDG1()).thenReturn(setId);

        CE diagnosisCode = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(code);
        when(diagnosisCode.getIdentifier()).thenReturn(identifier);

        ST codeText = mock(ST.class);
        when(codeText.getValue()).thenReturn(text);
        when(diagnosisCode.getText()).thenReturn(codeText);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn("ICD10");
        when(diagnosisCode.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(dg1.getDiagnosisCodeDG1()).thenReturn(diagnosisCode);

        return dg1;
    }

    private DG1 createMockDg1WithDetails() throws HL7Exception {
        DG1 dg1 = createMockDg1("E11.9", "Type 2 diabetes");

        IS diagnosisType = mock(IS.class);
        when(diagnosisType.getValue()).thenReturn("A");
        when(dg1.getDiagnosisType()).thenReturn(diagnosisType);

        ID priority = mock(ID.class);
        when(priority.getValue()).thenReturn("1");
        when(dg1.getDiagnosisPriority()).thenReturn(priority);

        return dg1;
    }

    private IN1 createMockIn1() throws HL7Exception {
        IN1 in1 = mock(IN1.class);

        SI setId = mock(SI.class);
        when(setId.getValue()).thenReturn("1");
        when(in1.getSetIDIN1()).thenReturn(setId);

        return in1;
    }

    private IN1 createMockIn1WithDetails() throws HL7Exception {
        IN1 in1 = createMockIn1();

        ST groupNumber = mock(ST.class);
        when(groupNumber.getValue()).thenReturn("GRP123");
        when(in1.getGroupNumber()).thenReturn(groupNumber);

        XON companyName = mock(XON.class);
        ST orgName = mock(ST.class);
        when(orgName.getValue()).thenReturn("Blue Cross");
        when(companyName.getOrganizationName()).thenReturn(orgName);
        when(in1.getInsuranceCompanyName(0)).thenReturn(companyName);

        return in1;
    }

    private IN1 createMockIn1WithCOB() throws HL7Exception {
        IN1 in1 = createMockIn1WithDetails();

        ID cob = mock(ID.class);
        when(cob.getValue()).thenReturn("1");
        when(in1.getCoordinationOfBenefits()).thenReturn(cob);

        return in1;
    }
}

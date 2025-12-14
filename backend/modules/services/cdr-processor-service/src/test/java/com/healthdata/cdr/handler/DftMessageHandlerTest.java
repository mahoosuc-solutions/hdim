package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.DFT_P03;
import ca.uhn.hl7v2.model.v25.message.DFT_P11;
import ca.uhn.hl7v2.model.v25.segment.*;
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
 * Unit tests for DftMessageHandler.
 * Tests DFT (Detailed Financial Transaction) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DFT Message Handler Tests")
class DftMessageHandlerTest {

    @InjectMocks
    private DftMessageHandler handler;

    @Nested
    @DisplayName("DFT^P03 - Post Detail Financial Transaction")
    class DftP03Tests {

        @Test
        @DisplayName("Should extract trigger event P03")
        void handle_withDftP03_extractsTriggerEvent() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(null);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getFINANCIALReps()).thenReturn(0);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsEntry("messageType", "DFT");
            assertThat(result).containsEntry("triggerEvent", "P03");
            assertThat(result).containsEntry("eventDescription", "Post detail financial transaction");
        }

        @Test
        @DisplayName("Should extract patient data")
        void handle_withDftP03_extractsPatientData() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            PID pid = createMockPid();

            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(pid);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getFINANCIALReps()).thenReturn(0);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
            assertThat(patient).containsEntry("familyName", "Smith");
        }

        @Test
        @DisplayName("Should extract financial transactions")
        void handle_withDftP03_extractsFinancialTransactions() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            setupDftP03WithTransactions(dft);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("financialTransactions");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("financialTransactions");
            assertThat(transactions).hasSize(1);
        }

        @Test
        @DisplayName("Should extract guarantor information")
        void handle_withDftP03_extractsGuarantor() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            setupDftP03WithGuarantor(dft);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("guarantors");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> guarantors = (List<Map<String, Object>>) result.get("guarantors");
            assertThat(guarantors).hasSize(1);
        }
    }

    @Nested
    @DisplayName("DFT^P11 - Post Detail Financial Transaction (New Format)")
    class DftP11Tests {

        @Test
        @DisplayName("Should extract trigger event P11")
        void handle_withDftP11_extractsTriggerEvent() throws HL7Exception {
            DFT_P11 dft = mock(DFT_P11.class);
            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(null);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);
            when(dft.getFINANCIALReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsEntry("messageType", "DFT");
            assertThat(result).containsEntry("triggerEvent", "P11");
            assertThat(result).containsEntry("eventDescription", "Post detail financial transaction (new format)");
        }

        @Test
        @DisplayName("Should extract financial transactions from P11")
        void handle_withDftP11_extractsTransactions() throws HL7Exception {
            DFT_P11 dft = mock(DFT_P11.class);
            setupDftP11WithTransactions(dft);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("financialTransactions");
        }
    }

    @Nested
    @DisplayName("Financial Transaction (FT1) Extraction")
    class FinancialTransactionTests {

        @Test
        @DisplayName("Should extract transaction code")
        void extractFinancialTransaction_withCode_extractsCode() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            setupDftP03WithTransactionCode(dft, "99213", "Office visit");

            Map<String, Object> result = handler.handle(dft);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("financialTransactions");
            assertThat(transactions).hasSize(1);
            @SuppressWarnings("unchecked")
            Map<String, String> code = (Map<String, String>) transactions.get(0).get("transactionCode");
            assertThat(code).containsEntry("identifier", "99213");
            assertThat(code).containsEntry("text", "Office visit");
        }

        @Test
        @DisplayName("Should extract transaction amount")
        void extractFinancialTransaction_withAmount_extractsAmount() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            setupDftP03WithTransactionAmount(dft);

            Map<String, Object> result = handler.handle(dft);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("financialTransactions");
            assertThat(transactions.get(0)).containsKey("extendedAmount");
        }

        @Test
        @DisplayName("Should extract transaction type")
        void extractFinancialTransaction_withType_extractsType() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            setupDftP03WithTransactionType(dft, "CG");

            Map<String, Object> result = handler.handle(dft);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("financialTransactions");
            assertThat(transactions.get(0)).containsEntry("transactionType", "CG");
        }
    }

    @Nested
    @DisplayName("Diagnosis Extraction")
    class DiagnosisTests {

        @Test
        @DisplayName("Should extract diagnoses from DFT")
        void handle_withDiagnoses_extractsDiagnoses() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            setupDftP03WithDiagnoses(dft);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("diagnoses");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> diagnoses = (List<Map<String, Object>>) result.get("diagnoses");
            assertThat(diagnoses).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Unsupported Message Types")
    class UnsupportedMessageTests {

        @Test
        @DisplayName("Should handle unsupported DFT message type")
        void handle_withUnsupportedType_returnsBasicData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).containsEntry("messageType", "DFT");
            assertThat(result).doesNotContainKey("triggerEvent");
        }
    }

    // Helper methods
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

        return pid;
    }

    private void setupDftP03WithTransactions(DFT_P03 dft) throws HL7Exception {
        when(dft.getEVN()).thenReturn(null);
        when(dft.getPID()).thenReturn(null);
        when(dft.getPV1()).thenReturn(null);
        when(dft.getFINANCIALReps()).thenReturn(1);
        when(dft.getDG1Reps()).thenReturn(0);
        when(dft.getGT1Reps()).thenReturn(0);
        when(dft.getINSURANCEReps()).thenReturn(0);

        DFT_P03.FINANCIAL financial = mock(DFT_P03.FINANCIAL.class);
        when(dft.getFINANCIAL(0)).thenReturn(financial);

        FT1 ft1 = createMockFt1();
        when(financial.getFT1()).thenReturn(ft1);
        when(financial.getFINANCIAL_PROCEDUREReps()).thenReturn(0);
        when(financial.getFINANCIAL_COMMON_ORDERReps()).thenReturn(0);
    }

    private void setupDftP03WithGuarantor(DFT_P03 dft) throws HL7Exception {
        when(dft.getEVN()).thenReturn(null);
        when(dft.getPID()).thenReturn(null);
        when(dft.getPV1()).thenReturn(null);
        when(dft.getFINANCIALReps()).thenReturn(0);
        when(dft.getDG1Reps()).thenReturn(0);
        when(dft.getGT1Reps()).thenReturn(1);
        when(dft.getINSURANCEReps()).thenReturn(0);

        GT1 gt1 = createMockGt1();
        when(dft.getGT1(0)).thenReturn(gt1);
    }

    private void setupDftP03WithTransactionCode(DFT_P03 dft, String code, String text) throws HL7Exception {
        when(dft.getEVN()).thenReturn(null);
        when(dft.getPID()).thenReturn(null);
        when(dft.getPV1()).thenReturn(null);
        when(dft.getFINANCIALReps()).thenReturn(1);
        when(dft.getDG1Reps()).thenReturn(0);
        when(dft.getGT1Reps()).thenReturn(0);
        when(dft.getINSURANCEReps()).thenReturn(0);

        DFT_P03.FINANCIAL financial = mock(DFT_P03.FINANCIAL.class);
        when(dft.getFINANCIAL(0)).thenReturn(financial);

        FT1 ft1 = createMockFt1WithCode(code, text);
        when(financial.getFT1()).thenReturn(ft1);
        when(financial.getFINANCIAL_PROCEDUREReps()).thenReturn(0);
        when(financial.getFINANCIAL_COMMON_ORDERReps()).thenReturn(0);
    }

    private void setupDftP03WithTransactionAmount(DFT_P03 dft) throws HL7Exception {
        when(dft.getEVN()).thenReturn(null);
        when(dft.getPID()).thenReturn(null);
        when(dft.getPV1()).thenReturn(null);
        when(dft.getFINANCIALReps()).thenReturn(1);
        when(dft.getDG1Reps()).thenReturn(0);
        when(dft.getGT1Reps()).thenReturn(0);
        when(dft.getINSURANCEReps()).thenReturn(0);

        DFT_P03.FINANCIAL financial = mock(DFT_P03.FINANCIAL.class);
        when(dft.getFINANCIAL(0)).thenReturn(financial);

        FT1 ft1 = createMockFt1WithAmount();
        when(financial.getFT1()).thenReturn(ft1);
        when(financial.getFINANCIAL_PROCEDUREReps()).thenReturn(0);
        when(financial.getFINANCIAL_COMMON_ORDERReps()).thenReturn(0);
    }

    private void setupDftP03WithTransactionType(DFT_P03 dft, String type) throws HL7Exception {
        when(dft.getEVN()).thenReturn(null);
        when(dft.getPID()).thenReturn(null);
        when(dft.getPV1()).thenReturn(null);
        when(dft.getFINANCIALReps()).thenReturn(1);
        when(dft.getDG1Reps()).thenReturn(0);
        when(dft.getGT1Reps()).thenReturn(0);
        when(dft.getINSURANCEReps()).thenReturn(0);

        DFT_P03.FINANCIAL financial = mock(DFT_P03.FINANCIAL.class);
        when(dft.getFINANCIAL(0)).thenReturn(financial);

        FT1 ft1 = createMockFt1WithType(type);
        when(financial.getFT1()).thenReturn(ft1);
        when(financial.getFINANCIAL_PROCEDUREReps()).thenReturn(0);
        when(financial.getFINANCIAL_COMMON_ORDERReps()).thenReturn(0);
    }

    private void setupDftP03WithDiagnoses(DFT_P03 dft) throws HL7Exception {
        when(dft.getEVN()).thenReturn(null);
        when(dft.getPID()).thenReturn(null);
        when(dft.getPV1()).thenReturn(null);
        when(dft.getFINANCIALReps()).thenReturn(0);
        when(dft.getDG1Reps()).thenReturn(1);
        when(dft.getGT1Reps()).thenReturn(0);
        when(dft.getINSURANCEReps()).thenReturn(0);

        DG1 dg1 = createMockDg1();
        when(dft.getDG1(0)).thenReturn(dg1);
    }

    private void setupDftP11WithTransactions(DFT_P11 dft) throws HL7Exception {
        when(dft.getEVN()).thenReturn(null);
        when(dft.getPID()).thenReturn(null);
        when(dft.getPV1()).thenReturn(null);
        when(dft.getDG1Reps()).thenReturn(0);
        when(dft.getGT1Reps()).thenReturn(0);
        when(dft.getINSURANCEReps()).thenReturn(0);
        when(dft.getFINANCIALReps()).thenReturn(1);

        DFT_P11.FINANCIAL financial = mock(DFT_P11.FINANCIAL.class);
        when(dft.getFINANCIAL(0)).thenReturn(financial);

        FT1 ft1 = createMockFt1();
        when(financial.getFT1()).thenReturn(ft1);
    }

    private FT1 createMockFt1() throws HL7Exception {
        FT1 ft1 = mock(FT1.class);

        SI setId = mock(SI.class);
        when(setId.getValue()).thenReturn("1");
        when(ft1.getSetIDFT1()).thenReturn(setId);

        return ft1;
    }

    private FT1 createMockFt1WithCode(String code, String text) throws HL7Exception {
        FT1 ft1 = createMockFt1();

        CE transactionCode = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(code);
        when(transactionCode.getIdentifier()).thenReturn(identifier);

        ST codeText = mock(ST.class);
        when(codeText.getValue()).thenReturn(text);
        when(transactionCode.getText()).thenReturn(codeText);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn("CPT");
        when(transactionCode.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(ft1.getTransactionCode()).thenReturn(transactionCode);

        return ft1;
    }

    private FT1 createMockFt1WithAmount() throws HL7Exception {
        FT1 ft1 = createMockFt1();

        CP amount = mock(CP.class);
        MO price = mock(MO.class);
        NM quantity = mock(NM.class);
        when(quantity.getValue()).thenReturn("150.00");
        when(price.getQuantity()).thenReturn(quantity);
        when(amount.getPrice()).thenReturn(price);
        when(ft1.getTransactionAmountExtended()).thenReturn(amount);

        return ft1;
    }

    private FT1 createMockFt1WithType(String type) throws HL7Exception {
        FT1 ft1 = createMockFt1();

        IS transactionType = mock(IS.class);
        when(transactionType.getValue()).thenReturn(type);
        when(ft1.getTransactionType()).thenReturn(transactionType);

        return ft1;
    }

    private GT1 createMockGt1() throws HL7Exception {
        GT1 gt1 = mock(GT1.class);

        SI setId = mock(SI.class);
        when(setId.getValue()).thenReturn("1");
        when(gt1.getSetIDGT1()).thenReturn(setId);

        CX guarantorNumber = mock(CX.class);
        ST idNumber = mock(ST.class);
        when(idNumber.getValue()).thenReturn("G001");
        when(guarantorNumber.getIDNumber()).thenReturn(idNumber);
        when(gt1.getGuarantorNumber(0)).thenReturn(guarantorNumber);

        XPN name = mock(XPN.class);
        FN familyName = mock(FN.class);
        ST surname = mock(ST.class);
        when(surname.getValue()).thenReturn("Smith");
        when(familyName.getSurname()).thenReturn(surname);
        when(name.getFamilyName()).thenReturn(familyName);

        ST givenName = mock(ST.class);
        when(givenName.getValue()).thenReturn("Jane");
        when(name.getGivenName()).thenReturn(givenName);
        when(gt1.getGuarantorName(0)).thenReturn(name);

        return gt1;
    }

    private DG1 createMockDg1() throws HL7Exception {
        DG1 dg1 = mock(DG1.class);

        SI setId = mock(SI.class);
        when(setId.getValue()).thenReturn("1");
        when(dg1.getSetIDDG1()).thenReturn(setId);

        CE diagnosisCode = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn("E11.9");
        when(diagnosisCode.getIdentifier()).thenReturn(identifier);

        ST text = mock(ST.class);
        when(text.getValue()).thenReturn("Type 2 diabetes");
        when(diagnosisCode.getText()).thenReturn(text);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn("ICD10");
        when(diagnosisCode.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(dg1.getDiagnosisCodeDG1()).thenReturn(diagnosisCode);

        return dg1;
    }
}

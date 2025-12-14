package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.RAS_O17;
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
 * Unit tests for RasMessageHandler.
 * Tests RAS^O17 (Pharmacy/Treatment Administration) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RAS Message Handler Tests")
class RasMessageHandlerTest {

    @InjectMocks
    private RasMessageHandler handler;

    @Nested
    @DisplayName("RAS^O17 - Pharmacy Administration")
    class RasO17Tests {

        @Test
        @DisplayName("Should extract medication administrations")
        void handle_withValidRas_extractsAdministrations() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithAdministration(ras);

            Map<String, Object> result = handler.handle(ras);

            assertThat(result).containsKey("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            assertThat(admins).hasSize(1);
        }

        @Test
        @DisplayName("Should extract patient data")
        void handle_withPatientData_extractsPatient() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithPatient(ras);

            Map<String, Object> result = handler.handle(ras);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("familyName", "Smith");
        }

        @Test
        @DisplayName("Should return empty map for non-RAS message")
        void handle_withNonRasMessage_returnsEmptyMap() throws HL7Exception {
            ca.uhn.hl7v2.model.Message otherMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(otherMessage);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Administration (RXA) Extraction")
    class AdministrationTests {

        @Test
        @DisplayName("Should extract administered code")
        void extractAdministration_withCode_extractsCode() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithAdministeredCode(ras, "RX001", "Lisinopril", "NDC");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            @SuppressWarnings("unchecked")
            Map<String, String> code = (Map<String, String>) rxaList.get(0).get("administeredCode");
            assertThat(code).containsEntry("code", "RX001");
            assertThat(code).containsEntry("display", "Lisinopril");
        }

        @Test
        @DisplayName("Should extract administered amount")
        void extractAdministration_withAmount_extractsAmount() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithAdministeredAmount(ras, "10", "mg");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            assertThat(rxaList.get(0)).containsEntry("administeredAmount", "10");
            assertThat(rxaList.get(0)).containsEntry("administeredUnits", "mg");
        }

        @Test
        @DisplayName("Should extract start and end datetime")
        void extractAdministration_withDateTimes_extractsTimes() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithDateTimes(ras, "202401151000", "202401151030");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            assertThat(rxaList.get(0)).containsEntry("startDateTime", "202401151000");
            assertThat(rxaList.get(0)).containsEntry("endDateTime", "202401151030");
        }

        @Test
        @DisplayName("Should extract administering provider")
        void extractAdministration_withProvider_extractsProvider() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithProvider(ras, "RN001", "Jones", "Mary");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            @SuppressWarnings("unchecked")
            Map<String, String> provider = (Map<String, String>) rxaList.get(0).get("administeringProvider");
            assertThat(provider).containsEntry("id", "RN001");
            assertThat(provider).containsEntry("familyName", "Jones");
        }

        @Test
        @DisplayName("Should extract lot number and expiration")
        void extractAdministration_withLotInfo_extractsLotInfo() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithLotInfo(ras, "LOT123", "20251231");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            assertThat(rxaList.get(0)).containsEntry("lotNumber", "LOT123");
            assertThat(rxaList.get(0)).containsEntry("expirationDate", "20251231");
        }
    }

    @Nested
    @DisplayName("Completion Status Mapping")
    class CompletionStatusTests {

        @Test
        @DisplayName("Should map CP to completed")
        void mapCompletionStatus_withCP_returnsCompleted() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithCompletionStatus(ras, "CP");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            assertThat(rxaList.get(0)).containsEntry("fhirStatus", "completed");
        }

        @Test
        @DisplayName("Should map RE to not-done")
        void mapCompletionStatus_withRE_returnsNotDone() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithCompletionStatus(ras, "RE");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            assertThat(rxaList.get(0)).containsEntry("fhirStatus", "not-done");
        }

        @Test
        @DisplayName("Should map PA to stopped")
        void mapCompletionStatus_withPA_returnsStopped() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithCompletionStatus(ras, "PA");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            assertThat(rxaList.get(0)).containsEntry("fhirStatus", "stopped");
        }
    }

    @Nested
    @DisplayName("Route (RXR) Extraction")
    class RouteTests {

        @Test
        @DisplayName("Should extract route information")
        void extractRoute_withValidRoute_extractsRoute() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class);
            setupRasWithRoute(ras, "IV", "Intravenous", "HL7");

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rxaList = (List<Map<String, Object>>) admins.get(0).get("administrations");
            @SuppressWarnings("unchecked")
            Map<String, String> route = (Map<String, String>) rxaList.get(0).get("route");
            assertThat(route).containsEntry("code", "IV");
            assertThat(route).containsEntry("display", "Intravenous");
        }
    }

    // Helper methods
    private void setupRasWithAdministration(RAS_O17 ras) throws HL7Exception {
        when(ras.getORDERReps()).thenReturn(1);

        RAS_O17.ORDER order = mock(RAS_O17.ORDER.class);
        when(ras.getORDER(0)).thenReturn(order);

        ORC orc = mock(ORC.class);
        when(order.getORC()).thenReturn(orc);

        when(order.getADMINISTRATIONReps()).thenReturn(1);

        RAS_O17.ORDER.ADMINISTRATION admin = mock(RAS_O17.ORDER.ADMINISTRATION.class);
        when(order.getADMINISTRATION(0)).thenReturn(admin);

        RXA rxa = mock(RXA.class);
        when(admin.getRXA()).thenReturn(rxa);
        when(admin.getRXR()).thenReturn(null);

        // Mock patient segment that throws exception
        RAS_O17.PATIENT patient = mock(RAS_O17.PATIENT.class);
        when(ras.getPATIENT()).thenReturn(patient);
        when(patient.getPID()).thenThrow(new HL7Exception("No PID"));
    }

    private void setupRasWithPatient(RAS_O17 ras) throws HL7Exception {
        when(ras.getORDERReps()).thenReturn(0);

        RAS_O17.PATIENT patient = mock(RAS_O17.PATIENT.class);
        when(ras.getPATIENT()).thenReturn(patient);

        PID pid = createMockPid();
        when(patient.getPID()).thenReturn(pid);
    }

    private void setupRasWithAdministeredCode(RAS_O17 ras, String code, String display, String system) throws HL7Exception {
        setupRasWithAdministration(ras);

        RXA rxa = ras.getORDER(0).getADMINISTRATION(0).getRXA();

        CE adminCode = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(code);
        when(adminCode.getIdentifier()).thenReturn(identifier);

        ST text = mock(ST.class);
        when(text.getValue()).thenReturn(display);
        when(adminCode.getText()).thenReturn(text);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn(system);
        when(adminCode.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(rxa.getAdministeredCode()).thenReturn(adminCode);
    }

    private void setupRasWithAdministeredAmount(RAS_O17 ras, String amount, String units) throws HL7Exception {
        setupRasWithAdministeredCode(ras, "RX001", "Test Drug", "NDC");

        RXA rxa = ras.getORDER(0).getADMINISTRATION(0).getRXA();

        NM adminAmount = mock(NM.class);
        when(adminAmount.getValue()).thenReturn(amount);
        when(rxa.getAdministeredAmount()).thenReturn(adminAmount);

        CE adminUnits = mock(CE.class);
        ST unitId = mock(ST.class);
        when(unitId.getValue()).thenReturn(units);
        when(adminUnits.getIdentifier()).thenReturn(unitId);

        ST unitText = mock(ST.class);
        when(unitText.getValue()).thenReturn(units);
        when(adminUnits.getText()).thenReturn(unitText);

        when(rxa.getAdministeredUnits()).thenReturn(adminUnits);
    }

    private void setupRasWithDateTimes(RAS_O17 ras, String startTime, String endTime) throws HL7Exception {
        setupRasWithAdministeredCode(ras, "RX001", "Test Drug", "NDC");

        RXA rxa = ras.getORDER(0).getADMINISTRATION(0).getRXA();

        TS startDateTime = mock(TS.class);
        DTM startDtm = mock(DTM.class);
        when(startDtm.getValue()).thenReturn(startTime);
        when(startDateTime.getTime()).thenReturn(startDtm);
        when(rxa.getDateTimeStartOfAdministration()).thenReturn(startDateTime);

        TS endDateTime = mock(TS.class);
        DTM endDtm = mock(DTM.class);
        when(endDtm.getValue()).thenReturn(endTime);
        when(endDateTime.getTime()).thenReturn(endDtm);
        when(rxa.getDateTimeEndOfAdministration()).thenReturn(endDateTime);
    }

    private void setupRasWithProvider(RAS_O17 ras, String id, String familyName, String givenName) throws HL7Exception {
        setupRasWithAdministeredCode(ras, "RX001", "Test Drug", "NDC");

        RXA rxa = ras.getORDER(0).getADMINISTRATION(0).getRXA();

        XCN provider = mock(XCN.class);
        ST idNumber = mock(ST.class);
        when(idNumber.getValue()).thenReturn(id);
        when(provider.getIDNumber()).thenReturn(idNumber);

        FN family = mock(FN.class);
        ST surname = mock(ST.class);
        when(surname.getValue()).thenReturn(familyName);
        when(family.getSurname()).thenReturn(surname);
        when(provider.getFamilyName()).thenReturn(family);

        ST given = mock(ST.class);
        when(given.getValue()).thenReturn(givenName);
        when(provider.getGivenName()).thenReturn(given);

        when(rxa.getAdministeringProvider(0)).thenReturn(provider);
    }

    private void setupRasWithLotInfo(RAS_O17 ras, String lotNumber, String expirationDate) throws HL7Exception {
        setupRasWithAdministeredCode(ras, "RX001", "Test Drug", "NDC");

        RXA rxa = ras.getORDER(0).getADMINISTRATION(0).getRXA();

        ST lot = mock(ST.class);
        when(lot.getValue()).thenReturn(lotNumber);
        when(rxa.getSubstanceLotNumber(0)).thenReturn(lot);

        TS expDate = mock(TS.class);
        DTM expDtm = mock(DTM.class);
        when(expDtm.getValue()).thenReturn(expirationDate);
        when(expDate.getTime()).thenReturn(expDtm);
        when(rxa.getSubstanceExpirationDate(0)).thenReturn(expDate);
    }

    private void setupRasWithCompletionStatus(RAS_O17 ras, String status) throws HL7Exception {
        setupRasWithAdministeredCode(ras, "RX001", "Test Drug", "NDC");

        RXA rxa = ras.getORDER(0).getADMINISTRATION(0).getRXA();

        ID completionStatus = mock(ID.class);
        when(completionStatus.getValue()).thenReturn(status);
        when(rxa.getCompletionStatus()).thenReturn(completionStatus);
    }

    private void setupRasWithRoute(RAS_O17 ras, String code, String display, String system) throws HL7Exception {
        setupRasWithAdministration(ras);

        RAS_O17.ORDER.ADMINISTRATION admin = ras.getORDER(0).getADMINISTRATION(0);

        RXR rxr = mock(RXR.class);
        when(admin.getRXR()).thenReturn(rxr);

        CE route = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(code);
        when(route.getIdentifier()).thenReturn(identifier);

        ST text = mock(ST.class);
        when(text.getValue()).thenReturn(display);
        when(route.getText()).thenReturn(text);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn(system);
        when(route.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(rxr.getRoute()).thenReturn(route);
    }

    private PID createMockPid() throws HL7Exception {
        PID pid = mock(PID.class);

        CX patientId = mock(CX.class);
        ST idNumber = mock(ST.class);
        when(idNumber.getValue()).thenReturn("12345");
        when(patientId.getIDNumber()).thenReturn(idNumber);
        when(pid.getPatientID()).thenReturn(patientId);
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
}

package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.VXU_V04;
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
 * Unit tests for VxuMessageHandler.
 * Tests VXU^V04 (Unsolicited Vaccination Record Update) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VXU Message Handler Tests")
class VxuMessageHandlerTest {

    @InjectMocks
    private VxuMessageHandler handler;

    @Nested
    @DisplayName("VXU^V04 - Vaccination Update")
    class VxuV04Tests {

        @Test
        @DisplayName("Should extract immunizations")
        void handle_withValidVxu_extractsImmunizations() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithImmunization(vxu);

            Map<String, Object> result = handler.handle(vxu);

            assertThat(result).containsKey("immunizations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations).hasSize(1);
        }

        @Test
        @DisplayName("Should extract patient data")
        void handle_withPatientData_extractsPatient() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithPatient(vxu);

            Map<String, Object> result = handler.handle(vxu);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("familyName", "Smith");
            assertThat(patient).containsEntry("givenName", "John");
        }

        @Test
        @DisplayName("Should return empty map for non-VXU message")
        void handle_withNonVxuMessage_returnsEmptyMap() throws HL7Exception {
            ca.uhn.hl7v2.model.Message otherMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(otherMessage);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Vaccine Administration (RXA) Extraction")
    class VaccineAdministrationTests {

        @Test
        @DisplayName("Should extract vaccine code (CVX)")
        void extractVaccineAdministration_withCvxCode_extractsCode() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithVaccineCode(vxu, "03", "MMR", "CVX");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            @SuppressWarnings("unchecked")
            Map<String, String> vaccineCode = (Map<String, String>) immunizations.get(0).get("vaccineCode");
            assertThat(vaccineCode).containsEntry("code", "03");
            assertThat(vaccineCode).containsEntry("display", "MMR");
            assertThat(immunizations.get(0)).containsEntry("vaccineCodeSystemUri", "http://hl7.org/fhir/sid/cvx");
        }

        @Test
        @DisplayName("Should extract administration date")
        void extractVaccineAdministration_withDate_extractsDate() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithAdministrationDate(vxu, "202401151000");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations.get(0)).containsEntry("administrationDate", "202401151000");
        }

        @Test
        @DisplayName("Should extract dose quantity")
        void extractVaccineAdministration_withDose_extractsDose() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithDose(vxu, "0.5", "mL");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations.get(0)).containsEntry("doseQuantity", "0.5");
            assertThat(immunizations.get(0)).containsEntry("doseUnit", "mL");
        }

        @Test
        @DisplayName("Should extract lot number and expiration")
        void extractVaccineAdministration_withLotInfo_extractsLotInfo() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithLotInfo(vxu, "LOT123", "20251231");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations.get(0)).containsEntry("lotNumber", "LOT123");
            assertThat(immunizations.get(0)).containsEntry("expirationDate", "20251231");
        }

        @Test
        @DisplayName("Should extract manufacturer (MVX)")
        void extractVaccineAdministration_withManufacturer_extractsManufacturer() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithManufacturer(vxu, "PFR", "Pfizer");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations.get(0)).containsEntry("manufacturer", "Pfizer");
            assertThat(immunizations.get(0)).containsEntry("manufacturerCode", "PFR");
        }

        @Test
        @DisplayName("Should extract performer")
        void extractVaccineAdministration_withPerformer_extractsPerformer() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithPerformer(vxu, "RN001", "Jones", "Mary");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            @SuppressWarnings("unchecked")
            Map<String, String> performer = (Map<String, String>) immunizations.get(0).get("performer");
            assertThat(performer).containsEntry("id", "RN001");
            assertThat(performer).containsEntry("familyName", "Jones");
        }
    }

    @Nested
    @DisplayName("Completion Status Mapping")
    class CompletionStatusTests {

        @Test
        @DisplayName("Should map CP to completed")
        void mapCompletionStatus_withCP_returnsCompleted() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithCompletionStatus(vxu, "CP");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations.get(0)).containsEntry("status", "completed");
        }

        @Test
        @DisplayName("Should map RE to not-done")
        void mapCompletionStatus_withRE_returnsNotDone() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithCompletionStatus(vxu, "RE");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations.get(0)).containsEntry("status", "not-done");
        }
    }

    @Nested
    @DisplayName("Route and Site (RXR) Extraction")
    class RouteTests {

        @Test
        @DisplayName("Should extract route information")
        void extractRoute_withValidRoute_extractsRoute() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithRoute(vxu, "IM", "Intramuscular", "HL7");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            @SuppressWarnings("unchecked")
            Map<String, String> route = (Map<String, String>) immunizations.get(0).get("route");
            assertThat(route).containsEntry("code", "IM");
            assertThat(route).containsEntry("display", "Intramuscular");
        }

        @Test
        @DisplayName("Should extract administration site")
        void extractRoute_withSite_extractsSite() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithSite(vxu, "LA", "Left Arm");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            @SuppressWarnings("unchecked")
            Map<String, String> site = (Map<String, String>) immunizations.get(0).get("site");
            assertThat(site).containsEntry("code", "LA");
            assertThat(site).containsEntry("display", "Left Arm");
        }
    }

    @Nested
    @DisplayName("Vaccine Observation (OBX) Extraction")
    class VaccineObservationTests {

        @Test
        @DisplayName("Should extract vaccine funding source")
        void extractObservation_withFundingSource_extractsFunding() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithFundingObservation(vxu, "64994-7", "VFC");

            Map<String, Object> result = handler.handle(vxu);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations.get(0)).containsEntry("fundingSource", "VFC");
        }
    }

    @Nested
    @DisplayName("Next of Kin (NK1) Extraction")
    class NextOfKinTests {

        @Test
        @DisplayName("Should extract next of kin information")
        void extractNextOfKin_withValidData_extractsNK() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class);
            setupVxuWithNextOfKin(vxu, "Smith", "Jane", "MTH", "Mother");

            Map<String, Object> result = handler.handle(vxu);

            assertThat(result).containsKey("nextOfKin");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nextOfKins = (List<Map<String, Object>>) result.get("nextOfKin");
            assertThat(nextOfKins).hasSize(1);
            assertThat(nextOfKins.get(0)).containsEntry("familyName", "Smith");
            assertThat(nextOfKins.get(0)).containsEntry("relationship", "MTH");
        }
    }

    // Helper methods
    private void setupVxuWithImmunization(VXU_V04 vxu) throws HL7Exception {
        when(vxu.getPID()).thenReturn(null);
        when(vxu.getNK1Reps()).thenReturn(0);
        when(vxu.getORDERReps()).thenReturn(1);

        VXU_V04.ORDER order = mock(VXU_V04.ORDER.class);
        when(vxu.getORDER(0)).thenReturn(order);

        RXA rxa = mock(RXA.class);
        when(order.getRXA()).thenReturn(rxa);
        when(order.getRXR()).thenReturn(null);
        when(order.getOBSERVATIONReps()).thenReturn(0);
    }

    private void setupVxuWithPatient(VXU_V04 vxu) throws HL7Exception {
        PID pid = createMockPid();
        when(vxu.getPID()).thenReturn(pid);
        when(vxu.getNK1Reps()).thenReturn(0);
        when(vxu.getORDERReps()).thenReturn(0);
    }

    private void setupVxuWithVaccineCode(VXU_V04 vxu, String code, String display, String system) throws HL7Exception {
        setupVxuWithImmunization(vxu);

        RXA rxa = vxu.getORDER(0).getRXA();

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

    private void setupVxuWithAdministrationDate(VXU_V04 vxu, String date) throws HL7Exception {
        setupVxuWithVaccineCode(vxu, "03", "MMR", "CVX");

        RXA rxa = vxu.getORDER(0).getRXA();

        TS adminDateTime = mock(TS.class);
        DTM dtm = mock(DTM.class);
        when(dtm.getValue()).thenReturn(date);
        when(adminDateTime.getTime()).thenReturn(dtm);
        when(rxa.getDateTimeStartOfAdministration()).thenReturn(adminDateTime);
    }

    private void setupVxuWithDose(VXU_V04 vxu, String amount, String unit) throws HL7Exception {
        setupVxuWithVaccineCode(vxu, "03", "MMR", "CVX");

        RXA rxa = vxu.getORDER(0).getRXA();

        NM adminAmount = mock(NM.class);
        when(adminAmount.getValue()).thenReturn(amount);
        when(rxa.getAdministeredAmount()).thenReturn(adminAmount);

        CE adminUnits = mock(CE.class);
        ST unitId = mock(ST.class);
        when(unitId.getValue()).thenReturn(unit);
        when(adminUnits.getIdentifier()).thenReturn(unitId);
        when(rxa.getAdministeredUnits()).thenReturn(adminUnits);
    }

    private void setupVxuWithLotInfo(VXU_V04 vxu, String lotNumber, String expirationDate) throws HL7Exception {
        setupVxuWithVaccineCode(vxu, "03", "MMR", "CVX");

        RXA rxa = vxu.getORDER(0).getRXA();

        ST lot = mock(ST.class);
        when(lot.getValue()).thenReturn(lotNumber);
        when(rxa.getSubstanceLotNumber(0)).thenReturn(lot);

        TS expDate = mock(TS.class);
        DTM expDtm = mock(DTM.class);
        when(expDtm.getValue()).thenReturn(expirationDate);
        when(expDate.getTime()).thenReturn(expDtm);
        when(rxa.getSubstanceExpirationDate(0)).thenReturn(expDate);
    }

    private void setupVxuWithManufacturer(VXU_V04 vxu, String code, String name) throws HL7Exception {
        setupVxuWithVaccineCode(vxu, "03", "MMR", "CVX");

        RXA rxa = vxu.getORDER(0).getRXA();

        CE manufacturer = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(code);
        when(manufacturer.getIdentifier()).thenReturn(identifier);

        ST text = mock(ST.class);
        when(text.getValue()).thenReturn(name);
        when(manufacturer.getText()).thenReturn(text);

        when(rxa.getSubstanceManufacturerName(0)).thenReturn(manufacturer);
    }

    private void setupVxuWithPerformer(VXU_V04 vxu, String id, String familyName, String givenName) throws HL7Exception {
        setupVxuWithVaccineCode(vxu, "03", "MMR", "CVX");

        RXA rxa = vxu.getORDER(0).getRXA();

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

    private void setupVxuWithCompletionStatus(VXU_V04 vxu, String status) throws HL7Exception {
        setupVxuWithVaccineCode(vxu, "03", "MMR", "CVX");

        RXA rxa = vxu.getORDER(0).getRXA();

        ID completionStatus = mock(ID.class);
        when(completionStatus.getValue()).thenReturn(status);
        when(rxa.getCompletionStatus()).thenReturn(completionStatus);
    }

    private void setupVxuWithRoute(VXU_V04 vxu, String code, String display, String system) throws HL7Exception {
        setupVxuWithImmunization(vxu);

        VXU_V04.ORDER order = vxu.getORDER(0);

        RXR rxr = mock(RXR.class);
        when(order.getRXR()).thenReturn(rxr);

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

    private void setupVxuWithSite(VXU_V04 vxu, String code, String display) throws HL7Exception {
        setupVxuWithRoute(vxu, "IM", "Intramuscular", "HL7");

        RXR rxr = vxu.getORDER(0).getRXR();

        CWE site = mock(CWE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(code);
        when(site.getIdentifier()).thenReturn(identifier);

        ST text = mock(ST.class);
        when(text.getValue()).thenReturn(display);
        when(site.getText()).thenReturn(text);

        when(rxr.getAdministrationSite()).thenReturn(site);
    }

    private void setupVxuWithFundingObservation(VXU_V04 vxu, String obsCode, String value) throws HL7Exception {
        setupVxuWithImmunization(vxu);

        VXU_V04.ORDER order = vxu.getORDER(0);
        when(order.getOBSERVATIONReps()).thenReturn(1);

        VXU_V04.ORDER.OBSERVATION observation = mock(VXU_V04.ORDER.OBSERVATION.class);
        when(order.getOBSERVATION(0)).thenReturn(observation);

        OBX obx = mock(OBX.class);
        when(observation.getOBX()).thenReturn(obx);

        CE obsIdentifier = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(obsCode);
        when(obsIdentifier.getIdentifier()).thenReturn(identifier);

        ST text = mock(ST.class);
        when(text.getValue()).thenReturn("Vaccine Funding Source");
        when(obsIdentifier.getText()).thenReturn(text);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn("LN");
        when(obsIdentifier.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(obx.getObservationIdentifier()).thenReturn(obsIdentifier);

        // Mock observation value
        Varies obsValue = mock(Varies.class);
        when(obsValue.getData()).thenReturn(mock(ca.uhn.hl7v2.model.Type.class));
        when(obsValue.getData().toString()).thenReturn(value);
        when(obx.getObservationValue(0)).thenReturn(obsValue);
    }

    private void setupVxuWithNextOfKin(VXU_V04 vxu, String familyName, String givenName, String relationshipCode, String relationshipDisplay) throws HL7Exception {
        when(vxu.getPID()).thenReturn(null);
        when(vxu.getNK1Reps()).thenReturn(1);
        when(vxu.getORDERReps()).thenReturn(0);

        NK1 nk1 = mock(NK1.class);
        when(vxu.getNK1(0)).thenReturn(nk1);

        XPN name = mock(XPN.class);
        FN family = mock(FN.class);
        ST surname = mock(ST.class);
        when(surname.getValue()).thenReturn(familyName);
        when(family.getSurname()).thenReturn(surname);
        when(name.getFamilyName()).thenReturn(family);

        ST given = mock(ST.class);
        when(given.getValue()).thenReturn(givenName);
        when(name.getGivenName()).thenReturn(given);
        when(nk1.getNKName(0)).thenReturn(name);

        CE relationship = mock(CE.class);
        ST relCode = mock(ST.class);
        when(relCode.getValue()).thenReturn(relationshipCode);
        when(relationship.getIdentifier()).thenReturn(relCode);

        ST relText = mock(ST.class);
        when(relText.getValue()).thenReturn(relationshipDisplay);
        when(relationship.getText()).thenReturn(relText);
        when(nk1.getRelationship()).thenReturn(relationship);
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

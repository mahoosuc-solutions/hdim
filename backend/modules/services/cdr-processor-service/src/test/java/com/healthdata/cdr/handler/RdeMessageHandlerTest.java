package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.RDE_O11;
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
 * Unit tests for RdeMessageHandler.
 * Tests RDE^O11 (Pharmacy/Treatment Encoded Order) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RDE Message Handler Tests")
class RdeMessageHandlerTest {

    @InjectMocks
    private RdeMessageHandler handler;

    @Nested
    @DisplayName("RDE^O11 - Pharmacy Encoded Order")
    class RdeO11Tests {

        @Test
        @DisplayName("Should extract orders from RDE message")
        void handle_withValidRde_extractsOrders() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithOrder(rde);

            Map<String, Object> result = handler.handle(rde);

            assertThat(result).containsKey("orders");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            assertThat(orders).hasSize(1);
        }

        @Test
        @DisplayName("Should extract patient data")
        void handle_withPatientData_extractsPatient() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithPatient(rde);

            Map<String, Object> result = handler.handle(rde);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("familyName", "Smith");
            assertThat(patient).containsEntry("givenName", "John");
        }

        @Test
        @DisplayName("Should return empty map for non-RDE message")
        void handle_withNonRdeMessage_returnsEmptyMap() throws HL7Exception {
            ca.uhn.hl7v2.model.Message otherMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(otherMessage);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Order Control (ORC) Extraction")
    class OrderControlTests {

        @Test
        @DisplayName("Should extract order control code")
        void extractOrderControl_withCode_extractsCode() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithOrderControl(rde, "NW");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            assertThat(orders.get(0)).containsEntry("orderControlCode", "NW");
        }

        @Test
        @DisplayName("Should extract placer order number")
        void extractOrderControl_withPlacerOrder_extractsNumber() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithPlacerOrder(rde, "ORD001");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            assertThat(orders.get(0)).containsEntry("placerOrderNumber", "ORD001");
        }

        @Test
        @DisplayName("Should extract ordering provider")
        void extractOrderControl_withProvider_extractsProvider() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithOrderingProvider(rde, "DR001", "Jones", "Mary");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            @SuppressWarnings("unchecked")
            Map<String, String> provider = (Map<String, String>) orders.get(0).get("orderingProvider");
            assertThat(provider).containsEntry("id", "DR001");
            assertThat(provider).containsEntry("familyName", "Jones");
        }
    }

    @Nested
    @DisplayName("Pharmacy Order (RXE) Extraction")
    class PharmacyOrderTests {

        @Test
        @DisplayName("Should extract drug code")
        void extractPharmacyOrder_withDrugCode_extractsCode() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithDrugCode(rde, "RX001", "Lisinopril 10mg", "NDC");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            @SuppressWarnings("unchecked")
            Map<String, Object> medication = (Map<String, Object>) orders.get(0).get("medication");
            @SuppressWarnings("unchecked")
            Map<String, String> drugCode = (Map<String, String>) medication.get("drugCode");
            assertThat(drugCode).containsEntry("code", "RX001");
            assertThat(drugCode).containsEntry("display", "Lisinopril 10mg");
        }

        @Test
        @DisplayName("Should extract dose amount")
        void extractPharmacyOrder_withDose_extractsAmount() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithDose(rde, "10", "20", "mg");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            @SuppressWarnings("unchecked")
            Map<String, Object> medication = (Map<String, Object>) orders.get(0).get("medication");
            assertThat(medication).containsEntry("giveAmountMin", "10");
            assertThat(medication).containsEntry("giveAmountMax", "20");
            assertThat(medication).containsEntry("giveUnits", "mg");
        }

        @Test
        @DisplayName("Should extract dispense amount and refills")
        void extractPharmacyOrder_withDispense_extractsDispense() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithDispense(rde, "30", "3");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            @SuppressWarnings("unchecked")
            Map<String, Object> medication = (Map<String, Object>) orders.get(0).get("medication");
            assertThat(medication).containsEntry("dispenseAmount", "30");
            assertThat(medication).containsEntry("numberOfRefills", "3");
        }

        @Test
        @DisplayName("Should extract prescription number")
        void extractPharmacyOrder_withRxNumber_extractsNumber() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithRxNumber(rde, "RX123456");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            @SuppressWarnings("unchecked")
            Map<String, Object> medication = (Map<String, Object>) orders.get(0).get("medication");
            assertThat(medication).containsEntry("prescriptionNumber", "RX123456");
        }
    }

    @Nested
    @DisplayName("Route (RXR) Extraction")
    class RouteTests {

        @Test
        @DisplayName("Should extract route information")
        void extractRoute_withValidRoute_extractsRoute() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithRoute(rde, "PO", "Oral", "HL7");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> routes = (List<Map<String, String>>) orders.get(0).get("routes");
            assertThat(routes).hasSize(1);
            assertThat(routes.get(0)).containsEntry("code", "PO");
            assertThat(routes.get(0)).containsEntry("display", "Oral");
        }

        @Test
        @DisplayName("Should extract administration site")
        void extractRoute_withSite_extractsSite() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class);
            setupRdeWithAdministrationSite(rde, "LA", "Left Arm");

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> routes = (List<Map<String, String>>) orders.get(0).get("routes");
            assertThat(routes.get(0)).containsEntry("siteCode", "LA");
            assertThat(routes.get(0)).containsEntry("siteDisplay", "Left Arm");
        }
    }

    // Helper methods
    private void setupRdeWithOrder(RDE_O11 rde) throws HL7Exception {
        when(rde.getORDERReps()).thenReturn(1);

        RDE_O11.ORDER order = mock(RDE_O11.ORDER.class);
        when(rde.getORDER(0)).thenReturn(order);

        ORC orc = mock(ORC.class);
        when(order.getORC()).thenReturn(orc);

        RXE rxe = mock(RXE.class);
        when(order.getRXE()).thenReturn(rxe);

        when(order.getRXRReps()).thenReturn(0);

        // Mock patient segment that throws exception
        RDE_O11.PATIENT patient = mock(RDE_O11.PATIENT.class);
        when(rde.getPATIENT()).thenReturn(patient);
        when(patient.getPID()).thenThrow(new HL7Exception("No PID"));
    }

    private void setupRdeWithPatient(RDE_O11 rde) throws HL7Exception {
        when(rde.getORDERReps()).thenReturn(0);

        RDE_O11.PATIENT patient = mock(RDE_O11.PATIENT.class);
        when(rde.getPATIENT()).thenReturn(patient);

        PID pid = createMockPid();
        when(patient.getPID()).thenReturn(pid);

        RDE_O11.PATIENT.PATIENT_VISIT patientVisit = mock(RDE_O11.PATIENT.PATIENT_VISIT.class);
        when(patient.getPATIENT_VISIT()).thenReturn(patientVisit);
        when(patientVisit.getPV1()).thenThrow(new HL7Exception("No PV1"));
    }

    private void setupRdeWithOrderControl(RDE_O11 rde, String controlCode) throws HL7Exception {
        setupRdeWithOrder(rde);

        ORC orc = rde.getORDER(0).getORC();
        ID orderControl = mock(ID.class);
        when(orderControl.getValue()).thenReturn(controlCode);
        when(orc.getOrderControl()).thenReturn(orderControl);
    }

    private void setupRdeWithPlacerOrder(RDE_O11 rde, String orderNumber) throws HL7Exception {
        setupRdeWithOrderControl(rde, "NW");

        ORC orc = rde.getORDER(0).getORC();
        EI placerOrder = mock(EI.class);
        ST entityId = mock(ST.class);
        when(entityId.getValue()).thenReturn(orderNumber);
        when(placerOrder.getEntityIdentifier()).thenReturn(entityId);
        when(orc.getPlacerOrderNumber()).thenReturn(placerOrder);
    }

    private void setupRdeWithOrderingProvider(RDE_O11 rde, String id, String familyName, String givenName) throws HL7Exception {
        setupRdeWithOrderControl(rde, "NW");

        ORC orc = rde.getORDER(0).getORC();
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

        when(orc.getOrderingProvider(0)).thenReturn(provider);
    }

    private void setupRdeWithDrugCode(RDE_O11 rde, String code, String display, String system) throws HL7Exception {
        setupRdeWithOrder(rde);

        RXE rxe = rde.getORDER(0).getRXE();
        CE giveCode = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(code);
        when(giveCode.getIdentifier()).thenReturn(identifier);

        ST text = mock(ST.class);
        when(text.getValue()).thenReturn(display);
        when(giveCode.getText()).thenReturn(text);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn(system);
        when(giveCode.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(rxe.getGiveCode()).thenReturn(giveCode);
    }

    private void setupRdeWithDose(RDE_O11 rde, String min, String max, String units) throws HL7Exception {
        setupRdeWithDrugCode(rde, "RX001", "Test Drug", "NDC");

        RXE rxe = rde.getORDER(0).getRXE();

        NM minAmount = mock(NM.class);
        when(minAmount.getValue()).thenReturn(min);
        when(rxe.getGiveAmountMinimum()).thenReturn(minAmount);

        NM maxAmount = mock(NM.class);
        when(maxAmount.getValue()).thenReturn(max);
        when(rxe.getGiveAmountMaximum()).thenReturn(maxAmount);

        CE giveUnits = mock(CE.class);
        ST unitId = mock(ST.class);
        when(unitId.getValue()).thenReturn(units);
        when(giveUnits.getIdentifier()).thenReturn(unitId);
        when(rxe.getGiveUnits()).thenReturn(giveUnits);
    }

    private void setupRdeWithDispense(RDE_O11 rde, String amount, String refills) throws HL7Exception {
        setupRdeWithDrugCode(rde, "RX001", "Test Drug", "NDC");

        RXE rxe = rde.getORDER(0).getRXE();

        NM dispenseAmount = mock(NM.class);
        when(dispenseAmount.getValue()).thenReturn(amount);
        when(rxe.getDispenseAmount()).thenReturn(dispenseAmount);

        NM numberOfRefills = mock(NM.class);
        when(numberOfRefills.getValue()).thenReturn(refills);
        when(rxe.getNumberOfRefills()).thenReturn(numberOfRefills);
    }

    private void setupRdeWithRxNumber(RDE_O11 rde, String rxNumber) throws HL7Exception {
        setupRdeWithDrugCode(rde, "RX001", "Test Drug", "NDC");

        RXE rxe = rde.getORDER(0).getRXE();
        ST prescriptionNumber = mock(ST.class);
        when(prescriptionNumber.getValue()).thenReturn(rxNumber);
        when(rxe.getPrescriptionNumber()).thenReturn(prescriptionNumber);
    }

    private void setupRdeWithRoute(RDE_O11 rde, String code, String display, String system) throws HL7Exception {
        setupRdeWithOrder(rde);

        RDE_O11.ORDER order = rde.getORDER(0);
        when(order.getRXRReps()).thenReturn(1);

        RXR rxr = mock(RXR.class);
        when(order.getRXR(0)).thenReturn(rxr);

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

    private void setupRdeWithAdministrationSite(RDE_O11 rde, String siteCode, String siteDisplay) throws HL7Exception {
        setupRdeWithRoute(rde, "PO", "Oral", "HL7");

        RXR rxr = rde.getORDER(0).getRXR(0);

        CWE site = mock(CWE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(siteCode);
        when(site.getIdentifier()).thenReturn(identifier);

        ST text = mock(ST.class);
        when(text.getValue()).thenReturn(siteDisplay);
        when(site.getText()).thenReturn(text);

        when(rxr.getAdministrationSite()).thenReturn(site);
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

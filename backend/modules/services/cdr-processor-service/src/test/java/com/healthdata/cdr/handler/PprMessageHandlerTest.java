package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.PPR_PC1;
import ca.uhn.hl7v2.model.v25.segment.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PprMessageHandler.
 * Tests PPR (Patient Problem) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PPR Message Handler Tests")
class PprMessageHandlerTest {

    @InjectMocks
    private PprMessageHandler handler;

    @Nested
    @DisplayName("PPR^PC1 - Problem Add")
    class PprPc1Tests {

        @Test
        @DisplayName("Should extract trigger event PC1 for problem add")
        void handle_withPprPc1_extractsTriggerEvent() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupMinimalPpr(ppr, "PC1");

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsEntry("messageType", "PPR");
            assertThat(result).containsEntry("triggerEvent", "PC1");
            assertThat(result).containsEntry("eventDescription", "Problem add");
        }

        @Test
        @DisplayName("Should extract trigger event PC2 for problem update")
        void handle_withPprPc2_extractsTriggerEvent() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupMinimalPpr(ppr, "PC2");

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsEntry("triggerEvent", "PC2");
            assertThat(result).containsEntry("eventDescription", "Problem update");
        }

        @Test
        @DisplayName("Should extract trigger event PC3 for problem delete")
        void handle_withPprPc3_extractsTriggerEvent() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupMinimalPpr(ppr, "PC3");

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsEntry("triggerEvent", "PC3");
            assertThat(result).containsEntry("eventDescription", "Problem delete");
        }

        @Test
        @DisplayName("Should extract patient data from PID segment")
        void handle_withPprPc1_extractsPatientData() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithPatient(ppr);

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
            assertThat(patient).containsEntry("familyName", "Smith");
        }

        @Test
        @DisplayName("Should handle default trigger event")
        void handle_withDefaultTrigger_returnsPC1() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupMinimalPprWithException(ppr);

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsEntry("triggerEvent", "PC1");
        }
    }

    @Nested
    @DisplayName("Unsupported Message Types")
    class UnsupportedMessageTests {

        @Test
        @DisplayName("Should handle unsupported PPR message type gracefully")
        void handle_withUnsupportedType_returnsBasicData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).containsEntry("messageType", "PPR");
            assertThat(result).doesNotContainKey("triggerEvent");
        }
    }

    // Helper methods
    private void setupMinimalPpr(PPR_PC1 ppr, String triggerEvent) throws HL7Exception {
        MSH msh = mock(MSH.class);
        MSG messageType = mock(MSG.class);
        ID trigger = mock(ID.class);
        when(trigger.getValue()).thenReturn(triggerEvent);
        when(messageType.getTriggerEvent()).thenReturn(trigger);
        when(msh.getMessageType()).thenReturn(messageType);
        when(ppr.getMSH()).thenReturn(msh);

        when(ppr.getPID()).thenReturn(null);

        // Create mock for patient visit structure
        try {
            Object patientVisit = mock(Object.class);
            // Use lenient() for methods that may not be called
            lenient().when(ppr.getPATIENT_VISIT()).thenThrow(new RuntimeException("No visit"));
        } catch (Exception e) {
            // Ignore
        }

        when(ppr.getPROBLEMReps()).thenReturn(0);
    }

    private void setupMinimalPprWithException(PPR_PC1 ppr) throws HL7Exception {
        MSH msh = mock(MSH.class);
        when(msh.getMessageType()).thenThrow(new HL7Exception("No message type"));
        when(ppr.getMSH()).thenReturn(msh);

        when(ppr.getPID()).thenReturn(null);
        when(ppr.getPROBLEMReps()).thenReturn(0);
    }

    private void setupPprWithPatient(PPR_PC1 ppr) throws HL7Exception {
        setupMinimalPpr(ppr, "PC1");

        PID pid = createMockPidWithBasicData();
        when(ppr.getPID()).thenReturn(pid);
    }

    private PID createMockPidWithBasicData() throws HL7Exception {
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

        TS dob = mock(TS.class);
        DTM dobTime = mock(DTM.class);
        when(dobTime.getValue()).thenReturn("19800101");
        when(dob.getTime()).thenReturn(dobTime);
        when(pid.getDateTimeOfBirth()).thenReturn(dob);

        IS gender = mock(IS.class);
        when(gender.getValue()).thenReturn("M");
        when(pid.getAdministrativeSex()).thenReturn(gender);

        return pid;
    }
}

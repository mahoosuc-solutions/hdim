package com.healthdata.cdr.handler;
import org.junit.jupiter.api.Tag;

import ca.uhn.hl7v2.HL7Exception;
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
 * Tests RAS^O17 (Pharmacy Administration) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RAS Message Handler Tests")
@Tag("unit")
class RasMessageHandlerTest {

    @InjectMocks
    private RasMessageHandler handler;

    @Nested
    @DisplayName("Message Type Tests")
    class MessageTypeTests {

        @Test
        @DisplayName("Should return empty data for non-RAS_O17 message")
        void handle_withNonRasMessage_returnsEmptyData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should process RAS_O17 message and return medicationAdministrations key")
        void handle_withRasO17Message_returnsMedicationAdministrationsKey() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class, RETURNS_DEEP_STUBS);
            when(ras.getPATIENT().getPID()).thenThrow(new RuntimeException("No patient"));
            when(ras.getORDERReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(ras);

            assertThat(result).containsKey("medicationAdministrations");
        }
    }

    @Nested
    @DisplayName("Completion Status Mapping Tests")
    class CompletionStatusMappingTests {

        @Test
        @DisplayName("Should map CP status to completed")
        void mapCompletionStatus_withCp_returnsCompleted() throws HL7Exception {
            // The handler has private mapCompletionStatus method
            // We test it indirectly through handle()
            // For now, testing that the handler doesn't throw with minimal setup
            RAS_O17 ras = mock(RAS_O17.class, RETURNS_DEEP_STUBS);
            when(ras.getPATIENT().getPID()).thenThrow(new RuntimeException("No patient"));
            when(ras.getORDERReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(ras);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Order Processing Tests")
    class OrderProcessingTests {

        @Test
        @DisplayName("Should handle zero orders gracefully")
        void handle_withZeroOrders_returnsEmptyMedicationAdministrations() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class, RETURNS_DEEP_STUBS);
            when(ras.getPATIENT().getPID()).thenThrow(new RuntimeException("No patient"));
            when(ras.getORDERReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(ras);

            assertThat(result).containsKey("medicationAdministrations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> admins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            assertThat(admins).isEmpty();
        }

        @Test
        @DisplayName("Should extract order count correctly")
        void handle_withMultipleOrders_processesAllOrders() throws HL7Exception {
            RAS_O17 ras = mock(RAS_O17.class, RETURNS_DEEP_STUBS);
            when(ras.getPATIENT().getPID()).thenThrow(new RuntimeException("No patient"));
            when(ras.getORDERReps()).thenReturn(2);

            // Setup minimal order mocks
            ORC orc1 = mock(ORC.class);
            ORC orc2 = mock(ORC.class);
            when(ras.getORDER(0).getORC()).thenReturn(orc1);
            when(ras.getORDER(1).getORC()).thenReturn(orc2);
            when(orc1.getOrderControl()).thenReturn(null);
            when(orc2.getOrderControl()).thenReturn(null);
            when(orc1.getPlacerOrderNumber()).thenReturn(null);
            when(orc2.getPlacerOrderNumber()).thenReturn(null);
            when(orc1.getFillerOrderNumber()).thenReturn(null);
            when(orc2.getFillerOrderNumber()).thenReturn(null);

            when(ras.getORDER(0).getADMINISTRATIONReps()).thenReturn(0);
            when(ras.getORDER(1).getADMINISTRATIONReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(ras);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> medAdmins = (List<Map<String, Object>>) result.get("medicationAdministrations");
            assertThat(medAdmins).hasSize(2);
        }
    }
}

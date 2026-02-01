package com.healthdata.cdr.handler;
import org.junit.jupiter.api.Tag;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.RDE_O11;
import ca.uhn.hl7v2.model.v25.segment.ORC;
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
 * Tests RDE^O11 (Pharmacy Encoded Order) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RDE Message Handler Tests")
@Tag("unit")
class RdeMessageHandlerTest {

    @InjectMocks
    private RdeMessageHandler handler;

    @Nested
    @DisplayName("Message Type Tests")
    class MessageTypeTests {

        @Test
        @DisplayName("Should return empty data for non-RDE_O11 message")
        void handle_withNonRdeMessage_returnsEmptyData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should process RDE_O11 message and return orders key")
        void handle_withRdeO11Message_returnsOrdersKey() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class, RETURNS_DEEP_STUBS);
            when(rde.getPATIENT().getPID()).thenThrow(new RuntimeException("No patient"));
            when(rde.getORDERReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(rde);

            assertThat(result).containsKey("orders");
        }
    }

    @Nested
    @DisplayName("Order Processing Tests")
    class OrderProcessingTests {

        @Test
        @DisplayName("Should handle zero orders gracefully")
        void handle_withZeroOrders_returnsEmptyOrders() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class, RETURNS_DEEP_STUBS);
            when(rde.getPATIENT().getPID()).thenThrow(new RuntimeException("No patient"));
            when(rde.getORDERReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(rde);

            assertThat(result).containsKey("orders");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            assertThat(orders).isEmpty();
        }

        @Test
        @DisplayName("Should extract order count correctly")
        void handle_withMultipleOrders_processesAllOrders() throws HL7Exception {
            RDE_O11 rde = mock(RDE_O11.class, RETURNS_DEEP_STUBS);
            when(rde.getPATIENT().getPID()).thenThrow(new RuntimeException("No patient"));
            when(rde.getORDERReps()).thenReturn(2);

            // Setup minimal order mocks
            ORC orc1 = mock(ORC.class);
            ORC orc2 = mock(ORC.class);
            when(rde.getORDER(0).getORC()).thenReturn(orc1);
            when(rde.getORDER(1).getORC()).thenReturn(orc2);
            when(orc1.getOrderControl()).thenReturn(null);
            when(orc2.getOrderControl()).thenReturn(null);
            when(orc1.getPlacerOrderNumber()).thenReturn(null);
            when(orc2.getPlacerOrderNumber()).thenReturn(null);
            when(orc1.getFillerOrderNumber()).thenReturn(null);
            when(orc2.getFillerOrderNumber()).thenReturn(null);
            when(orc1.getOrderStatus()).thenReturn(null);
            when(orc2.getOrderStatus()).thenReturn(null);
            when(orc1.getDateTimeOfTransaction()).thenReturn(null);
            when(orc2.getDateTimeOfTransaction()).thenReturn(null);
            when(orc1.getOrderingProvider(0)).thenReturn(null);
            when(orc2.getOrderingProvider(0)).thenReturn(null);
            when(orc1.getOrderingFacilityName(0)).thenReturn(null);
            when(orc2.getOrderingFacilityName(0)).thenReturn(null);

            when(rde.getORDER(0).getRXE()).thenReturn(null);
            when(rde.getORDER(1).getRXE()).thenReturn(null);
            when(rde.getORDER(0).getRXRReps()).thenReturn(0);
            when(rde.getORDER(1).getRXRReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(rde);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orders");
            assertThat(orders).hasSize(2);
        }
    }
}

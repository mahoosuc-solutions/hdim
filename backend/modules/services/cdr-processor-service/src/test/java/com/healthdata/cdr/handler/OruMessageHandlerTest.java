package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
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
 * Unit tests for OruMessageHandler.
 * Tests ORU^R01 (Observation Result / Lab Results) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ORU Message Handler Tests")
class OruMessageHandlerTest {

    @InjectMocks
    private OruMessageHandler handler;

    @Nested
    @DisplayName("Message Type Tests")
    class MessageTypeTests {

        @Test
        @DisplayName("Should return empty data for non-ORU_R01 message")
        void handle_withNonOruMessage_returnsEmptyData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should process ORU_R01 message and return orderObservations key")
        void handle_withOruR01Message_returnsOrderObservationsKey() throws HL7Exception {
            ORU_R01 oru = mock(ORU_R01.class, RETURNS_DEEP_STUBS);
            when(oru.getPATIENT_RESULT().getPATIENT().getPID()).thenReturn(null);
            when(oru.getPATIENT_RESULTReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(oru);

            assertThat(result).containsKey("orderObservations");
            assertThat(result).containsKey("totalOrders");
        }
    }

    @Nested
    @DisplayName("Order Processing Tests")
    class OrderProcessingTests {

        @Test
        @DisplayName("Should handle zero orders gracefully")
        void handle_withZeroOrders_returnsEmptyOrderObservations() throws HL7Exception {
            ORU_R01 oru = mock(ORU_R01.class, RETURNS_DEEP_STUBS);
            when(oru.getPATIENT_RESULT().getPATIENT().getPID()).thenReturn(null);
            when(oru.getPATIENT_RESULTReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(oru);

            assertThat(result).containsKey("orderObservations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) result.get("orderObservations");
            assertThat(orders).isEmpty();
            assertThat(result.get("totalOrders")).isEqualTo(0);
        }
    }
}

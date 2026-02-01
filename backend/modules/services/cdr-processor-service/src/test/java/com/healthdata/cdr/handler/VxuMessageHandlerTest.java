package com.healthdata.cdr.handler;
import org.junit.jupiter.api.Tag;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.VXU_V04;
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
@Tag("unit")
class VxuMessageHandlerTest {

    @InjectMocks
    private VxuMessageHandler handler;

    @Nested
    @DisplayName("Message Type Tests")
    class MessageTypeTests {

        @Test
        @DisplayName("Should return empty data for non-VXU_V04 message")
        void handle_withNonVxuMessage_returnsEmptyData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should process VXU_V04 message and return immunizations key")
        void handle_withVxuV04Message_returnsImmunizationsKey() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class, RETURNS_DEEP_STUBS);
            when(vxu.getPID()).thenReturn(null);
            when(vxu.getNK1Reps()).thenReturn(0);
            when(vxu.getORDERReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(vxu);

            assertThat(result).containsKey("immunizations");
        }
    }

    @Nested
    @DisplayName("Order Processing Tests")
    class OrderProcessingTests {

        @Test
        @DisplayName("Should handle zero orders gracefully")
        void handle_withZeroOrders_returnsEmptyImmunizations() throws HL7Exception {
            VXU_V04 vxu = mock(VXU_V04.class, RETURNS_DEEP_STUBS);
            when(vxu.getPID()).thenReturn(null);
            when(vxu.getNK1Reps()).thenReturn(0);
            when(vxu.getORDERReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(vxu);

            assertThat(result).containsKey("immunizations");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> immunizations = (List<Map<String, Object>>) result.get("immunizations");
            assertThat(immunizations).isEmpty();
        }
    }
}

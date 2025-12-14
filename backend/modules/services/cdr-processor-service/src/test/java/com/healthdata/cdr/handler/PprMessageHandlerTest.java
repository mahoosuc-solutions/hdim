package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.PPR_PC1;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.PV1;
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
 * Tests PPR^PC1-PC3 (Problem Add/Update/Delete) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PPR Message Handler Tests")
class PprMessageHandlerTest {

    @InjectMocks
    private PprMessageHandler handler;

    @Nested
    @DisplayName("Message Type Tests")
    class MessageTypeTests {

        @Test
        @DisplayName("Should return PPR messageType for non-PPR_PC1 message")
        void handle_withNonPprMessage_returnsMessageTypeOnly() throws HL7Exception {
            // The handler returns messageType: "PPR" for any message type
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            // The handler always adds messageType
            assertThat(result).containsKey("messageType");
            assertThat(result.get("messageType")).isEqualTo("PPR");
        }

        @Test
        @DisplayName("Should process PPR_PC1 message and extract trigger event")
        void handle_withPprPc1Message_extractsTriggerEvent() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class, RETURNS_DEEP_STUBS);

            // Setup MSH for trigger event
            MSH msh = mock(MSH.class);
            when(ppr.getMSH()).thenReturn(msh);
            when(msh.getMessageType()).thenReturn(null);

            // Setup PID returning null (handler checks for null before use)
            when(ppr.getPID()).thenReturn(null);

            // Setup empty PATIENT_VISIT
            PV1 pv1 = mock(PV1.class);
            when(ppr.getPATIENT_VISIT().getPV1()).thenReturn(pv1);
            when(pv1.getPatientClass()).thenReturn(null);
            when(pv1.getVisitNumber()).thenReturn(null);

            // Setup zero problems (no problems key will be added in this case)
            when(ppr.getPROBLEMReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(ppr);

            // The handler always includes these fields
            assertThat(result).containsKey("messageType");
            assertThat(result).containsKey("triggerEvent");
            assertThat(result).containsKey("eventDescription");
            assertThat(result).containsKey("visit");
        }
    }

    @Nested
    @DisplayName("Message Type Identification Tests")
    class MessageTypeIdentificationTests {

        @Test
        @DisplayName("Should always include messageType in result")
        void handle_withAnyMessage_returnsMessageType() throws HL7Exception {
            ca.uhn.hl7v2.model.Message msg = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(msg);

            assertThat(result.get("messageType")).isEqualTo("PPR");
        }
    }
}

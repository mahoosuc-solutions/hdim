package com.healthdata.ingestion.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.ingestion.application.AdtExchangeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdtInteroperabilityController.class)
@Import(AdtExchangeService.class)
@Tag("integration")
@DisplayName("AdtInteroperabilityController Tests")
class AdtInteroperabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/interoperability/adt/messages ingests supported ADT message")
    void shouldIngestAdtMessage() throws Exception {
        String request = """
            {
              "tenantId":"tenant-a",
              "sourceSystem":"hie-main",
              "sourceMessageId":"msg-001",
              "eventType":"A01",
              "patientExternalId":"pat-001",
              "encounterExternalId":"enc-001",
              "payloadHash":"abc123hash",
              "correlationId":"corr-adt-001"
            }
            """;

        mockMvc.perform(post("/api/v1/interoperability/adt/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").isNotEmpty())
                .andExpect(jsonPath("$.state").value("ROUTED"))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/interoperability/adt/messages suppresses duplicate sourceMessageId")
    void shouldSuppressDuplicateMessageReplay() throws Exception {
        String request = """
            {
              "tenantId":"tenant-a",
              "sourceSystem":"hie-main",
              "sourceMessageId":"msg-dup",
              "eventType":"A01",
              "patientExternalId":"pat-dup",
              "encounterExternalId":"enc-dup",
              "payloadHash":"dup-hash",
              "correlationId":"corr-dup"
            }
            """;

        mockMvc.perform(post("/api/v1/interoperability/adt/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(false));

        mockMvc.perform(post("/api/v1/interoperability/adt/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.auditEnvelope.outcome").value("DUPLICATE_SUPPRESSED"));
    }

    @Test
    @DisplayName("POST /api/v1/interoperability/adt/messages rejects unsupported event type")
    void shouldRejectUnsupportedEventType() throws Exception {
        String request = """
            {
              "tenantId":"tenant-a",
              "sourceSystem":"hie-main",
              "sourceMessageId":"msg-bad-type",
              "eventType":"A99",
              "patientExternalId":"pat-001",
              "encounterExternalId":"enc-001",
              "payloadHash":"bad-type-hash",
              "correlationId":"corr-bad-type"
            }
            """;

        mockMvc.perform(post("/api/v1/interoperability/adt/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REJECTED"))
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_EVENT_TYPE"));
    }

    @Test
    @DisplayName("POST /api/v1/interoperability/adt/acks transitions event to ACKNOWLEDGED")
    void shouldAcknowledgeEvent() throws Exception {
        String ingestRequest = """
            {
              "tenantId":"tenant-a",
              "sourceSystem":"hie-main",
              "sourceMessageId":"msg-ack",
              "eventType":"A03",
              "patientExternalId":"pat-ack",
              "encounterExternalId":"enc-ack",
              "payloadHash":"ack-hash",
              "correlationId":"corr-ack"
            }
            """;

        MvcResult ingestResult = mockMvc.perform(post("/api/v1/interoperability/adt/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode ingestJson = objectMapper.readTree(ingestResult.getResponse().getContentAsString());
        String eventId = ingestJson.get("eventId").asText();

        String ackRequest = """
            {
              "tenantId":"tenant-a",
              "eventId":"%s",
              "sourceSystem":"hie-main",
              "correlationId":"corr-ack"
            }
            """.formatted(eventId);

        mockMvc.perform(post("/api/v1/interoperability/adt/acks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ackRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.state").value("ACKNOWLEDGED"));

        mockMvc.perform(get("/api/v1/interoperability/adt/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACKNOWLEDGED"));
    }
}

package com.healthdata.common.external;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ExternalEventEnvelopeTest {

    @Test
    void of_shouldCreateEnvelopeWithAllFields() {
        ExternalEventMetadata metadata = ExternalEventMetadata.builder()
                .sourceSystem(SourceSystem.COREHIVE)
                .phiLevel(PhiLevel.NONE)
                .auditTraceId("trace-123")
                .build();

        ExternalEventEnvelope<Map<String, String>> envelope = ExternalEventEnvelope.of(
                "external.corehive.decisions.scored",
                "corehive-adapter-service",
                "tenant-1",
                Map.of("score", "0.85"),
                metadata);

        assertThat(envelope.getEventId()).isNotNull();
        assertThat(envelope.getEventType()).isEqualTo("external.corehive.decisions.scored");
        assertThat(envelope.getSource()).isEqualTo("corehive-adapter-service");
        assertThat(envelope.getTenantId()).isEqualTo("tenant-1");
        assertThat(envelope.getVersion()).isEqualTo("1.0");
        assertThat(envelope.getCorrelationId()).isNotNull();
        assertThat(envelope.getTimestamp()).isNotNull();
        assertThat(envelope.getPayload()).containsEntry("score", "0.85");
        assertThat(envelope.getMetadata().getSourceSystem()).isEqualTo(SourceSystem.COREHIVE);
        assertThat(envelope.getMetadata().getPhiLevel()).isEqualTo(PhiLevel.NONE);
    }

    @Test
    void builder_shouldGenerateDefaultValues() {
        ExternalEventEnvelope<String> envelope = ExternalEventEnvelope.<String>builder()
                .eventType("test.event")
                .source("test-service")
                .payload("test-data")
                .build();

        assertThat(envelope.getEventId()).isNotNull();
        assertThat(envelope.getTimestamp()).isNotNull();
        assertThat(envelope.getVersion()).isEqualTo("1.0");
    }

    @Test
    void phiLevel_shouldHaveAllClassifications() {
        assertThat(PhiLevel.values()).containsExactly(
                PhiLevel.NONE, PhiLevel.DE_IDENTIFIED, PhiLevel.LIMITED, PhiLevel.FULL);
    }

    @Test
    void sourceSystem_shouldHaveAllSystems() {
        assertThat(SourceSystem.values()).containsExactly(
                SourceSystem.HEALTHIX, SourceSystem.COREHIVE, SourceSystem.HEDIS, SourceSystem.HDIM);
    }
}

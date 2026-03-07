package com.healthdata.common.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard event envelope for all external integration events.
 * Wraps payloads crossing the HDIM boundary with correlation,
 * tenant context, PHI classification, and audit tracing.
 *
 * @param <T> the payload type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalEventEnvelope<T> {
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;
    private String source;
    private String tenantId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String version = "1.0";

    private String correlationId;
    private T payload;
    private ExternalEventMetadata metadata;

    public static <T> ExternalEventEnvelope<T> of(
            String eventType,
            String source,
            String tenantId,
            T payload,
            ExternalEventMetadata metadata) {
        return ExternalEventEnvelope.<T>builder()
                .eventType(eventType)
                .source(source)
                .tenantId(tenantId)
                .correlationId(UUID.randomUUID().toString())
                .payload(payload)
                .metadata(metadata)
                .build();
    }
}

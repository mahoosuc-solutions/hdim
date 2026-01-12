package com.healthdata.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka ProducerInterceptor that propagates distributed tracing context
 * to Kafka messages.
 *
 * <p>Injects W3C Trace Context and B3 propagation headers into Kafka message headers,
 * enabling end-to-end distributed tracing across async event-driven flows.
 *
 * <h2>Headers Injected</h2>
 * <ul>
 *   <li><b>traceparent</b> - W3C Trace Context format: {@code 00-<trace-id>-<span-id>-<flags>}</li>
 *   <li><b>tracestate</b> - Vendor-specific trace state (optional)</li>
 *   <li><b>b3</b> - B3 single header format (Zipkin-compatible)</li>
 * </ul>
 *
 * <h2>Implementation Details</h2>
 * <ul>
 *   <li>Uses OpenTelemetry propagators configured in shared tracing module</li>
 *   <li>Extracts current trace context from {@link Context#current()}</li>
 *   <li>Headers are stored as UTF-8 byte arrays in Kafka message headers</li>
 *   <li>Must be configured per Kafka producer via {@code interceptor.classes} property</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * Add to application.yml:
 * <pre>
 * spring:
 *   kafka:
 *     producer:
 *       properties:
 *         interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
 * </pre>
 *
 * <h2>Distributed Tracing Example</h2>
 * <pre>
 * CQL Engine Service (trace-id: abc123, publishes evaluation-complete event)
 *   → Kafka Topic: cql-evaluation-events
 *     → Quality Measure Service (span-id: def456, consumes event)  ← Trace propagated via headers
 * </pre>
 *
 * @see KafkaConsumerTraceInterceptor
 * @see io.opentelemetry.context.propagation.TextMapPropagator
 */
@Slf4j
public class KafkaProducerTraceInterceptor implements ProducerInterceptor<String, Object> {

    private OpenTelemetry openTelemetry;

    /**
     * TextMapSetter for injecting trace headers into Kafka Headers.
     */
    private static final TextMapSetter<Headers> SETTER = (carrier, key, value) -> {
        if (carrier != null && key != null && value != null) {
            carrier.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    };

    @Override
    public void configure(Map<String, ?> configs) {
        // OpenTelemetry instance is set via TracingAutoConfiguration
        // This method is required by ProducerInterceptor interface
        log.debug("KafkaProducerTraceInterceptor configured");
    }

    /**
     * Sets the OpenTelemetry instance (called by TracingAutoConfiguration).
     */
    public void setOpenTelemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        log.debug("KafkaProducerTraceInterceptor initialized - distributed tracing enabled for Kafka producers");
    }

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        if (openTelemetry == null) {
            log.warn("OpenTelemetry not initialized, skipping trace propagation for Kafka message");
            return record;
        }

        // Get current trace context
        Context currentContext = Context.current();
        Headers headers = record.headers();

        // Inject trace context headers (W3C traceparent + B3)
        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(currentContext, headers, SETTER);

        log.trace("Injected trace context into Kafka message to topic: {}",
                record.topic());

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // No action needed for trace propagation
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}

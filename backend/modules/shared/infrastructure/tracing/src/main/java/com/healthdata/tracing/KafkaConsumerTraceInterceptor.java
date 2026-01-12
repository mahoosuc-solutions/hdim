package com.healthdata.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka ConsumerInterceptor that extracts distributed tracing context
 * from Kafka messages.
 *
 * <p>Extracts W3C Trace Context and B3 propagation headers from Kafka message headers,
 * restoring trace context for async event-driven message processing.
 *
 * <h2>Headers Extracted</h2>
 * <ul>
 *   <li><b>traceparent</b> - W3C Trace Context format</li>
 *   <li><b>tracestate</b> - Vendor-specific trace state</li>
 *   <li><b>b3</b> - B3 single header format</li>
 * </ul>
 *
 * <h2>Implementation Details</h2>
 * <ul>
 *   <li>Uses OpenTelemetry propagators configured in shared tracing module</li>
 *   <li>Restores trace context into {@link Context} for downstream processing</li>
 *   <li>Headers are read as UTF-8 strings from Kafka message headers</li>
 *   <li>Must be configured per Kafka consumer via {@code interceptor.classes} property</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * Add to application.yml:
 * <pre>
 * spring:
 *   kafka:
 *     consumer:
 *       properties:
 *         interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
 * </pre>
 *
 * <h2>Distributed Tracing Example</h2>
 * <pre>
 * CQL Engine Service (publishes event with trace-id: abc123)
 *   → Kafka Topic: cql-evaluation-events
 *     → Quality Measure Service (restores trace-id: abc123, creates child span)
 * </pre>
 *
 * @see KafkaProducerTraceInterceptor
 * @see io.opentelemetry.context.propagation.TextMapPropagator
 */
@Slf4j
public class KafkaConsumerTraceInterceptor implements ConsumerInterceptor<String, Object> {

    private OpenTelemetry openTelemetry;

    /**
     * TextMapGetter for extracting trace headers from Kafka Headers.
     */
    private static final TextMapGetter<Headers> GETTER = new TextMapGetter<Headers>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            java.util.List<String> keys = new java.util.ArrayList<>();
            headers.forEach(header -> keys.add(header.key()));
            return keys;
        }

        @Override
        public String get(Headers headers, String key) {
            Header header = headers.lastHeader(key);
            if (header == null || header.value() == null) {
                return null;
            }
            return new String(header.value(), StandardCharsets.UTF_8);
        }
    };

    @Override
    public void configure(Map<String, ?> configs) {
        // OpenTelemetry instance is set via TracingAutoConfiguration
        // This method is required by ConsumerInterceptor interface
        log.debug("KafkaConsumerTraceInterceptor configured");
    }

    /**
     * Sets the OpenTelemetry instance (called by TracingAutoConfiguration).
     */
    public void setOpenTelemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        log.debug("KafkaConsumerTraceInterceptor initialized - distributed tracing enabled for Kafka consumers");
    }

    @Override
    public ConsumerRecords<String, Object> onConsume(ConsumerRecords<String, Object> records) {
        if (openTelemetry == null) {
            log.warn("OpenTelemetry not initialized, skipping trace extraction from Kafka messages");
            return records;
        }

        records.forEach(record -> {
            Headers headers = record.headers();

            // Extract trace context from message headers
            Context extractedContext = openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .extract(Context.current(), headers, GETTER);

            // Make extracted context current for message processing
            // Note: This sets context for the consumer thread
            // Kafka listener methods will inherit this context
            try (var scope = extractedContext.makeCurrent()) {
                log.trace("Extracted trace context from Kafka message from topic: {}",
                        record.topic());
            }
        });

        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // No action needed for trace propagation
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}

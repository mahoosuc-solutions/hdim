package com.healthdata.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Auto-configuration for distributed tracing with OpenTelemetry.
 *
 * Provides consistent tracing configuration across all HDIM microservices.
 *
 * Configuration Properties:
 * - tracing.enabled: Enable/disable tracing (default: true)
 * - tracing.url: OTLP exporter endpoint (default: http://jaeger:4318/v1/traces)
 * - spring.application.name: Used as service name in traces
 * - management.tracing.sampling.probability: Sampling rate (default: 1.0 for dev, recommend 0.1 for prod)
 *
 * Integration:
 * - Exports traces to OpenTelemetry Collector via OTLP HTTP
 * - Propagates trace context via W3C Trace Context headers
 * - Integrates with Spring Cloud Gateway for request tracing
 * - Compatible with Jaeger, Zipkin, or any OTLP-compatible backend
 *
 * Usage:
 * Simply add this module as a dependency to enable tracing automatically.
 * Configure the tracing.url property to point to your collector.
 */
@Configuration
@ConditionalOnClass(OpenTelemetry.class)
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TracingAutoConfiguration.class);

    private static final AttributeKey<String> SERVICE_NAME = AttributeKey.stringKey("service.name");
    private static final AttributeKey<String> SERVICE_NAMESPACE = AttributeKey.stringKey("service.namespace");
    private static final AttributeKey<String> DEPLOYMENT_ENVIRONMENT = AttributeKey.stringKey("deployment.environment");

    @Value("${spring.application.name:hdim-service}")
    private String serviceName;

    @Value("${tracing.url:http://jaeger:4318/v1/traces}")
    private String otlpEndpoint;

    @Value("${tracing.batch.max-queue-size:2048}")
    private int maxQueueSize;

    @Value("${tracing.batch.schedule-delay-ms:5000}")
    private long scheduleDelayMs;

    @Value("${tracing.batch.max-export-batch-size:512}")
    private int maxExportBatchSize;

    /**
     * OTLP HTTP Span Exporter for sending traces to collector.
     * Uses HTTP/protobuf format for broad compatibility.
     */
    @Bean
    @ConditionalOnMissingBean(SpanExporter.class)
    public SpanExporter spanExporter() {
        logger.info("Configuring OTLP trace exporter: {}", otlpEndpoint);
        return OtlpHttpSpanExporter.builder()
            .setEndpoint(otlpEndpoint)
            .build();
    }

    /**
     * Service resource with HDIM-specific attributes.
     * Adds service name and version for trace identification.
     */
    @Bean
    @ConditionalOnMissingBean(Resource.class)
    public Resource otelResource() {
        return Resource.getDefault().merge(
            Resource.create(Attributes.of(
                SERVICE_NAME, serviceName,
                SERVICE_NAMESPACE, "hdim",
                DEPLOYMENT_ENVIRONMENT, getEnvironment()
            ))
        );
    }

    /**
     * SDK Tracer Provider with batch processing.
     * Batches spans for efficient export to reduce network overhead.
     */
    @Bean
    @ConditionalOnMissingBean(SdkTracerProvider.class)
    public SdkTracerProvider tracerProvider(SpanExporter spanExporter, Resource resource) {
        logger.info("Initializing distributed tracing for service: {} (batch size: {}, delay: {}ms)",
            serviceName, maxExportBatchSize, scheduleDelayMs);

        return SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                .setMaxQueueSize(maxQueueSize)
                .setScheduleDelay(Duration.ofMillis(scheduleDelayMs))
                .setMaxExportBatchSize(maxExportBatchSize)
                .build())
            .setResource(resource)
            .build();
    }

    /**
     * Context propagators for distributed trace correlation.
     * Supports both W3C Trace Context (standard) and B3 (Zipkin compatibility).
     */
    @Bean
    @ConditionalOnMissingBean(ContextPropagators.class)
    public ContextPropagators contextPropagators() {
        // W3C Trace Context is the standard, B3 for Zipkin/legacy compatibility
        TextMapPropagator compositePropagator = TextMapPropagator.composite(
            W3CTraceContextPropagator.getInstance(),
            B3Propagator.injectingMultiHeaders()
        );
        logger.info("Configured trace propagators: W3C Trace Context, B3 Multi-Header");
        return ContextPropagators.create(compositePropagator);
    }

    /**
     * OpenTelemetry SDK instance.
     * Provides the central OpenTelemetry API for the application.
     */
    @Bean
    @ConditionalOnMissingBean(OpenTelemetry.class)
    public OpenTelemetry openTelemetry(SdkTracerProvider tracerProvider, ContextPropagators contextPropagators) {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(contextPropagators)
            .build();
    }

    /**
     * Tracer bean for creating custom spans in service code.
     * Uses the service name from spring.application.name for proper attribution.
     */
    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName);
    }

    /**
     * RestTemplate customizer to add trace propagation interceptor.
     * Automatically applied to all RestTemplate beans in the application.
     */
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnMissingBean(RestTemplateTraceInterceptor.class)
    public RestTemplateCustomizer restTemplateTracingCustomizer(OpenTelemetry openTelemetry) {
        logger.info("Configuring RestTemplate trace propagation interceptor");
        RestTemplateTraceInterceptor interceptor = new RestTemplateTraceInterceptor(openTelemetry);
        return restTemplate -> restTemplate.getInterceptors().add(interceptor);
    }

    /**
     * Kafka producer trace interceptor bean.
     * Configuration note: Services must add interceptor.classes to Kafka producer config.
     */
    @Bean
    @ConditionalOnClass(name = "org.apache.kafka.clients.producer.ProducerInterceptor")
    @ConditionalOnMissingBean(KafkaProducerTraceInterceptor.class)
    public KafkaProducerTraceInterceptor kafkaProducerTraceInterceptor(OpenTelemetry openTelemetry) {
        logger.info("Configuring Kafka producer trace propagation interceptor");
        KafkaProducerTraceInterceptor interceptor = new KafkaProducerTraceInterceptor();
        interceptor.setOpenTelemetry(openTelemetry);
        return interceptor;
    }

    /**
     * Kafka consumer trace interceptor bean.
     * Configuration note: Services must add interceptor.classes to Kafka consumer config.
     */
    @Bean
    @ConditionalOnClass(name = "org.apache.kafka.clients.consumer.ConsumerInterceptor")
    @ConditionalOnMissingBean(KafkaConsumerTraceInterceptor.class)
    public KafkaConsumerTraceInterceptor kafkaConsumerTraceInterceptor(OpenTelemetry openTelemetry) {
        logger.info("Configuring Kafka consumer trace propagation interceptor");
        KafkaConsumerTraceInterceptor interceptor = new KafkaConsumerTraceInterceptor();
        interceptor.setOpenTelemetry(openTelemetry);
        return interceptor;
    }

    private String getEnvironment() {
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        if (env == null || env.isEmpty()) {
            env = System.getProperty("spring.profiles.active", "development");
        }
        return env.contains("prod") ? "production" :
               env.contains("staging") ? "staging" : "development";
    }
}

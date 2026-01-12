package com.healthdata.authentication.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * Feign RequestInterceptor that propagates distributed tracing context
 * to downstream service calls.
 *
 * <p>Injects W3C Trace Context and B3 propagation headers into Feign HTTP requests,
 * enabling end-to-end distributed tracing across microservices.
 *
 * <h2>Headers Injected</h2>
 * <ul>
 *   <li><b>traceparent</b> - W3C Trace Context format: {@code 00-<trace-id>-<span-id>-<flags>}</li>
 *   <li><b>tracestate</b> - Vendor-specific trace state (optional)</li>
 *   <li><b>b3</b> - B3 single header format (Zipkin-compatible)</li>
 *   <li><b>X-B3-TraceId, X-B3-SpanId, X-B3-Sampled</b> - B3 multi-header format</li>
 * </ul>
 *
 * <h2>Implementation Details</h2>
 * <ul>
 *   <li>Uses OpenTelemetry propagators configured in shared tracing module</li>
 *   <li>Extracts current trace context from {@link Context#current()}</li>
 *   <li>Works alongside {@link AuthHeaderForwardingInterceptor} (both are applied)</li>
 *   <li>Auto-registered when OpenTelemetry is on classpath</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * This interceptor is auto-registered as a Spring component when Feign and OpenTelemetry
 * are on the classpath. No additional configuration needed.
 *
 * <pre>
 * // Trace context is automatically propagated
 * &#64;FeignClient(name = "patient-service")
 * public interface PatientServiceClient {
 *     &#64;GetMapping("/api/v1/patients/{id}")
 *     PatientResponse getPatient(@PathVariable String id);
 * }
 * </pre>
 *
 * <h2>Distributed Tracing Example</h2>
 * <pre>
 * Gateway (trace-id: abc123)
 *   → Quality Measure Service (span-id: def456)
 *     → CQL Engine Service (span-id: ghi789)  ← Trace propagated via this interceptor
 * </pre>
 *
 * @see AuthHeaderForwardingInterceptor
 * @see io.opentelemetry.context.propagation.TextMapPropagator
 */
@Slf4j
@Component
@ConditionalOnClass(name = {"feign.RequestInterceptor", "io.opentelemetry.api.OpenTelemetry"})
public class FeignTraceInterceptor implements RequestInterceptor {

    private final OpenTelemetry openTelemetry;

    /**
     * TextMapSetter for injecting trace headers into Feign RequestTemplate.
     */
    private static final TextMapSetter<RequestTemplate> SETTER = (carrier, key, value) -> {
        if (carrier != null && key != null && value != null) {
            carrier.header(key, value);
        }
    };

    public FeignTraceInterceptor(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        log.debug("FeignTraceInterceptor initialized - distributed tracing enabled for Feign clients");
    }

    @Override
    public void apply(RequestTemplate template) {
        // Get current trace context
        Context currentContext = Context.current();

        // Inject trace context headers (W3C traceparent + B3)
        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(currentContext, template, SETTER);

        log.trace("Injected trace context into Feign request to: {}",
                template.feignTarget().name());
    }
}

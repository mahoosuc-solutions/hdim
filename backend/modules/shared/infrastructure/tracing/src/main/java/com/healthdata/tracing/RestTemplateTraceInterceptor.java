package com.healthdata.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate interceptor that propagates distributed tracing context
 * to downstream HTTP calls.
 *
 * <p>Injects W3C Trace Context and B3 propagation headers into RestTemplate HTTP requests,
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
 *   <li>Applied to all RestTemplate instances via {@link org.springframework.boot.web.client.RestTemplateCustomizer}</li>
 *   <li>Auto-registered when OpenTelemetry is on classpath</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * This interceptor is auto-registered via RestTemplateCustomizer in {@link TracingAutoConfiguration}.
 * All RestTemplate beans will automatically have trace propagation enabled.
 *
 * <pre>
 * // Trace context is automatically propagated
 * &#64;Autowired
 * private RestTemplate restTemplate;
 *
 * ResponseEntity&lt;PatientResponse&gt; response = restTemplate.getForEntity(
 *     "http://patient-service/api/v1/patients/{id}",
 *     PatientResponse.class,
 *     patientId
 * );
 * </pre>
 *
 * <h2>Distributed Tracing Example</h2>
 * <pre>
 * Gateway (trace-id: abc123, span-id: xyz)
 *   → Patient Service (span-id: def456)  ← Trace propagated via this interceptor
 *     → FHIR Service (span-id: ghi789)
 * </pre>
 *
 * @see TracingAutoConfiguration
 * @see io.opentelemetry.context.propagation.TextMapPropagator
 */
@Slf4j
public class RestTemplateTraceInterceptor implements ClientHttpRequestInterceptor {

    private final OpenTelemetry openTelemetry;

    /**
     * TextMapSetter for injecting trace headers into HttpHeaders.
     */
    private static final TextMapSetter<HttpHeaders> SETTER = (carrier, key, value) -> {
        if (carrier != null && key != null && value != null) {
            carrier.set(key, value);
        }
    };

    public RestTemplateTraceInterceptor(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        log.debug("RestTemplateTraceInterceptor initialized - distributed tracing enabled for RestTemplate");
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // Get current trace context
        Context currentContext = Context.current();
        HttpHeaders headers = request.getHeaders();

        // Inject trace context headers (W3C traceparent + B3)
        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(currentContext, headers, SETTER);

        log.trace("Injected trace context into RestTemplate request to: {}",
                request.getURI());

        // Continue with request execution
        return execution.execute(request, body);
    }
}

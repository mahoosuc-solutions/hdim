package com.healthdata.corehiveadapter.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class AdapterSpanHelper {

    private final Tracer tracer;

    public <T> T traced(String spanName, Supplier<T> operation, String... attributes) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        for (int i = 0; i < attributes.length - 1; i += 2) {
            span.setAttribute(attributes[i], attributes[i + 1]);
        }
        try (var scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    public void tracedRun(String spanName, Runnable operation, String... attributes) {
        traced(spanName, () -> { operation.run(); return null; }, attributes);
    }
}

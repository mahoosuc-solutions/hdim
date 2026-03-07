package com.healthdata.hedisadapter.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AdapterSpanHelperTest {

    @Mock private Tracer tracer;
    @Mock private SpanBuilder spanBuilder;
    @Mock private Span span;
    @Mock private Scope scope;

    private AdapterSpanHelper helper;

    @BeforeEach
    void setUp() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        lenient().when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        helper = new AdapterSpanHelper(tracer);
    }

    @Test
    void traced_successfulOperation_setsOkStatus() {
        String result = helper.traced("test-span", () -> "hello", "adapter", "hedis");
        assertThat(result).isEqualTo("hello");
        verify(tracer).spanBuilder("test-span");
        verify(span).setAttribute("adapter", "hedis");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void traced_failedOperation_setsErrorStatusAndRecordsException() {
        RuntimeException ex = new RuntimeException("boom");
        assertThatThrownBy(() -> helper.traced("fail-span", () -> { throw ex; })).isSameAs(ex);
        verify(span).setStatus(StatusCode.ERROR, "boom");
        verify(span).recordException(ex);
        verify(span).end();
    }

    @Test
    void tracedRun_successfulRunnable_setsOkStatus() {
        Runnable op = mock(Runnable.class);
        helper.tracedRun("run-span", op, "phi.level", "LIMITED");
        verify(op).run();
        verify(span).setAttribute("phi.level", "LIMITED");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }
}

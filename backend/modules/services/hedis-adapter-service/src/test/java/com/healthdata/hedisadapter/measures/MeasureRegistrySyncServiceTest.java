package com.healthdata.hedisadapter.measures;

import com.healthdata.hedisadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("MeasureRegistrySyncService")
class MeasureRegistrySyncServiceTest {

    @Mock private RestTemplate hedisRestTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private Tracer tracer;
    @Mock private SpanBuilder spanBuilder;
    @Mock private Span span;
    @Mock private Scope scope;

    private MeasureRegistrySyncService service;

    @BeforeEach
    void setUp() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(tracer);
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        service = new MeasureRegistrySyncService(hedisRestTemplate, kafkaTemplate, registry, spanHelper);
    }

    @Test
    @DisplayName("should fetch measures and publish to Kafka")
    void syncMeasureRegistry_shouldFetchAndPublish() {
        List<Map<String, Object>> measures = List.of(
                Map.of("measureId", "BCS", "type", "HEDIS"),
                Map.of("measureId", "CBP", "type", "HEDIS"));

        when(hedisRestTemplate.getForObject(eq("/api/measures/sync"), eq(List.class)))
                .thenReturn(measures);

        service.syncMeasureRegistry();

        var envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.hedis.measures"), eq("system"), envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().toString()).contains("external.hedis.measures.synced");
    }

    @Test
    @DisplayName("should skip when no measures returned")
    void syncMeasureRegistry_shouldSkipWhenEmpty() {
        when(hedisRestTemplate.getForObject(eq("/api/measures/sync"), eq(List.class)))
                .thenReturn(List.of());

        service.syncMeasureRegistry();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("should skip when null returned")
    void syncMeasureRegistry_shouldSkipWhenNull() {
        when(hedisRestTemplate.getForObject(eq("/api/measures/sync"), eq(List.class)))
                .thenReturn(null);

        service.syncMeasureRegistry();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("should catch exception from external service without propagating")
    void syncMeasureRegistry_shouldCatchException() {
        when(hedisRestTemplate.getForObject(anyString(), any()))
                .thenThrow(new ResourceAccessException("hedis unavailable"));

        // Should not throw
        service.syncMeasureRegistry();

        verifyNoInteractions(kafkaTemplate);
    }
}

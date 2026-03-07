package com.healthdata.hedisadapter.crm;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CrmSyncService")
class CrmSyncServiceTest {

    @Mock private RestTemplate hedisRestTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private Tracer tracer;
    @Mock private SpanBuilder spanBuilder;
    @Mock private Span span;
    @Mock private Scope scope;

    private CrmSyncService service;

    @BeforeEach
    void setUp() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(tracer);
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        service = new CrmSyncService(hedisRestTemplate, kafkaTemplate, registry, spanHelper);
    }

    @Test
    @DisplayName("should push deal update to hedis CRM endpoint")
    void pushDealUpdate_shouldPostToHedis() {
        Map<String, Object> dealData = Map.of("dealId", "deal-001", "stage", "Closed Won");

        when(hedisRestTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.pushDealUpdate(dealData);

        verify(hedisRestTemplate).postForEntity(eq("/api/crm/deals/sync"), eq(dealData), eq(Void.class));
    }

    @Test
    @DisplayName("should propagate exception when hedis CRM is unreachable")
    void pushDealUpdate_shouldPropagateException() {
        Map<String, Object> dealData = Map.of("dealId", "deal-002");

        when(hedisRestTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> service.pushDealUpdate(dealData))
                .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    @DisplayName("should publish webhook payload to Kafka CRM topic")
    void onCrmWebhook_shouldPublishToKafka() {
        Map<String, Object> payload = Map.of("event", "deal.updated", "dealId", "deal-003");

        service.onCrmWebhook(payload);

        verify(kafkaTemplate).send(eq("external.hedis.crm"), eq("crm-sync"), eq(payload));
    }
}

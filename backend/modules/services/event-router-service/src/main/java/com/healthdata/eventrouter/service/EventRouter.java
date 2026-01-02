package com.healthdata.eventrouter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.dto.RoutingResult;
import com.healthdata.eventrouter.entity.RoutingRuleEntity;
import com.healthdata.eventrouter.persistence.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventRouter {

    private final RoutingRuleRepository ruleRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventFilterService filterService;
    private final EventTransformationService transformationService;
    private final RouteMetricsService metricsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoutingResult route(EventMessage event) {
        Instant startTime = Instant.now();

        try {
            // Find matching routing rules
            List<RoutingRuleEntity> rules = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(
                event.getTenantId(),
                event.getSourceTopic()
            );

            if (rules.isEmpty()) {
                log.debug("No matching routing rule for tenant: {}, topic: {}",
                    event.getTenantId(), event.getSourceTopic());
                metricsService.recordUnroutedEvent(event.getSourceTopic());
                return RoutingResult.failure("No matching routing rule found");
            }

            // Process first matching rule (can be extended for multiple rules)
            for (RoutingRuleEntity rule : rules) {
                // Check filter
                if (rule.getFilterExpression() != null && !rule.getFilterExpression().isEmpty()) {
                    if (!filterService.matches(event, rule.getFilterExpression())) {
                        log.debug("Event filtered out by rule: {}", rule.getRuleName());
                        metricsService.recordFilteredEvent(event.getSourceTopic());
                        return RoutingResult.failure("Event filtered out by routing rule");
                    }
                }

                // Apply transformation
                EventMessage transformedEvent = event;
                if (rule.getTransformationScript() != null && !rule.getTransformationScript().isEmpty()) {
                    transformedEvent = transformationService.transform(event, rule.getTransformationScript());
                }

                // Route to target topic
                String targetTopic = rule.getTargetTopic();
                String payload = objectMapper.writeValueAsString(transformedEvent);
                kafkaTemplate.send(targetTopic, payload);

                // Record metrics
                Duration latency = Duration.between(startTime, Instant.now());
                metricsService.recordRoutedEvent(targetTopic, rule.getPriority());
                metricsService.recordRoutingLatency(targetTopic, latency);

                log.debug("Event routed successfully - Rule: {}, Target: {}, Latency: {}ms",
                    rule.getRuleName(), targetTopic, latency.toMillis());

                return RoutingResult.success(targetTopic, rule.getRuleName());
            }

            return RoutingResult.failure("No rules matched filter criteria");

        } catch (Exception e) {
            log.error("Error routing event", e);
            metricsService.recordUnroutedEvent(event.getSourceTopic());
            return RoutingResult.failure("Routing error: " + e.getMessage());
        }
    }
}

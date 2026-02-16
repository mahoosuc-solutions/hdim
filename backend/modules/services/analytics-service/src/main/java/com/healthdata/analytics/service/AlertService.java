package com.healthdata.analytics.service;

import com.healthdata.analytics.dto.AlertDto;
import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.persistence.AlertRuleEntity;
import com.healthdata.analytics.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRuleRepository alertRepository;
    private final KpiService kpiService;

    @Transactional(readOnly = true)
    public List<AlertDto> getAlertRules(String tenantId) {
        return alertRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AlertDto> getAlertRulesPaginated(String tenantId, Pageable pageable) {
        return alertRepository.findByTenantId(tenantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<AlertDto> getAlertRule(UUID id, String tenantId) {
        return alertRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<AlertDto> getActiveAlertRules(String tenantId) {
        return alertRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AlertDto createAlertRule(AlertDto dto, String tenantId, String userId) {
        AlertRuleEntity entity = AlertRuleEntity.builder()
                .tenantId(tenantId)
                .name(dto.getName())
                .description(dto.getDescription())
                .metricType(dto.getMetricType())
                .metricName(dto.getMetricName())
                .conditionOperator(dto.getConditionOperator())
                .thresholdValue(dto.getThresholdValue())
                .secondaryThreshold(dto.getSecondaryThreshold())
                .severity(dto.getSeverity() != null ? dto.getSeverity() : "MEDIUM")
                .notificationChannels(dto.getNotificationChannels())
                .filters(dto.getFilters())
                .cooldownMinutes(dto.getCooldownMinutes() != null ? dto.getCooldownMinutes() : 60)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdBy(userId)
                .build();

        entity = alertRepository.save(entity);
        log.info("Created alert rule {} for tenant {}", entity.getId(), tenantId);
        return toDto(entity);
    }

    @Transactional
    public Optional<AlertDto> updateAlertRule(UUID id, AlertDto dto, String tenantId) {
        return alertRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> {
                    entity.setName(dto.getName());
                    entity.setDescription(dto.getDescription());
                    entity.setMetricType(dto.getMetricType());
                    entity.setMetricName(dto.getMetricName());
                    entity.setConditionOperator(dto.getConditionOperator());
                    entity.setThresholdValue(dto.getThresholdValue());
                    entity.setSecondaryThreshold(dto.getSecondaryThreshold());
                    entity.setSeverity(dto.getSeverity());
                    entity.setNotificationChannels(dto.getNotificationChannels());
                    entity.setFilters(dto.getFilters());
                    entity.setCooldownMinutes(dto.getCooldownMinutes());
                    entity.setIsActive(dto.getIsActive());

                    entity = alertRepository.save(entity);
                    log.info("Updated alert rule {}", id);
                    return toDto(entity);
                });
    }

    @Transactional
    public boolean deleteAlertRule(UUID id, String tenantId) {
        if (alertRepository.existsByIdAndTenantId(id, tenantId)) {
            alertRepository.deleteById(id);
            log.info("Deleted alert rule {}", id);
            return true;
        }
        return false;
    }

    @Transactional
    public List<AlertDto> checkAlerts(String tenantId) {
        List<AlertRuleEntity> activeRules = alertRepository.findByTenantIdAndIsActiveTrue(tenantId);
        List<AlertDto> triggeredAlerts = new ArrayList<>();

        Map<String, Object> allKpis = kpiService.getAllKpis(tenantId);

        for (AlertRuleEntity rule : activeRules) {
            if (!rule.canTrigger()) {
                continue;
            }

            BigDecimal currentValue = getCurrentMetricValue(allKpis, rule.getMetricType(), rule.getMetricName());
            if (currentValue != null && evaluateCondition(currentValue, rule)) {
                rule.setLastTriggeredAt(LocalDateTime.now());
                rule.setTriggerCount(rule.getTriggerCount() + 1);
                alertRepository.save(rule);

                log.info("Alert triggered: {} (value: {}, threshold: {})",
                        rule.getName(), currentValue, rule.getThresholdValue());
                triggeredAlerts.add(toDto(rule));
            }
        }

        return triggeredAlerts;
    }

    @Transactional
    public List<PredictiveAlertDto> checkPredictiveAlerts(String tenantId, int daysWindow) {
        List<AlertRuleEntity> activeRules = alertRepository.findByTenantIdAndIsActiveTrue(tenantId);
        List<PredictiveAlertDto> triggered = new ArrayList<>();

        for (AlertRuleEntity rule : activeRules) {
            if (!rule.canTrigger()) {
                continue;
            }

            List<KpiSummaryDto> trends = kpiService.getTrends(tenantId, rule.getMetricType(), daysWindow);
            KpiSummaryDto trend = findMatchingMetric(trends, rule.getMetricName());
            if (trend == null || trend.getCurrentValue() == null) {
                continue;
            }

            BigDecimal changePercent = trend.getChangePercent() == null ? BigDecimal.ZERO : trend.getChangePercent();
            BigDecimal predictedValue = applyPercentChange(trend.getCurrentValue(), changePercent);
            boolean shouldTrigger = shouldTriggerPredictive(rule, trend, changePercent, predictedValue);

            if (!shouldTrigger) {
                continue;
            }

            rule.setLastTriggeredAt(LocalDateTime.now());
            rule.setTriggerCount(rule.getTriggerCount() + 1);
            alertRepository.save(rule);

            triggered.add(new PredictiveAlertDto(
                rule.getId(),
                rule.getName(),
                rule.getMetricType(),
                trend.getMetricName(),
                trend.getCurrentValue(),
                predictedValue,
                changePercent,
                rule.getSeverity(),
                "Predictive threshold breach forecast"
            ));
        }

        return triggered;
    }

    @Transactional(readOnly = true)
    public List<AlertDto> getRecentlyTriggeredAlerts(String tenantId, int limit) {
        return alertRepository.findRecentlyTriggeredRules(tenantId, PageRequest.of(0, limit))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BigDecimal getCurrentMetricValue(Map<String, Object> allKpis, String metricType, String metricName) {
        String kpiKey = switch (metricType) {
            case "QUALITY_SCORE", "STAR_RATING" -> "quality";
            case "RAF_SCORE", "HCC_COUNT" -> "hcc";
            case "CARE_GAP_RATE" -> "careGaps";
            default -> null;
        };

        if (kpiKey == null || !allKpis.containsKey(kpiKey)) {
            return null;
        }

        Object kpis = allKpis.get(kpiKey);
        if (kpis instanceof List<?> kpiList) {
            for (Object kpi : kpiList) {
                if (kpi instanceof KpiSummaryDto kpiDto) {
                    if (metricName == null || metricName.equals(kpiDto.getMetricName())) {
                        return kpiDto.getCurrentValue();
                    }
                }
            }
        }

        return null;
    }

    private KpiSummaryDto findMatchingMetric(List<KpiSummaryDto> trends, String metricName) {
        if (trends == null || trends.isEmpty()) {
            return null;
        }
        if (metricName == null) {
            return trends.get(0);
        }
        return trends.stream()
            .filter(kpi -> metricName.equals(kpi.getMetricName()))
            .findFirst()
            .orElse(null);
    }

    private BigDecimal applyPercentChange(BigDecimal current, BigDecimal changePercent) {
        BigDecimal multiplier = BigDecimal.ONE.add(changePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return current.multiply(multiplier);
    }

    private boolean shouldTriggerPredictive(
        AlertRuleEntity rule,
        KpiSummaryDto trend,
        BigDecimal changePercent,
        BigDecimal predictedValue
    ) {
        if ("CHANGE_PCT".equals(rule.getConditionOperator())) {
            return changePercent.abs().compareTo(rule.getThresholdValue()) >= 0;
        }

        String direction = trend.getTrend();
        boolean directionalMatch = switch (rule.getConditionOperator()) {
            case "GT", "GTE" -> "UP".equals(direction) || "STABLE".equals(direction);
            case "LT", "LTE" -> "DOWN".equals(direction) || "STABLE".equals(direction);
            default -> true;
        };

        return directionalMatch && evaluateCondition(predictedValue, rule);
    }

    private boolean evaluateCondition(BigDecimal value, AlertRuleEntity rule) {
        BigDecimal threshold = rule.getThresholdValue();

        return switch (rule.getConditionOperator()) {
            case "GT" -> value.compareTo(threshold) > 0;
            case "GTE" -> value.compareTo(threshold) >= 0;
            case "LT" -> value.compareTo(threshold) < 0;
            case "LTE" -> value.compareTo(threshold) <= 0;
            case "EQ" -> value.compareTo(threshold) == 0;
            case "CHANGE_PCT" -> false;
            default -> false;
        };
    }

    private AlertDto toDto(AlertRuleEntity entity) {
        return AlertDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .metricType(entity.getMetricType())
                .metricName(entity.getMetricName())
                .conditionOperator(entity.getConditionOperator())
                .thresholdValue(entity.getThresholdValue())
                .secondaryThreshold(entity.getSecondaryThreshold())
                .severity(entity.getSeverity())
                .notificationChannels(entity.getNotificationChannels())
                .filters(entity.getFilters())
                .cooldownMinutes(entity.getCooldownMinutes())
                .isActive(entity.getIsActive())
                .lastTriggeredAt(entity.getLastTriggeredAt())
                .triggerCount(entity.getTriggerCount())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public record PredictiveAlertDto(
        UUID ruleId,
        String ruleName,
        String metricType,
        String metricName,
        BigDecimal currentValue,
        BigDecimal predictedValue,
        BigDecimal predictedChangePercent,
        String severity,
        String reason
    ) {}
}

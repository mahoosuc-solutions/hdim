package com.healthdata.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PredictiveAlertScheduler {

    private final AlertService alertService;

    @Value("${hdim.analytics.alerting.scheduled-tenants:}")
    private String scheduledTenants;

    @Scheduled(fixedDelayString = "${hdim.analytics.alerting.evaluation-interval-ms:300000}")
    public void evaluateScheduledPredictiveAlerts() {
        List<String> tenants = Arrays.stream(scheduledTenants.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();

        if (tenants.isEmpty()) {
            return;
        }

        for (String tenantId : tenants) {
            try {
                int standardTriggered = alertService.checkAlerts(tenantId).size();
                int predictiveTriggered = alertService.checkPredictiveAlerts(tenantId, 14).size();
                log.info("Scheduled alert evaluation complete for tenant {} (standard={}, predictive={})",
                    tenantId, standardTriggered, predictiveTriggered);
            } catch (Exception ex) {
                log.warn("Scheduled alert evaluation failed for tenant {}: {}", tenantId, ex.getMessage());
            }
        }
    }
}

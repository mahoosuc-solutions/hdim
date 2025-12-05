package com.healthdata.fhir.admin.model;

import java.time.Instant;
import java.util.List;

public record ServiceCatalog(
        Instant updatedAt,
        List<ServiceDefinition> services
) {

    public record ServiceDefinition(
            String serviceId,
            String displayName,
            String ownerTeam,
            ServiceContact ownerContact,
            String description,
            ServiceLifecycleStatus lifecycleStatus,
            ServiceLevelObjective serviceLevel,
            List<String> regions,
            List<String> complianceTags,
            String runtime,
            double uptimeTargetPercentage
    ) {
    }

    public record ServiceContact(
            String name,
            String email,
            String pagerDuty
    ) {
    }

    public record ServiceLevelObjective(
            double availabilityPercentage,
            int responseTimeTargetMs,
            Integer errorBudgetMinutes
    ) {
    }

    public enum ServiceLifecycleStatus {
        ACTIVE,
        MAINTENANCE,
        INVESTIGATE
    }
}

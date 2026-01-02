package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.AnalyticsMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsMetricRepository extends JpaRepository<AnalyticsMetricEntity, UUID> {
}

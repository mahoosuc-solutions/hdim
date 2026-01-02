package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.AnalyticsReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsReportRepository extends JpaRepository<AnalyticsReportEntity, UUID> {
}

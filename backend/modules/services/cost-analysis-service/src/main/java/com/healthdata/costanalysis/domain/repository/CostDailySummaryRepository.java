package com.healthdata.costanalysis.domain.repository;

import com.healthdata.costanalysis.domain.model.CostDailySummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CostDailySummaryRepository extends JpaRepository<CostDailySummaryEntity, UUID> {

    List<CostDailySummaryEntity> findByTenantIdAndSummaryDateBetween(String tenantId, LocalDate startDate, LocalDate endDate);
}

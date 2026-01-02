package com.healthdata.predictive.controller;

import com.healthdata.predictive.model.*;
import com.healthdata.predictive.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Predictive Analytics
 *
 * Endpoints:
 * - POST /api/v1/analytics/readmission-risk/{patientId} - Get readmission risk
 * - POST /api/v1/analytics/cost-prediction/{patientId} - Predict patient costs
 * - POST /api/v1/analytics/disease-progression/{patientId} - Disease trajectory
 * - GET /api/v1/analytics/population/risk-stratification - Population risk tiers
 * - GET /api/v1/analytics/population/high-risk - High-risk patient list
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class PredictiveAnalyticsController {

    private final ReadmissionRiskPredictor readmissionPredictor;
    private final CostPredictor costPredictor;
    private final DiseaseProgressionPredictor progressionPredictor;
    private final PopulationRiskStratifier riskStratifier;

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/readmission-risk/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReadmissionRiskScore> predictReadmissionRisk(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String patientId,
            @RequestParam(defaultValue = "30") int predictionPeriod,
            @RequestBody Map<String, Object> patientData
    ) {
        log.info("POST /api/v1/analytics/readmission-risk/{} - tenant: {}, period: {} days",
            patientId, tenantId, predictionPeriod);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ReadmissionRiskScore score;
        if (predictionPeriod == 90) {
            score = readmissionPredictor.predict90DayRisk(tenantId, patientId, patientData);
        } else {
            score = readmissionPredictor.predict30DayRisk(tenantId, patientId, patientData);
        }

        return ResponseEntity.ok(score);
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/cost-prediction/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CostBreakdown> predictCost(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String patientId,
            @RequestParam(defaultValue = "12") int predictionPeriodMonths,
            @RequestBody Map<String, Object> patientData
    ) {
        log.info("POST /api/v1/analytics/cost-prediction/{} - tenant: {}, period: {} months",
            patientId, tenantId, predictionPeriodMonths);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (predictionPeriodMonths <= 0) {
            return ResponseEntity.badRequest().build();
        }

        CostBreakdown cost = costPredictor.predictCosts(tenantId, patientId, patientData, predictionPeriodMonths);
        return ResponseEntity.ok(cost);
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/disease-progression/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProgressionRisk> predictDiseaseProgression(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String patientId,
            @RequestParam String condition,
            @RequestBody Map<String, Object> patientData
    ) {
        log.info("POST /api/v1/analytics/disease-progression/{} - tenant: {}, condition: {}",
            patientId, tenantId, condition);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (condition == null || condition.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ProgressionRisk risk = progressionPredictor.predictProgression(tenantId, patientId, patientData, condition);
        return ResponseEntity.ok(risk);
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/population/risk-stratification", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RiskCohort>> getPopulationRiskStratification(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String patientIds
    ) {
        log.info("GET /api/v1/analytics/population/risk-stratification - tenant: {}", tenantId);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<String> patientIdList = Arrays.asList(patientIds.split(","));
        Map<String, Map<String, Object>> patientDataMap = new HashMap<>();

        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation(tenantId, patientIdList, patientDataMap);
        return ResponseEntity.ok(cohorts);
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping(value = "/population/high-risk", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getHighRiskPatients(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String patientIds
    ) {
        log.info("GET /api/v1/analytics/population/high-risk - tenant: {}", tenantId);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<String> patientIdList = Arrays.asList(patientIds.split(","));
        Map<String, Map<String, Object>> patientDataMap = new HashMap<>();

        List<String> highRiskPatients = riskStratifier.getHighRiskPatients(tenantId, patientIdList, patientDataMap);
        return ResponseEntity.ok(highRiskPatients);
    }
}

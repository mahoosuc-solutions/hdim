package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.client.CareGapServiceClient;
import com.healthdata.quality.client.PatientServiceClient;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.persistence.SavedReportEntity;
import com.healthdata.quality.persistence.SavedReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Quality Report Service
 * Generates quality measure reports and analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QualityReportService {

    private final QualityMeasureResultRepository repository;
    private final SavedReportRepository savedReportRepository;
    private final PatientServiceClient patientServiceClient;
    private final CareGapServiceClient careGapServiceClient;
    private final MeasureCalculationService calculationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get comprehensive quality report for a patient
     */
    @Cacheable(value = "qualityReport", key = "#tenantId + ':' + #patientId")
    public QualityReport getPatientQualityReport(String tenantId, UUID patientId) {
        log.info("Generating quality report for patient: {}", patientId);

        List<QualityMeasureResultEntity> results = calculationService.getPatientMeasureResults(tenantId, patientId);
        MeasureCalculationService.QualityScore score = calculationService.getQualityScore(tenantId, patientId);

        // Get care gap summary
        String careGapSummaryJson = null;
        try {
            careGapSummaryJson = careGapServiceClient.getCareGapSummary(tenantId, patientId);
        } catch (Exception e) {
            log.warn("Could not fetch care gap summary: {}", e.getMessage());
        }

        // Group results by category
        Map<String, Long> resultsByCategory = results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getMeasureCategory() != null ? r.getMeasureCategory() : "Unknown",
                        Collectors.counting()
                ));

        return new QualityReport(
                patientId,
                results.size(),
                score.compliantMeasures(),
                score.scorePercentage(),
                resultsByCategory,
                careGapSummaryJson
        );
    }

    /**
     * Get population-level quality report
     */
    @Cacheable(value = "populationQualityReport", key = "#tenantId + ':' + #year")
    public PopulationQualityReport getPopulationQualityReport(String tenantId, int year) {
        log.info("Generating population quality report for year: {}", year);

        List<QualityMeasureResultEntity> results = repository.findByMeasureYear(tenantId, year);

        long totalMeasures = results.size();
        long compliantMeasures = results.stream()
                .filter(QualityMeasureResultEntity::getNumeratorCompliant)
                .count();

        double overallScore = totalMeasures > 0 ? (double) compliantMeasures / totalMeasures * 100 : 0.0;

        // Unique patients
        long uniquePatients = results.stream()
                .map(QualityMeasureResultEntity::getPatientId)
                .distinct()
                .count();

        // Measures by category
        Map<String, Long> measuresByCategory = results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getMeasureCategory() != null ? r.getMeasureCategory() : "Unknown",
                        Collectors.counting()
                ));

        return new PopulationQualityReport(
                year,
                uniquePatients,
                totalMeasures,
                compliantMeasures,
                overallScore,
                measuresByCategory
        );
    }

    public record QualityReport(
            UUID patientId,
            long totalMeasures,
            long compliantMeasures,
            double qualityScore,
            Map<String, Long> measuresByCategory,
            String careGapSummary
    ) {}

    public record PopulationQualityReport(
            int year,
            long uniquePatients,
            long totalMeasures,
            long compliantMeasures,
            double overallScore,
            Map<String, Long> measuresByCategory
    ) {}

    // ===== NEW: Saved Report Methods =====

    /**
     * Save patient quality report to database
     */
    @Transactional
    public SavedReportEntity savePatientReport(String tenantId, UUID patientId, String reportName, String createdBy) {
        log.info("Saving patient quality report: {} for patient: {}", reportName, patientId);

        // Generate the report
        QualityReport report = getPatientQualityReport(tenantId, patientId);

        // Serialize report to JSON
        String reportJson;
        try {
            reportJson = objectMapper.writeValueAsString(report);
        } catch (Exception e) {
            log.error("Error serializing report to JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize report", e);
        }

        // Create saved report entity
        SavedReportEntity entity = SavedReportEntity.builder()
                .tenantId(tenantId)
                .reportType("PATIENT")
                .reportName(reportName)
                .patientId(patientId)
                .reportData(reportJson)
                .createdBy(createdBy)
                .status("COMPLETED")
                .build();

        return savedReportRepository.save(entity);
    }

    /**
     * Save population quality report to database
     */
    @Transactional
    public SavedReportEntity savePopulationReport(String tenantId, int year, String reportName, String createdBy) {
        log.info("Saving population quality report: {} for year: {}", reportName, year);

        // Generate the report
        PopulationQualityReport report = getPopulationQualityReport(tenantId, year);

        // Serialize report to JSON
        String reportJson;
        try {
            reportJson = objectMapper.writeValueAsString(report);
        } catch (Exception e) {
            log.error("Error serializing report to JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize report", e);
        }

        // Create saved report entity
        SavedReportEntity entity = SavedReportEntity.builder()
                .tenantId(tenantId)
                .reportType("POPULATION")
                .reportName(reportName)
                .year(year)
                .reportData(reportJson)
                .createdBy(createdBy)
                .status("COMPLETED")
                .build();

        return savedReportRepository.save(entity);
    }

    /**
     * Get all saved reports for a tenant
     */
    public List<SavedReportEntity> getSavedReports(String tenantId) {
        log.info("Retrieving all saved reports for tenant: {}", tenantId);
        return savedReportRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    /**
     * Get saved reports by type
     */
    public List<SavedReportEntity> getSavedReportsByType(String tenantId, String reportType) {
        log.info("Retrieving saved reports for tenant: {} and type: {}", tenantId, reportType);
        return savedReportRepository.findByTenantIdAndReportTypeOrderByCreatedAtDesc(tenantId, reportType);
    }

    /**
     * Get a saved report by ID (with tenant isolation)
     */
    public SavedReportEntity getSavedReport(String tenantId, UUID reportId) {
        log.info("Retrieving saved report: {} for tenant: {}", reportId, tenantId);
        return savedReportRepository.findByTenantIdAndId(tenantId, reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
    }

    /**
     * Delete a saved report (with tenant isolation)
     */
    @Transactional
    public void deleteSavedReport(String tenantId, UUID reportId) {
        log.info("Deleting saved report: {} for tenant: {}", reportId, tenantId);
        SavedReportEntity report = getSavedReport(tenantId, reportId);
        savedReportRepository.delete(report);
    }

    /**
     * Count saved reports for a tenant
     */
    public long countSavedReports(String tenantId) {
        return savedReportRepository.countByTenantId(tenantId);
    }
}

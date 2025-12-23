package com.healthdata.caregap.service;

import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * Care Gap Report Service
 *
 * Provides reporting and analytics for care gaps including:
 * - Population-level gap reports
 * - Measure-specific gap analysis
 * - Gap closure trending
 * - Priority distribution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareGapReportService {

    private final CareGapRepository careGapRepository;

    /**
     * Get care gap summary for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Care gap summary
     */
    @Cacheable(value = "careGapSummary", key = "#tenantId + ':' + #patientId")
    public CareGapSummary getCareGapSummary(String tenantId, UUID patientId) {
        log.info("Generating care gap summary for patient: {}", patientId);

        List<CareGapEntity> allGaps = careGapRepository.findByTenantIdAndPatientId(tenantId, patientId);
        List<CareGapEntity> openGaps = careGapRepository.findOpenGapsByPatient(tenantId, patientId);
        List<CareGapEntity> closedGaps = careGapRepository.findClosedGapsByPatient(tenantId, patientId);
        List<CareGapEntity> highPriorityGaps = careGapRepository.findHighPriorityOpenGaps(tenantId, patientId);

        long overdueCount = careGapRepository.countOverdueGaps(tenantId, patientId, LocalDate.now());

        // Calculate closure rate
        double closureRate = allGaps.isEmpty() ? 0.0 :
                (double) closedGaps.size() / allGaps.size() * 100;

        return new CareGapSummary(
                allGaps.size(),
                openGaps.size(),
                closedGaps.size(),
                highPriorityGaps.size(),
                (int) overdueCount,
                closureRate,
                extractMeasureCategories(openGaps),
                extractTopMeasures(openGaps, 5)
        );
    }

    /**
     * Get care gaps grouped by measure category
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Map of measure category -> gap count
     */
    public Map<String, Long> getGapsByMeasureCategory(String tenantId, UUID patientId) {
        List<CareGapEntity> openGaps = careGapRepository.findOpenGapsByPatient(
                tenantId, patientId);

        return openGaps.stream()
                .collect(Collectors.groupingBy(
                        gap -> gap.getGapCategory() != null ? gap.getGapCategory() : "Unknown",
                        Collectors.counting()
                ));
    }

    /**
     * Get care gaps grouped by priority
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Map of priority -> gap count
     */
    public Map<String, Long> getGapsByPriority(String tenantId, UUID patientId) {
        List<CareGapEntity> openGaps = careGapRepository.findOpenGapsByPatient(
                tenantId, patientId);

        return openGaps.stream()
                .collect(Collectors.groupingBy(
                        gap -> gap.getPriority() != null ? gap.getPriority() : "Unknown",
                        Collectors.counting()
                ));
    }

    /**
     * Get overdue care gaps for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of overdue care gaps
     */
    public List<CareGapEntity> getOverdueGaps(String tenantId, UUID patientId) {
        List<CareGapEntity> openGaps = careGapRepository.findOpenGapsByPatient(
                tenantId, patientId);

        LocalDate today = LocalDate.now();
        return openGaps.stream()
                .filter(gap -> gap.getDueDate() != null && gap.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(CareGapEntity::getDueDate))
                .collect(Collectors.toList());
    }

    /**
     * Get gaps due within next N days
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param days Number of days to look ahead
     * @return List of upcoming care gaps
     */
    public List<CareGapEntity> getUpcomingGaps(String tenantId, UUID patientId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        return careGapRepository.findGapsDueInRange(
                tenantId, patientId, today, endDate);
    }

    /**
     * Get population-level gap report for a tenant
     *
     * @param tenantId Tenant ID
     * @return Population gap report
     */
    @Cacheable(value = "populationGapReport", key = "#tenantId")
    public PopulationGapReport getPopulationGapReport(String tenantId) {
        log.info("Generating population gap report for tenant: {}", tenantId);

        List<CareGapEntity> allOpenGaps = careGapRepository.findAllOpenGaps(tenantId);

        // Count patients with gaps
        long uniquePatients = allOpenGaps.stream()
                .map(CareGapEntity::getPatientId)
                .distinct()
                .count();

        // Calculate average gaps per patient
        double avgGapsPerPatient = uniquePatients > 0 ?
                (double) allOpenGaps.size() / uniquePatients : 0.0;

        // Get gaps by priority
        Map<String, Long> gapsByPriority = allOpenGaps.stream()
                .collect(Collectors.groupingBy(
                        gap -> gap.getPriority() != null ? gap.getPriority() : "Unknown",
                        Collectors.counting()
                ));

        // Get gaps by measure category
        Map<String, Long> gapsByCategory = allOpenGaps.stream()
                .collect(Collectors.groupingBy(
                        gap -> gap.getGapCategory() != null ? gap.getGapCategory() : "Unknown",
                        Collectors.counting()
                ));

        // Get top measures
        Map<String, Long> topMeasures = allOpenGaps.stream()
                .collect(Collectors.groupingBy(CareGapEntity::getMeasureId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return new PopulationGapReport(
                allOpenGaps.size(),
                uniquePatients,
                avgGapsPerPatient,
                gapsByPriority,
                gapsByCategory,
                topMeasures
        );
    }

    // ==================== Private Helper Methods ====================

    private List<String> extractMeasureCategories(List<CareGapEntity> gaps) {
        return gaps.stream()
                .map(CareGapEntity::getGapCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Map<String, Long> extractTopMeasures(List<CareGapEntity> gaps, int limit) {
        return gaps.stream()
                .collect(Collectors.groupingBy(CareGapEntity::getMeasureId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // ==================== Response Records ====================

    /**
     * Care gap summary for a patient
     */
    public record CareGapSummary(
            int totalGaps,
            int openGaps,
            int closedGaps,
            int highPriorityGaps,
            int overdueGaps,
            double closureRate,
            List<String> measureCategories,
            Map<String, Long> topMeasures
    ) {}

    /**
     * Population-level gap report
     */
    public record PopulationGapReport(
            long totalOpenGaps,
            long uniquePatients,
            double avgGapsPerPatient,
            Map<String, Long> gapsByPriority,
            Map<String, Long> gapsByCategory,
            Map<String, Long> topMeasures
    ) {}
}

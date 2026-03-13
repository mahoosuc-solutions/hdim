package com.healthdata.caregap.service;

import com.healthdata.caregap.api.v1.dto.SimulatedGapClosureRequest;
import com.healthdata.caregap.api.v1.dto.StarDomainSummaryResponse;
import com.healthdata.caregap.api.v1.dto.StarMeasureSummaryResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingSimulationRequest;
import com.healthdata.caregap.api.v1.dto.StarRatingTrendPointResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingTrendResponse;
import com.healthdata.caregap.persistence.CareGapProjectionRepository;
import com.healthdata.caregap.persistence.StarRatingProjectionRepository;
import com.healthdata.caregap.persistence.StarRatingSnapshotRepository;
import com.healthdata.caregap.projection.CareGapProjection;
import com.healthdata.caregap.projection.StarRatingProjection;
import com.healthdata.caregap.projection.StarRatingSnapshot;
import com.healthdata.starrating.domain.DomainScore;
import com.healthdata.starrating.domain.MeasureScore;
import com.healthdata.starrating.domain.StarRatingDomain;
import com.healthdata.starrating.domain.StarRatingMeasure;
import com.healthdata.starrating.service.StarRatingCalculator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StarsProjectionService {

    static final String WEEKLY = "WEEKLY";
    static final String MONTHLY = "MONTHLY";
    static final String ON_DEMAND_READ = "on-demand-read";
    static final String SIMULATION = "simulation";

    private final CareGapProjectionRepository careGapProjectionRepository;
    private final StarRatingProjectionRepository starRatingProjectionRepository;
    private final StarRatingSnapshotRepository starRatingSnapshotRepository;
    private final StarRatingCalculator starRatingCalculator;

    @Transactional
    public StarRatingResponse recalculateCurrentProjection(String tenantId, String triggerEvent) {
        ProjectionComputation computation = computeProjection(
            tenantId,
            careGapProjectionRepository.findAllByTenantId(tenantId),
            triggerEvent
        );

        StarRatingProjection projection = starRatingProjectionRepository.findById(tenantId)
            .orElseGet(() -> StarRatingProjection.builder().tenantId(tenantId).build());
        projection.setOverallRating(decimal(computation.getOverallRating(), 2));
        projection.setRoundedRating(decimal(computation.getRoundedRating(), 1));
        projection.setMeasureCount(computation.getMeasureScores().size());
        projection.setOpenGapCount(computation.getOpenGapCount());
        projection.setClosedGapCount(computation.getClosedGapCount());
        projection.setQualityBonusEligible(computation.getRoundedRating() >= 4.0d);
        projection.setLastTriggerEvent(triggerEvent);
        projection.setLastCalculatedAt(Instant.now());
        projection.setVersion(projection.getVersion() + 1);
        starRatingProjectionRepository.save(projection);

        return toResponse(tenantId, computation, projection.getLastTriggerEvent(), projection.getLastCalculatedAt());
    }

    @Transactional(readOnly = true)
    public StarRatingResponse getCurrentRating(String tenantId) {
        List<CareGapProjection> gaps = careGapProjectionRepository.findAllByTenantId(tenantId);
        ProjectionComputation computation = computeProjection(tenantId, gaps, ON_DEMAND_READ);
        Instant calculatedAt = Instant.now();

        return toResponse(tenantId, computation, ON_DEMAND_READ, calculatedAt);
    }

    @Transactional(readOnly = true)
    public StarRatingTrendResponse getTrend(String tenantId, int weeks, String granularity) {
        LocalDate startDate = LocalDate.now().minusWeeks(Math.max(1, weeks));
        String normalizedGranularity = normalizeGranularity(granularity);
        List<StarRatingTrendPointResponse> points = starRatingSnapshotRepository
            .findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
                tenantId,
                normalizedGranularity,
                startDate
            )
            .stream()
            .map(snapshot -> StarRatingTrendPointResponse.builder()
                .snapshotDate(snapshot.getSnapshotDate())
                .granularity(snapshot.getSnapshotGranularity())
                .overallRating(snapshot.getOverallRating().doubleValue())
                .roundedRating(snapshot.getRoundedRating().doubleValue())
                .openGapCount(snapshot.getOpenGapCount())
                .closedGapCount(snapshot.getClosedGapCount())
                .qualityBonusEligible(snapshot.isQualityBonusEligible())
                .build())
            .toList();

        return StarRatingTrendResponse.builder()
            .tenantId(tenantId)
            .points(points)
            .build();
    }

    @Transactional(readOnly = true)
    public StarRatingResponse simulate(String tenantId, StarRatingSimulationRequest request) {
        List<CareGapProjection> currentGaps = careGapProjectionRepository.findAllByTenantId(tenantId);
        List<CareGapProjection> simulated = applySimulation(currentGaps, request.getClosures());
        ProjectionComputation computation = computeProjection(tenantId, simulated, SIMULATION);
        Instant calculatedAt = Instant.now();
        return toResponse(tenantId, computation, SIMULATION, calculatedAt);
    }

    @Scheduled(cron = "${stars.snapshots.weekly-cron}")
    @Transactional
    public void captureWeeklySnapshots() {
        captureSnapshots(WEEKLY);
    }

    @Scheduled(cron = "${stars.snapshots.monthly-cron}")
    @Transactional
    public void captureMonthlySnapshots() {
        captureSnapshots(MONTHLY);
    }

    private void captureSnapshots(String granularity) {
        LocalDate snapshotDate = LocalDate.now();
        for (String tenantId : careGapProjectionRepository.findDistinctTenantIds()) {
            if (starRatingSnapshotRepository.findByTenantIdAndSnapshotDateAndSnapshotGranularity(
                tenantId, snapshotDate, granularity).isPresent()) {
                continue;
            }

            StarRatingResponse current = getCurrentRating(tenantId);
            starRatingSnapshotRepository.save(StarRatingSnapshot.builder()
                .tenantId(tenantId)
                .snapshotDate(snapshotDate)
                .snapshotGranularity(granularity)
                .overallRating(decimal(current.getOverallRating(), 2))
                .roundedRating(decimal(current.getRoundedRating(), 1))
                .measureCount(current.getMeasureCount())
                .openGapCount(current.getOpenGapCount())
                .closedGapCount(current.getClosedGapCount())
                .qualityBonusEligible(current.isQualityBonusEligible())
                .capturedAt(Instant.now())
                .build());
        }
    }

    private ProjectionComputation computeProjection(String tenantId, List<CareGapProjection> gaps, String triggerEvent) {
        Map<StarRatingMeasure, MeasureAccumulator> accumulators = new EnumMap<>(StarRatingMeasure.class);
        int openGapCount = 0;
        int closedGapCount = 0;

        for (CareGapProjection gap : gaps) {
            StarRatingMeasure measure;
            try {
                measure = StarRatingMeasure.fromCode(gap.getGapCode());
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            MeasureAccumulator accumulator = accumulators.computeIfAbsent(measure, unused -> new MeasureAccumulator());
            accumulator.denominator++;
            if ("CLOSED".equalsIgnoreCase(gap.getStatus())) {
                accumulator.numerator++;
                closedGapCount++;
            } else {
                openGapCount++;
            }
        }

        List<MeasureScore> measureScores = accumulators.entrySet().stream()
            .map(entry -> starRatingCalculator.calculateMeasureScore(
                entry.getKey(),
                entry.getValue().numerator,
                entry.getValue().denominator
            ))
            .sorted(Comparator.comparing(score -> score.getMeasure().getCode()))
            .toList();

        Map<StarRatingDomain, List<MeasureScore>> byDomain = new EnumMap<>(StarRatingDomain.class);
        for (MeasureScore score : measureScores) {
            byDomain.computeIfAbsent(score.getMeasure().getDomain(), unused -> new ArrayList<>()).add(score);
        }

        List<DomainScore> domainScores = byDomain.entrySet().stream()
            .map(entry -> starRatingCalculator.calculateDomainScore(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(score -> score.getDomain().name()))
            .toList();

        double overallRating = starRatingCalculator.calculateOverallStarRating(domainScores);
        double roundedRating = starRatingCalculator.roundToHalfStar(overallRating);

        return ProjectionComputation.builder()
            .tenantId(tenantId)
            .overallRating(overallRating)
            .roundedRating(roundedRating)
            .openGapCount(openGapCount)
            .closedGapCount(closedGapCount)
            .triggerEvent(triggerEvent)
            .measureScores(measureScores)
            .domainScores(domainScores)
            .build();
    }

    private List<CareGapProjection> applySimulation(
        List<CareGapProjection> source,
        List<SimulatedGapClosureRequest> closures
    ) {
        List<CareGapProjection> simulated = source.stream()
            .map(this::copyGap)
            .toList();

        for (SimulatedGapClosureRequest closure : closures) {
            int remaining = closure.getClosures();
            for (CareGapProjection gap : simulated) {
                if (remaining == 0) {
                    break;
                }
                if (!closure.getGapCode().equalsIgnoreCase(gap.getGapCode())) {
                    continue;
                }
                if (!"OPEN".equalsIgnoreCase(gap.getStatus())) {
                    continue;
                }

                gap.setStatus("CLOSED");
                gap.setClosureDate(LocalDate.now());
                remaining--;
            }
        }

        return new ArrayList<>(simulated);
    }

    private StarRatingResponse toResponse(String tenantId, ProjectionComputation computation, String triggerEvent, Instant calculatedAt) {
        return StarRatingResponse.builder()
            .tenantId(tenantId)
            .overallRating(computation.getOverallRating())
            .roundedRating(computation.getRoundedRating())
            .measureCount(computation.getMeasureScores().size())
            .openGapCount(computation.getOpenGapCount())
            .closedGapCount(computation.getClosedGapCount())
            .qualityBonusEligible(computation.getRoundedRating() >= 4.0d)
            .lastTriggerEvent(triggerEvent)
            .calculatedAt(calculatedAt)
            .domains(computation.getDomainScores().stream()
                .map(score -> StarDomainSummaryResponse.builder()
                    .domain(score.getDomain().getDisplayName())
                    .domainStars(score.getDomainStars())
                    .measureCount(score.getMeasureCount())
                    .averagePerformanceRate(score.getAveragePerformanceRate())
                    .build())
                .toList())
            .measures(computation.getMeasureScores().stream()
                .map(score -> StarMeasureSummaryResponse.builder()
                    .measureCode(score.getMeasure().getCode())
                    .measureName(score.getMeasure().getDisplayName())
                    .domain(score.getMeasure().getDomain().getDisplayName())
                    .numerator(score.getNumerator())
                    .denominator(score.getDenominator())
                    .performanceRate(score.getPerformanceRate())
                    .stars(score.getStars())
                    .build())
                .toList())
            .build();
    }

    private String normalizeGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return WEEKLY;
        }

        String normalized = granularity.trim().toUpperCase();
        if (WEEKLY.equals(normalized) || MONTHLY.equals(normalized)) {
            return normalized;
        }

        throw new IllegalArgumentException("Unsupported Stars trend granularity: " + granularity);
    }

    private CareGapProjection copyGap(CareGapProjection gap) {
        CareGapProjection copy = new CareGapProjection(
            gap.getPatientId(),
            gap.getTenantId(),
            gap.getGapCode(),
            gap.getGapDescription(),
            gap.getSeverity()
        );
        copy.setStatus(gap.getStatus());
        copy.setQualified(gap.isQualified());
        copy.setRecommendedIntervention(gap.getRecommendedIntervention());
        copy.setDetectionDate(gap.getDetectionDate());
        copy.setClosureDate(gap.getClosureDate());
        copy.setVersion(gap.getVersion());
        copy.setLastUpdated(gap.getLastUpdated());
        return copy;
    }

    private BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private static final class MeasureAccumulator {
        private int numerator;
        private int denominator;
    }

    @Value
    @Builder
    private static class ProjectionComputation {
        String tenantId;
        double overallRating;
        double roundedRating;
        int openGapCount;
        int closedGapCount;
        String triggerEvent;
        List<MeasureScore> measureScores;
        List<DomainScore> domainScores;
    }
}

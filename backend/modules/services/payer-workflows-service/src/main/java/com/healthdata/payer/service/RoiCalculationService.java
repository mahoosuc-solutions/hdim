package com.healthdata.payer.service;

import com.healthdata.payer.domain.RoiCalculation;
import com.healthdata.payer.domain.RoiCalculation.OrgType;
import com.healthdata.payer.dto.RoiCalculationRequest;
import com.healthdata.payer.dto.RoiCalculationResponse;
import com.healthdata.payer.repository.RoiCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * ROI Calculation Service.
 *
 * All formulas are ported from landing-page-v0/app/components/ROICalculator.tsx:57-135
 * and produce identical results to the frontend. Uses BigDecimal for financial precision.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoiCalculationService {

    private final RoiCalculationRepository repository;

    // Constants matching ROICalculator.tsx exactly
    private static final Map<BigDecimal, BigDecimal> STAR_BONUS_PER_MEMBER = Map.of(
            new BigDecimal("2.0"), BigDecimal.ZERO,
            new BigDecimal("2.5"), BigDecimal.ZERO,
            new BigDecimal("3.0"), BigDecimal.ZERO,
            new BigDecimal("3.5"), BigDecimal.ZERO,
            new BigDecimal("4.0"), new BigDecimal("850"),
            new BigDecimal("4.5"), new BigDecimal("1100"),
            new BigDecimal("5.0"), new BigDecimal("1350")
    );

    private static final BigDecimal MA_ATTRIBUTION = new BigDecimal("0.3");
    private static final BigDecimal HOURLY_RATE = new BigDecimal("75");
    private static final BigDecimal HOURS_REDUCTION_FACTOR = new BigDecimal("0.67");
    private static final int MONTHS_PER_YEAR = 12;
    private static final BigDecimal GAPS_PER_PATIENT = new BigDecimal("0.3");
    private static final BigDecimal CLOSURE_RATE_IMPROVEMENT = new BigDecimal("0.35");
    private static final BigDecimal AVG_GAP_VALUE = new BigDecimal("105");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.08");
    private static final BigDecimal YEAR2_GROWTH = new BigDecimal("1.1");
    private static final BigDecimal YEAR3_GROWTH = new BigDecimal("1.2");
    private static final BigDecimal RENEWAL_DISCOUNT = new BigDecimal("0.6");

    private static final MathContext MC = MathContext.DECIMAL128;

    /**
     * Calculate ROI from request inputs, optionally saving the result.
     */
    @Transactional
    public RoiCalculationResponse calculate(RoiCalculationRequest request, String tenantId) {
        OrgType orgType = parseOrgType(request.getOrgType());
        BigDecimal population = BigDecimal.valueOf(request.getPatientPopulation());
        BigDecimal qualityScore = BigDecimal.valueOf(request.getCurrentQualityScore());
        BigDecimal starRating = BigDecimal.valueOf(request.getCurrentStarRating());
        BigDecimal reportingHours = BigDecimal.valueOf(request.getManualReportingHours());

        // === Quality improvement (TSX lines 60-65) ===
        BigDecimal baseImprovement = orgType.baseImprovement;
        BigDecimal baselineGapFactor = new BigDecimal("100").subtract(qualityScore)
                .divide(new BigDecimal("30"), MC);
        BigDecimal projectedImprovement = baseImprovement.multiply(baselineGapFactor, MC);
        BigDecimal projectedScore = qualityScore.multiply(BigDecimal.ONE.add(projectedImprovement), MC)
                .min(new BigDecimal("95"));
        BigDecimal qualityImprovement = projectedScore.subtract(qualityScore);

        // === Star rating improvement (TSX lines 67-69) ===
        BigDecimal starImprovement = qualityImprovement
                .divide(BigDecimal.TEN, MC)
                .multiply(new BigDecimal("0.5"), MC);
        BigDecimal rawProjectedStar = starRating.add(starImprovement);
        // Round to nearest 0.5: Math.round(x * 2) / 2
        BigDecimal projectedStarRating = rawProjectedStar.multiply(new BigDecimal("2"))
                .setScale(0, RoundingMode.HALF_UP)
                .divide(new BigDecimal("2"), 1, RoundingMode.HALF_UP)
                .min(new BigDecimal("5.0"));

        // === Star bonus calculation (TSX lines 72-78) ===
        BigDecimal currentSnapped = snapToHalfStar(starRating);
        BigDecimal currentBonus = STAR_BONUS_PER_MEMBER.getOrDefault(currentSnapped, BigDecimal.ZERO);
        BigDecimal projectedBonus = STAR_BONUS_PER_MEMBER.getOrDefault(projectedStarRating, BigDecimal.ZERO);
        BigDecimal qualityBonusesRaw = projectedBonus.subtract(currentBonus)
                .multiply(population, MC)
                .multiply(MA_ATTRIBUTION, MC);

        // === Shared savings (TSX lines 81-83) ===
        BigDecimal sharedSavingsPerPoint;
        if (request.getPatientPopulation() < 10000) {
            sharedSavingsPerPoint = new BigDecimal("25000");
        } else if (request.getPatientPopulation() < 50000) {
            sharedSavingsPerPoint = new BigDecimal("75000");
        } else {
            sharedSavingsPerPoint = new BigDecimal("150000");
        }
        BigDecimal sharedSavings = qualityImprovement.multiply(sharedSavingsPerPoint, MC);

        // TSX line 127: qualityBonuses output = qualityBonusesRaw + sharedSavings (combined)
        BigDecimal qualityBonuses = qualityBonusesRaw.add(sharedSavings);

        // === Admin savings (TSX lines 85-88) ===
        BigDecimal hoursReduction = reportingHours.multiply(HOURS_REDUCTION_FACTOR, MC);
        BigDecimal adminSavings = hoursReduction.multiply(HOURLY_RATE, MC)
                .multiply(BigDecimal.valueOf(MONTHS_PER_YEAR), MC);

        // === Care gap closure value (TSX lines 91-94) ===
        BigDecimal gapClosureValue = population
                .multiply(GAPS_PER_PATIENT, MC)
                .multiply(CLOSURE_RATE_IMPROVEMENT, MC)
                .multiply(AVG_GAP_VALUE, MC);

        // === Total year 1 value (TSX line 97) ===
        BigDecimal totalYear1Value = qualityBonuses.add(adminSavings).add(gapClosureValue);

        // === Investment (TSX lines 100-105) ===
        BigDecimal baseFee;
        if (request.getPatientPopulation() > 100000) {
            baseFee = new BigDecimal("60000");
        } else if (request.getPatientPopulation() > 50000) {
            baseFee = new BigDecimal("48000");
        } else if (request.getPatientPopulation() > 20000) {
            baseFee = new BigDecimal("36000");
        } else {
            baseFee = new BigDecimal("24000");
        }
        BigDecimal year1Investment = baseFee;

        // === ROI metrics (TSX lines 108-109) ===
        BigDecimal year1ROI = year1Investment.compareTo(BigDecimal.ZERO) > 0
                ? totalYear1Value.subtract(year1Investment)
                        .divide(year1Investment, MC)
                        .multiply(new BigDecimal("100"), MC)
                : BigDecimal.ZERO;
        BigDecimal paybackDays = totalYear1Value.compareTo(BigDecimal.ZERO) > 0
                ? year1Investment.divide(totalYear1Value, MC)
                        .multiply(new BigDecimal("365"), MC)
                : BigDecimal.ZERO;

        // === 3-year NPV (TSX lines 112-120) ===
        BigDecimal year2Value = totalYear1Value.multiply(YEAR2_GROWTH, MC);
        BigDecimal year3Value = totalYear1Value.multiply(YEAR3_GROWTH, MC);
        BigDecimal year2Investment = baseFee.multiply(RENEWAL_DISCOUNT, MC);
        BigDecimal year3Investment = baseFee.multiply(RENEWAL_DISCOUNT, MC);
        BigDecimal onePlusR = BigDecimal.ONE.add(DISCOUNT_RATE);
        BigDecimal threeYearNPV = totalYear1Value.subtract(year1Investment).divide(onePlusR, MC)
                .add(year2Value.subtract(year2Investment).divide(onePlusR.pow(2, MC), MC))
                .add(year3Value.subtract(year3Investment).divide(onePlusR.pow(3, MC), MC));

        // === Round results to match TSX output (lines 122-135) ===
        qualityImprovement = qualityImprovement.multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
        projectedScore = projectedScore.multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
        starImprovement = starImprovement.multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
        qualityBonuses = qualityBonuses.setScale(0, RoundingMode.HALF_UP);
        adminSavings = adminSavings.setScale(0, RoundingMode.HALF_UP);
        gapClosureValue = gapClosureValue.setScale(0, RoundingMode.HALF_UP);
        totalYear1Value = totalYear1Value.setScale(0, RoundingMode.HALF_UP);
        year1Investment = year1Investment.setScale(0, RoundingMode.HALF_UP);
        year1ROI = year1ROI.setScale(0, RoundingMode.HALF_UP);
        paybackDays = paybackDays.setScale(0, RoundingMode.HALF_UP);
        threeYearNPV = threeYearNPV.setScale(0, RoundingMode.HALF_UP);

        // === Optionally save ===
        String savedId = null;
        if (Boolean.TRUE.equals(request.getSave())) {
            RoiCalculation entity = RoiCalculation.builder()
                    .tenantId(tenantId)
                    .orgType(orgType)
                    .patientPopulation(request.getPatientPopulation())
                    .currentQualityScore(BigDecimal.valueOf(request.getCurrentQualityScore()))
                    .currentStarRating(BigDecimal.valueOf(request.getCurrentStarRating()))
                    .manualReportingHours(request.getManualReportingHours())
                    .qualityImprovement(qualityImprovement)
                    .projectedScore(projectedScore)
                    .starImprovement(starImprovement)
                    .projectedStarRating(projectedStarRating)
                    .qualityBonuses(qualityBonuses)
                    .adminSavings(adminSavings)
                    .gapClosureValue(gapClosureValue)
                    .totalYear1Value(totalYear1Value)
                    .year1Investment(year1Investment)
                    .year1ROI(year1ROI)
                    .paybackDays(paybackDays)
                    .threeYearNPV(threeYearNPV)
                    .contactName(request.getContactName())
                    .contactEmail(request.getContactEmail())
                    .contactCompany(request.getContactCompany())
                    .build();
            entity = repository.save(entity);
            savedId = entity.getId();
        }

        return RoiCalculationResponse.builder()
                .id(savedId)
                .orgType(orgType.name())
                .patientPopulation(request.getPatientPopulation())
                .currentQualityScore(BigDecimal.valueOf(request.getCurrentQualityScore()))
                .currentStarRating(BigDecimal.valueOf(request.getCurrentStarRating()))
                .manualReportingHours(request.getManualReportingHours())
                .qualityImprovement(qualityImprovement)
                .projectedScore(projectedScore)
                .starImprovement(starImprovement)
                .projectedStarRating(projectedStarRating)
                .qualityBonuses(qualityBonuses)
                .adminSavings(adminSavings)
                .gapClosureValue(gapClosureValue)
                .totalYear1Value(totalYear1Value)
                .year1Investment(year1Investment)
                .year1ROI(year1ROI)
                .paybackDays(paybackDays)
                .threeYearNPV(threeYearNPV)
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .contactCompany(request.getContactCompany())
                .shareUrl(savedId != null ? "/api/v1/payer/roi/" + savedId : null)
                .build();
    }

    /**
     * Retrieve a saved calculation by ID (public — for shareable links).
     */
    public Optional<RoiCalculationResponse> getById(String id) {
        return repository.findById(id).map(this::toResponse);
    }

    /**
     * List recent calculations for a tenant (auth required).
     */
    public Page<RoiCalculationResponse> getRecent(String tenantId, Pageable pageable) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toResponse);
    }

    private RoiCalculationResponse toResponse(RoiCalculation entity) {
        return RoiCalculationResponse.builder()
                .id(entity.getId())
                .orgType(entity.getOrgType().name())
                .patientPopulation(entity.getPatientPopulation())
                .currentQualityScore(entity.getCurrentQualityScore())
                .currentStarRating(entity.getCurrentStarRating())
                .manualReportingHours(entity.getManualReportingHours())
                .qualityImprovement(entity.getQualityImprovement())
                .projectedScore(entity.getProjectedScore())
                .starImprovement(entity.getStarImprovement())
                .projectedStarRating(entity.getProjectedStarRating())
                .qualityBonuses(entity.getQualityBonuses())
                .adminSavings(entity.getAdminSavings())
                .gapClosureValue(entity.getGapClosureValue())
                .totalYear1Value(entity.getTotalYear1Value())
                .year1Investment(entity.getYear1Investment())
                .year1ROI(entity.getYear1ROI())
                .paybackDays(entity.getPaybackDays())
                .threeYearNPV(entity.getThreeYearNPV())
                .contactName(entity.getContactName())
                .contactEmail(entity.getContactEmail())
                .contactCompany(entity.getContactCompany())
                .shareUrl("/api/v1/payer/roi/" + entity.getId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    static OrgType parseOrgType(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Organization type is required");
        }
        // Accept both enum names and display names from frontend
        return switch (input.trim().toUpperCase().replace(" ", "_")) {
            case "ACO", "ACO_/_MSSP", "ACO_MSSP" -> OrgType.ACO;
            case "HEALTH_SYSTEM" -> OrgType.HEALTH_SYSTEM;
            case "HIE", "HEALTH_INFORMATION_EXCHANGE" -> OrgType.HIE;
            case "PAYER", "HEALTH_PLAN_/_PAYER", "HEALTH_PLAN" -> OrgType.PAYER;
            case "FQHC", "FQHC_/_COMMUNITY_HEALTH", "FQHC_COMMUNITY_HEALTH" -> OrgType.FQHC;
            default -> throw new IllegalArgumentException("Unknown organization type: " + input);
        };
    }

    private static BigDecimal snapToHalfStar(BigDecimal rating) {
        return rating.multiply(new BigDecimal("2"))
                .setScale(0, RoundingMode.HALF_UP)
                .divide(new BigDecimal("2"), 1, RoundingMode.HALF_UP);
    }
}

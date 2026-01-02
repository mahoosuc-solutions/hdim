package com.hdim.riskmodels.adjusters;

/**
 * Applies risk adjustment for Medicare/Medicaid dual eligible beneficiaries.
 * Dual eligible patients typically have higher complexity and social determinants.
 */
public class DualEligibilityAdjuster {

    public double adjust(double baseScore, boolean isDualEligible) {
        if (isDualEligible) {
            return baseScore * 1.25; // 25% increase for dual eligible
        }
        return baseScore;
    }

    public double calculateDualEligibilityCoefficient(boolean isDualEligible, boolean isFullBenefit) {
        if (!isDualEligible) {
            return 0.0;
        }

        if (isFullBenefit) {
            return 0.209; // Full dual eligible coefficient
        } else {
            return 0.119; // Partial dual eligible coefficient
        }
    }

    public String getDualEligibilityCategory(boolean isDualEligible, boolean isFullBenefit) {
        if (!isDualEligible) {
            return "Non-Dual Eligible";
        }
        return isFullBenefit ? "Full Dual Eligible" : "Partial Dual Eligible";
    }

    public boolean isHighRisk(boolean isDualEligible) {
        return isDualEligible; // Dual eligible inherently indicates higher risk
    }
}

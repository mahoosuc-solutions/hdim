package com.hdim.riskmodels.adjusters;

/**
 * Applies risk adjustment for disability status.
 * Disabled beneficiaries have different risk profiles than aged beneficiaries.
 */
public class DisabilityAdjuster {

    public double adjust(double baseScore, boolean isDisabled) {
        if (isDisabled) {
            return baseScore * 1.35; // 35% increase for disabled status
        }
        return baseScore;
    }

    public double calculateDisabilityCoefficient(boolean isDisabled, int age) {
        if (!isDisabled) {
            return 0.0;
        }

        // Disability coefficients vary by age
        if (age < 35) {
            return 0.45;
        } else if (age < 45) {
            return 0.50;
        } else if (age < 55) {
            return 0.55;
        } else if (age < 65) {
            return 0.60;
        } else {
            return 0.40; // Lower after eligible for aged Medicare
        }
    }

    public String getDisabilityCategory(boolean isDisabled, String disabilityType) {
        if (!isDisabled) {
            return "Non-Disabled";
        }

        if (disabilityType != null) {
            return "Disabled - " + disabilityType;
        }

        return "Disabled";
    }

    public boolean requiresEnhancedCare(boolean isDisabled, int comorbidityCount) {
        return isDisabled && comorbidityCount >= 3;
    }
}

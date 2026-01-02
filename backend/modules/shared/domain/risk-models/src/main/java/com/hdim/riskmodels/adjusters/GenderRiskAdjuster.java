package com.hdim.riskmodels.adjusters;

/**
 * Applies gender-based risk adjustment to base risk scores.
 * Accounts for gender-specific health risk patterns.
 */
public class GenderRiskAdjuster {

    public double adjust(double baseScore, boolean isMale, int age) {
        double genderMultiplier = calculateGenderMultiplier(isMale, age);
        return baseScore * genderMultiplier;
    }

    public double calculateGenderMultiplier(boolean isMale, int age) {
        // Males generally have higher mortality risk, especially in middle age
        if (isMale) {
            if (age < 50) {
                return 1.1;  // Slightly higher risk
            } else if (age < 70) {
                return 1.15; // Moderately higher risk
            } else {
                return 1.05; // Risks converge in elderly
            }
        } else {
            // Females
            if (age < 50) {
                return 0.9;  // Lower risk in younger years
            } else if (age < 70) {
                return 0.95; // Slightly lower risk
            } else {
                return 1.0;  // Baseline in elderly
            }
        }
    }

    public String getGenderRiskCategory(boolean isMale, int age) {
        if (isMale) {
            return age < 65 ? "Male - Elevated Risk" : "Male - Standard Risk";
        } else {
            return age < 65 ? "Female - Reduced Risk" : "Female - Standard Risk";
        }
    }
}

package com.hdim.riskmodels.adjusters;

/**
 * Applies age-based risk adjustment to base risk scores.
 * Uses actuarial tables and clinical evidence.
 */
public class AgeRiskAdjuster {

    public double adjust(double baseScore, int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age must be non-negative");
        }

        double ageMultiplier = calculateAgeMultiplier(age);
        return baseScore * ageMultiplier;
    }

    public double calculateAgeMultiplier(int age) {
        if (age < 18) {
            return 0.5;  // Children generally have lower risk
        } else if (age < 40) {
            return 0.8;  // Young adults
        } else if (age < 50) {
            return 1.0;  // Baseline
        } else if (age < 60) {
            return 1.2;  // Middle age increase
        } else if (age < 70) {
            return 1.5;  // Senior increase
        } else if (age < 80) {
            return 1.8;  // Elderly increase
        } else {
            return 2.2;  // Very elderly increase
        }
    }

    public String getAgeCategory(int age) {
        if (age < 18) return "Pediatric";
        if (age < 40) return "Young Adult";
        if (age < 65) return "Adult";
        if (age < 80) return "Senior";
        return "Elderly";
    }
}

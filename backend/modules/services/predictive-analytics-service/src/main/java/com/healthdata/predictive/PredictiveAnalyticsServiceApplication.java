package com.healthdata.predictive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Predictive Analytics Service - ML-powered predictive analytics for healthcare
 *
 * Provides risk prediction, cost forecasting, disease progression modeling,
 * and population health risk stratification using machine learning models.
 *
 * Features:
 * - Readmission risk prediction (30/90-day)
 * - Per-patient cost forecasting
 * - Disease progression trajectory modeling
 * - Population risk stratification
 * - Feature extraction and normalization
 * - Model explainability
 */
@SpringBootApplication
@EnableFeignClients
@EnableCaching
@EnableJpaRepositories(basePackages = {
    "com.healthdata.predictive.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.predictive.entity"
})
public class PredictiveAnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PredictiveAnalyticsServiceApplication.class, args);
    }
}

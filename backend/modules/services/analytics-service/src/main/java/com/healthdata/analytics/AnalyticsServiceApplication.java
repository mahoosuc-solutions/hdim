package com.healthdata.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Analytics Service - Performance metrics and STAR ratings
 *
 * Manages analytics metrics, report generation, and Medicare STAR ratings
 * for quality measurement and reporting.
 */
@SpringBootApplication
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}

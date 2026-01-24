package com.healthdata.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Data Ingestion Service - Standalone integration engine for load testing and data validation.
 *
 * <p>This service runs as a SEPARATE Docker container (NOT part of core platform) to enable:
 * <ul>
 *   <li>Performance measurement without load testing contamination</li>
 *   <li>Real-time data streaming visualization for customer demonstrations</li>
 *   <li>AI-powered validation of platform behavior</li>
 *   <li>Distributed tracing visibility across microservices</li>
 *   <li>Audit logging transparency for compliance demonstrations</li>
 * </ul>
 *
 * <p>Architecture:
 * - Independent Docker image with CPU/memory limits (2 cores, 2GB RAM)
 * - Separate "ingestion" profile (NOT in "core" or "full")
 * - Manual start only (restart: "no")
 * - Connects to healthdata-network for HTTP integration
 * - OpenTelemetry tracing shows clear service boundaries
 *
 * @see com.healthdata.ingestion.api.v1.IngestionController
 * @see com.healthdata.ingestion.application.DataIngestionService
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DataIngestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataIngestionApplication.class, args);
    }
}

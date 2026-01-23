package com.healthdata.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Data Ingestion Service - Standalone Integration Engine
 *
 * <p>This service provides a transparent, customer-visible data ingestion pipeline for HDIM platform
 * demonstrations and AI-powered validation. Unlike the demo-seeding-service which focuses on
 * scenario-based demo data, this service is designed to:
 *
 * <ul>
 *   <li>Stream data ingestion in real-time via Server-Sent Events (SSE)
 *   <li>Validate service consumption and storage across all microservices
 *   <li>Enable AI Solution Architects to demonstrate live data flow
 *   <li>Provide comprehensive audit trails for HIPAA compliance verification
 *   <li>Track distributed tracing through OpenTelemetry for observability demos
 * </ul>
 *
 * <p><strong>Key Differentiators from demo-seeding-service:</strong>
 *
 * <ul>
 *   <li><strong>Customer Visibility:</strong> Real-time event streaming shows data flowing through
 *       the platform
 *   <li><strong>AI Validation:</strong> Uses AI to verify system behavior and detect anomalies
 *   <li><strong>Audit Transparency:</strong> Every operation is logged and made visible for
 *       compliance demonstrations
 *   <li><strong>Service Impact Tracking:</strong> Shows which microservices processed the ingested
 *       data
 *   <li><strong>Production-Ready:</strong> Designed for customer-facing demonstrations and POC
 *       deployments
 * </ul>
 *
 * <p><strong>Integration Points:</strong>
 *
 * <ul>
 *   <li>FHIR Service (8085): POST /fhir/Bundle - Persist patient resources
 *   <li>Care Gap Service (8086): POST /care-gap/ - Create care gaps
 *   <li>Quality Measure Service (8087): POST /api/v1/measures/seed - Seed measures
 *   <li>Audit Service: Track all operations for HIPAA compliance
 * </ul>
 *
 * @see com.healthdata.ingestion.application.DataIngestionService
 * @see com.healthdata.ingestion.application.EventStreamService
 * @see com.healthdata.ingestion.application.ValidationService
 * @since 1.0.0
 * @author HDIM Platform Team
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
@ComponentScan(
    basePackages = {
      "com.healthdata.ingestion",
      "com.healthdata.common",
      "com.healthdata.persistence",
      "com.healthdata.audit"
    })
public class DataIngestionApplication {

  /**
   * Main entry point for the Data Ingestion Service.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(DataIngestionApplication.class, args);
  }
}

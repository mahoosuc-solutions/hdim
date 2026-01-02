package com.healthdata.enrichment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI Data Enrichment Service Application.
 *
 * Provides AI-powered data enrichment capabilities including:
 * - Clinical note NLP extraction
 * - Medical code validation (ICD-10, SNOMED, CPT, LOINC)
 * - Code suggestion from text
 * - Data completeness analysis
 * - Data quality assessment
 * - Multi-tenant support
 * - Redis caching for terminology lookups
 * - Async processing for large documents
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.enrichment",
    "com.healthdata.common",
    "com.healthdata.authentication",
    "com.healthdata.cache"
})
@EnableCaching
@EnableKafka
@EnableAsync
public class DataEnrichmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataEnrichmentServiceApplication.class, args);
    }
}

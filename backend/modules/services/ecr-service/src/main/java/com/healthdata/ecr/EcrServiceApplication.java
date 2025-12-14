package com.healthdata.ecr;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.service.JwtTokenService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Electronic Case Reporting (eCR) Service Application
 *
 * Provides automated electronic case reporting capabilities for public health
 * surveillance in compliance with CMS and CDC requirements.
 *
 * Key Features:
 * - RCTC (Reportable Condition Trigger Codes) rules evaluation
 * - eICR (electronic Initial Case Report) FHIR Bundle generation
 * - AIMS (APHL Informatics Messaging Services) platform integration
 * - RR (Reportability Response) processing
 * - Event-driven trigger detection via Kafka
 *
 * Regulatory Context:
 * - CMS eCR mandate effective December 2025
 * - USCDI v3 compatibility
 * - HL7 FHIR eCR Implementation Guide compliance
 *
 * @see <a href="https://ecr.aimsplatform.org/">AIMS Platform</a>
 * @see <a href="https://rctc.cdc.gov/">RCTC Value Sets</a>
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.ecr",
    "com.healthdata.authentication",
    "com.healthdata.audit"
})
@Import({JwtAuthenticationFilter.class, JwtTokenService.class, JwtConfig.class})
@EnableFeignClients
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableKafka
@EnableJpaRepositories(basePackages = {"com.healthdata.ecr.persistence"})
@EntityScan(basePackages = {"com.healthdata.ecr.persistence"})
public class EcrServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcrServiceApplication.class, args);
    }
}

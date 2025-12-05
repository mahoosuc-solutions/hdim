package com.healthdata.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Patient Service - Patient aggregation and timeline service
 *
 * Aggregates FHIR resources from FHIR Service, provides timeline views,
 * health status dashboards, and applies consent filters for patient data.
 */
@SpringBootApplication
@EnableFeignClients
@EnableCaching
@EnableJpaRepositories(basePackages = {
    "com.healthdata.patient.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.patient.entity"
})
public class PatientServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }
}

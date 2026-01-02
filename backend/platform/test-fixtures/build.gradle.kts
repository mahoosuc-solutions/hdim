plugins {
    `java-library`
}

description = "Shared test fixtures and utilities for HDIM backend services"

dependencies {
    // Spring Boot Test
    api(libs.spring.boot.starter.test)
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.data.jpa)

    // TestContainers
    api(libs.testcontainers)
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.postgresql)
    api(libs.testcontainers.kafka)
    api(libs.testcontainers.redis)

    // Testing frameworks
    api(libs.junit.jupiter)
    api(libs.mockito.core)
    api(libs.mockito.junit.jupiter)

    // Note: AssertJ is included transitively via spring-boot-starter-test

    // FHIR for test data generation
    implementation(libs.hapi.fhir.base)
    implementation(libs.hapi.fhir.structures.r4)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.jackson.databind)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

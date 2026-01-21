plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Shared test infrastructure for HDIM services - Testcontainers, base classes, and test utilities"

dependencies {
    // Spring Boot Test
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.kafka:spring-kafka")
    
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
    testRuntimeOnly(libs.junit.platform.launcher)
    
    // Audit module for test utilities
    api(project(":modules:shared:infrastructure:audit"))
    
    // FHIR for test data generation
    api(libs.hapi.fhir.base)
    api(libs.hapi.fhir.structures.r4)
    
    // Utilities
    api(libs.commons.lang3)
    api(libs.jackson.databind)
    api(libs.lombok)
    annotationProcessor(libs.lombok)
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}


plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Event Sourcing Infrastructure"

dependencies {
    // Spring Data JPA
    api(libs.spring.boot.starter.data.jpa)
    api("org.springframework.boot:spring-boot-starter-webflux")  // For Flux/Mono
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Database & Persistence
    api(libs.liquibase)
    implementation(libs.postgresql)

    // Hibernate JSONB type support for complex aggregates
    implementation(libs.hypersistence.utils.hibernate.v63)

    // Kafka
    api(libs.spring.kafka)
    api(libs.kafka.clients)
    implementation("io.projectreactor:reactor-core")
    implementation("io.projectreactor.kafka:reactor-kafka")

    // Spring AOP for Kafka consumer aspect
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Common models
    api(project(":modules:shared:domain:common"))

    // Authentication core for UserContext (NO Spring dependencies)
    api(project(":modules:shared:infrastructure:authentication-core"))

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    // Logging
    implementation(libs.logback.classic)
    implementation("org.springframework.boot:spring-boot-starter-logging")

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation("io.projectreactor:reactor-test")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

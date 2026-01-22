plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    // Authentication/Security excluded - event-store-service uses gateway trust only
    // implementation(project(":modules:shared:infrastructure:authentication"))
    // implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    // Tracing excluded - event-store-service doesn't need Kafka dependencies
    // implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.validation)

    // Database & Persistence
    implementation(libs.postgresql)
    implementation(libs.liquibase)

    // JSONB support for PostgreSQL
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.0")

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Resilience4j
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation("org.springframework.security:spring-security-test")
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    systemProperty("spring.profiles.active", "test")
}

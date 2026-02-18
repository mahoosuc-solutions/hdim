plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

description = "FHIR R4 Service - FHIR Resource Management"

// Exclude Flyway - this service uses Liquibase for migrations
// Exclude Jackson XML - FHIR Service uses JSON-only format (application/fhir+json)
configurations.all {
    exclude(group = "org.flywaydb", module = "flyway-core")
    exclude(group = "org.flywaydb", module = "flyway-database-postgresql")
    exclude(group = "com.fasterxml.jackson.dataformat", module = "jackson-dataformat-xml")
}

dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:gateway-core"))
    implementation(project(":platform:auth"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:cache"))
    implementation(project(":modules:shared:infrastructure:messaging"))
    implementation(project(":modules:shared:api-contracts:fhir-api"))
    implementation(project(":modules:shared:infrastructure:authentication"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.bundles.spring.boot.security)
    implementation(libs.bundles.spring.boot.redis)

    // HAPI FHIR Server
    implementation(libs.bundles.hapi.fhir.server)

    // API Documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Kafka
    implementation(libs.bundles.kafka)

    // Monitoring
    implementation(libs.bundles.monitoring)

    // Distributed Tracing (uses shared module for W3C + B3 context propagation)
    implementation(project(":modules:shared:infrastructure:tracing"))

    // Resilience4j for circuit breaker, retry, and rate limiting
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.ratelimiter)

    // JWT for SMART on FHIR OAuth
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // WebSocket for real-time subscriptions
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Utilities
    implementation(libs.bundles.utilities)
    annotationProcessor(libs.mapstruct.processor)

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)  // PostgreSQL JDBC driver for Testcontainers
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redis)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility:4.3.0")
}

tasks.bootJar {
    archiveFileName.set("fhir-service.jar")
}

tasks.withType<Test> {
    val taskNames = gradle.startParameter.taskNames
    useJUnitPlatform {
        when {
            taskNames.any { it.contains("testUnit") } -> {
                excludeTags("slow", "heavyweight", "integration", "e2e", "contract", "entity-migration-validation")
            }
            taskNames.any { it.contains("testFast") } -> {
                excludeTags("slow", "heavyweight", "e2e", "contract", "entity-migration-validation")
            }
            taskNames.any { it.contains("testIntegration") } -> {
                includeTags("integration")
                excludeTags("slow", "heavyweight", "e2e", "contract")
            }
            taskNames.any { it.contains("testSlow") } -> {
                includeTags("slow", "heavyweight", "e2e", "contract", "entity-migration-validation")
            }
            else -> {
                // Default: exclude integration, e2e, heavyweight, slow, contract (unit tests only)
                excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
            }
        }
    }
    // Testcontainers system properties disabled - using running Docker PostgreSQL
    // Configuration now managed in src/test/resources/application-test.yml
    systemProperty("spring.profiles.active", "test")
}

// Dedicated integration test task for FHIR service
// Named "testIntegrationFhir" so the root build's task-name-detection includes integration tags
// Usage: ./gradlew :modules:services:fhir-service:testIntegrationFhir
tasks.register<Test>("testIntegrationFhir") {
    group = "verification"
    description = "Run FHIR service integration tests (requires Docker for Testcontainers)"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
        excludeTags("slow", "heavyweight", "e2e", "contract")
    }
    systemProperty("spring.profiles.active", "test")
}

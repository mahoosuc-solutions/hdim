plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}




dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:messaging"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    // CDR Processor for HL7/CDA parsing
    implementation(project(":modules:services:cdr-processor-service"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.actuator)

    // WebFlux for WebClient
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // WebSocket support
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // SFTP support (JSch)
    implementation(libs.jsch)

    // HAPI HL7 for MLLP support
    implementation(libs.hapi.hl7.base)
    implementation(libs.hapi.hl7.structures.v25)

    // Resilience4j for fault tolerance
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.ratelimiter)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // JSON type support for JPA (JSONB, HSTORE, etc.)
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:${libs.versions.hypersistence.utils.get()}")

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Kafka for event publishing
    implementation(libs.bundles.kafka)

    // OpenAPI/Swagger
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.postgresql)  // PostgreSQL JDBC driver for Testcontainers

    // Testcontainers
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
    }
    systemProperty("spring.profiles.active", "test")
}

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

description = "FHIR R4 Service - FHIR Resource Management"

dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":platform:auth"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
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

    // Resilience4j for circuit breaker, retry, and rate limiting
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.1.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.1.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.1.0")

    // Utilities
    implementation(libs.bundles.utilities)
    annotationProcessor(libs.mapstruct.processor)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redis)
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.bootJar {
    archiveFileName.set("fhir-service.jar")
}

tasks.test {
    useJUnitPlatform()
}

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

group = "com.healthdata.testing"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Test - use version catalog
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    // spring-kafka-test is not in catalog, use direct reference (version managed by Spring Boot BOM)
    testImplementation("org.springframework.kafka:spring-kafka-test")
    
    // Testcontainers - use version catalog
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
    
    // Kafka - use version catalog
    testImplementation(libs.spring.kafka)
    testImplementation(libs.kafka.clients)
    
    // Jackson - use version catalog
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.datatype.jsr310)
    
    // JUnit - use version catalog
    testImplementation(libs.junit.jupiter)
    
    // Project dependencies
    testImplementation(project(":modules:shared:infrastructure:audit"))
}

// Disable bootJar task - this is a test-only module, not an application
tasks.named("bootJar") {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2g"
    jvmArgs = listOf("-XX:+UseG1GC")
    
    // Allow tests to run longer
    systemProperty("junit.jupiter.execution.timeout.testable.method.default", "5m")
}

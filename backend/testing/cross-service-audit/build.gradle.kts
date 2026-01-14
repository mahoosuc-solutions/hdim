plugins {
    id("java")
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.healthdata.testing"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    
    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    
    // Kafka
    testImplementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.apache.kafka:kafka-clients")
    
    // Jackson
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // JUnit & AssertJ
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    
    // Project dependencies
    testImplementation(project(":modules:shared:infrastructure:audit"))
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2g"
    jvmArgs = listOf("-XX:+UseG1GC")
    
    // Allow tests to run longer
    systemProperty("junit.jupiter.execution.timeout.testable.method.default", "5m")
}

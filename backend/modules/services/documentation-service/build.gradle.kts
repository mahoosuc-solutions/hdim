plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
}

group = "com.healthdata"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core:4.25.1")

    // Redis for caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("redis.clients:jedis:5.1.0")

    // Markdown processing
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")

    // YAML parsing
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Utilities
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.commons:commons-text:1.11.0")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Resilience4j for fault tolerance
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    implementation("io.github.resilience4j:resilience4j-timelimiter:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

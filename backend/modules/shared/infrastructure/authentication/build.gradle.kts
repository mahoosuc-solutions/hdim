plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Shared Authentication Infrastructure"

dependencies {
    // Spring Security
    api(libs.spring.security.core)
    api(libs.spring.security.config)
    api(libs.spring.security.web)
    api("org.springframework.boot:spring-boot-starter-security")

    // Spring Web (for servlet API)
    api("org.springframework.boot:spring-boot-starter-web")

    // Spring Data JPA
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring Boot Auto-configuration
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Spring AOP for audit logging
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Liquibase removed - migrations managed by Gateway service
    // implementation("org.liquibase:liquibase-core")

    // JWT Authentication
    api(libs.jjwt.api)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)

    // TOTP MFA (Time-based One-Time Password)
    api("dev.samstevens.totp:totp:1.7.1")

    // Spring Cloud OpenFeign for auth header forwarding interceptor
    compileOnly("org.springframework.cloud:spring-cloud-starter-openfeign:4.3.1")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Common domain models
    api(project(":modules:shared:domain:common"))

    // Cache eviction for HIPAA compliance (logout)
    // ⚠️ CRITICAL: Required for clearing PHI caches on user logout
    // See: /backend/HIPAA-CACHE-COMPLIANCE.md
    implementation(project(":modules:shared:infrastructure:cache"))

    // Utilities
    implementation(libs.commons.lang3)

    // Rate limiting disabled - will be managed by Gateway
    // implementation("com.bucket4j:bucket4j-core:8.7.0")
    // implementation("com.bucket4j:bucket4j-redis:8.7.0")
    // implementation("javax.cache:cache-api:1.1.1")

    // Redis (for distributed rate limiting) - disabled for production, enabled for tests
    // Production Redis disabled - managed by Gateway
    // api("org.springframework.boot:spring-boot-starter-data-redis")
    // implementation("org.redisson:redisson-spring-boot-starter:3.25.0")

    // Redis for tests only (needed by EmbeddedRedisTestConfig)
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("io.lettuce:lettuce-core")

    // Metrics
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation("com.github.codemonstur:embedded-redis:1.4.3")
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)  // PostgreSQL JDBC driver for Testcontainers
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redis)
    testRuntimeOnly("com.h2database:h2")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

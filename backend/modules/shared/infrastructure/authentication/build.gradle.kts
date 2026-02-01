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
    api(libs.totp)

    // Twilio SDK for SMS MFA (explicit version control to prevent Jackson conflicts)
    // Version 10.1.5 is compatible with Jackson 2.17.x and prevents transitive
    // dependency conflicts that cause ClassCastException in Jackson XML factory
    implementation("com.twilio.sdk:twilio") {
        version {
            strictly("[10.0,11.0[")
            prefer("10.1.5")
        }
    }

    // Spring Cloud OpenFeign for auth header forwarding interceptor
    compileOnly(libs.spring.cloud.starter.openfeign)

    // OpenTelemetry for trace propagation (compileOnly - optional at runtime)
    compileOnly(libs.opentelemetry.api)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Common domain models
    api(project(":modules:shared:domain:common"))

    // Audit service for HIPAA-compliant logging
    // Required for tenant registration audit trail
    implementation(project(":modules:shared:infrastructure:audit"))

    // Cache eviction for HIPAA compliance (logout)
    // ⚠️ CRITICAL: Required for clearing PHI caches on user logout
    // See: /backend/HIPAA-CACHE-COMPLIANCE.md
    implementation(project(":modules:shared:infrastructure:cache"))

    // Utilities
    implementation(libs.commons.lang3)

    // Rate limiting disabled - will be managed by Gateway
    // Bucket4j and Redis rate limiting dependencies removed

    // Redis (for distributed rate limiting) - disabled for production, enabled for tests
    // Production Redis disabled - managed by Gateway
    // Spring Boot Data Redis and Redisson dependencies removed

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
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}

plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Shared Gateway Core Infrastructure"

dependencies {
    // Spring Boot + Web + Security
    api(libs.bundles.spring.boot.web)
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Redis for rate limiting and caching
    api("org.springframework.boot:spring-boot-starter-data-redis")

    // Resilience4j for circuit breaker, retry, and rate limiting
    api("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    api("io.github.resilience4j:resilience4j-circuitbreaker:2.1.0")
    api("io.github.resilience4j:resilience4j-retry:2.1.0")
    api("io.github.resilience4j:resilience4j-ratelimiter:2.1.0")
    api("io.github.resilience4j:resilience4j-timelimiter:2.1.0")
    api("org.springframework.boot:spring-boot-starter-aop")

    // Bucket4j for distributed rate limiting
    api("com.bucket4j:bucket4j-core:8.7.0")
    api("com.bucket4j:bucket4j-redis:8.7.0")

    // OpenAPI/Swagger for API documentation
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Monitoring & Metrics
    api(libs.bundles.monitoring)

    // Distributed Tracing
    api(project(":modules:shared:infrastructure:tracing"))

    // Shared modules
    api(project(":modules:shared:domain:common"))
    api(project(":modules:shared:infrastructure:security"))
    api(project(":modules:shared:infrastructure:authentication"))
    api(project(":modules:shared:infrastructure:audit"))
    api(project(":modules:shared:infrastructure:persistence"))
    api(project(":modules:shared:infrastructure:cache"))

    // Spring Boot Auto-configuration
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

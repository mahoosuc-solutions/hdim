plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}


dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)

    // Spring Cloud OpenFeign (for Feign clients)
    implementation(libs.spring.cloud.starter.openfeign)

    // OpenAPI/Swagger documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // HAPI FHIR Client (for FHIR resource parsing and integration)
    implementation(libs.bundles.hapi.fhir.client)

    // Apache Commons Math for statistical calculations
    implementation(libs.commons.math3)

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Spring Cache for caching predictions
    implementation(libs.spring.boot.starter.data.redis)

    // Lombok for reducing boilerplate
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Resilience4j for fault tolerance
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.timelimiter)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.postgresql)  // PostgreSQL JDBC driver for Testcontainers
    testImplementation(libs.spring.kafka)
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.security:spring-security-test")
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
    }
    systemProperty("spring.profiles.active", "test")
}

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
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

    // Distributed Tracing (uses shared module for W3C + B3 context propagation)
    implementation(project(":modules:shared:infrastructure:tracing"))

    // Kafka for distributed tracing and audit events (required by TracingAutoConfiguration)
    implementation(libs.bundles.kafka)

    // HAPI FHIR Client (for FHIR resource parsing and integration)
    implementation(libs.bundles.hapi.fhir.client)

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Spring Cache for caching aggregated patient data
    implementation(libs.spring.boot.starter.data.redis)

    // Resilience4j for circuit breaker, retry, and rate limiting
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.ratelimiter)

    // Lombok for reducing boilerplate
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}

tasks.withType<Test> {
    // Testcontainers system properties disabled - using running Docker PostgreSQL
    // Configuration now managed in src/test/resources/application-test.yml
    // systemProperty("spring.datasource.url", "jdbc:tc:postgresql:16-alpine:///testdb")
    // systemProperty("spring.datasource.username", "test")
    // systemProperty("spring.datasource.password", "test")
    // systemProperty("spring.datasource.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver")
    // systemProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
    systemProperty("spring.profiles.active", "test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}

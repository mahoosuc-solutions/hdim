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
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:domain:cql-models"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:messaging"))
    implementation(project(":modules:shared:infrastructure:cache"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.actuator)
    implementation("org.springframework.boot:spring-boot-starter-webflux") // For reactive HTTP client

    // OAuth2 Resource Server for JWT authentication
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Spring Cloud OpenFeign (for calling other services)
    implementation(libs.spring.cloud.starter.openfeign)

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Redis for conversation memory
    implementation(libs.spring.boot.starter.data.redis)

    // Kafka for events
    implementation(libs.bundles.kafka)

    // Resilience4j for circuit breaker and rate limiting
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.timelimiter)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // AWS SDK for Bedrock
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.bedrock.runtime)
    implementation(libs.aws.auth)

    // Azure OpenAI SDK
    implementation(libs.azure.ai.openai)
    implementation(libs.azure.identity)

    // OpenAPI/Swagger for API documentation
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

    // Testcontainers
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redis)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)  // PostgreSQL JDBC driver for Testcontainers

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
    }
    systemProperty("spring.profiles.active", "test")
}

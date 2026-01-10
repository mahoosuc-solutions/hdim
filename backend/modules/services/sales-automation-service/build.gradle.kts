plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}


dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)

    // Spring Cloud OpenFeign (for Feign clients)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Email Support
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Scheduling for automated tasks
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // WebClient for Zoho API integration
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // OpenAPI/Swagger documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Distributed Tracing
    implementation(project(":modules:shared:infrastructure:tracing"))

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Spring Cache with Redis
    implementation(libs.spring.boot.starter.data.redis)

    // Kafka for event-driven communication
    implementation("org.springframework.kafka:spring-kafka")

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

tasks.withType<Test> {
    systemProperty("spring.datasource.url", "jdbc:tc:postgresql:15-alpine:///testdb")
    systemProperty("spring.datasource.username", "test")
    systemProperty("spring.datasource.password", "test")
    systemProperty("spring.datasource.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver")
    systemProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
    systemProperty("spring.profiles.active", "test")
}

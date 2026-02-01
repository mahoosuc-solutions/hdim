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
    // Testing Framework
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.security:spring-security-test")

    // Shared modules for test context
    testImplementation(project(":modules:shared:domain:common"))
    testImplementation(project(":modules:shared:infrastructure:security"))
    testImplementation(project(":modules:shared:infrastructure:audit"))
    testImplementation(project(":modules:shared:infrastructure:persistence"))
    testImplementation(project(":modules:shared:infrastructure:authentication"))
    testImplementation(project(":modules:shared:infrastructure:tracing"))

    // Spring Boot for testing
    testImplementation(libs.bundles.spring.boot.web)
    testImplementation(libs.bundles.spring.boot.data)
    testImplementation(libs.spring.boot.starter.security)
    testImplementation(libs.spring.boot.starter.validation)

    // Spring Cloud OpenFeign for test scenario
    testImplementation(libs.spring.cloud.starter.openfeign)

    // Monitoring & Metrics
    testImplementation(libs.bundles.monitoring)

    // Kafka for messaging validation tests
    testImplementation(libs.bundles.kafka)

    // HAPI FHIR for resource validation
    testImplementation(libs.bundles.hapi.fhir.client)

    // Jackson for JSON processing
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.datatype.jsr310)

    // Redis cache validation
    testImplementation(libs.spring.boot.starter.data.redis)

    // Testcontainers for external service simulation
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)

    // Lombok for test utilities
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Utilities
    testImplementation(libs.commons.lang3)
    testImplementation(libs.guava)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}

tasks.withType<Test> {
    // Use Testcontainers for isolated test environment
    systemProperty("spring.datasource.url", "jdbc:tc:postgresql:16-alpine:///testdb")
    systemProperty("spring.datasource.username", "testuser")
    systemProperty("spring.datasource.password", "testpass")
    systemProperty("spring.datasource.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver")
    systemProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
    systemProperty("spring.profiles.active", "test")
    systemProperty("spring.jpa.hibernate.ddl-auto", "validate")

    // Redis test configuration
    systemProperty("spring.redis.host", "localhost")
    systemProperty("spring.redis.port", "6380")

    // Kafka test configuration
    systemProperty("spring.kafka.bootstrap-servers", "localhost:9094")

    // Enable test logging for debugging
    systemProperty("logging.level.com.healthdata", "INFO")
    systemProperty("logging.level.org.springframework", "WARN")

    useJUnitPlatform()
}

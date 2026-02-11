plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

springBoot {
    mainClass.set("com.healthdata.qualityevent.QualityMeasureEventServiceApplication")
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
    implementation(project(":modules:shared:infrastructure:authentication-headers"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:messaging"))
    implementation(project(":modules:shared:infrastructure:event-store-client"))
    implementation(project(":modules:shared:infrastructure:event-sourcing"))

    // Event handler library (Phase 4)
    implementation(project(":modules:services:quality-measure-event-handler-service"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)

    // Kafka (Spring Kafka + Kafka Clients)
    implementation(libs.bundles.kafka)

    // Spring Cloud OpenFeign for event store client
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // OpenAPI/Swagger
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Distributed Tracing
    implementation(project(":modules:shared:infrastructure:tracing"))

    // Jackson
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Spring Cache - REMOVED: Event services use PostgreSQL only, no Redis needed
    // implementation(libs.spring.boot.starter.data.redis)

    // Resilience4j
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    // testcontainers.kafka removed - using @EmbeddedKafka instead for faster, Docker-free testing

    // Mockito for unit tests
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")

    // H2 for in-memory integration tests
    testImplementation("com.h2database:h2:2.2.224")

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
    }
    systemProperty("spring.profiles.active", "test")
}

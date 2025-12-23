plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
        mavenBom("com.fasterxml.jackson:jackson-bom:2.17.2")
    }
}

configurations.all {
    resolutionStrategy {
        force("com.fasterxml.jackson.core:jackson-databind:2.17.2")
        force("com.fasterxml.jackson.core:jackson-core:2.17.2")
        force("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
        force("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    }
}

// Exclude hibernate-types libraries from test runtime to avoid H2 compatibility issues
configurations {
    testRuntimeClasspath {
        exclude(group = "com.vladmihalcea", module = "hibernate-types-60")
        exclude(group = "io.hypersistence", module = "hypersistence-utils-hibernate-63")
    }
    testCompileClasspath {
        exclude(group = "com.vladmihalcea", module = "hibernate-types-60")
        exclude(group = "io.hypersistence", module = "hypersistence-utils-hibernate-63")
    }
}

dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:messaging"))
    implementation(project(":modules:shared:infrastructure:persistence"))

    // CDR Processor for HL7/CDA parsing
    implementation(project(":modules:services:cdr-processor-service"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.actuator)

    // WebFlux for WebClient
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // WebSocket support
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // SFTP support (JSch)
    implementation("com.jcraft:jsch:0.1.55")

    // HAPI HL7 for MLLP support
    implementation("ca.uhn.hapi:hapi-base:2.5.1")
    implementation("ca.uhn.hapi:hapi-structures-v25:2.5.1")

    // Resilience4j for fault tolerance
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // JSON type support for JPA
    implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Kafka for event publishing
    implementation(libs.bundles.kafka)

    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.security:spring-security-test")

    // Testcontainers
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

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

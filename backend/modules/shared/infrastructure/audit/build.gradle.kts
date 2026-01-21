plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "HIPAA Audit Logging Infrastructure"

dependencies {
    // Spring dependencies
    api("org.springframework:spring-context")
    api("org.springframework:spring-aop")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // AspectJ for AOP
    implementation("org.aspectj:aspectjweaver")

    // FHIR models (for FHIR AuditEvent) - optional
    compileOnly(project(":modules:shared:domain:fhir-models"))

    // Common models - optional
    compileOnly(project(":modules:shared:domain:common"))

    // JSON Processing
    api(libs.jackson.databind)

    // Logging
    api("org.slf4j:slf4j-api")

    // OpenAPI/Swagger Documentation
    api(libs.swagger.annotations)
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)

    // Hibernate types for JSON mapping
    api("io.hypersistence:hypersistence-utils-hibernate-63:3.9.0")

    // Kafka (optional - for audit event streaming)
    compileOnly("org.springframework.kafka:spring-kafka")

    // Utilities
    implementation(libs.commons.lang3)

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.postgresql)
    
    // Testcontainers for integration tests
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation("org.springframework.boot:spring-boot-starter-web")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

tasks.test {
    useJUnitPlatform()
}

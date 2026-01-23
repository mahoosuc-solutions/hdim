plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

group = "com.healthdata"
version = "1.0.0"

dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:audit"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.validation)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // OpenTelemetry for distributed tracing (TODO: Add proper version from libs.versions.toml)
    // implementation("io.opentelemetry:opentelemetry-api")
    // implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")

    // HAPI FHIR
    implementation(libs.bundles.hapi.fhir.client)

    // JSON Processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Utilities
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.commons.lang3)

    // Synthetic data generation - using Datafaker (modern successor to JavaFaker)
    implementation(libs.datafaker)

    // HTTP Client for service integration
    implementation(libs.spring.cloud.starter.openfeign)

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

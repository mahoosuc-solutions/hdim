plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
}

group = "com.healthdata"
version = "1.0.0"

// Force correct snakeyaml version (fix for Android variant issue)
configurations.all {
    resolutionStrategy {
        force("org.yaml:snakeyaml:2.2")
        eachDependency {
            if (requested.group == "org.yaml" && requested.name == "snakeyaml") {
                useVersion("2.2")
                because("Fix for Android variant resolution issue")
            }
        }
    }
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // FHIR (for patient bundle generation)
    implementation(libs.hapi.fhir.base)
    implementation(libs.hapi.fhir.structures.r4)

    // HTTP Client (for calling FHIR/Care Gap/Quality Measure services)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Observability (OpenTelemetry for distributed tracing)
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")

    // Faker for realistic data generation
    implementation("com.github.javafaker:javafaker:1.0.2")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
}

tasks.test {
    useJUnitPlatform()
}

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
    implementation(project(":modules:shared:infrastructure:gateway-core"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)

    // OpenAPI/Swagger documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Kafka for audit event consumption
    implementation(libs.bundles.kafka)

    // Redis for caching (optional - for performance)
    implementation(libs.spring.boot.starter.data.redis)

    // CSV export support
    implementation(libs.opencsv)

    // Excel export support (Apache POI)
    implementation(libs.poi.ooxml)

    // PDF export support (using iText)
    implementation(libs.itext7.core)

    // Liquibase for database migrations
    implementation(libs.liquibase)

    // PostgreSQL driver
    runtimeOnly(libs.postgresql)

    // OpenTelemetry for distributed tracing
    implementation(libs.bundles.tracing)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.jdbc)
    testImplementation(project(":modules:shared:test-infrastructure"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Bootable JAR configuration
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
    mainClass.set("com.healthdata.auditquery.AuditQueryServiceApplication")
}

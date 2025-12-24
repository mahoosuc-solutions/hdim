plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Shared distributed tracing configuration with OpenTelemetry"

dependencies {
    // Spring Boot (for auto-configuration)
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // Micrometer Tracing
    api("io.micrometer:micrometer-tracing:1.3.5")
    api("io.micrometer:micrometer-tracing-bridge-otel:1.3.5")

    // OpenTelemetry SDK and OTLP Exporter
    api("io.opentelemetry:opentelemetry-sdk:1.32.0")
    api("io.opentelemetry:opentelemetry-exporter-otlp:1.32.0")

    // Context propagation (W3C Trace Context + B3 for Zipkin compatibility)
    api("io.opentelemetry:opentelemetry-extension-trace-propagators:1.32.0")

    // Actuator for tracing endpoints
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")

    // Logging
    compileOnly("org.slf4j:slf4j-api")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

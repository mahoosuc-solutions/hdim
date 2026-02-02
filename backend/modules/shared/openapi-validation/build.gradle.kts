plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "OpenAPI specification validation for API compliance testing"

dependencies {
    // Swagger Request Validator from Atlassian
    api("com.atlassian.oai:swagger-request-validator-core:2.40.0")
    api("com.atlassian.oai:swagger-request-validator-springmvc:2.40.0")

    // Shared test infrastructure
    implementation(project(":modules:shared:test-infrastructure"))

    // Spring Boot Test
    implementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // OpenAPI/SpringDoc for spec generation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Testing frameworks
    api(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Utilities
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
    `java-library`
}

description = "Shared OpenAPI/Swagger documentation configuration"

dependencies {
    // Spring Boot (for auto-configuration)
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // OpenAPI/SpringDoc
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Optional: Validation annotations for API models
    compileOnly("jakarta.validation:jakarta.validation-api")
}

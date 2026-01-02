plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Redis Cache Infrastructure"

dependencies {
    // Redis
    api(libs.spring.boot.starter.data.redis)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Common models
    api(project(":modules:shared:domain:common"))

    // JSON Processing
    api(libs.jackson.databind)

    // Utilities
    implementation(libs.commons.lang3)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

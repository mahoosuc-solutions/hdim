plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Authentication Headers - Gateway-trust pattern with NO @EntityScan"

dependencies {
    // Core types (NO Spring dependencies)
    api(project(":modules:shared:infrastructure:authentication-core"))

    // Spring Security (for filters and authentication)
    api(libs.spring.security.core)
    api(libs.spring.security.config)
    api(libs.spring.security.web)

    // Spring Web (for servlet API and filters)
    api("org.springframework.boot:spring-boot-starter-web")

    // Spring Boot Auto-configuration
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Metrics
    implementation("io.micrometer:micrometer-core")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.security:spring-security-test")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

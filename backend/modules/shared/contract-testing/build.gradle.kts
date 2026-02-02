plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Shared Pact contract testing infrastructure"

dependencies {
    // Pact Provider Verification
    api("au.com.dius.pact.provider:junit5:4.6.5")
    api("au.com.dius.pact.provider:spring:4.6.5")
    api("au.com.dius.pact.provider:junit5spring:4.6.5")

    // Shared test infrastructure
    implementation(project(":modules:shared:test-infrastructure"))

    // Spring Boot Test
    implementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")

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

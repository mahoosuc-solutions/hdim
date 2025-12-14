plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Security and HIPAA Compliance Infrastructure"

dependencies {
    // Spring Security
    api(libs.spring.security.core)
    api(libs.spring.security.config)

    // Spring Web for HTTP interceptors
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // JWT
    api(libs.jjwt.api)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)

    // Encryption
    api(libs.bouncycastle.bcprov)

    // JPA for attribute converters (optional - only for encryption annotations)
    compileOnly("jakarta.persistence:jakarta.persistence-api")

    // Common domain models
    api(project(":modules:shared:domain:common"))

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

plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "JPA/Hibernate Persistence Infrastructure"

dependencies {
    // Spring Data JPA
    api(libs.spring.boot.starter.data.jpa)

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")  // For servlet classes and filters
    implementation("org.springframework.boot:spring-boot-starter-aop")  // For AOP aspects
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Database
    api(libs.postgresql)
    api(libs.hikaricp)

    // Liquibase for schema management
    api(libs.liquibase)

    // Common models
    api(project(":modules:shared:domain:common"))

    // Utilities
    implementation(libs.commons.lang3)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("com.h2database:h2")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

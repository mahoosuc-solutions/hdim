plugins {
    `java-library`
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

description = "Shared Database Connection Pool Configuration"

dependencies {
    // Spring Boot auto-configuration
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-jdbc")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // HikariCP
    api(libs.hikaricp)

    // PostgreSQL driver (compileOnly - services provide their own)
    compileOnly(libs.postgresql)

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("com.h2database:h2")
    testImplementation(libs.postgresql)
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

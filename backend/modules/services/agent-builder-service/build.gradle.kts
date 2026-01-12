plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}

dependencies {
    // Shared modules
    // Note: This service uses Pattern 3 (No Auth) - gateway handles authentication
    // We do NOT include authentication or security modules to avoid bean conflicts
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    // Spring Boot
    // Note: No spring-security or oauth2-resource-server - gateway handles auth
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    // Liquibase included via :modules:shared:infrastructure:persistence

    // OpenFeign for service communication
    implementation(libs.spring.cloud.starter.openfeign)

    // Kafka for event publishing
    implementation(libs.spring.kafka)

    // JSON processing
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // Resilience
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.ratelimiter)

    // Observability
    implementation(libs.micrometer.registry.prometheus)

    // API Documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.testcontainers)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.postgresql:postgresql")  // PostgreSQL JDBC driver for Testcontainers
}


tasks.bootJar {
    archiveFileName.set("agent-builder-service.jar")
}

tasks.withType<Test> {
    systemProperty("spring.datasource.url", "jdbc:tc:postgresql:15-alpine:///testdb")
    systemProperty("spring.datasource.username", "test")
    systemProperty("spring.datasource.password", "test")
    systemProperty("spring.datasource.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver")
    systemProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
    systemProperty("spring.profiles.active", "test")
}

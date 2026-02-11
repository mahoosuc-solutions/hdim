plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}


dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:cache"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.redis)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring Cloud OpenFeign (for HTTP clients)
    implementation(libs.spring.cloud.starter.openfeign)

    // Resilience4j for circuit breaker and retry
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.reactor)

    // HAPI FHIR for FHIR parsing and conversion
    implementation(libs.bundles.hapi.fhir.client)

    // OAuth2 Client for CMS APIs
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-client")

    // HTTP Client
    implementation(libs.httpclient5)

    // JSON Processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Database
    implementation(libs.postgresql)

    // OpenAPI/Swagger
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Lombok for reducing boilerplate
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Testing
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.postgresql)  // PostgreSQL JDBC driver for Testcontainers
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.1")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.springframework.kafka:spring-kafka")
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

tasks.withType<Test> {
    // Testcontainers system properties disabled - using running Docker PostgreSQL
    // Configuration now managed in src/test/resources/application-test.yml
    useJUnitPlatform()
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

tasks.withType<Test> {
    // Testcontainers system properties disabled - using running Docker PostgreSQL
    // Configuration now managed in src/test/resources/application-test.yml
    // systemProperty("spring.datasource.url", "jdbc:tc:postgresql:///testdb")
    // systemProperty("spring.datasource.username", "test")
    // systemProperty("spring.datasource.password", "test")
    // systemProperty("spring.datasource.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver")
    // systemProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
    systemProperty("spring.profiles.active", "test")
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

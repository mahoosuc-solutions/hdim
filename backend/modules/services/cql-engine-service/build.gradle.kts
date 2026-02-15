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

// Use a user-owned build directory to avoid permission issues from prior runs.
buildDir = file("build-user")

// Add repositories for CQL engine libraries
repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/releases/")
    }
}

// Configure Java version
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Configure Spring Boot
springBoot {
    mainClass.set("com.healthdata.cql.CqlEngineServiceApplication")
}


dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:gateway-core"))  // For HdimPermissionEvaluator
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:cache"))

    // JWT Authentication (local implementation, no User domain dependency)
    implementation(libs.jjwt.api)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)

    // Jackson for JSON serialization/deserialization
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)

    // Spring AOP for audit aspects
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring Cloud OpenFeign for FHIR client
    implementation(libs.spring.cloud.starter.openfeign)

    // HAPI FHIR for resource handling
    implementation(libs.bundles.hapi.fhir.client)

    // CQL-to-ELM Translation
    implementation(libs.cql.to.elm)
    implementation(libs.cql.cql)
    implementation(libs.cql.elm)
    implementation(libs.cql.model)
    implementation(libs.cql.model.jackson)
    implementation(libs.cql.elm.jackson)

    // Redis for caching (used for ELM template caching)
    implementation(libs.spring.boot.starter.data.redis)
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Kafka for event publishing
    implementation(libs.bundles.kafka)

    // Resilience4j for circuit breaker, retry, and rate limiting
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.ratelimiter)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // WebSocket for real-time event streaming to frontend
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // OpenAPI/Swagger for API documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Distributed Tracing (uses shared module for W3C + B3 context propagation)
    implementation(project(":modules:shared:infrastructure:tracing"))

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)
}

tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
    }
    systemProperty("spring.profiles.active", "test")
}

tasks.bootJar {
    // Stable filename for Docker COPY instructions.
    archiveFileName.set("cql-engine-service.jar")
}

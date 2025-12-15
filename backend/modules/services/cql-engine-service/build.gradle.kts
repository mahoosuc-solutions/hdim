plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

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

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
    }
}

dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:cache"))

    // JWT Authentication (local implementation, no User domain dependency)
    implementation(libs.jjwt.api)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)

    // Spring AOP for audit aspects
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring Cloud OpenFeign for FHIR client
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // HAPI FHIR for resource handling
    implementation(libs.bundles.hapi.fhir.client)

    // CQL-to-ELM Translation (https://mvnrepository.com/artifact/info.cqframework)
    implementation("info.cqframework:cql-to-elm:3.20.0")
    implementation("info.cqframework:cql:3.20.0")
    implementation("info.cqframework:elm:3.20.0")
    implementation("info.cqframework:model:3.20.0")
    implementation("info.cqframework:elm-jackson:3.20.0") // For ELM JSON serialization

    // Redis for caching (used for ELM template caching)
    implementation(libs.spring.boot.starter.data.redis)
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Kafka for event publishing
    implementation(libs.bundles.kafka)

    // Resilience4j for circuit breaker, retry, and rate limiting
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.1.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.1.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.1.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // WebSocket for real-time event streaming to frontend
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // OpenAPI/Swagger for API documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2:2.2.224")
}

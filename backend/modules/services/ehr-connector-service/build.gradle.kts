plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
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
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:cache"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.redis)

    // Spring WebFlux for reactive HTTP client
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Resilience4j for circuit breaker and retry
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
    implementation("io.github.resilience4j:resilience4j-reactor:2.2.0")

    // Spring Cloud OpenFeign (for REST clients)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // HAPI FHIR Client (for FHIR resource parsing and integration)
    implementation(libs.bundles.hapi.fhir.client)

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // OAuth2 Client
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-client")

    // HTTP Client
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")

    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Lombok for reducing boilerplate
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.1")
    testImplementation("io.projectreactor:reactor-test") // For StepVerifier
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0") // For MockWebServer
    testRuntimeOnly("com.h2database:h2:2.2.224")
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

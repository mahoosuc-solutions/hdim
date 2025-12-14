plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
        mavenBom("com.fasterxml.jackson:jackson-bom:2.17.2")
    }
}

configurations.all {
    resolutionStrategy {
        force("com.fasterxml.jackson.core:jackson-databind:2.17.2")
        force("com.fasterxml.jackson.core:jackson-core:2.17.2")
        force("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
        force("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    }
}

dependencies {
    // HAPI HL7 v2 Libraries
    implementation("ca.uhn.hapi:hapi-base:2.5.1")
    implementation("ca.uhn.hapi:hapi-structures-v25:2.5.1")
    implementation("ca.uhn.hapi:hapi-structures-v26:2.5.1")

    // XML Parsing for CDA documents
    implementation("xerces:xercesImpl:2.12.2")
    implementation("xml-apis:xml-apis:1.4.01")
    implementation("javax.xml.bind:jaxb-api:2.3.1")

    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:messaging"))
    implementation(project(":modules:shared:infrastructure:persistence"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.actuator)

    // HAPI FHIR for conversion
    implementation(libs.bundles.hapi.fhir.client)

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Spring Cache for caching parsed messages
    implementation(libs.spring.boot.starter.data.redis)

    // Kafka for message processing and DLQ
    implementation(libs.bundles.kafka)

    // OpenAPI/Swagger for API documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Resilience4j for fault tolerance
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    implementation("io.github.resilience4j:resilience4j-timelimiter:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2:2.2.224")

    // Testcontainers
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redis)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

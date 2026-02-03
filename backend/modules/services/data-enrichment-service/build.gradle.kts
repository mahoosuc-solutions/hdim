plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}



dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:cache"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.actuator)
    implementation("org.springframework.boot:spring-boot-starter-webflux") // For reactive HTTP client

    // Spring Cloud OpenFeign (for calling other services)
    implementation(libs.spring.cloud.starter.openfeign)

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Spring Cache for caching terminology lookups
    implementation(libs.spring.boot.starter.data.redis)

    // Kafka for audit events
    implementation(libs.bundles.kafka)

    // NLP Libraries
    // Apache OpenNLP for NER and text processing
    implementation(libs.opennlp.tools)

    // Stanford CoreNLP for advanced NLP (optional - can be heavy)
    implementation(libs.stanford.corenlp)
    // Use explicit artifact syntax for models classifier
    implementation("edu.stanford.nlp:stanford-corenlp:${libs.versions.stanford.corenlp.get()}:models")

    // Apache cTAKES-inspired clinical text processing
    // Using simpler regex-based approach for this implementation

    // Medical terminology libraries
    // Note: In production, you'd integrate with UMLS API or similar

    // Resilience4j for circuit breaker and rate limiting
    implementation(libs.resilience4j.spring.boot3)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // OpenAPI/Swagger for API documentation
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Regular expression utilities
    implementation(libs.re2j)

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.security:spring-security-test")

    // Testcontainers
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redis)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

// Handle duplicate JAR files from Stanford CoreNLP dependencies
tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
    }
    systemProperty("spring.profiles.active", "test")
}

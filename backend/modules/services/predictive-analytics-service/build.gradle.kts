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
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)

    // Spring Cloud OpenFeign (for Feign clients)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // HAPI FHIR Client (for FHIR resource parsing and integration)
    implementation(libs.bundles.hapi.fhir.client)

    // Apache Commons Math for statistical calculations (replacing Smile ML)
    // Using commons-math3 for ML algorithms as Smile is not available via standard Maven repos

    // Apache Commons Math for statistical calculations
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Jackson for JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Spring Cache for caching predictions
    implementation(libs.spring.boot.starter.data.redis)

    // Lombok for reducing boilerplate
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2:2.2.224")
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

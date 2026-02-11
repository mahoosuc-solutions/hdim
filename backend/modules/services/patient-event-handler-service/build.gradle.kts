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
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:event-sourcing"))
    implementation(project(":modules:shared:infrastructure:messaging"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.cloud.starter.openfeign)

    // Messaging & Events
    implementation("org.springframework.kafka:spring-kafka")

    // JSON Processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
}

tasks.test {
    useJUnitPlatform()
}

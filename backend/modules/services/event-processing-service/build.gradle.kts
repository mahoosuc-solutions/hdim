plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)

    // Kafka
    implementation(libs.bundles.kafka)

    // Email
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Testing
    testImplementation(libs.bundles.testing)
    testRuntimeOnly("com.h2database:h2")
}

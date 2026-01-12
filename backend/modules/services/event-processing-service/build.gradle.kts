plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencies {
    // Shared modules
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    // Spring Boot
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)

    // Kafka
    implementation(libs.bundles.kafka)

    // Email
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Monitoring & Metrics
    implementation(libs.bundles.monitoring)

    // Resilience4j for fault tolerance
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.timelimiter)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Testing
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.bundles.testing)
}

tasks.withType<Test> {
    systemProperty("spring.datasource.url", "jdbc:tc:postgresql:15-alpine:///testdb")
    systemProperty("spring.datasource.username", "test")
    systemProperty("spring.datasource.password", "test")
    systemProperty("spring.datasource.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver")
    systemProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
    systemProperty("spring.profiles.active", "test")
}

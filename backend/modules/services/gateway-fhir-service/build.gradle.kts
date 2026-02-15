plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencies {
    implementation(project(":modules:shared:infrastructure:gateway-core"))
    // Kafka for distributed tracing (required by TracingAutoConfiguration)
    implementation(libs.bundles.kafka)

    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.postgresql)
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

tasks.bootJar {
    // Stable filename for Docker COPY instructions.
    archiveFileName.set("gateway-fhir-service.jar")
}

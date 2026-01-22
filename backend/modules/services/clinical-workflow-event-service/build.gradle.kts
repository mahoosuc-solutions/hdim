plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

springBoot {
    mainClass.set("com.healthdata.clinicalworkflowevent.ClinicalWorkflowEventServiceApplication")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}

dependencies {
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:messaging"))
    implementation(project(":modules:shared:infrastructure:event-store-client"))

    // Event handler library (Phase 4)
    implementation(project(":modules:services:clinical-workflow-event-handler-service"))
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.kafka)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.bundles.monitoring)
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    // REMOVED: Event services use PostgreSQL only, no Redis needed
    // implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.commons.lang3)
    implementation(libs.guava)
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.bundles.testing)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)

    // Mockito for unit tests
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")

    // H2 for in-memory integration tests
    testImplementation("com.h2database:h2:2.2.224")

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
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

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.healthdata"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // Spring Boot Web Stack
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation("org.springframework.boot:spring-boot-starter-logging")

    // Spring OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Dependency on Query Services from Phase 1.7
    implementation(project(":modules:shared:infrastructure:event-sourcing"))

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("com.healthdata.queryapi.QueryApiApplication")
}

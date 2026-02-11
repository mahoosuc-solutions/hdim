plugins {
    `java-library`
}

description = "Clinical Risk Stratification Models and Indices"

dependencies {
    api(project(":modules:shared:domain:common"))

    // HAPI FHIR for terminology support
    api(libs.hapi.fhir.base)
    api(libs.hapi.fhir.structures.r4)

    // JSON Processing
    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)

    // Validation (from Spring Boot)
    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")

    // Logging (SLF4J)
    compileOnly("org.slf4j:slf4j-api:2.0.17")

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

plugins {
    `java-library`
}

description = "Common Domain Models and Value Objects"

dependencies {
    // JSON Processing
    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)

    // Validation
    api("jakarta.validation:jakarta.validation-api:3.1.0")

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)
}

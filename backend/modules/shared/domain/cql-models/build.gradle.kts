plugins {
    `java-library`
}

description = "CQL (Clinical Quality Language) Expression Models"

dependencies {
    api(project(":modules:shared:domain:common"))

    // Depend on FHIR models
    api(project(":modules:shared:domain:fhir-models"))

    // JSON Processing
    api(libs.jackson.databind)

    // Utilities
    implementation(libs.commons.lang3)
}

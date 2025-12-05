plugins {
    `java-library`
}

description = "HEDIS Quality Measure Models"

dependencies {
    // Depend on FHIR models (HEDIS measures use FHIR resources)
    api(project(":modules:shared:domain:fhir-models"))

    // JSON Processing
    api(libs.jackson.databind)

    // Utilities
    implementation(libs.commons.lang3)
}

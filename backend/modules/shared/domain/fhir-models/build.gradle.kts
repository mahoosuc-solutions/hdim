plugins {
    `java-library`
}

description = "FHIR R4 Resource Models"

dependencies {
    api(project(":modules:shared:domain:common"))

    // HAPI FHIR R4 Structures
    api(libs.hapi.fhir.base)
    api(libs.hapi.fhir.structures.r4)
    api(libs.hapi.fhir.validation)
    api(libs.hapi.fhir.validation.resources.r4)
    implementation("ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:7.6.0")

    // JSON Processing
    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.guava)
}

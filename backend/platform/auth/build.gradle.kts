plugins {
    `java-library`
}

description = "Auth abstractions for the Health Data In Motion platform"

dependencies {
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.security)
    implementation(libs.commons.lang3)
}

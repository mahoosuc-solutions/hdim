plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

group = "com.healthdata"
version = "1.0.0"

dependencies {
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.bundles.hapi.fhir.client)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.commons.lang3)
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
}

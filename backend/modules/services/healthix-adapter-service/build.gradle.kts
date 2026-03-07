plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencies {
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:feature-flags"))
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.bundles.kafka)
    implementation(libs.bundles.resilience4j.common)
    implementation(libs.hapi.fhir.base)
    implementation(libs.hapi.fhir.structures.r4)
    implementation(libs.bundles.hapi.fhir.client)
    implementation(libs.hapi.hl7.base)
    implementation(libs.hapi.hl7.structures.v25)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.commons.lang3)
    implementation(libs.spring.boot.starter.security)
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.h2)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.kafka.test)
}

tasks.withType<Test> {
    val taskNames = gradle.startParameter.taskNames
    val isFullRun = taskNames.any { it.contains("testAll") || it.contains("testParallel") || it.contains("testIntegration") }
    useJUnitPlatform {
        if (!isFullRun) {
            excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
        }
    }
    if (!isFullRun) {
        systemProperty("spring.profiles.active", "test")
    }
}

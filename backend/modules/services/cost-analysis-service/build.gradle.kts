plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

dependencies {
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:api-docs"))

    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.data.redis)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.bundles.monitoring)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:${libs.versions.hypersistence.utils.get()}")
    implementation(libs.liquibase)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.security.test)
    testRuntimeOnly(libs.h2)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    systemProperty("spring.profiles.active", "test")
}

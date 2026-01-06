plugins {
    java
    application
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.healthdata.democli.DemoCliApplication")
}

dependencies {
    // Spring Boot (minimal)
    implementation(libs.spring.boot.starter.web)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Picocli for CLI
    implementation("info.picocli:picocli:4.7.6")
    implementation("info.picocli:picocli-spring-boot-starter:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    // HTTP client
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // JSON processing
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // Utilities
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=${project.name}")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("demo-cli")
    launchScript()
}

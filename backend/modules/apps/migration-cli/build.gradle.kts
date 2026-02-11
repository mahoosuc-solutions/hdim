plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
    application
}

application {
    mainClass.set("com.healthdata.migration.cli.MigrationCliApplication")
}

dependencies {
    // Spring Boot (minimal for CLI)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Picocli for CLI
    implementation("info.picocli:picocli-spring-boot-starter:4.7.5")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")

    // ANSI terminal colors
    implementation("org.fusesource.jansi:jansi:2.4.2")

    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Shared DTOs from migration service
    implementation(project(":modules:services:migration-workflow-service"))

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
}

tasks.bootJar {
    archiveFileName.set("hdim-migrate.jar")
    launchScript()
}

tasks.jar {
    enabled = false
}

// Generate GraalVM native image config
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

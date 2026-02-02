plugins {
    `java-library`
}

description = "Authentication Core - Shared types with NO Spring dependencies"

dependencies {
    // NO Spring dependencies - this is a pure Java module

    // Lombok for data classes
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Testing
    testImplementation(libs.junit.jupiter)
}

// Ensure this module stays lightweight - no Spring context scanning
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Xlint:all"
        // Note: -Werror disabled to allow incremental development
    ))
}

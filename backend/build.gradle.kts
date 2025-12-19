plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.versions)
    java
}

group = "com.healthdata"
version = "1.0.0-SNAPSHOT"

// Configure all projects
allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://build.fhir.org/ig/") } // For FHIR resources
    }
}

// Configure subprojects
subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.fasterxml.jackson.core") {
                useVersion(libs.versions.jackson.get())
            }
            if (requested.group == "com.fasterxml.jackson.dataformat" && requested.name.startsWith("jackson-")) {
                useVersion(libs.versions.jackson.get())
            }
            // Security patches - CVE-2024-12798, CVE-2024-12801
            if (requested.group == "ch.qos.logback") {
                useVersion(libs.versions.logback.get())
            }
            // Security patches - CVE-2024-12829, CVE-2025-48795, CVE-2025-48794
            if (requested.group == "org.eclipse.jetty" && requested.name.startsWith("jetty-")) {
                useVersion(libs.versions.jetty.get())
            }
            // Security patch - CVE-2025-46969
            if (requested.group == "io.projectreactor.netty" && requested.name == "reactor-netty-http") {
                useVersion(libs.versions.reactor.netty.get())
            }
            // Security patch - CVE-2025-24970 (netty SslHandler)
            if (requested.group == "io.netty") {
                useVersion(libs.versions.netty.get())
            }
            // Security patch - CVE-2025-48976 (commons-fileupload DoS)
            if (requested.group == "commons-fileupload" && requested.name == "commons-fileupload") {
                useVersion(libs.versions.commons.fileupload.get())
            }
            // Note: lz4-java CVE-2025-12183 has capability conflict between org.lz4 and at.yawk.lz4
            // Accept risk until Kafka updates dependency
            // Security patch - CVE-2024-55887 (ucum XXE)
            if (requested.group == "org.fhir" && requested.name == "ucum") {
                useVersion(libs.versions.ucum.get())
            }
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf(
            "-parameters",
            "-Xlint:unchecked",
            "-Xlint:deprecation"
        ))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    // Common dependencies for all subprojects
    dependencies {
        // Lombok for all modules
        compileOnly("org.projectlombok:lombok:1.18.34")
        annotationProcessor("org.projectlombok:lombok:1.18.34")

        // Testing for all modules
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
        testImplementation("org.mockito:mockito-core:5.14.1")
        testImplementation("org.mockito:mockito-junit-jupiter:5.14.1")
        testImplementation("org.assertj:assertj-core:3.26.3")
    }
}

// Task to check for dependency updates
tasks.dependencyUpdates {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// Custom tasks
tasks.register("buildAllServices") {
    group = "build"
    description = "Build all microservices"
    dependsOn(
        ":modules:services:fhir-service:build",
        ":modules:services:cql-engine-service:build",
        ":modules:services:consent-service:build",
        ":modules:services:event-processing-service:build",
        ":modules:services:patient-service:build",
        ":modules:services:quality-measure-service:build",
        ":modules:services:care-gap-service:build",
        ":modules:services:analytics-service:build",
        ":modules:services:gateway-service:build"
    )
}

tasks.register("testAllServices") {
    group = "verification"
    description = "Test all microservices"
    dependsOn(
        ":modules:services:fhir-service:test",
        ":modules:services:cql-engine-service:test",
        ":modules:services:consent-service:test",
        ":modules:services:event-processing-service:test",
        ":modules:services:patient-service:test",
        ":modules:services:quality-measure-service:test",
        ":modules:services:care-gap-service:test",
        ":modules:services:analytics-service:test",
        ":modules:services:gateway-service:test"
    )
}

tasks.register("cleanAll") {
    group = "build"
    description = "Clean all modules"
    dependsOn(subprojects.map { it.tasks.named("clean") })
}

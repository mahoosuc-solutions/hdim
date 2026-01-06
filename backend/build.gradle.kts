import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

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
    apply(plugin = "jacoco")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
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

    val mockitoAgent by configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        isTransitive = false
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
        if (project.name == "patient-service") {
            // Isolate each test class to avoid hung test workers from lingering threads.
            maxParallelForks = 1
            forkEvery = 1
        }
        // Default timeout prevents a single test from stalling the entire run.
        systemProperty("junit.jupiter.execution.timeout.default", "2m")
        systemProperty("junit.jupiter.execution.timeout.mode", "enabled")
        // Match Docker Desktop's API version to avoid Testcontainers 400s.
        systemProperty("api.version", "1.52")
        systemProperty("docker.api.version", "1.52")
        systemProperty("DOCKER_API_VERSION", "1.52")
        systemProperty("docker.host", "unix:///var/run/docker.sock")
        environment("DOCKER_API_VERSION", "1.52")
        // Ensure test workers see docker-java overrides.
        jvmArgs(
            "-Dapi.version=1.52",
            "-Ddocker.api.version=1.52",
            "-Ddocker.host=unix:///var/run/docker.sock"
        )
        doFirst {
            if (mockitoAgent.files.isNotEmpty()) {
                jvmArgs("-javaagent:${mockitoAgent.singleFile}")
            }
        }
        environment("DOCKER_HOST", "unix:///var/run/docker.sock")
        environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
        testLogging {
            // Emit start/finish signals and stream output to make long-running tests observable.
            events("started", "passed", "skipped", "failed", "standardOut", "standardError")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
        }
        addTestListener(object : org.gradle.api.tasks.testing.TestListener {
            override fun beforeSuite(suite: org.gradle.api.tasks.testing.TestDescriptor) = Unit
            override fun afterSuite(
                suite: org.gradle.api.tasks.testing.TestDescriptor,
                result: org.gradle.api.tasks.testing.TestResult
            ) {
                if (suite.parent == null) {
                    logger.lifecycle(
                        "Test run complete: {} tests, {} passed, {} failed, {} skipped ({}).",
                        result.testCount,
                        result.successfulTestCount,
                        result.failedTestCount,
                        result.skippedTestCount,
                        result.resultType
                    )
                }
            }
            override fun beforeTest(testDescriptor: org.gradle.api.tasks.testing.TestDescriptor) = Unit
            override fun afterTest(
                testDescriptor: org.gradle.api.tasks.testing.TestDescriptor,
                result: org.gradle.api.tasks.testing.TestResult
            ) = Unit
        })
    }

    tasks.withType<JacocoReport>().configureEach {
        val execFile = file("$buildDir/jacoco/test.exec")
        onlyIf { execFile.exists() }
        dependsOn(tasks.withType<Test>())
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude("**/*Application.class")
                }
            })
        )
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    tasks.withType<JacocoCoverageVerification>().configureEach {
        val execFile = file("$buildDir/jacoco/test.exec")
        onlyIf { execFile.exists() }
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/Application.class",
                        "**/Application\$*.class",
                        "**/config/**",
                        "**/dto/**",
                        "**/entity/**",
                        "**/model/**",
                        "**/exception/**"
                    )
                }
            })
        )
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.60".toBigDecimal()
                }
            }
        }
    }

    // Common dependencies for all subprojects
    dependencies {
        // Lombok for all modules
        compileOnly("org.projectlombok:lombok:1.18.34")
        annotationProcessor("org.projectlombok:lombok:1.18.34")

        // Testing for all modules
        testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
        testImplementation("org.mockito:mockito-core:5.2.0")
        testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
        testImplementation("org.mockito:mockito-inline:5.2.0")
        testImplementation("org.assertj:assertj-core:3.27.6")
        mockitoAgent("net.bytebuddy:byte-buddy-agent:1.18.3")
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

// Coverage report tasks for critical services
val criticalServices = listOf(
    "quality-measure-service",
    "cql-engine-service",
    "fhir-service",
    "patient-service",
    "care-gap-service",
    "consent-service"
)

tasks.register("testCriticalServicesWithCoverage") {
    group = "verification"
    description = "Run tests with coverage on critical services"
    dependsOn(
        criticalServices.map { ":modules:services:$it:test" }
    )
    finalizedBy(
        criticalServices.map { ":modules:services:$it:jacocoTestReport" }
    )
}

tasks.register("verifyCriticalServicesCoverage") {
    group = "verification"
    description = "Verify coverage thresholds on critical services"
    dependsOn(
        criticalServices.map { ":modules:services:$it:jacocoTestCoverageVerification" }
    )
}

tasks.register("coverageReport") {
    group = "verification"
    description = "Generate coverage reports for all services"
    dependsOn(subprojects.filter {
        it.path.contains(":modules:services:")
    }.map { "${it.path}:jacocoTestReport" })
    doLast {
        println("\n=== Coverage Reports Generated ===")
        subprojects.filter {
            it.path.contains(":modules:services:")
        }.forEach { project ->
            val reportDir = project.layout.buildDirectory.dir("reports/jacoco/test/html").get().asFile
            if (reportDir.exists()) {
                println("${project.name}: file://${reportDir.absolutePath}/index.html")
            }
        }
    }
}

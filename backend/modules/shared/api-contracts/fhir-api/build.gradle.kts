import java.io.ByteArrayOutputStream
import org.gradle.api.GradleException

plugins {
    `java-library`
}

description = "FHIR API Contracts and DTOs"

val openApiGeneratorCli by configurations.creating

dependencies {
    // FHIR models
    api(project(":modules:shared:domain:fhir-models"))

    // OpenAPI/Swagger annotations
    api(libs.swagger.annotations)

    // Validation
    api("jakarta.validation:jakarta.validation-api:3.1.0")

    // Spring Web annotations used in generated interfaces
    api(libs.spring.boot.starter.web)

    // JSON Processing
    api(libs.jackson.databind)
    api("org.openapitools:jackson-databind-nullable:0.2.6")

    // Utilities
    implementation(libs.commons.lang3)

    openApiGeneratorCli("org.openapitools:openapi-generator-cli:${libs.versions.openapi.generator.get()}")
}

val generatedDir = layout.buildDirectory.dir("generated")

val openApiGenerate by tasks.registering(JavaExec::class) {
    group = "openapi"
    description = "Generate Spring interfaces from fhir-api OpenAPI spec"
    classpath = openApiGeneratorCli
    mainClass.set("org.openapitools.codegen.OpenAPIGenerator")

    val additionalProps = listOf(
        "dateLibrary=java8",
        "serializationLibrary=jackson",
        "useTags=true",
        "interfaceOnly=true",
        "skipDefaultInterface=true",
        "useSpringBoot3=true"
    ).joinToString(",")

    val globalProps = listOf(
        "apiDocs=false",
        "modelDocs=false",
        "apiTests=false",
        "modelTests=false"
    ).joinToString(",")

    args(
        "generate",
        "-g", "spring",
        "-i", "$projectDir/src/main/resources/openapi/fhir-api.yaml",
        "-o", generatedDir.get().asFile.absolutePath,
        "--api-package", "com.healthdata.fhir.api",
        "--model-package", "com.healthdata.fhir.api.dto",
        "--skip-validate-spec",
        "--additional-properties", additionalProps,
        "--global-property", globalProps
    )
}

sourceSets {
    main {
        java {
            srcDir(generatedDir.map { it.dir("src/main/java") })
        }
    }
}

tasks.compileJava {
    dependsOn(openApiGenerate)
}

tasks.named("clean") {
    doLast {
        delete(generatedDir.get().asFile)
    }
}

tasks.register("checkOpenApiSync") {
    dependsOn(openApiGenerate)
    doLast {
        val status = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "status", "--porcelain", generatedDir.get().asFile.absolutePath)
            standardOutput = status
            isIgnoreExitValue = true
        }
        if (status.toString().isNotBlank()) {
            throw GradleException("OpenAPI generated sources are out of date. Run npm run openapi:generate and commit the changes.")
        }
    }
}

rootProject.name = "healthdata-in-motion-backend"

// Enable type-safe project accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Enable automatic JDK provisioning
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// Shared Domain Modules
include(
    "modules:shared:domain:fhir-models",
    "modules:shared:domain:hedis-models",
    "modules:shared:domain:cql-models",
    "modules:shared:domain:common",
    "modules:shared:domain:risk-models"
)

// Shared Infrastructure Modules
include(
    "modules:shared:infrastructure:security",
    "modules:shared:infrastructure:audit",
    "modules:shared:infrastructure:messaging",
    "modules:shared:infrastructure:cache",
    "modules:shared:infrastructure:persistence",
    "modules:shared:infrastructure:authentication",
    "modules:shared:infrastructure:api-docs",
    "modules:shared:infrastructure:tracing",
    "modules:shared:infrastructure:metrics"
)

// Shared API Contracts
include(
    "modules:shared:api-contracts:fhir-api",
    "modules:shared:api-contracts:cql-api",
    "modules:shared:api-contracts:consent-api",
    "modules:shared:api-contracts:events"
)

// Microservices
include(
    "modules:services:fhir-service",
    "modules:services:cql-engine-service",
    "modules:services:consent-service",
    "modules:services:event-processing-service",
    "modules:services:event-router-service",
    "modules:services:patient-service",
    "modules:services:quality-measure-service",
    "modules:services:care-gap-service",
    "modules:services:analytics-service",
    "modules:services:documentation-service",
    "modules:services:gateway-service",
    "modules:services:ai-assistant-service",
    "modules:services:cdr-processor-service",
    "modules:services:ehr-connector-service",
    "modules:services:payer-workflows-service",
    "modules:services:data-enrichment-service",
    "modules:services:predictive-analytics-service",
    "modules:services:sdoh-service",
    "modules:services:agent-runtime-service",
    "modules:services:agent-builder-service",
    "modules:services:approval-service",
    "modules:services:qrda-export-service",
    "modules:services:hcc-service",
    "modules:services:ecr-service",
    "modules:services:prior-auth-service",
    "modules:services:migration-workflow-service",
    "modules:services:sales-automation-service"
)

// Applications
include(
    "modules:apps:migration-cli"
)

// Platform Modules
include(
    "platform:bom",
    "platform:build-logic",
    "platform:test-fixtures",
    "platform:auth"
)

#!/bin/bash
# Standardizes dependency versions in build.gradle.kts files

SERVICE_PATH=$1

if [ -z "$SERVICE_PATH" ]; then
    echo "Usage: $0 <path-to-service>"
    exit 1
fi

BUILD_FILE="$SERVICE_PATH/build.gradle.kts"

if [ ! -f "$BUILD_FILE" ]; then
    echo "Error: $BUILD_FILE not found"
    exit 1
fi

echo "Standardizing $BUILD_FILE"

# Backup
cp "$BUILD_FILE" "$BUILD_FILE.bak"

# Remove Spring Cloud override
sed -i '/mavenBom("org.springframework.cloud:spring-cloud-dependencies:/d' "$BUILD_FILE"

# Remove Jackson BOM and force() blocks
sed -i '/mavenBom("com.fasterxml.jackson:jackson-bom:/d' "$BUILD_FILE"
sed -i '/force("com.fasterxml.jackson.core:jackson/d' "$BUILD_FILE"

# Replace springdoc-openapi
sed -i 's|implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:[^"]*")|implementation(libs.springdoc.openapi.starter.webmvc.ui)|g' "$BUILD_FILE"

# Replace Resilience4j
sed -i 's|implementation("io.github.resilience4j:resilience4j-spring-boot3:[^"]*")|implementation(libs.resilience4j.spring.boot3)|g' "$BUILD_FILE"
sed -i 's|implementation("io.github.resilience4j:resilience4j-circuitbreaker:[^"]*")|implementation(libs.resilience4j.circuitbreaker)|g' "$BUILD_FILE"
sed -i 's|implementation("io.github.resilience4j:resilience4j-retry:[^"]*")|implementation(libs.resilience4j.retry)|g' "$BUILD_FILE"
sed -i 's|implementation("io.github.resilience4j:resilience4j-ratelimiter:[^"]*")|implementation(libs.resilience4j.ratelimiter)|g' "$BUILD_FILE"
sed -i 's|implementation("io.github.resilience4j:resilience4j-timelimiter:[^"]*")|implementation(libs.resilience4j.timelimiter)|g' "$BUILD_FILE"
sed -i 's|implementation("io.github.resilience4j:resilience4j-reactor:[^"]*")|implementation(libs.resilience4j.reactor)|g' "$BUILD_FILE"

# Replace PostgreSQL
sed -i 's|implementation("org.postgresql:postgresql:[^"]*")|implementation(libs.postgresql)|g' "$BUILD_FILE"
sed -i 's|runtimeOnly("org.postgresql:postgresql:[^"]*")|runtimeOnly(libs.postgresql)|g' "$BUILD_FILE"

# Replace JJWT (fhir-service)
sed -i 's|implementation("io.jsonwebtoken:jjwt-api:[^"]*")|implementation(libs.jjwt.api)|g' "$BUILD_FILE"
sed -i 's|runtimeOnly("io.jsonwebtoken:jjwt-impl:[^"]*")|runtimeOnly(libs.jjwt.impl)|g' "$BUILD_FILE"
sed -i 's|runtimeOnly("io.jsonwebtoken:jjwt-jackson:[^"]*")|runtimeOnly(libs.jjwt.jackson)|g' "$BUILD_FILE"

echo "✅ Complete. Review changes and run: ./gradlew :modules:services:$(basename $SERVICE_PATH):build"

#!/bin/bash
# New Service Setup Workflow
# Scaffolds a new HDIM microservice with proper structure

set -e

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <service-name> <port>"
    echo "Example: $0 prescription-service 8091"
    exit 1
fi

SERVICE_NAME=$1
PORT=$2
PACKAGE_NAME=$(echo "$SERVICE_NAME" | sed 's/-//g')

echo "🚀 Creating new HDIM service: $SERVICE_NAME"
echo "   Port: $PORT"
echo "   Package: com.healthdata.$PACKAGE_NAME"
echo ""

SERVICE_DIR="backend/modules/services/$SERVICE_NAME"

# Create directory structure
echo "📁 Creating directory structure..."
mkdir -p "$SERVICE_DIR/src/main/java/com/healthdata/$PACKAGE_NAME"/{api/v1,application,domain/{model,repository},config,infrastructure}
mkdir -p "$SERVICE_DIR/src/main/resources"/{db/changelog,static,templates}
mkdir -p "$SERVICE_DIR/src/test/java/com/healthdata/$PACKAGE_NAME"

# Create build.gradle.kts
echo "📝 Creating build.gradle.kts..."
cat > "$SERVICE_DIR/build.gradle.kts" <<'EOF'
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // Shared modules
    implementation(project(":modules:shared:domain"))
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation(project(":modules:shared:api-contracts"))

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
EOF

# Create application.yml
echo "📝 Creating application.yml..."
cat > "$SERVICE_DIR/src/main/resources/application.yml" <<EOF
spring:
  application:
    name: $SERVICE_NAME

  datasource:
    url: jdbc:postgresql://\${POSTGRES_HOST:localhost}:\${POSTGRES_PORT:5435}/healthdata_\${PACKAGE_NAME}
    username: \${POSTGRES_USER:healthdata}
    password: \${POSTGRES_PASSWORD:password}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

  jpa:
    hibernate:
      ddl-auto: validate  # CRITICAL: Always validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes for PHI

  redis:
    host: \${REDIS_HOST:localhost}
    port: \${REDIS_PORT:6380}
    password: \${REDIS_PASSWORD:}

server:
  port: $PORT

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
EOF

# Create db.changelog-master.xml
echo "📝 Creating Liquibase master changelog..."
mkdir -p "$SERVICE_DIR/src/main/resources/db/changelog"
cat > "$SERVICE_DIR/src/main/resources/db/changelog/db.changelog-master.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <!-- Add migrations here -->
    <!-- <include file="db/changelog/0001-create-initial-tables.xml"/> -->
</databaseChangeLog>
EOF

# Create Main application class
echo "📝 Creating Spring Boot application class..."
SERVICE_CLASS=$(echo "$SERVICE_NAME" | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++)sub(/./,toupper(substr($i,1,1)),$i)}1' | sed 's/ //g')
cat > "$SERVICE_DIR/src/main/java/com/healthdata/$PACKAGE_NAME/${SERVICE_CLASS}Application.java" <<EOF
package com.healthdata.$PACKAGE_NAME;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ${SERVICE_CLASS}Application {
    public static void main(String[] args) {
        SpringApplication.run(${SERVICE_CLASS}Application.class, args);
    }
}
EOF

# Create Security Config
echo "📝 Creating security configuration..."
cat > "$SERVICE_DIR/src/main/java/com/healthdata/$PACKAGE_NAME/config/SecurityConfig.java" <<EOF
package com.healthdata.$PACKAGE_NAME.config;

import com.healthdata.authentication.security.TrustedHeaderAuthFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TrustedHeaderAuthFilter trustedHeaderAuthFilter;
    private final TrustedTenantAccessFilter trustedTenantAccessFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(trustedHeaderAuthFilter,
                UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(trustedTenantAccessFilter,
                TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
EOF

# Create validation test
echo "📝 Creating entity-migration validation test..."
cat > "$SERVICE_DIR/src/test/java/com/healthdata/$PACKAGE_NAME/EntityMigrationValidationTest.java" <<EOF
package com.healthdata.$PACKAGE_NAME;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.liquibase.enabled=true"
})
class EntityMigrationValidationTest {

    @Test
    void contextLoads() {
        // If this test passes, JPA entities match Liquibase migrations
        // If it fails, there's a schema mismatch
    }
}
EOF

# Update settings.gradle.kts
echo "📝 Updating settings.gradle.kts..."
if ! grep -q "include.*$SERVICE_NAME" backend/settings.gradle.kts; then
    echo "include(\":modules:services:$SERVICE_NAME\")" >> backend/settings.gradle.kts
fi

echo ""
echo "✅ Service scaffolding complete!"
echo ""
echo "📋 Next steps:"
echo "   1. Add service to docker-compose.yml"
echo "   2. Create initial migration in db/changelog/"
echo "   3. Define domain entities"
echo "   4. Implement API controllers"
echo "   5. Add service to .serena/SERVICE_INDEX.md"
echo ""
echo "🏗️  Build: cd backend && ./gradlew :modules:services:$SERVICE_NAME:build"
echo "▶️  Run: cd backend && ./gradlew :modules:services:$SERVICE_NAME:bootRun"
echo ""

# Third-Party Notices (Initial Inventory)

Scope
- This file tracks third-party software and licensed content used by this repository.
- It starts with direct dependencies and licensed content sources; expand it as part of release readiness.
- License verification steps live in docs/compliance/LICENSES/README.md.

## Licensed content and trademarks

NCQA / HEDIS
- HEDIS measure specifications and related NCQA content are licensed and cannot be redistributed without NCQA approval.
- Required notices/disclaimers apply for any licensed HEDIS use.
- References:
  - https://www.ncqa.org/hedis/using-hedis-measures/
  - https://wpcdn.ncqa.org/www-prod/wp-content/uploads/Notices-and-Disclaimers-for-Licensees-Products-%E2%80%93-HEDIS-Measure-Specifications.pdf
  - https://wpcdn.ncqa.org/www-prod/wp-content/uploads/Notices-and-Disclaimers-for-Licensees-Products-%E2%80%93-HEDIS-MLD.pdf
  - https://wpcdn.ncqa.org/www-prod/wp-content/uploads/Notices-and-Disclaimers-for-Licensees-Products-%E2%80%93-HEDIS-Research-Use-Only.pdf

HL7 CQL specification
- The HL7 CQL specification text is all-rights-reserved; use is governed by HL7 IP policy.
- Reference: https://cql.hl7.org/license.html

Third-party code systems referenced in NCQA notices
- CPT (AMA), CDT (ADA), UB codes (AHA), LOINC (Regenstrief), SNOMED CT (IHTSDO), UCUM (Regenstrief), RadLex (RSNA), UMLS (NLM), RxNorm (NLM).
- These code systems may require separate licenses or terms for commercial use.

## Direct dependency inventory (to be license-verified)

The sections below are auto-updated by `scripts/compliance/update-third-party-notices.py`.

### Frontend (npm) direct dependencies
<!-- BEGIN: FRONTEND_DEPENDENCIES -->
- @angular-devkit/build-angular ~20.3.0
- @angular/cdk ^20.2.13
- @angular/common ~20.3.0
- @angular/compiler ~20.3.0
- @angular/core ~20.3.0
- @angular/forms ~20.3.0
- @angular/material ^20.2.13
- @angular/platform-browser ~20.3.0
- @angular/platform-browser-dynamic ~20.3.0
- @angular/platform-server ~20.3.0
- @angular/router ~20.3.0
- @angular/ssr ~20.3.0
- @ngrx/effects ^20.1.0
- @ngrx/store ^20.1.0
- @swimlane/ngx-charts ^23.1.0
- @types/d3-scale-chromatic ^3.1.0
- @types/three ^0.181.0
- d3-color ^3.1.0
- d3-scale ^4.0.2
- d3-scale-chromatic ^3.1.0
- express ^4.21.2
- gsap ^3.13.0
- monaco-editor ^0.54.0
- ngx-monaco-editor-v2 ^20.3.0
- rxjs ~7.8.0
- three ^0.181.1
- zone.js ~0.15.0
<!-- END: FRONTEND_DEPENDENCIES -->

### Frontend (npm) dev dependencies
<!-- BEGIN: FRONTEND_DEV_DEPENDENCIES -->
- @angular-devkit/core ~20.3.0
- @angular-devkit/schematics ~20.3.0
- @angular/animations ~20.3.0
- @angular/build ~20.3.0
- @angular/cli ~20.3.0
- @angular/compiler-cli ~20.3.0
- @angular/language-service ~20.3.0
- @eslint/js ^9.8.0
- @nx/angular 22.0.2
- @nx/devkit 22.0.2
- @nx/eslint 22.0.2
- @nx/eslint-plugin 22.0.2
- @nx/jest 22.0.2
- @nx/js 22.0.2
- @nx/playwright 22.0.2
- @nx/web 22.0.2
- @nx/workspace 22.0.2
- @playwright/test ^1.36.0
- @schematics/angular ~20.3.0
- @swc-node/register ~1.9.1
- @swc/core ~1.5.7
- @swc/helpers ~0.5.11
- @types/d3-color ^3.1.3
- @types/d3-scale ^4.0.9
- @types/express ^4.17.21
- @types/jest ^29.5.12
- @types/node 18.16.9
- @typescript-eslint/utils ^8.40.0
- angular-eslint ^20.3.0
- eslint ^9.8.0
- eslint-config-prettier ^10.0.0
- eslint-plugin-playwright ^1.6.2
- jest ^29.7.0
- jest-environment-jsdom ^29.7.0
- jest-preset-angular ~14.6.1
- jest-util ^29.7.0
- nx 22.0.2
- prettier ^2.6.2
- ts-jest ^29.1.0
- ts-node 10.9.1
- tslib ^2.3.0
- typescript ~5.9.2
- typescript-eslint ^8.40.0
<!-- END: FRONTEND_DEV_DEPENDENCIES -->

### Backend (Gradle) libraries from version catalog
<!-- BEGIN: BACKEND_LIBRARIES -->
- ca.uhn.hapi.fhir:hapi-fhir-base 7.6.0
- ca.uhn.hapi.fhir:hapi-fhir-client 7.6.0
- ca.uhn.hapi.fhir:hapi-fhir-jpaserver-base 7.6.0
- ca.uhn.hapi.fhir:hapi-fhir-server 7.6.0
- ca.uhn.hapi.fhir:hapi-fhir-structures-r4 7.6.0
- ca.uhn.hapi.fhir:hapi-fhir-validation 7.6.0
- ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4 7.6.0
- ch.qos.logback:logback-classic 1.5.16
- ch.qos.logback:logback-core 1.5.16
- com.azure:azure-ai-openai 1.0.0-beta.10
- com.azure:azure-identity 1.12.2
- com.fasterxml.jackson.core:jackson-databind 2.17.2
- com.fasterxml.jackson.datatype:jackson-datatype-jsr310 2.17.2
- com.fasterxml.jackson.module:jackson-module-kotlin 2.17.2
- com.google.guava:guava 33.3.1-jre
- com.redis:testcontainers-redis 2.2.2
- com.zaxxer:HikariCP 6.0.0
- io.github.resilience4j:resilience4j-circuitbreaker 2.2.0
- io.github.resilience4j:resilience4j-ratelimiter 2.2.0
- io.github.resilience4j:resilience4j-reactor 2.2.0
- io.github.resilience4j:resilience4j-retry 2.2.0
- io.github.resilience4j:resilience4j-spring-boot3 2.2.0
- io.github.resilience4j:resilience4j-timelimiter 2.2.0
- io.hypersistence:hypersistence-utils-hibernate-63 3.9.0
- io.jsonwebtoken:jjwt-api 0.12.6
- io.jsonwebtoken:jjwt-impl 0.12.6
- io.jsonwebtoken:jjwt-jackson 0.12.6
- io.micrometer:micrometer-core 1.13.6
- io.micrometer:micrometer-registry-prometheus 1.13.6
- io.micrometer:micrometer-tracing 1.3.5
- io.micrometer:micrometer-tracing-bridge-otel 1.3.5
- io.opentelemetry:opentelemetry-exporter-otlp 1.32.0
- io.opentelemetry:opentelemetry-sdk 1.32.0
- io.projectreactor.netty:reactor-netty-http 1.2.4
- io.rest-assured:rest-assured 5.5.0
- io.swagger.core.v3:swagger-annotations 2.2.41
- org.apache.commons:commons-lang3 3.17.0
- org.apache.httpcomponents.client5:httpclient5 5.2.1
- org.apache.kafka:kafka-clients 3.8.0
- org.bouncycastle:bcprov-jdk18on 1.78.1
- org.eclipse.jetty:jetty-http 12.0.19
- org.eclipse.jetty:jetty-io 12.0.19
- org.eclipse.jetty:jetty-server 12.0.19
- org.junit.jupiter:junit-jupiter 5.14.1
- org.liquibase:liquibase-core 4.29.2
- org.mapstruct:mapstruct 1.6.2
- org.mapstruct:mapstruct-processor 1.6.2
- org.mockito:mockito-core 5.2.0
- org.mockito:mockito-inline 5.2.0
- org.mockito:mockito-junit-jupiter 5.2.0
- org.opencds.cqf.cql:engine 3.3.1
- org.opencds.cqf.cql:engine.fhir 3.3.1
- org.postgresql:postgresql 42.7.7
- org.projectlombok:lombok 1.18.34
- org.springdoc:springdoc-openapi-starter-webmvc-ui 2.6.0
- org.springframework.boot:spring-boot-starter-actuator 3.3.6
- org.springframework.boot:spring-boot-starter-data-jpa 3.3.6
- org.springframework.boot:spring-boot-starter-data-redis 3.3.6
- org.springframework.boot:spring-boot-starter-security 3.3.6
- org.springframework.boot:spring-boot-starter-test 3.3.6
- org.springframework.boot:spring-boot-starter-validation 3.3.6
- org.springframework.boot:spring-boot-starter-web 3.3.6
- org.springframework.cloud:spring-cloud-starter-config 2023.0.6
- org.springframework.cloud:spring-cloud-starter-netflix-eureka-client 2023.0.6
- org.springframework.cloud:spring-cloud-starter-openfeign
- org.springframework.cloud:spring-cloud-starter-vault-config
- org.springframework.data:spring-data-redis 3.5.7
- org.springframework.kafka:spring-kafka 3.3.11
- org.springframework.security:spring-security-config 6.5.7
- org.springframework.security:spring-security-core 6.5.7
- org.springframework.security:spring-security-web 6.5.7
- org.testcontainers:junit-jupiter 1.20.4
- org.testcontainers:kafka 1.20.4
- org.testcontainers:postgresql 1.20.4
- org.testcontainers:testcontainers 1.20.4
- software.amazon.awssdk:auth
- software.amazon.awssdk:bedrockruntime
- software.amazon.awssdk:bom 2.25.0
<!-- END: BACKEND_LIBRARIES -->

## License verification (direct dependencies)

<!-- BEGIN: LICENSE_VERIFICATION -->
### NPM direct dependencies
- @angular-devkit/build-angular ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/cdk ^20.2.13 (license: MIT; source: npm registry (package-lock version))
- @angular/common ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/compiler ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/core ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/forms ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/material ^20.2.13 (license: MIT; source: npm registry (package-lock version))
- @angular/platform-browser ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/platform-browser-dynamic ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/platform-server ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/router ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/ssr ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @ngrx/effects ^20.1.0 (license: MIT; source: npm registry (package-lock version))
- @ngrx/store ^20.1.0 (license: MIT; source: npm registry (package-lock version))
- @swimlane/ngx-charts ^23.1.0 (license: MIT; source: npm registry (package-lock version))
- @types/d3-scale-chromatic ^3.1.0 (license: MIT; source: npm registry (package-lock version))
- @types/three ^0.181.0 (license: MIT; source: npm registry (package-lock version))
- d3-color ^3.1.0 (license: ISC; source: npm registry (package-lock version))
- d3-scale ^4.0.2 (license: ISC; source: npm registry (package-lock version))
- d3-scale-chromatic ^3.1.0 (license: ISC; source: npm registry (package-lock version))
- express ^4.21.2 (license: MIT; source: npm registry (package-lock version))
- gsap ^3.13.0 (license: GSAP Standard License; source: npm registry (package-lock version))
- monaco-editor ^0.54.0 (license: MIT; source: npm registry (package-lock version))
- ngx-monaco-editor-v2 ^20.3.0 (license: MIT; source: npm registry (package-lock version))
- rxjs ~7.8.0 (license: Apache-2.0; source: npm registry (package-lock version))
- three ^0.181.1 (license: MIT; source: npm registry (package-lock version))
- zone.js ~0.15.0 (license: MIT; source: npm registry (package-lock version))

### NPM dev dependencies
- @angular-devkit/core ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular-devkit/schematics ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/animations ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/build ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/cli ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/compiler-cli ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @angular/language-service ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @eslint/js ^9.8.0 (license: MIT; source: npm registry (package-lock version))
- @nx/angular 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @nx/devkit 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @nx/eslint 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @nx/eslint-plugin 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @nx/jest 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @nx/js 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @nx/playwright 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @nx/web 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @nx/workspace 22.0.2 (license: MIT; source: npm registry (package-lock version))
- @playwright/test ^1.36.0 (license: Apache-2.0; source: npm registry (package-lock version))
- @schematics/angular ~20.3.0 (license: MIT; source: npm registry (package-lock version))
- @swc-node/register ~1.9.1 (license: MIT; source: npm registry (package-lock version))
- @swc/core ~1.5.7 (license: Apache-2.0; source: npm registry (package-lock version))
- @swc/helpers ~0.5.11 (license: Apache-2.0; source: npm registry (package-lock version))
- @types/d3-color ^3.1.3 (license: MIT; source: npm registry (package-lock version))
- @types/d3-scale ^4.0.9 (license: MIT; source: npm registry (package-lock version))
- @types/express ^4.17.21 (license: MIT; source: npm registry (package-lock version))
- @types/jest ^29.5.12 (license: MIT; source: npm registry (package-lock version))
- @types/node 18.16.9 (license: MIT; source: npm registry (package-lock version))
- @typescript-eslint/utils ^8.40.0 (license: MIT; source: npm registry (package-lock version))
- angular-eslint ^20.3.0 (license: MIT; source: npm registry (package-lock version))
- eslint ^9.8.0 (license: MIT; source: npm registry (package-lock version))
- eslint-config-prettier ^10.0.0 (license: MIT; source: npm registry (package-lock version))
- eslint-plugin-playwright ^1.6.2 (license: MIT; source: npm registry (package-lock version))
- jest ^29.7.0 (license: MIT; source: npm registry (package-lock version))
- jest-environment-jsdom ^29.7.0 (license: MIT; source: npm registry (package-lock version))
- jest-preset-angular ~14.6.1 (license: MIT; source: npm registry (package-lock version))
- jest-util ^29.7.0 (license: MIT; source: npm registry (package-lock version))
- nx 22.0.2 (license: MIT; source: npm registry (package-lock version))
- prettier ^2.6.2 (license: MIT; source: npm registry (package-lock version))
- ts-jest ^29.1.0 (license: MIT; source: npm registry (package-lock version))
- ts-node 10.9.1 (license: MIT; source: npm registry (package-lock version))
- tslib ^2.3.0 (license: 0BSD; source: npm registry (package-lock version))
- typescript ~5.9.2 (license: Apache-2.0; source: npm registry (package-lock version))
- typescript-eslint ^8.40.0 (license: MIT; source: npm registry (package-lock version))

### Maven/Gradle libraries
- ca.uhn.hapi.fhir:hapi-fhir-base 7.6.0 (license: Apache-2.0; source: Maven Central (pom))
- ca.uhn.hapi.fhir:hapi-fhir-client 7.6.0 (license: Apache-2.0; source: Maven Central (pom))
- ca.uhn.hapi.fhir:hapi-fhir-jpaserver-base 7.6.0 (license: Apache-2.0; source: Maven Central (pom))
- ca.uhn.hapi.fhir:hapi-fhir-server 7.6.0 (license: Apache-2.0; source: Maven Central (pom))
- ca.uhn.hapi.fhir:hapi-fhir-structures-r4 7.6.0 (license: Apache-2.0; source: Maven Central (pom))
- ca.uhn.hapi.fhir:hapi-fhir-validation 7.6.0 (license: Apache-2.0; source: Maven Central (pom))
- ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4 7.6.0 (license: Apache-2.0; source: Maven Central (pom))
- ch.qos.logback:logback-classic 1.5.16 (license: EPL-1.0; source: Maven Central (pom))
- ch.qos.logback:logback-core 1.5.16 (license: EPL-1.0; source: Maven Central (pom))
- com.azure:azure-ai-openai 1.0.0-beta.10 (license: MIT; source: Maven Central (pom))
- com.azure:azure-identity 1.12.2 (license: MIT; source: Maven Central (pom))
- com.fasterxml.jackson.core:jackson-databind 2.17.2 (license: Apache-2.0; source: Maven Central (pom))
- com.fasterxml.jackson.datatype:jackson-datatype-jsr310 2.17.2 (license: Apache-2.0; source: Maven Central (pom))
- com.fasterxml.jackson.module:jackson-module-kotlin 2.17.2 (license: Apache-2.0; source: Maven Central (pom))
- com.google.guava:guava 33.3.1-jre (license: Apache-2.0; source: Maven Central (pom))
- com.redis:testcontainers-redis 2.2.2 (license: Apache-2.0; source: Maven Central (pom))
- com.zaxxer:HikariCP 6.0.0 (license: Apache-2.0; source: Maven Central (pom))
- io.github.resilience4j:resilience4j-circuitbreaker 2.2.0 (license: Apache-2.0; source: Maven Central (pom))
- io.github.resilience4j:resilience4j-ratelimiter 2.2.0 (license: Apache-2.0; source: Maven Central (pom))
- io.github.resilience4j:resilience4j-reactor 2.2.0 (license: Apache-2.0; source: Maven Central (pom))
- io.github.resilience4j:resilience4j-retry 2.2.0 (license: Apache-2.0; source: Maven Central (pom))
- io.github.resilience4j:resilience4j-spring-boot3 2.2.0 (license: Apache-2.0; source: Maven Central (pom))
- io.github.resilience4j:resilience4j-timelimiter 2.2.0 (license: Apache-2.0; source: Maven Central (pom))
- io.hypersistence:hypersistence-utils-hibernate-63 3.9.0 (license: Apache-2.0; source: Maven Central (pom))
- io.jsonwebtoken:jjwt-api 0.12.6 (license: Apache-2.0; source: Maven Central (pom))
- io.jsonwebtoken:jjwt-impl 0.12.6 (license: Apache-2.0; source: Maven Central (pom))
- io.jsonwebtoken:jjwt-jackson 0.12.6 (license: Apache-2.0; source: Maven Central (pom))
- io.micrometer:micrometer-core 1.13.6 (license: Apache-2.0; source: Maven Central (pom))
- io.micrometer:micrometer-registry-prometheus 1.13.6 (license: Apache-2.0; source: Maven Central (pom))
- io.micrometer:micrometer-tracing 1.3.5 (license: Apache-2.0; source: Maven Central (pom))
- io.micrometer:micrometer-tracing-bridge-otel 1.3.5 (license: Apache-2.0; source: Maven Central (pom))
- io.opentelemetry:opentelemetry-exporter-otlp 1.32.0 (license: Apache-2.0; source: Maven Central (pom))
- io.opentelemetry:opentelemetry-sdk 1.32.0 (license: Apache-2.0; source: Maven Central (pom))
- io.projectreactor.netty:reactor-netty-http 1.2.4 (license: Apache-2.0; source: Maven Central (pom))
- io.rest-assured:rest-assured 5.5.0 (license: Apache-2.0; source: Maven Central (pom))
- io.swagger.core.v3:swagger-annotations 2.2.41 (license: Apache-2.0; source: Maven Central (pom))
- org.apache.commons:commons-lang3 3.17.0 (license: Apache-2.0; source: Maven Central (pom))
- org.apache.httpcomponents.client5:httpclient5 5.2.1 (license: Apache-2.0; source: Maven Central (pom))
- org.apache.kafka:kafka-clients 3.8.0 (license: Apache-2.0; source: Maven Central (pom))
- org.bouncycastle:bcprov-jdk18on 1.78.1 (license: Bouncy Castle License; source: Maven Central (pom))
- org.eclipse.jetty:jetty-http 12.0.19 (license: EPL-2.0; source: Maven Central (pom))
- org.eclipse.jetty:jetty-io 12.0.19 (license: EPL-2.0; source: Maven Central (pom))
- org.eclipse.jetty:jetty-server 12.0.19 (license: EPL-2.0; source: Maven Central (pom))
- org.junit.jupiter:junit-jupiter 5.14.1 (license: EPL-2.0; source: Maven Central (pom))
- org.liquibase:liquibase-core 4.29.2 (license: Apache-2.0; source: Maven Central (pom))
- org.mapstruct:mapstruct 1.6.2 (license: Apache-2.0; source: Maven Central (pom))
- org.mapstruct:mapstruct-processor 1.6.2 (license: Apache-2.0; source: Maven Central (pom))
- org.mockito:mockito-core 5.2.0 (license: MIT; source: Maven Central (pom))
- org.mockito:mockito-inline 5.2.0 (license: MIT; source: Maven Central (pom))
- org.mockito:mockito-junit-jupiter 5.2.0 (license: MIT; source: Maven Central (pom))
- org.opencds.cqf.cql:engine 2.4.0 (license: Apache-2.0; source: Maven Central (metadata); note: version resolved from metadata (requested 3.3.1))
- org.opencds.cqf.cql:engine.fhir 2.4.0 (license: Apache-2.0; source: Maven Central (metadata); note: version resolved from metadata (requested 3.3.1))
- org.postgresql:postgresql 42.7.7 (license: BSD-2-Clause; source: Maven Central (pom))
- org.projectlombok:lombok 1.18.34 (license: MIT; source: Maven Central (pom))
- org.springdoc:springdoc-openapi-starter-webmvc-ui 2.6.0 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.boot:spring-boot-starter-actuator 3.3.6 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.boot:spring-boot-starter-data-jpa 3.3.6 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.boot:spring-boot-starter-data-redis 3.3.6 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.boot:spring-boot-starter-security 3.3.6 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.boot:spring-boot-starter-test 3.3.6 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.boot:spring-boot-starter-validation 3.3.6 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.boot:spring-boot-starter-web 3.3.6 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.cloud:spring-cloud-starter-config 5.0.0 (license: Apache-2.0; source: Maven Central (metadata); note: version resolved from metadata (requested 2023.0.6))
- org.springframework.cloud:spring-cloud-starter-netflix-eureka-client 5.0.0 (license: Apache-2.0; source: Maven Central (metadata); note: version resolved from metadata (requested 2023.0.6))
- org.springframework.cloud:spring-cloud-starter-openfeign 5.0.0 (license: Apache-2.0; source: Maven Central (metadata); note: version resolved from metadata (requested 2023.0.6))
- org.springframework.cloud:spring-cloud-starter-vault-config 5.0.0 (license: Apache-2.0; source: Maven Central (metadata); note: version resolved from metadata (requested 2023.0.6))
- org.springframework.data:spring-data-redis 3.5.7 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.kafka:spring-kafka 3.3.11 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.security:spring-security-config 6.5.7 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.security:spring-security-core 6.5.7 (license: Apache-2.0; source: Maven Central (pom))
- org.springframework.security:spring-security-web 6.5.7 (license: Apache-2.0; source: Maven Central (pom))
- org.testcontainers:junit-jupiter 1.20.4 (license: MIT; source: Maven Central (pom))
- org.testcontainers:kafka 1.20.4 (license: MIT; source: Maven Central (pom))
- org.testcontainers:postgresql 1.20.4 (license: MIT; source: Maven Central (pom))
- org.testcontainers:testcontainers 1.20.4 (license: MIT; source: Maven Central (pom))
- software.amazon.awssdk:auth 2.25.0 (license: Apache-2.0; source: Maven Central (pom); note: version from aws-sdk BOM)
- software.amazon.awssdk:bedrockruntime 2.25.0 (license: Apache-2.0; source: Maven Central (pom); note: version from aws-sdk BOM)
- software.amazon.awssdk:bom 2.25.0 (license: Apache-2.0; source: Maven Central (pom))
<!-- END: LICENSE_VERIFICATION -->

License texts for common OSS licenses used in this repo are stored in `docs/compliance/LICENSES/`.

## License risk register
- GSAP Standard License (non-OSI; commercial restrictions may apply) — verify usage terms before distribution.
- NCQA/HEDIS content is licensed and cannot be redistributed without NCQA approval; see boundary doc.
- HL7 CQL specification text is all-rights-reserved and may not be reproduced without permission.
- `com.alphora:cql-evaluator-fhir` removed from dependency catalog; no license tracking required.
- Spring Cloud starters resolved via Maven metadata (release train vs. artifact version); confirm against the BOM in production builds.

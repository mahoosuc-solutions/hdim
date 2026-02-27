# CVE Remediation Wave 1

**Date:** 2026-02-27
**Source:** `backend/build/reports/dependency-check-report.json`

## Summary

- Packages with vulnerabilities: **88**
- Vulnerability occurrences: **228**
- Findings with CVSS >= 9.0: **7**

## Priority Buckets

### P0 (CVSS >= 9.0)

| Package | Max CVSS | CVE Count | Example CVEs | Project Refs |
|---|---:|---:|---|---|
| `pkg:maven/org.xerial/sqlite-jdbc@3.45.3.0` | 9.8 | 3 | CVE-2025-29087, CVE-2025-3277, CVE-2025-6965 | agent-runtime-service:compileClasspath, agent-runtime-service:productionRuntimeClasspath, audit:compileClasspath |
| `pkg:maven/org.quartz-scheduler/quartz@2.3.2` | 9.8 | 1 | CVE-2023-39017 | fhir-service:compileClasspath, fhir-service:productionRuntimeClasspath, sales-automation-service:compileClasspath |
| `pkg:maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.40` | 9.6 | 11 | CVE-2025-46701, CVE-2025-48988, CVE-2025-48989, CVE-2025-49124 | auth:compileClasspath, auth:runtimeClasspath, fhir-api:compileClasspath |
| `pkg:maven/org.apache.tomcat.embed/tomcat-embed-websocket@10.1.40` | 9.6 | 11 | CVE-2025-46701, CVE-2025-48988, CVE-2025-48989, CVE-2025-49124 | auth:compileClasspath, auth:runtimeClasspath, fhir-api:compileClasspath |
| `pkg:maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.42` | 9.6 | 6 | CVE-2025-48989, CVE-2025-52520, CVE-2025-53506, CVE-2025-55752 | agent-builder-service:compileClasspath, agent-builder-service:productionRuntimeClasspath, agent-runtime-service:compileClasspath |
| `pkg:maven/org.apache.tomcat.embed/tomcat-embed-websocket@10.1.42` | 9.6 | 6 | CVE-2025-48989, CVE-2025-52520, CVE-2025-53506, CVE-2025-55752 | agent-builder-service:compileClasspath, agent-builder-service:productionRuntimeClasspath, agent-runtime-service:compileClasspath |

### P1 (7.0 <= CVSS < 9.0)

| Package | Max CVSS | CVE Count | Example CVEs |
|---|---:|---:|---|
| `pkg:maven/org.apache.kafka/kafka-clients@3.7.1` | 8.8 | 3 | CVE-2024-56128, CVE-2025-27817, CVE-2025-27818 |
| `pkg:maven/com.azure/azure-identity@1.12.2` | 8.8 | 2 | CVE-2023-36415, CVE-2024-35255 |
| `pkg:maven/org.apache.jena/jena-arq@4.9.0` | 8.8 | 2 | CVE-2025-49656, CVE-2025-50151 |
| `pkg:maven/org.apache.jena/jena-base@4.9.0` | 8.8 | 2 | CVE-2025-49656, CVE-2025-50151 |
| `pkg:maven/org.apache.jena/jena-core@4.9.0` | 8.8 | 2 | CVE-2025-49656, CVE-2025-50151 |
| `pkg:maven/org.apache.jena/jena-iri@4.9.0` | 8.8 | 2 | CVE-2025-49656, CVE-2025-50151 |
| `pkg:maven/org.apache.kafka/kafka-clients@3.7.2` | 8.8 | 2 | CVE-2025-27817, CVE-2025-27818 |
| `pkg:maven/commons-beanutils/commons-beanutils@1.9.4` | 8.8 | 1 | CVE-2025-48734 |
| `pkg:maven/org.springframework/spring-aop@6.0.11` | 8.1 | 3 | CVE-2023-34053, CVE-2024-22259, CVE-2024-38820 |
| `pkg:maven/org.springframework/spring-beans@6.0.11` | 8.1 | 3 | CVE-2023-34053, CVE-2024-22259, CVE-2024-38820 |
| `pkg:maven/org.springframework/spring-context@6.0.11` | 8.1 | 3 | CVE-2023-34053, CVE-2024-22259, CVE-2024-38820 |
| `pkg:maven/org.springframework/spring-core@6.0.11` | 8.1 | 3 | CVE-2023-34053, CVE-2024-22259, CVE-2024-38820 |
| `pkg:maven/org.springframework/spring-expression@6.0.11` | 8.1 | 3 | CVE-2023-34053, CVE-2024-22259, CVE-2024-38820 |
| `pkg:maven/org.springframework/spring-jcl@6.0.11` | 8.1 | 3 | CVE-2023-34053, CVE-2024-22259, CVE-2024-38820 |
| `pkg:maven/org.springframework/spring-web@6.0.11` | 8.1 | 3 | CVE-2023-34053, CVE-2024-22259, CVE-2024-38820 |
| `pkg:maven/org.springframework/spring-webmvc@6.0.11` | 8.1 | 3 | CVE-2023-34053, CVE-2024-22259, CVE-2024-38820 |
| `pkg:maven/co.elastic.clients/elasticsearch-java@8.13.4` | 7.5 | 9 | CVE-2024-23445, CVE-2024-37280, CVE-2024-52979, CVE-2024-52980 |
| `pkg:maven/org.elasticsearch.client/elasticsearch-rest-client-sniffer@8.13.4` | 7.5 | 9 | CVE-2024-23445, CVE-2024-37280, CVE-2024-52979, CVE-2024-52980 |
| `pkg:maven/org.elasticsearch.client/elasticsearch-rest-client@8.13.4` | 7.5 | 9 | CVE-2024-23445, CVE-2024-37280, CVE-2024-52979, CVE-2024-52980 |
| `pkg:maven/io.netty/netty-buffer@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-codec-dns@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-codec-http2@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-codec-http@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-codec-socks@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-codec@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-common@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-handler-proxy@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-handler@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-resolver-dns-classes-macos@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |
| `pkg:maven/io.netty/netty-resolver-dns-native-macos@4.1.122.Final` | 7.5 | 4 | CVE-2025-55163, CVE-2025-58056, CVE-2025-58057, CVE-2025-67735 |

## Recommended Execution Order

1. Spring/Tomcat stack remediation (critical web stack CVEs).
2. Quartz upgrade/removal for schedulers and transitive chains.
3. SQLite/Tika/PlantUML tooling dependencies remediation.
4. gRPC/Kafka/Netty stream stack updates and regression tests.
5. iText and document-processing dependency upgrades.

## Notes

- Run `./gradlew dependencyCheckAggregate --no-daemon` after each wave.
- Keep `scripts/validation/validate-compliance-evidence-gate.sh` in strict mode for go/no-go.

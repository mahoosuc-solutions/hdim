# Version Manifest

This file captures the pinned runtime/tooling versions referenced by Docker Compose and the core platform dependency versions defined in Gradle version catalogs.

## Container Images (Infrastructure and Tooling)

- PostgreSQL: `postgres:16-alpine`
- Redis: `redis:7-alpine`
- Kafka: `confluentinc/cp-kafka:7.5.0`
- Zookeeper: `confluentinc/cp-zookeeper:7.5.0`
- Jaeger: `jaegertracing/all-in-one:1.53`
- OpenTelemetry Collector: `otel/opentelemetry-collector-contrib:0.91.0`
- Prometheus: `prom/prometheus:v2.48.0`
- Grafana: `grafana/grafana:10.2.3`
- HAPI FHIR: `hapiproject/hapi:v7.0.2`
- Nginx: `nginx:1.27-alpine`
- Vault: `hashicorp/vault:1.15`
- PgBouncer: `edoburu/pgbouncer:1.21.0`
- pgAdmin: `dpage/pgadmin4:8.10`
- Redis Commander: `rediscommander/redis-commander:0.8.0`
- Synthea: `synthetichealth/synthea:3.1.1`
- curl (utility image): `curlimages/curl:8.10.1`

## Core Platform Dependencies (Gradle)

- Java: `21`
- Kotlin: `2.0.21`
- Spring Boot: `3.3.6`
- Spring Cloud: `2023.0.6`
- Spring Security: `6.5.7`
- Kafka clients: `3.8.0`
- Spring Kafka: `3.3.11`
- PostgreSQL JDBC: `42.7.7`
- HikariCP: `6.0.0`
- Liquibase: `4.29.2`

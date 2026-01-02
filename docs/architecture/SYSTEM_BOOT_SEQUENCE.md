# System Boot Sequence & Dependency Plan

This document captures the order and prerequisites for bringing the Health Data In Motion platform online in a concurrent, high-volume environment.

## 1. Foundational Infrastructure

1. **Secrets / Configuration**
   - Provision Vault or secrets manager entries for database credentials, JWT signing keys, TLS certificates, and Kafka credentials.
   - Publish `.env` or Kubernetes `Secret` references consumed by downstream services.

2. **Networking & Observability**
   - Create the VPC / cluster network, service mesh (mTLS), and shared observability stack (Prometheus, Grafana, Loki/ELK, Jaeger).

## 2. Data Stores

1. **PostgreSQL Cluster**
   - Create logical databases / schemas: `healthdata_fhir`, `healthdata_consent`, `healthdata_patient`, `healthdata_metrics`.
   - Apply migrations (Flyway) to each service schema.
   - Configure read replicas for analytics workloads.

2. **Redis Cluster**
   - Bring up Redis with persistence (AOF) and clustering enabled.
   - Seed initial caches (e.g., measure definitions) if required.

3. **Object Storage (optional)**
   - Provision S3/GCS buckets for bulk FHIR exports and analytics snapshots.

## 3. Messaging Fabric

1. **Kafka Brokers & Schema Registry**
   - Start Zookeeper/KRaft (managed or self-hosted) and Kafka brokers.
   - Configure topics with baseline partitions (e.g., 12 partitions for `healthdata.resources`, `healthdata.consents`).
   - Register Avro/JSON schemas if governed.

2. **Dead Letter Queues**
   - Create DLQ topics for each critical stream (e.g., `healthdata.resources.dlq`).

## 4. Core Microservices (Spring Boot)

Start services in dependency order (all in Kubernetes / Compose with health checks):

1. **Security/Audit shared services** (if externalized).
2. **Patient Service** (baseline identity store) – depends on PostgreSQL.
3. **FHIR Service** – depends on Postgres + Redis + Kafka.
4. **Consent Service** – depends on Postgres + Kafka.
5. **CQL Engine Service** – depends on FHIR + Redis + Postgres.
6. **Event Processing Service** – depends on Kafka, FHIR, CQL.
7. **Quality Measure & Care Gap Services** – depend on the above for orchestration.
8. **Analytics Service** – depends on Kafka streams and Postgres replicas.

Ensure each service registers with service discovery / mesh, exposes Prometheus metrics, and supports readiness/liveness probes.

## 5. Gateway & External Edge

1. **Kong API Gateway**
   - Load declarative config for routes, auth, rate limiting.
   - Attach to service mesh if applicable.
2. **Identity Provider Integration**
   - Configure OAuth2/OpenID Connect clients for end-users and service-to-service tokens.

## 6. Frontend & Tooling

1. **Admin Portal**
   - Deploy Angular static assets (from `/opt/healthdata/ui`).
   - Point to gateway endpoints via environment variables.

2. **Developer Tools**
   - Enable API playground presets referencing the gateway base URL.
   - Publish documentation portals (Swagger UI, AsyncAPI consoles).

## 7. Validation & Monitoring

- Run smoke tests and contract tests across services.
- Monitor dashboards for latency, Kafka lag, DB health.
- Configure alerting for SLO breaches and DLQ growth.

This sequence ensures dependencies are satisfied as each tier comes online, enabling horizontal scaling and high-volume throughput with predictable service startup.

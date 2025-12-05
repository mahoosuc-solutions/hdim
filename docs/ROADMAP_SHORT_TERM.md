# Health Data In Motion – Short-Term Roadmap

_Updated: October 2025_

This document breaks down the immediate milestones required to unblock vertical feature delivery. Each milestone should be completed with accompanying automated tests and documentation updates.

## 1. Foundation Hardening (Weeks 1-2)

- **Shared Infrastructure Modules**
  - Implement security, persistence, cache, and messaging modules currently defined in Gradle but lacking source code.
  - Expose reusable Spring Boot auto-configuration where it simplifies adoption in downstream services.
  - Add unit tests covering encryption helpers, JWT utilities, repository factories, and Kafka/Redis configuration.
- **Database & Messaging Baseline**
  - Define PostgreSQL schemas using migration tooling (Liquibase or Flyway) for the patient, audit, and consent domains.
  - Document Redis key conventions and Kafka topic contracts; commit seed data or scripts for local development.
- **Test Fixtures & Tooling**
  - Provide shared Testcontainers configuration for PostgreSQL, Redis, and Kafka.
  - Publish reusable integration testing utilities (e.g., RestAssured base client, Kafka event helpers).
- **Quality Gates**
  - Ensure `nx lint`, `nx test shared`, and `./gradlew test` run cleanly in CI after the module implementations.

## 2. FHIR Vertical Slice (Weeks 3-5)

- **Patient CRUD Implementation**
  - Introduce controllers, service layer, repositories, and mappers inside `fhir-service` to support create, read, update, delete, and search operations for Patient resources.
  - Persist audit metadata using the shared persistence module and emit Kafka notifications for create/update actions.
- **Data Validation & Caching**
  - Leverage the shared FHIR validation utilities to reject invalid payloads; add Redis-backed caching for patient reads.
- **API Contracts & Tests**
  - Produce OpenAPI/contract definitions that align with admin portal expectations.
  - Add unit, integration, and contract tests (Spring MockMvc + Testcontainers) covering the new endpoints and Kafka publishing.
- **Admin Portal Integration Readiness**
  - Provide stubbed aggregation endpoints (or a lightweight BFF) to support dashboard, service catalog, and health snapshots fed from live data.
  - Update fallback fixtures so they mirror the live contract shape for graceful degradation.
- **Acceptance Criteria**
  - Running `nx serve admin-portal` against the local backend surfaces real patient data without manual data seeding.
  - CI executes backend and frontend test suites with the new functionality enabled.


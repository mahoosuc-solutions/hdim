# Test Coverage Improvement Design — Low-Coverage Services

Date: March 5, 2026
Status: Complete

## Problem

9 Java services were identified with only 1-2 test files. After investigation, 4 of them (event handler libraries) already have 31-39 tests in single files with comprehensive coverage. The remaining 5 services have genuine gaps.

## Services Assessed

### Tier A — Already Well-Tested (No Action Needed)

| Service | Tests | Coverage |
|---------|-------|----------|
| care-gap-event-handler-service | 31 tests | All 4 event handlers, multi-tenant, idempotency |
| clinical-workflow-event-handler-service | 33 tests | All 7 event handlers, state transitions |
| patient-event-handler-service | 39+ tests | All 5 handlers + merge handler |
| quality-measure-event-handler-service | 39 tests | All 5 handlers, score thresholds, cohort aggregation |

### Tier B — New Tests Written

| Service | Existing | New Tests | What Was Missing |
|---------|----------|-----------|-----------------|
| admin-service | 17 integration | AlertConfigServiceTest (unit) | Service layer logic: partial updates, markTriggered, not-found errors |
| clinical-workflow-event-service | 24 unit | WorkflowProjectionControllerTest | Controller layer: 11 REST endpoints, 404 handling, stats |
| devops-agent-service | 14 unit | FhirValidationControllerTest + FhirServiceClientTest | Controller delegation, HTTP client error handling |
| gateway-fhir-service | 11 unit | GatewayFhirRoutingEdgeCaseTest | Error propagation, non-2xx passthrough, missing route coverage |
| fhir-event-bridge-service | 6 unit | KafkaConfigTest | Topic configuration, consumer factory, ack mode |

## Test Design Principles

1. **Unit tests only** (`@Tag("unit")`) — no Spring context boot, no Docker dependencies
2. **Mock-based** — `@ExtendWith(MockitoExtension.class)` or standalone MockMvc
3. **Follow HDIM patterns** — `@Nested` organization, AssertJ assertions, Mockito verification
4. **Focus on business logic** — not boilerplate; test what can actually break
5. **Multi-tenant isolation** — verify tenant ID filtering where applicable

## Non-Service Directories

| Directory | What It Is | Action |
|-----------|-----------|--------|
| `backend/modules/services/bin/` | Gradle wrapper output | No tests needed |
| `backend/modules/services/build/` | Gradle build artifacts | No tests needed |

## Test Verification Results

| Service | New Tests | Verified |
|---------|-----------|----------|
| gateway-fhir-service | 9 (GatewayFhirRoutingEdgeCaseTest) | 9/9 passing |
| clinical-workflow-event-service | 12 (WorkflowProjectionControllerTest) | 12/12 passing |
| admin-service | 22 (AlertConfigServiceTest) | Written; not compilable (B7: unregistered in settings.gradle.kts) |
| devops-agent-service | 13 (FhirValidationControllerTest + FhirServiceClientTest) | Written; not compilable (B7: unregistered) |
| fhir-event-bridge-service | 10 (KafkaConfigTest) | Written; not compilable (B7: unregistered) |

### Fixes Applied During Verification

1. **WorkflowProjectionControllerTest** — Added `PageableHandlerMethodArgumentResolver` to standalone MockMvc setup (required for `Pageable` parameter resolution outside `@WebMvcTest`)
2. **WorkflowProjectionControllerTest** — Changed JSON path `$[0].overdue` → `$[0].isOverdue` (Lombok `@Data` Boolean field serialization)

## Test Count Impact

| Metric | Before | After |
|--------|--------|-------|
| Services with 1-2 test files | 9 | 4 (only the well-tested single-file event handlers) |
| New test files | 0 | 6 |
| New test methods | 0 | 66 |
| Verified passing | — | 21/21 (2 registered services) |
| Pending B7 registration | — | 45 (3 unregistered services) |

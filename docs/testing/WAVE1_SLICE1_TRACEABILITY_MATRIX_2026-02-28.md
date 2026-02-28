# Wave-1 Slice-1 Traceability Matrix (2026-02-28)

This matrix maps locked contract requirements from GitHub issues `#506` and `#507` to implemented code, current tests, and remaining gaps.

## Evidence Run Summary

- Revenue contract controller tests:
  - Command: `./gradlew :modules:services:payer-workflows-service:test --tests com.healthdata.payer.controller.RevenueContractControllerTest`
  - Result: `5 passed, 0 failed`
- Revenue contract service tests:
  - Command: `./gradlew :modules:services:payer-workflows-service:test --tests com.healthdata.payer.service.RevenueContractServiceTest`
  - Result: `2 passed, 0 failed`
- ADT interoperability controller tests:
  - Command: `./gradlew :modules:services:data-ingestion-service:test --tests com.healthdata.ingestion.api.v1.AdtInteroperabilityControllerTest`
  - Result: `6 passed, 0 failed`

## Issue #506: Revenue Cycle Backbone

| Requirement | Implementation | Verification | Status |
|---|---|---|---|
| `POST /api/v1/revenue/claims/submissions` | `RevenueContractController.submitClaim` | `RevenueContractControllerTest.shouldSubmitClaim` | Implemented |
| `POST /api/v1/revenue/eligibility/checks` | `RevenueContractController.checkEligibility` | `RevenueContractControllerTest.shouldCheckEligibility` | Implemented |
| `POST /api/v1/revenue/claim-status/checks` | `RevenueContractController.checkClaimStatus` | `RevenueContractControllerTest.shouldReturnClaimStatus` | Implemented |
| Core DTO scaffolding | `ClaimSubmission*`, `EligibilityCheck*`, `ClaimStatus*`, `RevenueAuditEnvelope` | Compile + controller tests | Implemented |
| Error taxonomy scaffold | `RevenueErrorCode` enum | Compile-time contract presence | Implemented |
| Idempotency/replay scaffold | `RevenueContractService.claimSubmissionsByIdempotencyKey` | Service test coverage + controller contract tests | Implemented |
| Audit trail scaffold | `RevenueAuditEnvelope`, `/api/v1/revenue/audit/{correlationId}` | `RevenueContractControllerTest.shouldReturnAuditTrail` | Implemented |
| Remittance event contract object | `RemittanceAdviceEvent`, `ReconciliationPreviewResponse`, `/api/v1/revenue/remittance/advice` | `RevenueContractControllerTest.shouldReturnReconciliationPreview`, `RevenueContractServiceTest.*` | Implemented |
| Adapter timeout/retry/backoff behavior | (Not yet added) | N/A | Gap |

## Issue #507: HIE/ADT Exchange Backbone

| Requirement | Implementation | Verification | Status |
|---|---|---|---|
| `POST /api/v1/interoperability/adt/messages` | `AdtInteroperabilityController.ingestMessage` | `AdtInteroperabilityControllerTest.shouldIngestAdtMessage` | Implemented |
| `POST /api/v1/interoperability/adt/acks` | `AdtInteroperabilityController.acknowledge` | `AdtInteroperabilityControllerTest.shouldAcknowledgeEvent` | Implemented |
| `GET /api/v1/interoperability/adt/events/{eventId}` | `AdtInteroperabilityController.getEvent` | `AdtInteroperabilityControllerTest.shouldAcknowledgeEvent` (readback assertion) | Implemented |
| Core DTO scaffolding | `AdtMessageIngest*`, `AdtAcknowledgement*`, `EncounterEventRecord`, `InteroperabilityAuditEnvelope` | Compile + controller tests | Implemented |
| Error taxonomy scaffold | `InteroperabilityErrorCode` enum | `shouldRejectUnsupportedEventType` | Implemented |
| Idempotency/replay scaffold | `eventIdBySourceMessageId` map in `AdtExchangeService` | `shouldSuppressDuplicateMessageReplay` | Implemented |
| Allowlist/source authz scaffold | `SOURCE_ALLOWLIST` in `AdtExchangeService` | `shouldRejectUnauthorizedSourceSystem` | Implemented |
| Deterministic patient-match failure path | `isUnmatchedPatient` in `AdtExchangeService` | `shouldRejectUnmatchedPatient` | Implemented |
| Downstream routing failure handling (`PARTIAL_ROUTE_FAILURE`) | enum + placeholder only | N/A | Gap |

## Code Artifacts Added

- Revenue slice scaffold:
  - `backend/modules/services/payer-workflows-service/src/main/java/com/healthdata/payer/controller/RevenueContractController.java`
  - `backend/modules/services/payer-workflows-service/src/main/java/com/healthdata/payer/service/RevenueContractService.java`
  - `backend/modules/services/payer-workflows-service/src/main/java/com/healthdata/payer/revenue/RevenueClaimState.java`
  - `backend/modules/services/payer-workflows-service/src/main/java/com/healthdata/payer/revenue/RevenueErrorCode.java`
  - `backend/modules/services/payer-workflows-service/src/main/java/com/healthdata/payer/revenue/dto/*`
  - `backend/modules/services/payer-workflows-service/src/test/java/com/healthdata/payer/controller/RevenueContractControllerTest.java`
- ADT slice scaffold:
  - `backend/modules/services/data-ingestion-service/src/main/java/com/healthdata/ingestion/api/v1/AdtInteroperabilityController.java`
  - `backend/modules/services/data-ingestion-service/src/main/java/com/healthdata/ingestion/application/AdtExchangeService.java`
  - `backend/modules/services/data-ingestion-service/src/main/java/com/healthdata/ingestion/interoperability/AdtEventState.java`
  - `backend/modules/services/data-ingestion-service/src/main/java/com/healthdata/ingestion/interoperability/InteroperabilityErrorCode.java`
  - `backend/modules/services/data-ingestion-service/src/main/java/com/healthdata/ingestion/interoperability/dto/*`
  - `backend/modules/services/data-ingestion-service/src/test/java/com/healthdata/ingestion/api/v1/AdtInteroperabilityControllerTest.java`

## Residual Risk / Next TDD Increments

1. Add dedicated service-level tests for revenue idempotency replay, illegal transitions, and audit append order.
2. Add state transition guard tests for illegal financial transitions (e.g., `PAID` to `PARTIALLY_PAID`).
3. Add integration tests with adapter stubs for retry/backoff and upstream timeout taxonomy.

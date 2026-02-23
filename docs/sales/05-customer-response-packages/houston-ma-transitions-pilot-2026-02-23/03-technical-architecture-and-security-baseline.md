# Technical Architecture and Security Baseline

Prepared: 2026-02-23

## 1) Reference Architecture (Phase 1)

1. Source systems provide discharge and patient context through controlled interfaces.
2. HDIM orchestrates workflow and interaction state for pilot operations.
3. AI assistant receives minimum necessary context per request and returns patient-safe explanations.
4. Clinician escalations route to secure operational channels.
5. Audit and observability pipeline records events, access, and response SLIs.

## 2) Data Handling Model

Design principle: minimum necessary PHI with no unnecessary persistence.

1. No long-term PHI retention in AI provider logs.
2. No AI training on customer data.
3. Request-scoped context windows with strict truncation/redaction.
4. Persist only operational metadata required for care operations and compliance.

## 3) Security Controls

1. Tenant isolation on all API calls using `X-Tenant-ID` and auth claims.
2. Encryption in transit (TLS 1.2+) and at rest for approved operational stores.
3. Role-based access controls for care teams and operations staff.
4. Immutable audit trails for data access, AI interactions, and escalations.
5. PHI-safe logging policy with redaction.
6. Incident response playbook with notification timelines.

## 4) Secure Communication Paths

Primary path:

1. In-platform secure clinician workflow and escalation queues.

Fallback/adjacent path:

1. HIPAA-secure email push alerts for escalation notifications.
2. Email content constrained to minimum necessary and no full chart payloads.

## 5) MyChart Read/Write Roadmap

Phase 1:

1. Read-oriented context pull and patient guidance support.

Phase 2 (post-pilot):

1. Controlled write-back (messages/tasks) with explicit consent, policy checks, and full auditability.

## 6) Deployment Topologies Supported

Phase 1 architecture supports:

1. HDIM-hosted deployment.
2. Customer-hosted cloud deployment.
3. On-prem/private data center deployment (including air-gapped variant).

Topology-specific controls are documented in `07-deployment-models-hosted-vs-onprem.md` and finalized at pilot kickoff.

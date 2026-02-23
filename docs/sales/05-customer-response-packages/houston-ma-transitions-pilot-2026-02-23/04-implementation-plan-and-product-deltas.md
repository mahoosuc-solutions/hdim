# Implementation Plan and Product Deltas

Prepared: 2026-02-23

## 1) What Has Already Been Implemented

1. Patient engagement domain entities, repositories, service, and controller in `nurse-workflow-service`.
2. Patient engagement persistence migration: `0005-create-patient-engagement-tables.xml`.
3. Gateway routing for `/nurse-workflow/**`.
4. Clinical portal and gateway-edge route support for `/nurse-workflow/`.
5. Pilot overlay update to include `nurse-workflow-service` and `ai-assistant-service`.
6. AI service/controller changes to require tenant header and tenant-aware methods.

## 2) Remaining Build Work for Pilot Completion

1. Add patient-facing portal workflow for discharge summary and Q&A.
2. Implement secure email escalation adapter and policy filters.
3. Add metric aggregation endpoints and dashboard widgets for pilot KPIs.
4. Add configuration flags for zero-retention AI mode and redaction profiles.
5. Add integration adapters for the selected pilot data feed.
6. Complete load and security validation for pilot traffic assumptions.

## 3) Workstreams

1. Clinical Workflow
   - Discharge thread creation
   - Guided question templates
   - Escalation state machine

2. AI and Safety
   - Prompt policy for plain-language output
   - Grounding and confidence signals
   - Guardrails and escalation triggers

3. Integrations
   - Discharge feed ingestion
   - Notification pathways
   - Future MyChart integration prep

4. Security and Compliance
   - Data minimization enforcement
   - Audit events and evidence exports
   - Access control and tenant isolation checks

5. Measurement and Operations
   - Pilot KPI instrumentation
   - Weekly reporting pipeline
   - Incident and support runbooks

## 4) Validation Gates

1. Gate A (end of Week 2): architecture and security sign-off.
2. Gate B (end of Week 7): UAT pass and pilot readiness.
3. Gate C (end of Week 13): outcomes review and scale decision.

## 5) Deployment-Specific Implementation Deltas

1. HDIM-hosted
   - Standard deployment path with lowest setup overhead.
   - Baseline pilot timeline assumptions apply.

2. Customer-hosted cloud
   - Add customer account bootstrap, IAM alignment, and network controls.
   - Add environment observability handoff artifacts for customer operations.

3. On-prem/private data center
   - Add installation packaging for customer-controlled runtime.
   - Add local registry/image transfer workflow and offline validation steps.
   - Add runbooks for restricted outbound dependencies.

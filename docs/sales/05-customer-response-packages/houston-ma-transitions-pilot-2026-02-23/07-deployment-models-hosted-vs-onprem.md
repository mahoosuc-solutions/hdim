# Deployment Models: Hosted vs On-Prem

Prepared: 2026-02-23

## 1) Deployment Options

### Option 1: HDIM-Hosted (Recommended for Pilot Speed)

1. HDIM runs the platform in a dedicated hosted environment.
2. Customer systems integrate through secure APIs and private connectivity options.
3. Fastest timeline to production pilot start.

Best fit:

1. Need to launch within 90 days with lowest internal IT burden.
2. Prefer managed operations by vendor team.

### Option 2: Customer-Hosted Cloud (Customer Subscription, HDIM Managed)

1. Platform is deployed into customer-owned cloud subscription/account.
2. HDIM provides deployment automation and managed operations.
3. Customer retains infrastructure ownership and native cloud controls.

Best fit:

1. Customer requires cloud account ownership and direct infrastructure visibility.
2. Security/compliance teams require tenant-specific control planes.

### Option 3: On-Prem / Private Data Center (Including Air-Gapped Variant)

1. Platform is deployed in customer data center or private virtualized environment.
2. Supports restricted outbound connectivity and offline patch workflows.
3. Optional air-gapped operating mode with controlled update bundles.

Best fit:

1. Strict data locality and network isolation requirements.
2. Organizational policy against hosted PHI processing.

## 2) PHI and Control Boundaries

All options follow minimum necessary and zero-retention AI patterns for Phase 1.

1. Source-of-truth PHI remains in payer/provider systems and approved operational stores.
2. AI requests are request-scoped with redaction and no training on customer data.
3. Audit and access telemetry is retained per contractual retention policy.

Operational ownership by model:

1. HDIM-hosted: HDIM owns platform operations and shared controls.
2. Customer-hosted cloud: customer owns cloud account; HDIM operates platform layer.
3. On-prem: customer owns infrastructure/network perimeter; HDIM operates application layer under joint runbook.

## 3) Shared Responsibility Matrix

| Control Area | HDIM-Hosted | Customer-Hosted Cloud | On-Prem / Air-Gapped |
|---|---|---|---|
| Physical/Datacenter security | HDIM | Customer cloud provider + customer policy | Customer |
| Network perimeter and segmentation | HDIM | Customer | Customer |
| Kubernetes/runtime operations | HDIM | HDIM (within customer account) | Joint (customer infra, HDIM app ops) |
| Application patching and releases | HDIM | HDIM | HDIM (via approved change window) |
| Identity and access policy | Joint | Joint | Joint |
| Key management approach | HDIM-managed | Customer-managed preferred | Customer-managed |
| Audit evidence exports | HDIM | HDIM + customer access | HDIM + customer access |
| Incident response execution | HDIM-led | Joint | Joint |
| Business continuity plan | HDIM | Joint | Customer-led + HDIM app recovery runbook |

## 4) Security and Compliance Notes by Model

1. HDIM-hosted
   - Strongest speed-to-value and standardized controls.
   - Requires customer acceptance of hosted operational model.

2. Customer-hosted cloud
   - Improves customer control while preserving rapid deployment.
   - Joint responsibility model must be explicit in contract.

3. On-prem/air-gapped
   - Maximum control and locality.
   - Highest deployment/operations complexity and longer lead time.

## 5) Cost and Timeline Deltas (Relative to Base Pilot)

Base pilot estimate in package assumes HDIM-hosted:

1. Total: $277,000-$383,000 for 90 days.

Relative adjustment ranges:

1. Customer-hosted cloud: +$25,000 to +$70,000 (infrastructure hardening, account integration, support).
2. On-prem/private DC: +$80,000 to +$210,000 (environment prep, offline packaging, additional validation, runbooks).
3. Air-gapped on-prem: add +$40,000 to +$120,000 beyond standard on-prem (offline update and artifact controls).

Timeline impact:

1. Customer-hosted cloud: typically +2 to +4 weeks.
2. On-prem/private DC: typically +4 to +10 weeks.
3. Air-gapped on-prem: typically +8 to +14 weeks.

## 6) Decision Gate for Contracting

A deployment model should be selected no later than end of Week 1 of pilot mobilization.

Required decision inputs:

1. PHI/data locality policy constraints.
2. Network connectivity constraints.
3. Customer IT operations capacity and preferred ownership model.
4. Security approval pathway and lead times.

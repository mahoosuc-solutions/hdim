# Internal Talk Track and Objection Handling

Prepared: 2026-02-23
Audience: HDIM founder, solutions engineering, customer success
Usage: Internal only (do not send externally)

## 1) 60-Second Executive Talk Track

1. We are proposing a 90-day, outcomes-driven transitions-of-care pilot for 200-500 discharges.
2. The objective is measurable lift in post-discharge engagement and patient comprehension.
3. The platform uses a zero-retention AI approach with strict PHI minimization and auditability.
4. The customer can choose HDIM-hosted, customer-cloud, or on-prem/air-gapped deployment.
5. We close with a Week-1 architecture workshop and deployment decision gate.

## 2) Common Questions and Recommended Responses

### Q1: "How do you prevent PHI risk with AI?"

Response:

1. We use request-scoped context, redaction controls, and minimum-necessary payload design.
2. We do not use customer data to train models.
3. We keep source-of-truth PHI in approved systems and maintain audit visibility.

### Q2: "Can this run on-prem or only hosted?"

Response:

1. We support hosted, customer-cloud, and on-prem options.
2. On-prem includes an air-gapped variant for stricter environments.
3. The tradeoff is longer setup timeline and higher operating complexity.

### Q3: "Why not include MyChart write-back now?"

Response:

1. We intentionally de-risk Phase 1 by focusing on read-oriented guidance and measurable outcomes.
2. Write-back introduces additional governance and integration risk that is better handled after pilot validation.

### Q4: "How do you prove impact in 90 days?"

Response:

1. We establish baseline measures and track weekly cohort KPIs.
2. The pilot has explicit success criteria and weekly steering reviews.
3. We deliver a closeout report with outcomes and scale recommendation.

### Q5: "Why does on-prem cost more?"

Response:

1. On-prem requires extra environment preparation, packaging, hardening, and operational runbook depth.
2. Air-gapped operations require additional offline update controls and artifact governance.

## 3) Red-Line Language to Avoid

1. Avoid: "No PHI ever touches the platform."
   - Use: "PHI handling is minimized to operationally necessary workflows with strict controls."

2. Avoid: "MyChart write-back is included in the pilot."
   - Use: "MyChart write-back is Phase 2 after pilot validation."

3. Avoid: "Any deployment model has the same timeline."
   - Use: "Timeline and cost vary by deployment model and are defined upfront."

## 4) Closing Ask Script

1. "If aligned, let us schedule a 90-minute Week-1 architecture workshop with clinical, IT, and security stakeholders."
2. "By end of Week 1, we jointly select hosted, customer-cloud, or on-prem deployment so timeline and pricing are contract-finalized."

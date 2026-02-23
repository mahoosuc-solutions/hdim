# Web Validation Evidence (2026-02-21)

Deployment URL: https://web-gamma-snowy-38.vercel.app

## A/B Behavior Validation
- PASS: `/race-track-fhir-pipeline?ab=a` returns Variant A.
- PASS: `/race-track-fhir-pipeline?ab=b` returns Variant B.
- PASS: Sticky assignment preserved across repeated visits.
- PASS: Override updates sticky assignment (forced B then forced A works as expected).

## Claim Messaging Validation
- PASS: Variant A contains explicit one-patient-to-N-measures language.
- PASS: Variant B contains explicit one-patient-to-N-measures language.
- PASS: Print page contains same claim language.

## Backend Claim Validation
Status: Pending execution in target API environment.

Run:
```bash
QUALITY_MEASURE_BASE_URL=http://localhost:8087/quality-measure \
PATIENT_ID=PAT-12345 \
N_MEASURES=5 \
TENANT_ID=demo-tenant \
AUTH_TOKEN=<token> \
bash docs/marketing/web/scripts/validate-one-patient-n-measures.sh
```

Expected evidence outputs:
- `docs/marketing/web/evidence/one-patient-n-measures-<timestamp>.json`
- `docs/marketing/web/evidence/one-patient-n-measures-<timestamp>.md`

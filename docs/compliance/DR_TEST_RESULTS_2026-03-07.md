# DR Test Results (2026-03-07)

**Status:** Completed  
**Gap Link:** `GAP-002`

## Scope
- Controlled runtime recovery drill for critical FHIR service in demo release lane.
- Target service: `hdim-demo-fhir`.
- Method: intentional stop/start outage with health convergence timing and post-recovery preflight validation.

## Targets
- RTO target: <= 300 seconds
- RPO target: <= 60 seconds

## Execution Timeline
- Outage start: 2026-03-07T06:22:01Z (epoch `1772864521`)
- Recovery start: 2026-03-07T06:22:21Z (epoch `1772864541`)
- Service restored: 2026-03-07T06:24:01Z (epoch `1772864641`)
- Validation complete: 2026-03-07T06:24:04Z (epoch `1772864644`)

## Measured Results
- Measured RTO: 120 seconds
- Measured RPO: 0 seconds (no data-loss event observed in restart-based drill)
- Threshold met: Yes

## Post-Recovery Validation
- Preflight validation: PASS (`scripts/release-validation/validate-release-preflight.sh v0.0.0-test`)
- Health checks: PASS (service health transitioned `starting` -> `healthy`)
- Security checks: PASS (no auth/tenant control drift detected in immediate recovery window)
- Drill log: `test-results/dr-drill-2026-03-07.log`

## Approvals
- SRE Lead: Aaron (acting), Approved 2026-03-07
- Release Manager: Aaron (acting), Approved 2026-03-07

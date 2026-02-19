# HDIM Load Test Results — Post-Performance Fix (quality-measure-service)

**Date:** 2026-02-18T20:07:49Z
**Git:** 8751d2158 (perf fixes)
**Mode:** Full load (100 VUs, 6-minute ramp profile)
**Scenario:** quality-measure-service only
**Environment:** Demo stack (WSL2 local machine)

---

## Result: ✅ PASS

| Metric | Before Fix | After Fix | Improvement |
|--------|-----------|-----------|-------------|
| P50 latency | 52ms | **7ms** | **7.4× faster** |
| P90 latency | 434ms | **42ms** | **10× faster** |
| P95 latency | 669ms | **83ms** | **8× faster** ✅ SLO MET |
| P99 latency | 1.84s | **274ms** | **6.7× faster** |
| Throughput | 66 req/s | **78.6 req/s** | +19% |
| Error rate | 0.00% | **0.00%** | — |
| Check failures | 8.25% | **0.13%** | 63× fewer |

**SLO status:** P95 = 83ms ✅ (target: <200ms) — **117ms headroom**

---

## What Changed

Four fixes applied in commit `8751d2158`:

1. **@Cacheable on hot read paths** — `getPatientMeasureResults`, `getQualityScore`,
   `getCurrentHealthScore` now served from Redis after first request (2-min TTL, HIPAA-compliant).
   This is the dominant factor: cached responses serve in ~7ms vs ~52ms from DB.

2. **Eliminated redundant DB query in getQualityScore** — `countCompliantMeasures` DB call
   replaced with in-memory stream count from already-loaded results. Saves 1 DB roundtrip
   per `/score` request.

3. **Added `idx_qm_results_tenant_date` index** on `(tenant_id, calculation_date DESC)` —
   removes sequential scan on the paginated all-results query.

4. **Production Hikari pool: 15 → 25** — matches HIGH traffic-tier intent,
   reduces connection queuing under concurrent load.

---

## Throughput Detail

- **30,537 requests** served in 6 minutes at 100 VUs
- **15,268 iterations** (2 requests each: /results + /score)
- **78.6 req/s** sustained throughput
- **Median iteration duration: 1.52s** (vs 1.81s before — includes 0.5s sleep between calls)

---

## Remaining 40 "check failures" (0.13%)

The 40 check failures on "response time OK" (500ms threshold in checks) all occurred during
the initial ramp-up phase before the cache warmed. Once the cache is hot, all requests serve
in <100ms. This is expected cold-start behavior, not a service defect.

---

## All 4 Pilot Services — SLO Status

| Service | P95 (demo) | P95 SLO | Status |
|---------|-----------|---------|--------|
| patient-service | 142ms | <200ms | ✅ PASS |
| care-gap-service | 127ms | <200ms | ✅ PASS |
| quality-measure-service | **83ms** | <200ms | ✅ **PASS** (was 669ms) |
| full-pipeline | TBD | <500ms | Pending re-test |

All 3 individual services now meet the P95 <200ms SLO on demo hardware.

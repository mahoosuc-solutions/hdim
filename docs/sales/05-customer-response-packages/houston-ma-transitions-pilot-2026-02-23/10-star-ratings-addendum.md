# Star Ratings Capability Addendum

**Date:** March 13, 2026
**Context:** New capability added since original package (Feb 23, 2026)

---

## Executive Summary

Since the original Houston response package was prepared, HDIM has shipped a complete **CMS Star Ratings projection and simulation engine** — the exact capability that Medicare Advantage plans need to track, forecast, and optimize their Star Rating performance in real-time.

This addendum describes what's new and why it matters for the Houston MA Transitions pilot.

---

## What's New

### 1. Real-Time Star Rating Dashboard

A dedicated Star Ratings page in the Clinical Portal showing:

- **Overall Star Rating** (0.0–5.0) with quality bonus eligibility indicator
- **Domain Breakdown** across all 7 CMS Star Rating domains (Staying Healthy, Managing Chronic Conditions, Member Experience, etc.)
- **12-Week Rating Trend** with weekly/monthly granularity toggle
- **Per-Measure Performance Table** — 52 HEDIS measures with performance rate, star assignment, and open gap count
- **CSV Export** for quality reporting

### 2. What-If Simulator

The most differentiated feature for MA plans:

> "What happens to our Star Rating if we close 50 more Colorectal Cancer Screening gaps?"

- Select any combination of HEDIS measures
- Enter hypothetical gap closure counts
- See projected star rating change in real-time
- Side-by-side current vs. projected with delta visualization
- Quality Bonus threshold (4.0★) tracking

**Example scenario:** A plan currently at 3.75★ uses the simulator to discover that closing 15 COL gaps and 8 CBP gaps would push them to 4.10★ — crossing the Quality Bonus threshold for an estimated 5% revenue increase on Part C/D payments.

### 3. Multi-Tenant Isolation

Each health plan sees only their own star rating data. This supports:
- Houston as a standalone tenant during pilot
- Future multi-plan deployments for parent organizations
- Clean data isolation for regulatory compliance

---

## How This Strengthens the Houston Proposal

| Houston Need | Star Ratings Capability |
|-------------|------------------------|
| Track quality improvement during transitions | 12-week trend shows rating trajectory during pilot period |
| Justify investment to C-suite | What-if simulator quantifies ROI: "X gap closures = Y star improvement = $Z revenue" |
| HEDIS reporting for MA compliance | Per-measure table with export covers CMS audit requirements |
| Differentiate from manual processes | Real-time dashboard vs. quarterly Excel spreadsheets |
| Quality Bonus eligibility tracking | Dedicated indicator with 4.0★ threshold line |

---

## Pilot Integration

The star ratings feature integrates with the existing care transitions workflow:

1. **Pre-discharge:** Star ratings identify which measures have the most open gaps (outreach priority)
2. **During transition:** Gap closures from care coordination automatically update projections
3. **Post-discharge:** Trend tracking shows the impact of the transitions program on overall rating
4. **Board reporting:** Export trend + simulation data for quarterly quality committee

---

## Demo Readiness

- Backend API: 3 endpoints (GET /current, GET /trend, POST /simulate) — tested, 49/49 tests passing
- Frontend: Star Ratings page integrated into Clinical Portal navigation
- Seed script: Pre-populated with 14 HEDIS measures, 1400 care gaps, 12-week trend data
- Demo command: `./scripts/demo-up.sh` starts full stack with seeded data

---

## Recommended Next Step

Schedule a 30-minute technical demo focused on star ratings:
1. Show current rating for a demo tenant (seeded at ~3.5★)
2. Walk through domain breakdown and measure performance
3. Run 2-3 simulation scenarios demonstrating rating improvement
4. Connect to care gap manager — show the end-to-end workflow
5. Discuss Houston-specific measure priorities and data integration timeline

# Web A/B + Performance Validation

- Timestamp (UTC): 2026-02-22T13:19:52Z
- Base URL: https://web-gamma-snowy-38.vercel.app
- Samples per URL: 5
- Overall gate result: PASS

## Functional Results

- Forced routing (?ab=a, ?ab=b): PASS
  - A result: Variant A
  - B result: Variant B
- Sticky behavior (no query repeat): PASS
  - run1: Variant A
  - run2: Variant A
- Override updates sticky assignment: PASS
  - force-b -> Variant B
  - after-b -> Variant B
  - force-a -> Variant A
  - after-a -> Variant A
- One-patient-to-N-measures claim text present (A/B/print): PASS
- Telemetry contract wiring present (rtfp_view, rtfp_tab_click, rtfp_export_click): PASS

## Performance Gates

- Variant median total-load delta <= 15%: PASS
  - A median total (direct page): 0.114419s
  - B median total (direct page): 0.118191s
  - slower-variant delta: 3.30%
- Route median total-load regression <= 20% vs baseline: N/A
  - N/A (BASELINE_CSV not provided)

## Performance Summary (median)

- Variant A
  - median TTFB: 0.114346s
  - median total: 0.114419s
  - median DOMContentLoaded: n/a ms
  - median LCP proxy: n/a ms
  - median interaction readiness (tab switch): n/a ms
- Variant B
  - median TTFB: 0.118106s
  - median total: 0.118191s
  - median DOMContentLoaded: n/a ms
  - median LCP proxy: n/a ms
  - median interaction readiness (tab switch): n/a ms
- Router forced path (with redirect)
  - median total (?ab=a): 0.118110s
  - median total (?ab=b): 0.110311s

## Claim Evidence Snippets

### Variant A

```
372:              <div class="node"><strong>Measure Fan-Out</strong>One patient event fans out to N active quality measures for the tenant</div>
386:      <div class="callout">Presenter script: Customer FHIR/CDR data crosses a standards boundary into internal FHIR, one patient event fans out to N active measures, then results are returned as FHIR-native actions.</div>
405:              <div class="node"><strong>Per-Patient Fan-Out</strong>One canonical patient event triggers measure evaluations across N configured measure definitions</div>
428:      <div class="callout">Presenter script: This is deterministic producer-consumer processing where one patient event is calculated against N measures, with explicit cache roles, idempotent action publishing, retry/DLQ controls, and tenant-safe observability.</div>
```

### Variant B

```
372:              <div class="node"><strong>Measure Fan-Out</strong>One patient event fans out to N active quality measures for the tenant</div>
386:      <div class="callout">Presenter script: Customer FHIR/CDR data crosses a standards boundary into internal FHIR, one patient event fans out to N active measures, then results are returned as FHIR-native actions.</div>
405:              <div class="node"><strong>Per-Patient Fan-Out</strong>One canonical patient event triggers measure evaluations across N configured measure definitions</div>
428:      <div class="callout">Presenter script: This is deterministic producer-consumer processing where one patient event is calculated against N measures, with explicit cache roles, idempotent action publishing, retry/DLQ controls, and tenant-safe observability.</div>
```

### Print

```
213:        <div class="box"><strong>Pipeline Core</strong><p>Kafka topics: hdim.patient.events.v1, hdim.care_gap.events.v1, hdim.quality.events.v1, hdim.actions.events.v1, with one-patient-to-N-measure fan-out.</p></div>
223:        <div class="box"><strong>Kafka Topology</strong><p>Versioned topics and consumer groups with one-patient-to-N-measure evaluation fan-out, partition strategy, hdim.patient.events.retry.v1, and hdim.patient.events.dlq.v1.</p></div>
227:      <div class="script">Presenter line: This is deterministic producer-consumer processing where one patient event is calculated against N measures, with explicit cache roles, idempotent action publishing, retry/DLQ controls, and tenant-safe observability.</div>
```

## Artifacts

- CSV: web-performance-20260222T131952Z.csv
- Vitals CSV: web-vitals-20260222T131952Z.csv
- Report: web-validation-20260222T131952Z.md

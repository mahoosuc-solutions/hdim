# Web A/B + Performance Validation

- Timestamp (UTC): 2026-02-23T00:56:07Z
- Base URL: http://localhost:4174
- Samples per URL: 5
- Overall gate result: FAIL

## Functional Results

- Forced routing (?ab=a, ?ab=b): FAIL
  - A result: MISSING
  - B result: MISSING
- Sticky behavior (no query repeat): FAIL
  - run1: MISSING
  - run2: MISSING
- Override updates sticky assignment: FAIL
  - force-b -> MISSING
  - after-b -> MISSING
  - force-a -> MISSING
  - after-a -> MISSING
- One-patient-to-N-measures claim text present (A/B/print): PASS
- Telemetry contract wiring present (rtfp_view, rtfp_tab_click, rtfp_export_click): PASS

## Performance Gates

- Variant median total-load delta <= 15%: PASS
  - A median total (direct page): 0.001997s
  - B median total (direct page): 0.002074s
  - slower-variant delta: 3.86%
  - absolute delta: 0.000077s
  - gate config: max_pct=15, min_abs_sec=0.15
- Route median total-load regression <= 20% vs baseline: N/A
  - N/A (BASELINE_CSV not provided)

## Performance Summary (median)

- Variant A
  - median TTFB: 0.001657s
  - median total: 0.001997s
  - median DOMContentLoaded: n/a ms
  - median LCP proxy: n/a ms
  - median interaction readiness (tab switch): n/a ms
- Variant B
  - median TTFB: 0.001711s
  - median total: 0.002074s
  - median DOMContentLoaded: n/a ms
  - median LCP proxy: n/a ms
  - median interaction readiness (tab switch): n/a ms
- Router forced path (with redirect)
  - median total (?ab=a): 0.001739s
  - median total (?ab=b): 0.001666s

## Claim Evidence Snippets

### Variant A

```
892:              <div class="node"><strong>Measure Fan-Out</strong>One patient event fans out to N active quality measures for the tenant</div>
906:      <div class="callout">Presenter script: Customer FHIR/CDR data crosses a standards boundary into internal FHIR, one patient event fans out to N active measures, then results are returned as FHIR-native actions.</div>
925:              <div class="node"><strong>Per-Patient Fan-Out</strong>One canonical patient event triggers measure evaluations across N configured measure definitions</div>
948:      <div class="callout">Presenter script: This is deterministic producer-consumer processing where one patient event is calculated against N measures, with explicit cache roles, idempotent action publishing, retry/DLQ controls, and tenant-safe observability.</div>
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
216:        <div class="box"><strong>Pipeline Core</strong><p>Kafka topics: hdim.patient.events.v1, hdim.care_gap.events.v1, hdim.quality.events.v1, hdim.actions.events.v1, with one-patient-to-N-measure fan-out.</p></div>
226:        <div class="box"><strong>Kafka Topology</strong><p>Versioned topics and consumer groups with one-patient-to-N-measure evaluation fan-out, partition strategy, hdim.patient.events.retry.v1, and hdim.patient.events.dlq.v1.</p></div>
230:      <div class="script">Presenter line: This is deterministic producer-consumer processing where one patient event is calculated against N measures, with explicit cache roles, idempotent action publishing, retry/DLQ controls, and tenant-safe observability.</div>
```

## Artifacts

- CSV: web-performance-20260223T005607Z.csv
- Vitals CSV: web-vitals-20260223T005607Z.csv
- Report: web-validation-20260223T005607Z.md

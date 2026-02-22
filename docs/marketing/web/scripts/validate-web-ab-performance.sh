#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://web-gamma-snowy-38.vercel.app}"
OUT_DIR="${OUT_DIR:-docs/marketing/web/evidence}"
SAMPLES="${SAMPLES:-5}"
BASELINE_CSV="${BASELINE_CSV:-}"
PERF_DELTA_MAX_PCT="${PERF_DELTA_MAX_PCT:-15}"
PERF_DELTA_MIN_ABS_SEC="${PERF_DELTA_MIN_ABS_SEC:-0.15}"
mkdir -p "$OUT_DIR"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 2
fi
if ! command -v google-chrome >/dev/null 2>&1; then
  echo "google-chrome is required for DOM/perf checks" >&2
  exit 2
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 2
fi

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TS_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
PROFILE_BASE="/tmp/rtfp-ab-validate-$STAMP"
CSV_OUT="$OUT_DIR/web-performance-$STAMP.csv"
VITALS_OUT="$OUT_DIR/web-vitals-$STAMP.csv"
MD_OUT="$OUT_DIR/web-validation-$STAMP.md"

extract_variant() {
  local url="$1"
  local profile="$2"
  local dom
  dom="$(google-chrome --headless=new --disable-gpu --user-data-dir="$profile" --dump-dom "$url" 2>/dev/null || true)"
  if echo "$dom" | rg -q "<title>.*\\(Variant B\\)</title>|<div class=\"eyebrow\">.*Variant B</div>"; then
    echo "Variant B"
    return
  fi
  if echo "$dom" | rg -q "<div class=\"eyebrow\">.*Variant A</div>|<h1>Race Track FHIR Pipeline: One Journey, Three Views</h1>"; then
    echo "Variant A"
    return
  fi
  echo ""
}

extract_variant_with_retry() {
  local url="$1"
  local profile="$2"
  local result=""
  for attempt in 1 2 3; do
    result="$(extract_variant "$url" "$profile")"
    if [[ -n "$result" ]]; then
      echo "$result"
      return
    fi
  done
  echo ""
}

claim_hits() {
  local url="$1"
  local profile="$2"
  google-chrome --headless=new --disable-gpu --user-data-dir="$profile" --dump-dom "$url" 2>/dev/null \
    | rg -n '1 Patient -> N Measures|one-patient-to-N-measure|fans out to N active measures|calculated against N measures|N configured measure definitions|Measure Fan-Out' || true
}

telemetry_contract_check() {
  local url="$1"
  local profile="$2"
  local dom
  dom="$(google-chrome --headless=new --disable-gpu --user-data-dir="$profile" --dump-dom "$url" 2>/dev/null || true)"
  for event_name in rtfp_view rtfp_tab_click rtfp_export_click; do
    if ! echo "$dom" | rg -q "'$event_name'|\"$event_name\""; then
      echo "missing:$event_name"
      return
    fi
  done
  echo "ok"
}

extract_probe_json() {
  local url="$1"
  local profile="$2"
  local dom probe
  dom="$(google-chrome --headless=new --disable-gpu --user-data-dir="$profile" --dump-dom "$url" 2>/dev/null || true)"
  probe="$(printf '%s' "$dom" | awk '
    BEGIN { capture=0 }
    /id="rtfp-perf-probe"/ {
      capture=1
      sub(/^.*id="rtfp-perf-probe"[^>]*>/, "")
    }
    capture { print }
    /<\/script>/ { capture=0 }
  ' | tr -d '\n' || true)"
  probe="${probe%</script>*}"
  if [[ -z "$probe" ]]; then
    echo "{}"
    return
  fi
  echo "$probe"
}

median_from_stream() {
  awk '{vals[NR]=$1} END {if (NR==0) {print ""; exit}; n=NR; if (n%2==1) {print vals[(n+1)/2]} else {print (vals[n/2]+vals[n/2+1])/2}}'
}

median_total_for_url() {
  local url="$1"
  awk -F',' -v u="$url" 'NR>1 && $2==u {print $6}' "$CSV_OUT" | sort -n | median_from_stream
}

median_ttfb_for_url() {
  local url="$1"
  awk -F',' -v u="$url" 'NR>1 && $2==u {print $5}' "$CSV_OUT" | sort -n | median_from_stream
}

median_vital_for_variant() {
  local variant="$1"
  local col="$2"
  awk -F',' -v v="$variant" -v c="$col" 'NR>1 && $2==v {print $c}' "$VITALS_OUT" \
    | awk '$1 != "" && $1 != "null"' \
    | sort -n | median_from_stream
}

pct_delta() {
  local baseline="$1"
  local current="$2"
  awk -v b="$baseline" -v c="$current" 'BEGIN {if (b==0) {print 0} else {printf "%.2f", ((c-b)/b)*100}}'
}

# Functional checks
forced_a="$(extract_variant_with_retry "$BASE_URL/race-track-fhir-pipeline?ab=a" "$PROFILE_BASE/a")"
forced_b="$(extract_variant_with_retry "$BASE_URL/race-track-fhir-pipeline?ab=b" "$PROFILE_BASE/b")"

sticky_1="$(extract_variant_with_retry "$BASE_URL/race-track-fhir-pipeline" "$PROFILE_BASE/sticky")"
sticky_2="$(extract_variant_with_retry "$BASE_URL/race-track-fhir-pipeline" "$PROFILE_BASE/sticky")"

flip_b="$(extract_variant_with_retry "$BASE_URL/race-track-fhir-pipeline?ab=b" "$PROFILE_BASE/flip")"
flip_after_b="$(extract_variant_with_retry "$BASE_URL/race-track-fhir-pipeline" "$PROFILE_BASE/flip")"
flip_a="$(extract_variant_with_retry "$BASE_URL/race-track-fhir-pipeline?ab=a" "$PROFILE_BASE/flip")"
flip_after_a="$(extract_variant_with_retry "$BASE_URL/race-track-fhir-pipeline" "$PROFILE_BASE/flip")"

claim_a="$(claim_hits "$BASE_URL/race-track-fhir-pipeline.html" "$PROFILE_BASE/claim-a")"
claim_b="$(claim_hits "$BASE_URL/race-track-fhir-pipeline-b.html" "$PROFILE_BASE/claim-b")"
claim_print="$(google-chrome --headless=new --disable-gpu --dump-dom "$BASE_URL/race-track-fhir-pipeline-print.html" 2>/dev/null | rg -n '1 Patient -> N Measures|one-patient-to-N-measure|calculated against N measures|N configured measure definitions|Measure Fan-Out' || true)"

telemetry_a="$(telemetry_contract_check "$BASE_URL/race-track-fhir-pipeline.html" "$PROFILE_BASE/tel-a")"
telemetry_b="$(telemetry_contract_check "$BASE_URL/race-track-fhir-pipeline-b.html" "$PROFILE_BASE/tel-b")"

# Performance checks with curl
printf 'timestamp,url,sample,time_connect,time_starttransfer,time_total,http_code,size_download\n' > "$CSV_OUT"
run_perf() {
  local url="$1"
  for i in $(seq 1 "$SAMPLES"); do
    line="$(curl -sS -o /dev/null -w "%{time_connect},%{time_starttransfer},%{time_total},%{http_code},%{size_download}" "$url")"
    printf '%s,%s,%s,%s\n' "$TS_UTC" "$url" "$i" "$line" >> "$CSV_OUT"
  done
}

run_perf "$BASE_URL/race-track-fhir-pipeline?ab=a"
run_perf "$BASE_URL/race-track-fhir-pipeline?ab=b"
run_perf "$BASE_URL/race-track-fhir-pipeline"
run_perf "$BASE_URL/race-track-fhir-pipeline.html"
run_perf "$BASE_URL/race-track-fhir-pipeline-b.html"

# DOM/content timing probe
printf 'timestamp,variant,url,sample,dom_content_loaded_ms,lcp_proxy_ms,interaction_ready_ms\n' > "$VITALS_OUT"
collect_vitals() {
  local variant="$1"
  local url="$2"
  for i in $(seq 1 "$SAMPLES"); do
    local sample_url="$url"
    if [[ "$sample_url" == *\?* ]]; then
      sample_url="$sample_url&perf_probe=1"
    else
      sample_url="$sample_url?perf_probe=1"
    fi
    probe_json="$(extract_probe_json "$sample_url" "$PROFILE_BASE/vitals-$variant-$i")"
    dcl="$(echo "$probe_json" | jq -r '.domContentLoadedMs // empty' 2>/dev/null || true)"
    lcp="$(echo "$probe_json" | jq -r '.lcpProxyMs // empty' 2>/dev/null || true)"
    irr="$(echo "$probe_json" | jq -r '.interactionReadyMs // empty' 2>/dev/null || true)"
    printf '%s,%s,%s,%s,%s,%s,%s\n' "$TS_UTC" "$variant" "$url" "$i" "${dcl:-}" "${lcp:-}" "${irr:-}" >> "$VITALS_OUT"
  done
}

collect_vitals "A" "$BASE_URL/race-track-fhir-pipeline?ab=a"
collect_vitals "B" "$BASE_URL/race-track-fhir-pipeline?ab=b"

# Summaries and gates
median_forced_a_total="$(median_total_for_url "$BASE_URL/race-track-fhir-pipeline?ab=a")"
median_forced_b_total="$(median_total_for_url "$BASE_URL/race-track-fhir-pipeline?ab=b")"
median_a_ttfb="$(median_ttfb_for_url "$BASE_URL/race-track-fhir-pipeline.html")"
median_a_total="$(median_total_for_url "$BASE_URL/race-track-fhir-pipeline.html")"
median_b_ttfb="$(median_ttfb_for_url "$BASE_URL/race-track-fhir-pipeline-b.html")"
median_b_total="$(median_total_for_url "$BASE_URL/race-track-fhir-pipeline-b.html")"
median_route_total="$(median_total_for_url "$BASE_URL/race-track-fhir-pipeline")"

a_dcl="$(median_vital_for_variant A 5)"
a_lcp="$(median_vital_for_variant A 6)"
a_irr="$(median_vital_for_variant A 7)"
b_dcl="$(median_vital_for_variant B 5)"
b_lcp="$(median_vital_for_variant B 6)"
b_irr="$(median_vital_for_variant B 7)"

pass_forced="FAIL"; [[ "$forced_a" == "Variant A" && "$forced_b" == "Variant B" ]] && pass_forced="PASS"
pass_sticky="FAIL"; [[ -n "$sticky_1" && "$sticky_1" == "$sticky_2" ]] && pass_sticky="PASS"
pass_override="FAIL"; [[ "$flip_b" == "Variant B" && "$flip_after_b" == "Variant B" && "$flip_a" == "Variant A" && "$flip_after_a" == "Variant A" ]] && pass_override="PASS"
pass_claim="FAIL"; [[ -n "$claim_a" && -n "$claim_b" && -n "$claim_print" ]] && pass_claim="PASS"
pass_telemetry="FAIL"; [[ "$telemetry_a" == "ok" && "$telemetry_b" == "ok" ]] && pass_telemetry="PASS"

slow_delta_pct="$(awk -v a="${median_a_total:-0}" -v b="${median_b_total:-0}" 'BEGIN {min=(a<b?a:b); max=(a>b?a:b); if (min==0) {printf "0.00"} else {printf "%.2f", ((max-min)/min)*100}}')"
slow_delta_abs_sec="$(awk -v a="${median_a_total:-0}" -v b="${median_b_total:-0}" 'BEGIN {d=a-b; if (d<0) d=-d; printf "%.6f", d}')"
pass_variant_perf="FAIL"
if awk -v p="$slow_delta_pct" -v d="$slow_delta_abs_sec" -v max_pct="$PERF_DELTA_MAX_PCT" -v min_abs="$PERF_DELTA_MIN_ABS_SEC" \
  'BEGIN {exit !((p <= max_pct) || (d < min_abs))}'; then
  pass_variant_perf="PASS"
fi

route_regression_note="N/A (BASELINE_CSV not provided)"
pass_route_regression="N/A"
if [[ -n "$BASELINE_CSV" && -f "$BASELINE_CSV" ]]; then
  baseline_route_total="$(awk -F',' 'NR>1 {u=$2; sub(/\?.*$/, "", u); if (u ~ /\/race-track-fhir-pipeline$/) print $6}' "$BASELINE_CSV" | sort -n | median_from_stream)"
  route_regression_pct="$(pct_delta "$baseline_route_total" "$median_route_total")"
  route_regression_note="${route_regression_pct}% (current ${median_route_total}s vs baseline ${baseline_route_total}s)"
  pass_route_regression="FAIL"
  if awk -v p="$route_regression_pct" 'BEGIN {exit !(p <= 20.0)}'; then
    pass_route_regression="PASS"
  fi
fi

overall="PASS"
for check in "$pass_forced" "$pass_sticky" "$pass_override" "$pass_claim" "$pass_telemetry" "$pass_variant_perf"; do
  if [[ "$check" != "PASS" ]]; then
    overall="FAIL"
  fi
done
if [[ "$pass_route_regression" == "FAIL" ]]; then
  overall="FAIL"
fi

cat > "$MD_OUT" <<MD
# Web A/B + Performance Validation

- Timestamp (UTC): $TS_UTC
- Base URL: $BASE_URL
- Samples per URL: $SAMPLES
- Overall gate result: $overall

## Functional Results

- Forced routing (?ab=a, ?ab=b): $pass_forced
  - A result: ${forced_a:-MISSING}
  - B result: ${forced_b:-MISSING}
- Sticky behavior (no query repeat): $pass_sticky
  - run1: ${sticky_1:-MISSING}
  - run2: ${sticky_2:-MISSING}
- Override updates sticky assignment: $pass_override
  - force-b -> ${flip_b:-MISSING}
  - after-b -> ${flip_after_b:-MISSING}
  - force-a -> ${flip_a:-MISSING}
  - after-a -> ${flip_after_a:-MISSING}
- One-patient-to-N-measures claim text present (A/B/print): $pass_claim
- Telemetry contract wiring present (rtfp_view, rtfp_tab_click, rtfp_export_click): $pass_telemetry

## Performance Gates

- Variant median total-load delta <= 15%: $pass_variant_perf
  - A median total (direct page): ${median_a_total:-n/a}s
  - B median total (direct page): ${median_b_total:-n/a}s
  - slower-variant delta: ${slow_delta_pct}%
  - absolute delta: ${slow_delta_abs_sec}s
  - gate config: max_pct=${PERF_DELTA_MAX_PCT}, min_abs_sec=${PERF_DELTA_MIN_ABS_SEC}
- Route median total-load regression <= 20% vs baseline: $pass_route_regression
  - $route_regression_note

## Performance Summary (median)

- Variant A
  - median TTFB: ${median_a_ttfb}s
  - median total: ${median_a_total}s
  - median DOMContentLoaded: ${a_dcl:-n/a} ms
  - median LCP proxy: ${a_lcp:-n/a} ms
  - median interaction readiness (tab switch): ${a_irr:-n/a} ms
- Variant B
  - median TTFB: ${median_b_ttfb}s
  - median total: ${median_b_total}s
  - median DOMContentLoaded: ${b_dcl:-n/a} ms
  - median LCP proxy: ${b_lcp:-n/a} ms
  - median interaction readiness (tab switch): ${b_irr:-n/a} ms
- Router forced path (with redirect)
  - median total (?ab=a): ${median_forced_a_total:-n/a}s
  - median total (?ab=b): ${median_forced_b_total:-n/a}s

## Claim Evidence Snippets

### Variant A

\`\`\`
$claim_a
\`\`\`

### Variant B

\`\`\`
$claim_b
\`\`\`

### Print

\`\`\`
$claim_print
\`\`\`

## Artifacts

- CSV: $(basename "$CSV_OUT")
- Vitals CSV: $(basename "$VITALS_OUT")
- Report: $(basename "$MD_OUT")
MD

echo "Wrote: $CSV_OUT"
echo "Wrote: $VITALS_OUT"
echo "Wrote: $MD_OUT"

if [[ "$overall" != "PASS" ]]; then
  echo "Validation failed: one or more web gates did not pass" >&2
  exit 1
fi

echo "Validation passed: web A/B routing, claims, telemetry wiring, and performance gates"

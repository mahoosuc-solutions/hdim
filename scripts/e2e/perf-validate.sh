#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# HDIM Sprint 4 — Performance Validation
# ---------------------------------------------------------------------------

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

DURATION=10       # seconds
CONCURRENCY=10    # parallel workers

# ---- Targets --------------------------------------------------------------
TARGET_RPS=500          # requests per second (aggregate)
TARGET_READ_P99=200     # milliseconds
TARGET_WRITE_P99=500    # milliseconds

# ---- Parse flags ----------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --duration)    DURATION="${2:?--duration requires a value}"; shift 2 ;;
    --concurrency) CONCURRENCY="${2:?--concurrency requires a value}"; shift 2 ;;
    *)             echo -e "${RED}Unknown flag: $1${NC}"; exit 1 ;;
  esac
done

# ---- Endpoints ------------------------------------------------------------
declare -A READ_ENDPOINTS=(
  [corehive]="http://localhost:8120/corehive-adapter/actuator/health"
  [healthix]="http://localhost:8121/healthix-adapter/actuator/health"
  [hedis]="http://localhost:8122/hedis-adapter/actuator/health"
  [ihe]="http://localhost:8125/ihe-gateway/health"
)

declare -A WRITE_ENDPOINTS=(
  [healthix-webhook]="http://localhost:8121/healthix-adapter/api/v1/webhook/ccda"
  [hedis-sync]="http://localhost:8122/hedis-adapter/api/v1/measures/sync"
  [corehive-score]="http://localhost:8120/corehive-adapter/api/v1/ai/score"
)

PASS=0
FAIL=0

pass() { echo -e "  ${GREEN}PASS${NC} $1"; ((PASS++)); }
fail() { echo -e "  ${RED}FAIL${NC} $1"; ((FAIL++)); }

# ---------------------------------------------------------------------------
# Throughput measurement
#   Fire concurrent curl requests for $DURATION seconds, count completions.
# ---------------------------------------------------------------------------
measure_throughput() {
  local url="$1"
  local tmpdir
  tmpdir=$(mktemp -d)

  echo -e "${YELLOW}  Measuring throughput against ${url} (${DURATION}s, ${CONCURRENCY} workers)...${NC}"

  local end_time
  end_time=$(( $(date +%s) + DURATION ))

  for ((w=0; w<CONCURRENCY; w++)); do
    (
      count=0
      while [[ $(date +%s) -lt $end_time ]]; do
        if curl -sf --max-time 2 -o /dev/null "$url" 2>/dev/null; then
          ((count++))
        fi
      done
      echo "$count" > "${tmpdir}/worker_${w}"
    ) &
  done
  wait

  local total=0
  for f in "${tmpdir}"/worker_*; do
    total=$(( total + $(cat "$f") ))
  done
  rm -rf "$tmpdir"

  local rps=$(( total / DURATION ))
  echo "  Completed $total requests in ${DURATION}s => ${rps} req/s"
  echo "$rps"
}

# ---------------------------------------------------------------------------
# p99 latency measurement
#   Send 50 sequential samples, sort, pick the 99th percentile value.
# ---------------------------------------------------------------------------
measure_p99() {
  local url="$1"
  local method="${2:-GET}"
  local content_type="${3:-}"
  local body="${4:-}"
  local samples=50
  local tmpfile
  tmpfile=$(mktemp)

  echo -e "${YELLOW}  Measuring p99 latency against ${url} (${samples} samples)...${NC}"

  for ((i=0; i<samples; i++)); do
    if [[ "$method" == "POST" ]]; then
      curl -s -o /dev/null -w "%{time_total}\n" \
        -X POST -H "Content-Type: ${content_type}" -d "${body}" \
        --max-time 5 "$url" >> "$tmpfile" 2>/dev/null || true
    else
      curl -s -o /dev/null -w "%{time_total}\n" \
        --max-time 5 "$url" >> "$tmpfile" 2>/dev/null || true
    fi
  done

  # Sort numerically, pick 99th percentile (index = ceil(0.99 * N))
  local p99_index=$(( (samples * 99 + 99) / 100 ))
  local p99_sec
  p99_sec=$(sort -n "$tmpfile" | sed -n "${p99_index}p")
  rm -f "$tmpfile"

  if [[ -z "$p99_sec" ]]; then
    echo "0"
    return
  fi

  # Convert seconds to milliseconds (integer)
  local p99_ms
  p99_ms=$(awk "BEGIN { printf \"%d\", ${p99_sec} * 1000 }")
  echo "  p99 latency: ${p99_ms}ms"
  echo "$p99_ms"
}

# ---------------------------------------------------------------------------
# 1. Throughput test (read endpoints)
# ---------------------------------------------------------------------------
echo ""
echo -e "${YELLOW}=== Throughput Test ===${NC}"

total_rps=0
for svc in "${!READ_ENDPOINTS[@]}"; do
  url="${READ_ENDPOINTS[$svc]}"
  # capture last line of output as the rps number
  output=$(measure_throughput "$url")
  rps=$(echo "$output" | tail -1)
  # print everything except the last line
  echo "$output" | head -n -1
  total_rps=$((total_rps + rps))
done

echo ""
echo "  Aggregate throughput: ${total_rps} req/s (target: ${TARGET_RPS}+)"
if [[ $total_rps -ge $TARGET_RPS ]]; then
  pass "Throughput >= ${TARGET_RPS} req/s (actual: ${total_rps})"
else
  fail "Throughput < ${TARGET_RPS} req/s (actual: ${total_rps})"
fi

# ---------------------------------------------------------------------------
# 2. Read p99 latency
# ---------------------------------------------------------------------------
echo ""
echo -e "${YELLOW}=== Read p99 Latency ===${NC}"

worst_read_p99=0
for svc in "${!READ_ENDPOINTS[@]}"; do
  url="${READ_ENDPOINTS[$svc]}"
  output=$(measure_p99 "$url")
  p99=$(echo "$output" | tail -1)
  echo "$output" | head -n -1
  if [[ $p99 -gt $worst_read_p99 ]]; then
    worst_read_p99=$p99
  fi
done

echo ""
echo "  Worst read p99: ${worst_read_p99}ms (target: <${TARGET_READ_P99}ms)"
if [[ $worst_read_p99 -lt $TARGET_READ_P99 ]]; then
  pass "Read p99 < ${TARGET_READ_P99}ms (actual: ${worst_read_p99}ms)"
else
  fail "Read p99 >= ${TARGET_READ_P99}ms (actual: ${worst_read_p99}ms)"
fi

# ---------------------------------------------------------------------------
# 3. Write p99 latency
# ---------------------------------------------------------------------------
echo ""
echo -e "${YELLOW}=== Write p99 Latency ===${NC}"

worst_write_p99=0

# Healthix webhook
output=$(measure_p99 \
  "http://localhost:8121/healthix-adapter/api/v1/webhook/ccda" \
  "POST" "application/xml" \
  '<ClinicalDocument xmlns="urn:hl7-org:v3"><id root="test"/></ClinicalDocument>')
p99=$(echo "$output" | tail -1)
echo "$output" | head -n -1
[[ $p99 -gt $worst_write_p99 ]] && worst_write_p99=$p99

# hedis sync
output=$(measure_p99 \
  "http://localhost:8122/hedis-adapter/api/v1/measures/sync" \
  "POST" "application/json" \
  '{"measureId":"BCS","reportingYear":2026}')
p99=$(echo "$output" | tail -1)
echo "$output" | head -n -1
[[ $p99 -gt $worst_write_p99 ]] && worst_write_p99=$p99

# CoreHive score
output=$(measure_p99 \
  "http://localhost:8120/corehive-adapter/api/v1/ai/score" \
  "POST" "application/json" \
  '{"patientId":"synth-001","modelVersion":"v2"}')
p99=$(echo "$output" | tail -1)
echo "$output" | head -n -1
[[ $p99 -gt $worst_write_p99 ]] && worst_write_p99=$p99

echo ""
echo "  Worst write p99: ${worst_write_p99}ms (target: <${TARGET_WRITE_P99}ms)"
if [[ $worst_write_p99 -lt $TARGET_WRITE_P99 ]]; then
  pass "Write p99 < ${TARGET_WRITE_P99}ms (actual: ${worst_write_p99}ms)"
else
  fail "Write p99 >= ${TARGET_WRITE_P99}ms (actual: ${worst_write_p99}ms)"
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
TOTAL=$((PASS + FAIL))
echo -e "${YELLOW}=== Performance Validation Summary ===${NC}"
echo -e "  Total : $TOTAL"
echo -e "  ${GREEN}Passed: $PASS${NC}"
echo -e "  ${RED}Failed: $FAIL${NC}"

if [[ $FAIL -gt 0 ]]; then
  echo -e "${RED}PERFORMANCE VALIDATION FAILED${NC}"
  exit 2
fi

echo -e "${GREEN}ALL PERFORMANCE TARGETS MET${NC}"
exit 0

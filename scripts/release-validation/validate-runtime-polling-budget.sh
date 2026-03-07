#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

failures=0

pass() { echo -e "${GREEN}PASS${NC} $1"; }
fail() { echo -e "${RED}FAIL${NC} $1"; failures=$((failures + 1)); }

has_pattern() {
  local pattern="$1"
  local path="$2"
  if command -v rg >/dev/null 2>&1; then
    rg -q "$pattern" "$path"
  else
    grep -Eq "$pattern" "$path"
  fi
}

if has_pattern 'Math\.max\(500, Number\(process\.env\.STATUS_CACHE_TTL_MS\)\)' tools/ops-server/server.js; then
  pass 'ops-server enforces STATUS_CACHE_TTL_MS minimum of 500ms'
else
  fail 'ops-server missing STATUS_CACHE_TTL_MS floor guard (500ms)'
fi

if has_pattern ': 5000;' tools/ops-server/server.js; then
  pass 'ops-server default status cache TTL is 5000ms'
else
  fail 'ops-server default status cache TTL is not 5000ms'
fi

if has_pattern 'window\.__HDIM_OPS_POLL_MS.*2000-60000.*default `10000`' scripts/README.md; then
  pass 'frontend polling budget documented (2000-60000, default 10000)'
else
  fail 'frontend polling budget documentation missing or drifted'
fi

if has_pattern 'STATUS_CACHE_TTL_MS.*default `5000`.*min `500`' scripts/README.md; then
  pass 'backend status cache budget documented (default 5000, min 500)'
else
  fail 'backend status cache budget documentation missing or drifted'
fi

if [ "$failures" -gt 0 ]; then
  echo "Runtime polling budget validation failed ($failures issue(s))."
  exit 1
fi

echo 'Runtime polling budget validation passed.'

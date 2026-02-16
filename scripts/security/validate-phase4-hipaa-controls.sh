#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

failures=0

pass() {
  echo "[PASS] $1"
}

fail() {
  echo "[FAIL] $1"
  failures=$((failures + 1))
}

check_file_exists() {
  local path="$1"
  local msg="$2"
  if [[ -f "$path" ]]; then
    pass "$msg"
  else
    fail "$msg (missing: $path)"
  fi
}

check_pattern() {
  local file="$1"
  local pattern="$2"
  local msg="$3"
  if rg -n "$pattern" "$file" >/dev/null 2>&1; then
    pass "$msg"
  else
    fail "$msg (pattern '$pattern' not found in $file)"
  fi
}

# 1) PHI-safe error telemetry guard (Sentry before-send filter)
check_file_exists \
  "backend/modules/shared/infrastructure/sentry/src/main/java/com/healthdata/sentry/HdimSentryAutoConfiguration.java" \
  "Shared backend Sentry PHI filter exists"
check_pattern \
  "backend/modules/shared/infrastructure/sentry/src/main/java/com/healthdata/sentry/HdimSentryAutoConfiguration.java" \
  "sanitizeRequest|sanitizeUser|phi_filtered" \
  "Sentry callback sanitizes request/user payloads"

# 2) Audit service TLS and encryption config presence
check_file_exists \
  "backend/modules/shared/infrastructure/audit/src/main/resources/application-prod.yml" \
  "Audit production config exists"
check_pattern \
  "backend/modules/shared/infrastructure/audit/src/main/resources/application-prod.yml" \
  "ssl\.protocol:\s*TLSv1\.3|ssl\.enabled\.protocols:\s*TLSv1\.3" \
  "Audit service enforces TLS 1.3"
check_pattern \
  "backend/modules/shared/infrastructure/audit/src/main/resources/application-prod.yml" \
  "encryption:[[:space:]]*\\$\\{FEATURE_AUDIT_ENCRYPTION:true\\}" \
  "Audit event encryption flag enabled"

# 3) Multi-tenant controls in authentication headers/shared security
check_file_exists \
  "backend/modules/shared/infrastructure/authentication-headers/src/main/java/com/healthdata/authentication/filter/TrustedHeaderAuthFilter.java" \
  "Trusted header auth filter exists"
check_pattern \
  "backend/modules/shared/infrastructure/authentication-headers/src/main/java/com/healthdata/authentication/filter/TrustedHeaderAuthFilter.java" \
  "X-Tenant-ID|tenant" \
  "Tenant-aware auth header handling present"

# 4) Observability content sanity scan: metric names should not include common PHI key words
metric_refs="$(rg -n "(Counter|Timer|Gauge)\\.builder\\(" backend/modules/services backend/modules/shared -g"*.java" || true)"
if echo "$metric_refs" | rg -i "patientName|ssn|socialSecurity|dob|birthDate|phoneNumber|mrn|memberId" >/dev/null 2>&1; then
  fail "Potential PHI markers found in metric builder references"
else
  pass "No obvious PHI markers in metric builder references"
fi

# 5) Operational docs for incident/breach/auth handling
check_file_exists "docs/runbooks/SECURITY_INCIDENT_RESPONSE.md" "Security incident runbook exists"
check_file_exists "docs/runbooks/authentication-failures.md" "Authentication failure runbook exists"
check_file_exists "docs/BACKEND_SENTRY_CONFIGURATION.md" "Backend Sentry configuration doc exists"

if [[ "$failures" -gt 0 ]]; then
  echo "\nHIPAA control validation failed with $failures issue(s)."
  exit 1
fi

echo "\nHIPAA control validation checks passed."

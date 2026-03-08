#!/usr/bin/env bash
set -euo pipefail

APPLY=0
if [[ "${1:-}" == "--apply" ]]; then
  APPLY=1
fi

readonly FORBIDDEN_PATTERNS=(
  'backend/modules/services/*/bin/main/**'
  'backend/modules/services/*/bin/test/**'
  'backend/modules/shared/*/bin/main/**'
  'backend/modules/shared/*/bin/test/**'
  'backend/modules/apps/*/bin/main/**'
  'backend/modules/apps/*/bin/test/**'
  'backend/platform/*/bin/main/**'
  'backend/platform/*/bin/test/**'
  'backend/tools/*/bin/main/**'
  'backend/tools/*/bin/test/**'
)

say() {
  printf '%s\n' "$*"
}

is_forbidden_path() {
  local path="$1"
  local pattern
  for pattern in "${FORBIDDEN_PATTERNS[@]}"; do
    if [[ "$path" == $pattern ]]; then
      return 0
    fi
  done
  return 1
}

main() {
  if [[ $APPLY -ne 1 ]]; then
    say "[hygiene] Dry run. Use --apply to perform cleanup."
    say "[hygiene] Would clean only dirty files that match forbidden generated patterns."
    exit 0
  fi

  local restored=0
  local removed=0

  while IFS= read -r line; do
    [[ -z "$line" ]] && continue

    local status path
    status="${line:0:2}"
    path="${line:3}"

    if [[ "$path" == *" -> "* ]]; then
      path="${path##* -> }"
    fi

    if ! is_forbidden_path "$path"; then
      continue
    fi

    if [[ "$status" == "??" ]]; then
      git clean -fd -- "$path" >/dev/null 2>&1 || true
      removed=$((removed + 1))
    else
      git restore --staged --worktree -- "$path" >/dev/null 2>&1 || true
      restored=$((restored + 1))
    fi
  done < <(git status --porcelain)

  say "[hygiene] Restored tracked forbidden paths: $restored"
  say "[hygiene] Removed untracked forbidden paths: $removed"

  say "[hygiene] Re-running audit..."
  bash scripts/repo-hygiene-audit.sh
}

main "$@"

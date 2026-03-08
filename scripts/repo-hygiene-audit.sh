#!/usr/bin/env bash
set -euo pipefail

CI_MODE=0
if [[ "${1:-}" == "--ci" ]]; then
  CI_MODE=1
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

collect_dirty_paths() {
  git status --porcelain | awk '{print substr($0,4)}' | while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    # For rename entries, only validate the destination path.
    if [[ "$path" == *" -> "* ]]; then
      path="${path##* -> }"
    fi
    printf '%s\n' "$path"
  done
}

main() {
  local failures=0
  local dirty_forbidden=()
  local newly_added_forbidden=()
  local legacy_tracked_count=0

  while IFS= read -r path; do
    if is_forbidden_path "$path"; then
      dirty_forbidden+=("$path")
    fi
  done < <(collect_dirty_paths)

  while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    newly_added_forbidden+=("$path")
  done < <(git diff --name-only --diff-filter=AR HEAD -- "${FORBIDDEN_PATTERNS[@]}" || true)

  legacy_tracked_count="$(git ls-files -- "${FORBIDDEN_PATTERNS[@]}" | wc -l | tr -d ' ')"

  if [[ ${#dirty_forbidden[@]} -gt 0 ]]; then
    failures=1
    say "[hygiene] FAIL: forbidden generated paths are dirty:"
    printf '  - %s\n' "${dirty_forbidden[@]}"
  fi

  if [[ ${#newly_added_forbidden[@]} -gt 0 ]]; then
    failures=1
    say "[hygiene] FAIL: newly added forbidden generated paths detected:"
    printf '  - %s\n' "${newly_added_forbidden[@]}"
  fi

  if [[ $failures -eq 1 ]]; then
    say ""
    say "[hygiene] Suggested recovery:"
    say "  - npm run hygiene:clean"
    say "  - git restore --staged <forbidden path>      # for accidental adds"
    if [[ $CI_MODE -eq 1 ]]; then
      say "[hygiene] CI mode: failing job."
    fi
    exit 1
  fi

  if [[ "${legacy_tracked_count}" -gt 0 ]]; then
    say "[hygiene] WARN: ${legacy_tracked_count} forbidden generated files are historically tracked."
    say "[hygiene] WARN: this audit blocks new additions and dirty state, not legacy baseline files."
  fi

  say "[hygiene] PASS: no forbidden dirty paths or newly added forbidden files found."
}

main "$@"

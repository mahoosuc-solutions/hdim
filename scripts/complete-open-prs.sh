#!/usr/bin/env bash
set -euo pipefail

# Complete open PRs with validation using gh CLI.
# Defaults to dry-run unless CONFIRM=1 is set.

REPO="${REPO:-}"
LIMIT="${LIMIT:-50}"
MERGE_METHOD="${MERGE_METHOD:-squash}" # merge|squash|rebase
REQUIRE_CHECKS="${REQUIRE_CHECKS:-1}"
REQUIRE_APPROVAL="${REQUIRE_APPROVAL:-1}"
SKIP_DRAFTS="${SKIP_DRAFTS:-1}"
WATCH_CHECKS="${WATCH_CHECKS:-0}"
CONFIRM="${CONFIRM:-0}"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI not found. Install gh and authenticate first." >&2
  exit 1
fi

repo_args=()
if [[ -n "$REPO" ]]; then
  repo_args+=(--repo "$REPO")
fi

prs_json=$(gh pr list --state open --limit "$LIMIT" --json number,title,headRefName,baseRefName,isDraft,reviewDecision,mergeable,statusCheckRollup,url "${repo_args[@]}")

python3 - <<'PY' "$prs_json" "$MERGE_METHOD" "$REQUIRE_CHECKS" "$REQUIRE_APPROVAL" "$SKIP_DRAFTS" "$WATCH_CHECKS" "$CONFIRM" "${repo_args[@]}"
import json
import subprocess
import sys

prs = json.loads(sys.argv[1])
merge_method = sys.argv[2]
require_checks = sys.argv[3] == '1'
require_approval = sys.argv[4] == '1'
skip_drafts = sys.argv[5] == '1'
watch_checks = sys.argv[6] == '1'
confirm = sys.argv[7] == '1'
repo_args = sys.argv[8:]

if not prs:
    print('No open PRs found.')
    sys.exit(0)


def check_rollup(pr):
    rollup = pr.get('statusCheckRollup') or []
    states = [c.get('conclusion') for c in rollup if c.get('conclusion')]
    # Accept SUCCESS, NEUTRAL, SKIPPED
    bad = [s for s in states if s not in ('SUCCESS', 'NEUTRAL', 'SKIPPED')]
    pending = any(c.get('status') in ('IN_PROGRESS', 'QUEUED', 'REQUESTED') for c in rollup)
    return bad, pending


def run(cmd):
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    return result.returncode, result.stdout.strip()

print(f"Found {len(prs)} open PR(s).")

for pr in prs:
    number = pr['number']
    title = pr['title']
    is_draft = pr.get('isDraft')
    review = pr.get('reviewDecision')
    mergeable = pr.get('mergeable')

    if skip_drafts and is_draft:
        print(f"- PR #{number}: draft skipped ({title})")
        continue

    bad, pending = check_rollup(pr)
    if require_checks and pending and watch_checks:
        print(f"- PR #{number}: waiting for checks...")
        code, out = run(['gh', 'pr', 'checks', str(number), '--watch', *repo_args])
        if code != 0:
            print(f"  checks failed to watch: {out}")
            continue
        # re-fetch
        code, out = run(['gh', 'pr', 'view', str(number), '--json', 'statusCheckRollup', *repo_args])
        if code != 0:
            print(f"  failed to re-fetch checks: {out}")
            continue
        pr['statusCheckRollup'] = json.loads(out).get('statusCheckRollup')
        bad, pending = check_rollup(pr)

    if require_checks and (bad or pending):
        print(f"- PR #{number}: checks not clean (bad={bad}, pending={pending})")
        continue

    if require_approval and review not in (None, 'APPROVED'):
        print(f"- PR #{number}: review required ({review})")
        continue

    if mergeable == 'CONFLICTING':
        print(f"- PR #{number}: merge conflicts")
        continue

    print(f"- PR #{number}: ready to merge ({title})")
    if not confirm:
        print("  dry-run (set CONFIRM=1 to merge)")
        continue

    code, out = run(['gh', 'pr', 'merge', str(number), '--'+merge_method, '--delete-branch', *repo_args])
    if code != 0:
        print(f"  merge failed: {out}")
    else:
        print("  merged")
PY

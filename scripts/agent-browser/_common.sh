#!/usr/bin/env bash
set -euo pipefail

AB_BASE_URL="${PORTAL_URL:-http://localhost:4200}"
AB_SCREENSHOT_DIR="${SCREENSHOT_DIR:-/tmp/agent-browser-smoke}"
AB_SLEEP_SECS="${AB_SLEEP_SECS:-2}"

require_agent_browser() {
  if ! command -v agent-browser >/dev/null 2>&1; then
    echo "agent-browser is not installed or not on PATH."
    echo "Install: npm install -g agent-browser && agent-browser install"
    exit 1
  fi
}

ab() {
  agent-browser "$@"
}

ensure_portal_reachable() {
  if command -v curl >/dev/null 2>&1; then
    if ! curl -fsS --max-time 5 "${AB_BASE_URL}" >/dev/null; then
      echo "Portal URL not reachable: ${AB_BASE_URL}"
      echo "Make sure the demo stack or UI server is running."
      exit 1
    fi
  fi
}

open_portal() {
  require_agent_browser
  ensure_portal_reachable
  mkdir -p "${AB_SCREENSHOT_DIR}"
  ab open "${AB_BASE_URL}"
  sleep "${AB_SLEEP_SECS}"
}

screenshot() {
  local name="$1"
  ab screenshot "${AB_SCREENSHOT_DIR}/${name}"
}

click_by_label() {
  local label="$1"
  ab find role link click --name "${label}" \
    || ab find role button click --name "${label}" \
    || ab find text "${label}" click
}

assert_text_visible() {
  local text="$1"
  ab find text "${text}"
}

set_role() {
  local role_label="$1"
  ab find role button click --name "View dashboard as ${role_label}"
  sleep "${AB_SLEEP_SECS}"
}

wait_for_heading() {
  local heading="$1"
  ab find role heading --name "${heading}"
}

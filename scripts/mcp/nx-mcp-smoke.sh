#!/usr/bin/env bash
set -euo pipefail

if [[ "${RUN_MCP_SMOKE:-}" != "1" ]]; then
  exit 0
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

tmpfile="$(mktemp)"
cleanup() {
  rm -f "$tmpfile"
}
trap cleanup EXIT

timeout_cmd=()
if command -v timeout >/dev/null 2>&1; then
  timeout_cmd=(timeout 60s)
fi

printf '%s\n' \
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"hdim-nx-mcp-smoke","version":"0.0.0"}}}' \
  '{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}' \
  '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' \
  '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"nx_workspace_path","arguments":{}}}' \
  | "${timeout_cmd[@]}" node scripts/mcp/nx-mcp.mjs >"$tmpfile" || true

grep -Fq '"id":1' "$tmpfile"
grep -Fq '"serverInfo":{"name":"Nx MCP"' "$tmpfile"

grep -Fq '"id":2' "$tmpfile"
grep -Fq '"name":"nx_workspace_path"' "$tmpfile"

grep -Fq '"id":3' "$tmpfile"
grep -Fq "\"text\":\"$ROOT_DIR\"" "$tmpfile"

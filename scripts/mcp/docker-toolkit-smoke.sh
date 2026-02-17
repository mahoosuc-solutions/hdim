#!/usr/bin/env bash
set -euo pipefail

# Smoke test for Docker MCP Toolkit / Gateway integration.
# Requires Docker Desktop with MCP Toolkit enabled.
#
# Usage:
#   bash scripts/mcp/docker-toolkit-smoke.sh duckduckgo github
#   SERVERS="duckduckgo,github" bash scripts/mcp/docker-toolkit-smoke.sh

info() { printf '[mcp-toolkit] %s\n' "$*"; }

docker_bin="${DOCKER_BIN:-docker}"
if grep -qi microsoft /proc/version 2>/dev/null && command -v docker.exe >/dev/null 2>&1; then
  docker_bin="${DOCKER_BIN:-docker.exe}"
fi

servers=()
if [[ "${SERVERS:-}" != "" ]]; then
  IFS=',' read -r -a servers <<<"${SERVERS}"
else
  servers=("$@")
fi

if [[ ${#servers[@]} -eq 0 ]]; then
  servers=("duckduckgo")
fi

servers_csv="$(IFS=,; echo "${servers[*]}")"

info "Checking docker mcp is available..."
"${docker_bin}" mcp version

info "Enabled servers (before):"
"${docker_bin}" mcp server ls

for s in "${servers[@]}"; do
  info "Enabling server: ${s}"
  "${docker_bin}" mcp server enable "${s}"
done

info "Enabled servers (after):"
"${docker_bin}" mcp server ls

info "Dry-running gateway config..."
"${docker_bin}" mcp gateway run --dry-run --transport stdio --servers "${servers_csv}"

info "Done. Next: connect Claude Desktop to 'docker mcp gateway run'."

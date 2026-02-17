#!/usr/bin/env bash
set -euo pipefail

image="${IMAGE:-hdim-platform-mcp:local}"
base_url="${HDIM_BASE_URL:-http://host.docker.internal:18080}"
dry_run="${DRY_RUN:-0}"
connect_client="${CONNECT_CLIENT:-0}"

info() { printf '[hdim-toolkit] %s\n' "$*"; }

engine_bin="${DOCKER_ENGINE_BIN:-docker}"
toolkit_bin="${DOCKER_TOOLKIT_BIN:-docker}"
if grep -qi microsoft /proc/version 2>/dev/null && command -v docker.exe >/dev/null 2>&1; then
  # Prefer native Linux `docker` for builds (path compatibility),
  # but use `docker.exe` for MCP Toolkit integration.
  toolkit_bin="${DOCKER_TOOLKIT_BIN:-docker.exe}"
fi

dockerfile="docker/mcp/hdim-platform-mcp/Dockerfile"
context="."

info "Building MCP server image: ${image}"
"${engine_bin}" build -t "${image}" -f "${dockerfile}" "${context}"

info "Installing/refreshing Toolkit catalog entry (hdim-platform)..."
ENABLE_SERVER=1 DOCKER_TOOLKIT_BIN="${toolkit_bin}" bash scripts/mcp/docker-toolkit-install-hdim-catalog.sh >/dev/null

if [[ "${connect_client}" == "1" ]]; then
  info "Connecting Docker MCP Toolkit to Claude Desktop (global)..."
  "${toolkit_bin}" mcp client connect -q -g claude-desktop
fi

info "Starting Docker MCP Toolkit gateway (stdio)..."
gateway_args=(mcp gateway run --transport stdio --servers hdim-platform)
if [[ "${dry_run}" == "1" ]]; then
  gateway_args+=(--dry-run)
fi

HDIM_BASE_URL="${base_url}" "${toolkit_bin}" "${gateway_args[@]}"

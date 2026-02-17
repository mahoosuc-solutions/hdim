#!/usr/bin/env bash
set -euo pipefail

catalog_name="${CATALOG_NAME:-hdim}"
server_name="${SERVER_NAME:-hdim-platform}"
catalog_file="${CATALOG_FILE:-docker/mcp/catalogs/hdim.yaml}"
enable="${ENABLE_SERVER:-0}"

info() { printf '[hdim-catalog] %s\n' "$*"; }

toolkit_bin="${DOCKER_TOOLKIT_BIN:-docker}"
if grep -qi microsoft /proc/version 2>/dev/null && command -v docker.exe >/dev/null 2>&1; then
  toolkit_bin="${DOCKER_TOOLKIT_BIN:-docker.exe}"
fi

catalog_file_arg="${catalog_file}"
if [[ "${toolkit_bin}" == *.exe ]]; then
  # docker.exe cannot reliably read UNC paths from WSL; stage to Windows drive.
  windows_tmp_dir="${WINDOWS_TMP_DIR:-/mnt/c/Temp}"
  mkdir -p "${windows_tmp_dir}"
  staged="${windows_tmp_dir}/hdim-mcp-catalog.yaml"
  cp "${catalog_file}" "${staged}"
  if command -v wslpath >/dev/null 2>&1; then
    catalog_file_arg="$(wslpath -w "${staged}")"
  else
    catalog_file_arg="${staged}"
  fi
fi

info "Initializing catalogs (ok if already initialized)..."
"${toolkit_bin}" mcp catalog init >/dev/null 2>&1 || true

info "Creating catalog '${catalog_name}' (ok if it exists)..."
"${toolkit_bin}" mcp catalog create "${catalog_name}" >/dev/null 2>&1 || true

info "Adding/updating server '${server_name}' from '${catalog_file}'..."
"${toolkit_bin}" mcp catalog add "${catalog_name}" "${server_name}" "${catalog_file_arg}" --force

info "Catalog contents:"
"${toolkit_bin}" mcp catalog show "${catalog_name}" --format=yaml

if [[ "${enable}" == "1" ]]; then
  info "Enabling server '${server_name}'..."
  "${toolkit_bin}" mcp server enable "${server_name}"
  info "Enabled servers:"
  "${toolkit_bin}" mcp server ls
fi

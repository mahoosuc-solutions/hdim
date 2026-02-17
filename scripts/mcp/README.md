# MCP (Model Context Protocol)

This repo uses the Nx MCP server for workspace-aware tooling.

## Configure

The MCP server entry lives in `.mcp.json` and runs:

- `node scripts/mcp/nx-mcp.mjs`

This wrapper keeps the invocation pinned and cross-platform.

This repo also includes a lightweight platform management MCP server:

- `node scripts/mcp/hdim-platform-mcp.mjs`

And a Docker control-plane MCP server for the HDIM compose stack:

- `node scripts/mcp/hdim-docker-mcp.mjs`

## Validate

Run config checks:

- `npm run test:mcp`

Run the optional end-to-end MCP smoke test:

- `RUN_MCP_SMOKE=1 npm run test:mcp`

## Live Test

Start the server:

- `npm run mcp:nx`
- `npm run mcp:hdim`
- `npm run mcp:docker`
- `npm run mcp:release-gate -- --mode strict`
- `npm run mcp:evidence-pack`
- `npm run mcp:evidence-package`
- `npm run mcp:controlled-restart -- --service gateway-edge --warmup-timeout 180 --stable-passes 2`
- `npm run mcp:controlled-restart -- --service gateway-edge --append-to-latest-bundle`
- `npm run mcp:controlled-restart -- --service gateway-edge --dry-run --skip-runtime-checks`
- `npm run mcp:operator:go-no-go -- --mode strict`
- `npm run mcp:spec:check`
- `npm run mcp:spec:sync`
- `npm run mcp:pretest`

Or run the OS-specific smoke script directly:

- macOS/Linux: `RUN_MCP_SMOKE=1 bash scripts/mcp/nx-mcp-smoke.sh`
- Windows: `RUN_MCP_SMOKE=1 powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mcp/nx-mcp-smoke.ps1`

## Docker Host (Claude Desktop)

This runs the MCP server inside Docker, but still speaks stdio to Claude Desktop.

1) Start the host container:

- `docker compose -f docker-compose.mcp.yml up -d`

2) Point Claude Desktop at it using `docker exec`:

```json
{
  "mcpServers": {
    "nx-mcp": {
      "command": "docker",
      "args": ["exec", "-i", "hdim-nx-mcp", "node", "scripts/mcp/nx-mcp.mjs"]
    }
  }
}
```

## WSL Host (Claude Desktop on Windows)

If this repo lives in WSL, Claude Desktop (Windows) can run the MCP servers via `wsl.exe` (no Docker required).

Example `claude_desktop_config.json` entries (replace `<distro>` and `<repo-path>`):

```json
{
  "mcpServers": {
    "nx-mcp": {
      "command": "wsl.exe",
      "args": ["-d", "<distro>", "--", "bash", "-lc", "cd <repo-path> && node scripts/mcp/nx-mcp.mjs"]
    },
    "hdim-platform": {
      "command": "wsl.exe",
      "args": ["-d", "<distro>", "--", "bash", "-lc", "cd <repo-path> && node scripts/mcp/hdim-platform-mcp.mjs"]
    }
  }
}
```

Tip: if the platform gateway is also running inside WSL, `HDIM_BASE_URL=http://localhost:18080` works by default.

## Docker MCP Toolkit (Gateway)

If you have Docker Desktop’s MCP Toolkit enabled, you can also run the Docker MCP gateway in `stdio` mode and connect it to Claude Desktop.

Enable one or more Toolkit servers (example: `duckduckgo`):

- `docker mcp server enable duckduckgo`

Run the gateway directly (stdio):

- `docker mcp gateway run --transport stdio --servers duckduckgo`

Or dry-run the gateway config (no listening):

- `docker mcp gateway run --dry-run --transport stdio --servers duckduckgo`

Connect Docker MCP Toolkit to Claude Desktop automatically:

- `docker mcp client connect -g claude-desktop`

Smoke test scripts:

- macOS/Linux: `bash scripts/mcp/docker-toolkit-smoke.sh`
- Windows: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mcp/docker-toolkit-smoke.ps1`

Note: the Toolkit gateway manages Toolkit servers. Hosting `nx-mcp` inside the Toolkit gateway is a separate packaging step (OCI image + catalog/registry entry).

## HDIM Docker MCP (Local/WSL/VS Code)

`hdim-docker` is intended to run from your workspace (not from `mcp-edge-*` of another project).

It provides Docker compose controls for HDIM:

- `hdim_docker_info`
- `hdim_docker_ps`
- `hdim_docker_up`
- `hdim_docker_down`
- `hdim_docker_logs`
- `hdim_gateway_validate`
- `hdim_demo_seed`
- `hdim_system_validate`
- `hdim_seed_diagnostics`
- `hdim_live_readiness`
- `hdim_topology_report`
- `hdim_config_audit`
- `hdim_service_catalog`
- `hdim_service_config_contracts`
- `hdim_policy_registry`
- `hdim_service_restart_plan`
- `hdim_service_operate`
- `hdim_release_artifact_diff`
- `hdim_release_evidence_pack`
- `hdim_tenant_isolation_check`
- `hdim_release_gate`

Defaults:

- compose file: `docker-compose.demo.yml` (`HDIM_COMPOSE_FILE` override)
- docker command: `docker` (`DOCKER_BIN` override)
- gateway base URL: `http://localhost:18080` (`HDIM_BASE_URL` override)
- seed tool is non-interactive with bounded request timeout (`CURL_MAX_TIME`)
- live readiness uses a 30s seed request timeout by default
- gateway validate supports deterministic warmup gating (`warmupTimeoutSecs`, `pollIntervalMs`, `stablePasses`)
- release gate and tenant isolation support `policyMode` (`strict` or `permissive`) and tenant no-header allowlists

Context-aware release gate runner:

- command: `npm run mcp:release-gate -- --mode strict|permissive`
- artifacts: `logs/mcp-reports/release-gate-<timestamp>.json` and `.md`
- runbook: `docs/runbooks/MCP_CONTEXT_AWARE_RELEASE_GATE.md`
- CI/CD: `.github/workflows/mcp-release-gate.yml` runs on code changes (push/PR), not nightly schedule
- profiles: `scripts/mcp/release-gate-policy.json` (`--profile local-dev|staging|production`)
- evidence pack: `npm run mcp:evidence-pack -- --report-dir logs/mcp-reports --output-dir logs/mcp-reports`
- deployment bundle: `npm run mcp:evidence-package -- --report-dir logs/mcp-reports --out-dir logs/mcp-reports/packages`
- controlled restart smoke: `npm run mcp:controlled-restart -- --service gateway-edge --warmup-timeout 180 --stable-passes 2`
  - prerequisite: demo stack is running (`docker compose -f docker-compose.demo.yml up -d`)

Pre-test checklist runner:

- command: `npm run mcp:pretest`
- checks: MCP tests, compose status, gateway + seeding endpoints, release-gate permissive + strict evaluation
- output: single go/no-go summary plus auto-generated release evidence pack

### HDIM Platform MCP via Toolkit

Build + run the `hdim-platform` MCP server as a Toolkit-managed server (catalog + local image):

- macOS/Linux: `bash scripts/mcp/docker-toolkit-hdim-platform.sh`
- Windows: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mcp/docker-toolkit-hdim-platform.ps1`

Image build details: `docker/mcp/hdim-platform-mcp/Dockerfile`

WSL note: the bash scripts use Linux `docker` for image builds, and prefer `docker.exe` for MCP Toolkit commands when available.

Optional flags:

- Dry-run only: `DRY_RUN=1 bash scripts/mcp/docker-toolkit-hdim-platform.sh`
- Also connect Toolkit to Claude Desktop: `CONNECT_CLIENT=1 bash scripts/mcp/docker-toolkit-hdim-platform.sh`

### Install as a Toolkit-managed server (enable by name)

This adds an `hdim` catalog and registers `hdim-platform` so you can `docker mcp server enable hdim-platform`.

- macOS/Linux: `bash scripts/mcp/docker-toolkit-install-hdim-catalog.sh`
- Windows: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mcp/docker-toolkit-install-hdim-catalog.ps1`

To also enable the server during install:

- macOS/Linux: `ENABLE_SERVER=1 bash scripts/mcp/docker-toolkit-install-hdim-catalog.sh`
- Windows: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mcp/docker-toolkit-install-hdim-catalog.ps1 -Enable`

Catalog file: `docker/mcp/catalogs/hdim.yaml`

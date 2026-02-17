param(
  [string[]]$Servers = @("duckduckgo")
)

# Smoke test for Docker MCP Toolkit / Gateway integration.
# Requires Docker Desktop with MCP Toolkit enabled.

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host ("[mcp-toolkit] " + $msg) }

$serversCsv = ($Servers -join ",")

Info "Checking docker mcp is available..."
docker mcp version | Out-Host

Info "Enabled servers (before):"
docker mcp server ls | Out-Host

foreach ($s in $Servers) {
  Info "Enabling server: $s"
  docker mcp server enable $s | Out-Host
}

Info "Enabled servers (after):"
docker mcp server ls | Out-Host

Info "Dry-running gateway config..."
docker mcp gateway run --dry-run --transport stdio --servers $serversCsv | Out-Host

Info "Done. Next: connect Claude Desktop to 'docker mcp gateway run'."

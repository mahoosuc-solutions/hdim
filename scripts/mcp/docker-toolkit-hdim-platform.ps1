param(
  [string]$Image = "hdim-platform-mcp:local",
  [string]$BaseUrl = "http://host.docker.internal:18080",
  [switch]$DryRun,
  [switch]$ConnectClient
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host ("[hdim-toolkit] " + $msg) }

Info "Building MCP server image: $Image"
docker build -t $Image -f docker/mcp/hdim-platform-mcp/Dockerfile .

Info "Installing/refeshing Toolkit catalog entry (hdim-platform)..."
& "$PSScriptRoot/docker-toolkit-install-hdim-catalog.ps1" -Enable | Out-Host

if ($ConnectClient) {
  Info "Connecting Docker MCP Toolkit to Claude Desktop (global)..."
  docker mcp client connect -q -g claude-desktop | Out-Host
}

Info "Starting Docker MCP Toolkit gateway (stdio)..."
$env:HDIM_BASE_URL = $BaseUrl
$args = @("mcp", "gateway", "run", "--transport", "stdio", "--servers", "hdim-platform")
if ($DryRun) { $args += "--dry-run" }
docker @args

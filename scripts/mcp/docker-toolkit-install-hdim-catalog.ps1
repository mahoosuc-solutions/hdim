param(
  [string]$CatalogName = "hdim",
  [string]$ServerName = "hdim-platform",
  [string]$CatalogFile = "docker/mcp/catalogs/hdim.yaml",
  [switch]$Enable
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host ("[hdim-catalog] " + $msg) }

Info "Initializing catalogs (ok if already initialized)..."
try { docker mcp catalog init | Out-Host } catch { }

Info "Creating catalog '$CatalogName' (ok if it exists)..."
try { docker mcp catalog create $CatalogName | Out-Host } catch { }

Info "Adding/updating server '$ServerName' from '$CatalogFile'..."
docker mcp catalog add $CatalogName $ServerName $CatalogFile --force | Out-Host

Info "Catalog contents:"
docker mcp catalog show $CatalogName --format=yaml | Out-Host

if ($Enable) {
  Info "Enabling server '$ServerName'..."
  docker mcp server enable $ServerName | Out-Host
  Info "Enabled servers:"
  docker mcp server ls | Out-Host
}

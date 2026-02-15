param()

if ($env:RUN_MCP_SMOKE -ne "1") { exit 0 }

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\\..")).Path
Set-Location $root

$lines = @(
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"hdim-nx-mcp-smoke","version":"0.0.0"}}}'
  '{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}'
  '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
  '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"nx_workspace_path","arguments":{}}}'
)

$output = $lines | node scripts/mcp/nx-mcp.mjs 2>$null

if ($output -notmatch '"id":1') { throw "Missing initialize response" }
if ($output -notmatch '"serverInfo":\\{"name":"Nx MCP"') { throw "Missing Nx MCP serverInfo" }
if ($output -notmatch '"id":2') { throw "Missing tools/list response" }
if ($output -notmatch '"name":"nx_workspace_path"') { throw "Missing nx_workspace_path tool" }
if ($output -notmatch '"id":3') { throw "Missing tools/call response" }
if ($output -notmatch [regex]::Escape("\"text\":\"$root\"")) { throw "workspace_path mismatch" }


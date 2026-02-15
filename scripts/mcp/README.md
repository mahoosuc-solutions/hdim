# MCP (Model Context Protocol)

This repo uses the Nx MCP server for workspace-aware tooling.

## Configure

The MCP server entry lives in `.mcp.json` and runs:

- `node scripts/mcp/nx-mcp.mjs`

This wrapper keeps the invocation pinned and cross-platform.

## Validate

Run config checks:

- `npm run test:mcp`

Run the optional end-to-end MCP smoke test:

- `RUN_MCP_SMOKE=1 npm run test:mcp`

## Live Test

Start the server:

- `npm run mcp:nx`

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

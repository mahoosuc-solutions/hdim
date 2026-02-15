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


import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(dirname, '..', '..');

test('.mcp.json pins nx-mcp and uses wrapper script', () => {
  const mcpConfigPath = path.join(repoRoot, '.mcp.json');
  const config = JSON.parse(readFileSync(mcpConfigPath, 'utf8'));

  assert.equal(config.mcpServers?.['nx-mcp']?.type, 'stdio');
  assert.equal(config.mcpServers?.['nx-mcp']?.command, 'node');
  assert.deepEqual(config.mcpServers?.['nx-mcp']?.args, ['scripts/mcp/nx-mcp.mjs']);

  const packageJsonPath = path.join(repoRoot, 'package.json');
  const pkg = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
  assert.equal(pkg.devDependencies?.['nx-mcp'], '0.21.0');

  const wrapperPath = path.join(repoRoot, 'scripts', 'mcp', 'nx-mcp.mjs');
  const wrapper = readFileSync(wrapperPath, 'utf8');
  assert.match(wrapper, /--transport/);
  assert.match(wrapper, /stdio/);
  assert.match(wrapper, /--disableTelemetry/);
});

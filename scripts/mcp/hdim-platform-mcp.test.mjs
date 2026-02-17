import assert from 'node:assert/strict';
import test from 'node:test';

import { createServer, normalizeBaseUrl, pathToUrl } from './hdim-platform-mcp.mjs';

test('normalizeBaseUrl defaults and trims', () => {
  assert.equal(normalizeBaseUrl('http://localhost:18080/'), 'http://localhost:18080');
});

test('pathToUrl joins baseUrl and path safely', () => {
  assert.equal(
    pathToUrl('http://localhost:18080', '/actuator/health'),
    'http://localhost:18080/actuator/health',
  );
});

test('pathToUrl rejects invalid paths', () => {
  assert.throws(() => pathToUrl('http://localhost:18080', 'actuator/health'));
  assert.throws(() => pathToUrl('http://localhost:18080', '//evil.com/'));
  assert.throws(() => pathToUrl('http://localhost:18080', '/http://evil.com/'));
});

test('server registers expected tools', () => {
  const server = createServer();
  const toolNames = Object.keys(server._registeredTools ?? {});

  assert.ok(toolNames.includes('hdim_info'));
  assert.ok(toolNames.includes('hdim_http_get'));
  assert.ok(toolNames.includes('hdim_health_check'));
  assert.ok(toolNames.includes('hdim_fhir_metadata'));
});


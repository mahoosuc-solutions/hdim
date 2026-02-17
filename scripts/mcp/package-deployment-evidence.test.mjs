import assert from 'node:assert/strict';
import test from 'node:test';

import { parseArgs } from './package-deployment-evidence.mjs';

test('parseArgs default values', () => {
  const parsed = parseArgs([]);
  assert.equal(parsed.reportDir, 'logs/mcp-reports');
  assert.equal(parsed.outDir, 'logs/mcp-reports/packages');
});

test('parseArgs explicit values', () => {
  const parsed = parseArgs(['--report-dir', 'tmp/reports', '--out-dir', 'tmp/bundles']);
  assert.equal(parsed.reportDir, 'tmp/reports');
  assert.equal(parsed.outDir, 'tmp/bundles');
});

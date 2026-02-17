import assert from 'node:assert/strict';
import test from 'node:test';

import { parseArgs } from './build-release-evidence-pack.mjs';

test('parseArgs defaults', () => {
  const parsed = parseArgs([]);
  assert.equal(parsed.reportDir, 'logs/mcp-reports');
  assert.equal(parsed.outputDir, 'logs/mcp-reports');
  assert.equal(parsed.includeDiff, true);
});

test('parseArgs handles explicit flags', () => {
  const parsed = parseArgs([
    '--report-dir',
    'tmp/reports',
    '--output-dir',
    'tmp/out',
    '--no-diff',
  ]);
  assert.equal(parsed.reportDir, 'tmp/reports');
  assert.equal(parsed.outputDir, 'tmp/out');
  assert.equal(parsed.includeDiff, false);
});

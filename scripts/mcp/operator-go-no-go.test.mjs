import assert from 'node:assert/strict';
import test from 'node:test';

import { parseArgs, parseJsonFromStdout, summarizeMarkdown } from './operator-go-no-go.mjs';

test('parseArgs defaults', () => {
  const parsed = parseArgs([]);
  assert.equal(parsed.outDir, 'logs/mcp-reports');
  assert.equal(parsed.bundleOutDir, 'logs/mcp-reports/packages');
  assert.equal(parsed.policyMode, 'strict');
  assert.equal(parsed.requestTimeoutSecs, 8);
  assert.equal(parsed.includePretest, true);
  assert.equal(parsed.includeControlledRestart, true);
  assert.equal(parsed.controlledRestartDryRun, false);
  assert.equal(parsed.controlledRestartSkipRuntimeChecks, false);
});

test('parseArgs explicit toggles', () => {
  const parsed = parseArgs([
    '--out-dir',
    'tmp/reports',
    '--bundle-out-dir',
    'tmp/bundles',
    '--mode',
    'permissive',
    '--request-timeout',
    '11',
    '--service',
    'gateway-admin-service',
    '--no-pretest',
    '--no-controlled-restart',
    '--controlled-restart-dry-run',
    '--controlled-restart-skip-runtime',
  ]);
  assert.equal(parsed.outDir, 'tmp/reports');
  assert.equal(parsed.bundleOutDir, 'tmp/bundles');
  assert.equal(parsed.policyMode, 'permissive');
  assert.equal(parsed.requestTimeoutSecs, 11);
  assert.equal(parsed.service, 'gateway-admin-service');
  assert.equal(parsed.includePretest, false);
  assert.equal(parsed.includeControlledRestart, false);
  assert.equal(parsed.controlledRestartDryRun, true);
  assert.equal(parsed.controlledRestartSkipRuntimeChecks, true);
});

test('parseJsonFromStdout parses trailing JSON with preface noise', () => {
  const parsed = parseJsonFromStdout('line one\nline two\n{"go":true,"x":1}\n');
  assert.deepEqual(parsed, { go: true, x: 1 });
});

test('summarizeMarkdown renders go decision and step results', () => {
  const markdown = summarizeMarkdown(
    {
      go: false,
      inputs: {
        includePretest: true,
        includeControlledRestart: true,
      },
      steps: [
        { name: 'pretest', exitCode: 0, pass: true },
        { name: 'release-gate', exitCode: 2, pass: false },
      ],
      releaseGate: { pass: false, summary: { runningServices: 19, tenantPolicyPass: false, readiness: true } },
      controlledRestart: { pass: true, restartOk: true, gatewayHealthy: true, dryRun: false },
    },
    '20260216-200000',
  );
  assert.match(markdown, /MCP Operator Go\/No-Go Report/);
  assert.match(markdown, /Go decision: false/);
  assert.match(markdown, /release-gate: exitCode=2 pass=false/);
  assert.match(markdown, /runningServices: 19/);
});

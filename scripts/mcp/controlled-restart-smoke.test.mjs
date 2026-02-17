import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';

import {
  appendRestartArtifactsToBundle,
  parseArgs,
  resolveBundleDir,
  summarizeResult,
} from './controlled-restart-smoke.mjs';

test('parseArgs returns defaults', () => {
  const parsed = parseArgs([]);
  assert.equal(parsed.composeFile, 'docker-compose.demo.yml');
  assert.equal(parsed.gatewayUrl, 'http://localhost:18080');
  assert.equal(parsed.service, 'gateway-edge');
  assert.equal(parsed.outDir, 'logs/mcp-reports');
  assert.equal(parsed.requestTimeoutSecs, 5);
  assert.equal(parsed.warmupTimeoutSecs, 120);
  assert.equal(parsed.pollIntervalMs, 1000);
  assert.equal(parsed.stablePasses, 1);
  assert.equal(parsed.appendToLatestBundle, false);
  assert.equal(parsed.bundleDir, '');
});

test('parseArgs applies explicit options', () => {
  const parsed = parseArgs([
    '--compose-file',
    'docker-compose.custom.yml',
    '--gateway-url',
    'http://localhost:28080',
    '--service',
    'gateway-admin-service',
    '--out-dir',
    'tmp/reports',
    '--request-timeout',
    '9',
    '--warmup-timeout',
    '180',
    '--poll-interval-ms',
    '1500',
    '--stable-passes',
    '2',
    '--append-to-latest-bundle',
    '--bundle-dir',
    'logs/mcp-reports/packages/deployment-evidence-test',
  ]);

  assert.equal(parsed.composeFile, 'docker-compose.custom.yml');
  assert.equal(parsed.gatewayUrl, 'http://localhost:28080');
  assert.equal(parsed.service, 'gateway-admin-service');
  assert.equal(parsed.outDir, 'tmp/reports');
  assert.equal(parsed.requestTimeoutSecs, 9);
  assert.equal(parsed.warmupTimeoutSecs, 180);
  assert.equal(parsed.pollIntervalMs, 1500);
  assert.equal(parsed.stablePasses, 2);
  assert.equal(parsed.appendToLatestBundle, true);
  assert.equal(parsed.bundleDir, 'logs/mcp-reports/packages/deployment-evidence-test');
});

test('summarizeResult includes key health details', () => {
  const summary = summarizeResult(
    {
      restart: { result: { ok: true, exitCode: 0 } },
      gatewayValidate: {
        allHealthy: false,
        warmup: { attempts: 4, achievedStablePasses: false },
        checks: [
          { endpoint: '/actuator/health', status: 200, ok: true },
          { endpoint: '/fhir/metadata', status: 502, ok: false },
        ],
      },
    },
    {
      service: 'gateway-edge',
      composeFile: 'docker-compose.demo.yml',
    },
    '20260216-190000',
  );

  assert.match(summary, /MCP Controlled Restart Report/);
  assert.match(summary, /Service: gateway-edge/);
  assert.match(summary, /Restart ok: true/);
  assert.match(summary, /Gateway healthy after warmup: false/);
  assert.match(summary, /\/fhir\/metadata: 502 \(fail\)/);
});

test('resolveBundleDir prefers explicit bundleDir', () => {
  const resolved = resolveBundleDir({
    appendToLatestBundle: true,
    bundleDir: 'logs/mcp-reports/packages/deployment-evidence-abc',
  });
  assert.equal(resolved, 'logs/mcp-reports/packages/deployment-evidence-abc');
});

test('appendRestartArtifactsToBundle copies files and updates summary', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'mcp-restart-bundle-'));
  const bundleDir = path.join(root, 'deployment-evidence-test');
  mkdirSync(bundleDir, { recursive: true });

  const jsonReport = path.join(root, 'controlled-restart-20260216-000001.json');
  const markdownReport = path.join(root, 'controlled-restart-20260216-000001.md');
  writeFileSync(jsonReport, '{"pass":true}');
  writeFileSync(markdownReport, '# report');
  writeFileSync(path.join(bundleDir, 'DEPLOYMENT_EVIDENCE_SUMMARY.md'), '# Deployment Evidence Bundle\n');

  const result = appendRestartArtifactsToBundle({ bundleDir, jsonReport, markdownReport });
  assert.equal(result.appended, true);
  const summary = readFileSync(path.join(bundleDir, 'DEPLOYMENT_EVIDENCE_SUMMARY.md'), 'utf8');
  assert.match(summary, /## Controlled Restart Report \(Latest\)/);
  assert.match(summary, /controlled-restart-20260216-000001\.json/);
});

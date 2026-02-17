import assert from 'node:assert/strict';
import test from 'node:test';

import { parseArgs, summarizeGate, toTimestamp } from './context-aware-release-gate.mjs';

test('toTimestamp creates sortable UTC token', () => {
  const ts = toTimestamp(new Date('2026-02-16T10:30:45Z'));
  assert.equal(ts, '20260216-103045');
});

test('parseArgs handles policy and allowlist flags', () => {
  const parsed = parseArgs([
    '--mode',
    'permissive',
    '--allow-no-header',
    '/fhir/metadata',
    '--allow-no-header',
    '/actuator',
    '--no-system-validate',
    '--skip-frontend',
    '--skip-fhir-query',
    '--request-timeout',
    '6',
    '--compose-file',
    'docker-compose.demo.yml',
  ]);

  assert.equal(parsed.policyMode, 'permissive');
  assert.deepEqual(parsed.noHeaderAllowlist, ['/fhir/metadata', '/actuator']);
  assert.equal(parsed.runSystemValidate, false);
  assert.equal(parsed.skipFrontend, true);
  assert.equal(parsed.skipFhirQuery, true);
  assert.equal(parsed.requestTimeoutSecs, 6);
  assert.equal(parsed.composeFile, 'docker-compose.demo.yml');
});

test('summarizeGate includes warnings and violations counts', () => {
  const markdown = summarizeGate(
    {
      pass: false,
      summary: {
        runningServices: 18,
        readiness: true,
        tenantPolicyPass: false,
        missingRequiredConfig: 0,
      },
      tenantIsolation: {
        warnings: [{ endpoint: '/a', reason: 'warn' }],
        violations: [{ endpoint: '/b', reason: 'fail' }],
      },
    },
    { policyMode: 'strict' },
    '20260216-103045',
  );

  assert.match(markdown, /Policy mode: strict/);
  assert.match(markdown, /Tenant warnings: 1/);
  assert.match(markdown, /Tenant violations: 1/);
  assert.match(markdown, /FAIL \/b: fail/);
});

import assert from 'node:assert/strict';
import test from 'node:test';

import {
  parseArgs,
  renderExampleSection,
  splitSpecAroundSection21,
  syncSpec,
} from './sync-reference-spec-examples.mjs';

test('parseArgs defaults to check mode', () => {
  const parsed = parseArgs([]);
  assert.equal(parsed.mode, 'check');
  assert.match(parsed.specPath, /MCP_IMPLEMENTATION_REFERENCE_SPEC\.md$/);
  assert.match(parsed.examplesPath, /MCP_IMPLEMENTATION_REFERENCE_SPEC\.examples\.json$/);
});

test('parseArgs supports explicit write mode and paths', () => {
  const parsed = parseArgs([
    '--write',
    '--spec',
    'docs/spec.md',
    '--examples',
    'docs/examples.json',
  ]);
  assert.equal(parsed.mode, 'write');
  assert.equal(parsed.specPath, 'docs/spec.md');
  assert.equal(parsed.examplesPath, 'docs/examples.json');
});

test('renderExampleSection emits required headings', () => {
  const section = renderExampleSection({
    examples: {
      hdim_docker_ps: { request: { name: 'hdim_docker_ps' }, response: { ok: true } },
      hdim_service_operate: { request: { name: 'hdim_service_operate' }, response: { dryRun: true } },
      hdim_gateway_validate: { request: { name: 'hdim_gateway_validate' }, response: { allHealthy: true } },
      hdim_release_gate: { request: { name: 'hdim_release_gate' }, response: { pass: false } },
      hdim_release_evidence_pack: {
        request: { name: 'hdim_release_evidence_pack' },
        response: { includeDiff: true },
      },
    },
  });

  assert.match(section, /## 21\. Example Payload Appendix/);
  assert.match(section, /### 21\.1 `hdim_docker_ps` example/);
  assert.match(section, /### 21\.5 `hdim_release_evidence_pack` example/);
});

test('syncSpec replaces appendix section deterministically', () => {
  const spec = `# Title

## 20. Tool Schema Appendix (Reference)

something

## 21. Example Payload Appendix

old
`;

  const next = syncSpec(spec, {
    examples: {
      hdim_docker_ps: { request: { name: 'hdim_docker_ps' }, response: { ok: true } },
      hdim_service_operate: { request: { name: 'hdim_service_operate' }, response: { dryRun: true } },
      hdim_gateway_validate: { request: { name: 'hdim_gateway_validate' }, response: { allHealthy: true } },
      hdim_release_gate: { request: { name: 'hdim_release_gate' }, response: { pass: false } },
      hdim_release_evidence_pack: {
        request: { name: 'hdim_release_evidence_pack' },
        response: { includeDiff: true },
      },
    },
  });

  assert.match(next, /### 21\.1 `hdim_docker_ps` example/);
  assert.match(next, /"name": "hdim_release_gate"/);
  assert.ok(!/\nold\n/.test(next));
});

test('syncSpec preserves content after section 21', () => {
  const spec = `# Title

## 21. Example Payload Appendix

old

## 22. Failure-Mode Appendix

tail
`;

  const next = syncSpec(spec, {
    examples: {
      hdim_docker_ps: { request: { name: 'hdim_docker_ps' }, response: { ok: true } },
      hdim_service_operate: { request: { name: 'hdim_service_operate' }, response: { dryRun: true } },
      hdim_gateway_validate: { request: { name: 'hdim_gateway_validate' }, response: { allHealthy: true } },
      hdim_release_gate: { request: { name: 'hdim_release_gate' }, response: { pass: false } },
      hdim_release_evidence_pack: {
        request: { name: 'hdim_release_evidence_pack' },
        response: { includeDiff: true },
      },
    },
  });

  assert.match(next, /## 22\. Failure-Mode Appendix/);
  assert.match(next, /\ntail\n/);
});

test('splitSpecAroundSection21 throws when section missing', () => {
  assert.throws(() => splitSpecAroundSection21('# no section'));
});

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(dirname, '..', '..');
const specPath = path.join(repoRoot, 'docs', 'runbooks', 'MCP_IMPLEMENTATION_REFERENCE_SPEC.md');
const schemaPath = path.join(
  repoRoot,
  'docs',
  'runbooks',
  'MCP_IMPLEMENTATION_REFERENCE_SPEC.schema.json',
);
const examplesPath = path.join(
  repoRoot,
  'docs',
  'runbooks',
  'MCP_IMPLEMENTATION_REFERENCE_SPEC.examples.json',
);

function resolveRef(rootSchema, ref) {
  assert.ok(ref.startsWith('#/'), `Unsupported ref: ${ref}`);
  return ref
    .slice(2)
    .split('/')
    .reduce((acc, key) => acc?.[key], rootSchema);
}

function validateJsonSchema(rootSchema, schema, value, ctx = '$', errors = []) {
  if (!schema || typeof schema !== 'object') return errors;

  if (schema.$ref) {
    const resolved = resolveRef(rootSchema, schema.$ref);
    return validateJsonSchema(rootSchema, resolved, value, ctx, errors);
  }

  if (Array.isArray(schema.allOf)) {
    for (const entry of schema.allOf) validateJsonSchema(rootSchema, entry, value, ctx, errors);
  }

  if (schema.const !== undefined && value !== schema.const) {
    errors.push(`${ctx}: expected const ${JSON.stringify(schema.const)}, got ${JSON.stringify(value)}`);
  }

  if (Array.isArray(schema.enum) && !schema.enum.includes(value)) {
    errors.push(`${ctx}: expected enum ${JSON.stringify(schema.enum)}, got ${JSON.stringify(value)}`);
  }

  if (schema.type) {
    const types = Array.isArray(schema.type) ? schema.type : [schema.type];
    const matchesType = types.some((type) => {
      if (type === 'null') return value === null;
      if (type === 'array') return Array.isArray(value);
      if (type === 'integer') return Number.isInteger(value);
      return typeof value === type;
    });
    if (!matchesType) {
      errors.push(`${ctx}: expected type ${JSON.stringify(types)}, got ${value === null ? 'null' : typeof value}`);
      return errors;
    }
  }

  if (schema.type === 'object' || (Array.isArray(schema.type) && schema.type.includes('object'))) {
    const required = schema.required ?? [];
    for (const key of required) {
      if (!Object.prototype.hasOwnProperty.call(value ?? {}, key)) {
        errors.push(`${ctx}: missing required property '${key}'`);
      }
    }

    const properties = schema.properties ?? {};
    for (const [key, subschema] of Object.entries(properties)) {
      if (Object.prototype.hasOwnProperty.call(value ?? {}, key)) {
        validateJsonSchema(rootSchema, subschema, value[key], `${ctx}.${key}`, errors);
      }
    }

    if (schema.additionalProperties === false && value && typeof value === 'object' && !Array.isArray(value)) {
      for (const key of Object.keys(value)) {
        if (!Object.prototype.hasOwnProperty.call(properties, key)) {
          errors.push(`${ctx}: additional property '${key}' is not allowed`);
        }
      }
    }
  }

  if (schema.type === 'array' || (Array.isArray(schema.type) && schema.type.includes('array'))) {
    const minItems = schema.minItems ?? 0;
    if (Array.isArray(value)) {
      if (value.length < minItems) {
        errors.push(`${ctx}: expected minItems ${minItems}, got ${value.length}`);
      }
      if (schema.items) {
        value.forEach((item, index) =>
          validateJsonSchema(rootSchema, schema.items, item, `${ctx}[${index}]`, errors),
        );
      }
    }
  }

  return errors;
}

test('MCP implementation reference spec exists with required sections', () => {
  const spec = readFileSync(specPath, 'utf8');

  const requiredHeadings = [
    '# MCP Implementation Reference Specification',
    '## 1. Purpose',
    '## 2. Scope',
    '## 3. Runtime & Identity',
    '## 6. Current Tool Surface (Normative)',
    '## 9. Warmup & Readiness Semantics',
    '## 12. Release Gate Decision Contract',
    '## 13. Evidence Artifact Contract',
    '## 16. Testing Requirements',
    '## 17. CLI Runner Contracts',
    '## 19. Planned Extensions (v1.1)',
    '## 20. Tool Schema Appendix (Reference)',
    '### 20.1 `hdim_docker_ps`',
    '### 20.2 `hdim_service_operate`',
    '### 20.3 `hdim_gateway_validate`',
    '### 20.4 `hdim_release_gate`',
    '### 20.5 `hdim_release_evidence_pack`',
    '### 20.6 `hdim_tenant_isolation_check`',
    '## 21. Example Payload Appendix',
    '### 21.1 `hdim_docker_ps` example',
    '### 21.2 `hdim_service_operate` dry-run example',
    '### 21.3 `hdim_gateway_validate` warmup example',
    '### 21.4 `hdim_release_gate` example',
    '### 21.5 `hdim_release_evidence_pack` example',
    '## 22. Failure-Mode Appendix',
    '### 22.1 Gateway warmup timeout (readiness false)',
    '### 22.2 Strict tenant policy violation',
    '### 22.3 Docker socket permission denied',
  ];

  for (const heading of requiredHeadings) {
    assert.match(spec, new RegExp(heading.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }

  const requiredTools = [
    '`hdim_gateway_validate`',
    '`hdim_release_gate`',
    '`hdim_release_evidence_pack`',
    '`hdim_service_operate`',
    '`hdim_tenant_isolation_check`',
  ];

  for (const tool of requiredTools) {
    assert.match(spec, new RegExp(tool.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }

  const requiredSchemaFields = [
    'warmup.stablePassesRequired',
    'warmup.history[]',
    'summary.runningServices',
    'summary.tenantPolicyPass',
    'evidencePackJson',
    'commandPreview',
  ];

  for (const field of requiredSchemaFields) {
    assert.match(spec, new RegExp(field.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }

  const requiredExampleSnippets = [
    '"name": "hdim_docker_ps"',
    '"name": "hdim_service_operate"',
    '"dryRun": true',
    '"name": "hdim_gateway_validate"',
    '"stablePasses": 1',
    '"name": "hdim_release_gate"',
    '"name": "hdim_release_evidence_pack"',
    '"includeDiff": true',
  ];

  for (const snippet of requiredExampleSnippets) {
    assert.match(spec, new RegExp(snippet.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }

  const requiredFailureSignals = [
    'allHealthy: false',
    'warmup.achievedStablePasses: false',
    'summary.tenantPolicyPass: false',
    'tenantIsolation.violations[]',
    'docker socket permission error',
  ];

  for (const signal of requiredFailureSignals) {
    assert.match(spec, new RegExp(signal.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }
});

test('schema companion validates examples and markdown appendix round-trip', () => {
  const schema = JSON.parse(readFileSync(schemaPath, 'utf8'));
  const examples = JSON.parse(readFileSync(examplesPath, 'utf8'));
  const spec = readFileSync(specPath, 'utf8');

  const errors = validateJsonSchema(schema, schema, examples);
  assert.deepEqual(errors, [], `schema validation failed:\n${errors.join('\n')}`);

  const orderedToolSections = [
    'hdim_docker_ps',
    'hdim_service_operate',
    'hdim_gateway_validate',
    'hdim_release_gate',
    'hdim_release_evidence_pack',
  ];

  for (const tool of orderedToolSections) {
    const headingLine = spec
      .split('\n')
      .find((line) => line.startsWith('### 21.') && line.includes(`\`${tool}\``));
    assert.ok(headingLine, `Missing heading for ${tool}`);
    const start = spec.indexOf(headingLine);
    const nextHeading = spec.slice(start + 1).search(/\n### 21\./);
    const end = nextHeading === -1 ? spec.length : start + 1 + nextHeading;
    const section = spec.slice(start, end);
    const codeBlocks = [...section.matchAll(/```json\s*([\s\S]*?)```/g)].map((m) => m[1].trim());
    assert.equal(codeBlocks.length, 2, `Expected 2 JSON blocks for ${tool}`);

    const requestBlock = JSON.parse(codeBlocks[0]);
    const responseBlock = JSON.parse(codeBlocks[1]);
    assert.deepEqual(requestBlock, examples.examples[tool].request, `Request mismatch for ${tool}`);
    assert.deepEqual(responseBlock, examples.examples[tool].response, `Response mismatch for ${tool}`);
  }
});

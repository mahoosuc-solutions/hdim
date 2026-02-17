import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(dirname, '..', '..');

const defaultSpecPath = path.join(repoRoot, 'docs', 'runbooks', 'MCP_IMPLEMENTATION_REFERENCE_SPEC.md');
const defaultExamplesPath = path.join(
  repoRoot,
  'docs',
  'runbooks',
  'MCP_IMPLEMENTATION_REFERENCE_SPEC.examples.json',
);

const TOOL_ORDER = [
  'hdim_docker_ps',
  'hdim_service_operate',
  'hdim_gateway_validate',
  'hdim_release_gate',
  'hdim_release_evidence_pack',
];

const SECTION_HEADINGS = {
  hdim_docker_ps: '### 21.1 `hdim_docker_ps` example',
  hdim_service_operate: '### 21.2 `hdim_service_operate` dry-run example',
  hdim_gateway_validate: '### 21.3 `hdim_gateway_validate` warmup example',
  hdim_release_gate: '### 21.4 `hdim_release_gate` example',
  hdim_release_evidence_pack: '### 21.5 `hdim_release_evidence_pack` example',
};

function parseArgs(argv) {
  const args = [...argv];
  const parsed = {
    mode: 'check',
    specPath: defaultSpecPath,
    examplesPath: defaultExamplesPath,
  };

  while (args.length > 0) {
    const token = args.shift();
    switch (token) {
      case '--write':
        parsed.mode = 'write';
        break;
      case '--check':
        parsed.mode = 'check';
        break;
      case '--spec':
        parsed.specPath = args.shift() ?? parsed.specPath;
        break;
      case '--examples':
        parsed.examplesPath = args.shift() ?? parsed.examplesPath;
        break;
      default:
        break;
    }
  }

  return parsed;
}

function renderExampleSection(examplesDoc) {
  const lines = [];
  lines.push('## 21. Example Payload Appendix');
  lines.push('');
  lines.push(
    'The following examples are canonical reference shapes (fields may include additional metadata in runtime output).',
  );
  lines.push('Machine-readable companions:');
  lines.push('');
  lines.push('- `docs/runbooks/MCP_IMPLEMENTATION_REFERENCE_SPEC.schema.json`');
  lines.push('- `docs/runbooks/MCP_IMPLEMENTATION_REFERENCE_SPEC.examples.json`');
  lines.push('');

  for (const tool of TOOL_ORDER) {
    const heading = SECTION_HEADINGS[tool];
    const envelope = examplesDoc.examples?.[tool];
    if (!heading || !envelope) continue;

    lines.push(heading);
    lines.push('');
    lines.push('Request:');
    lines.push('');
    lines.push('```json');
    lines.push(JSON.stringify(envelope.request, null, 2));
    lines.push('```');
    lines.push('');
    lines.push('Response payload excerpt:');
    lines.push('');
    lines.push('```json');
    lines.push(JSON.stringify(envelope.response, null, 2));
    lines.push('```');
    lines.push('');
  }

  return `${lines.join('\n').trimEnd()}\n`;
}

function splitSpecAroundSection21(specText) {
  const marker = '\n## 21. Example Payload Appendix\n';
  const markerIndex = specText.indexOf(marker);
  if (markerIndex === -1) {
    throw new Error('Section 21 marker not found in reference spec');
  }
  const afterSection21SearchStart = markerIndex + marker.length;
  const nextTopLevelHeadingOffset = specText.slice(afterSection21SearchStart).search(/\n## [0-9]+\./);
  const nextTopLevelHeadingIndex =
    nextTopLevelHeadingOffset === -1 ? -1 : afterSection21SearchStart + nextTopLevelHeadingOffset;
  return {
    before: specText.slice(0, markerIndex + 1),
    after: nextTopLevelHeadingIndex === -1 ? '' : specText.slice(nextTopLevelHeadingIndex + 1),
  };
}

function syncSpec(specText, examplesDoc) {
  const { before, after } = splitSpecAroundSection21(specText);
  const rendered = renderExampleSection(examplesDoc);
  return `${before}${rendered}${after}`.replace(/\n{3,}/g, '\n\n');
}

function run(argv = process.argv.slice(2)) {
  const options = parseArgs(argv);
  const specText = readFileSync(options.specPath, 'utf8');
  const examplesDoc = JSON.parse(readFileSync(options.examplesPath, 'utf8'));
  const next = syncSpec(specText, examplesDoc);
  const changed = next !== specText;

  if (options.mode === 'write') {
    if (changed) writeFileSync(options.specPath, next);
    process.stdout.write(JSON.stringify({ mode: options.mode, changed, path: options.specPath }, null, 2) + '\n');
    return;
  }

  if (changed) {
    process.stdout.write(
      JSON.stringify(
        {
          mode: options.mode,
          changed,
          path: options.specPath,
          message: 'Reference spec appendix is out of sync with examples JSON',
        },
        null,
        2,
      ) + '\n',
    );
    process.exit(1);
  }

  process.stdout.write(JSON.stringify({ mode: options.mode, changed: false, path: options.specPath }, null, 2) + '\n');
}

if (import.meta.url === `file://${process.argv[1]}`) {
  run();
}

export { parseArgs, renderExampleSection, splitSpecAroundSection21, syncSpec };

import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { spawnSync } from 'node:child_process';

function toTimestamp(date = new Date()) {
  const pad = (value) => `${value}`.padStart(2, '0');
  return [
    date.getUTCFullYear(),
    pad(date.getUTCMonth() + 1),
    pad(date.getUTCDate()),
    '-',
    pad(date.getUTCHours()),
    pad(date.getUTCMinutes()),
    pad(date.getUTCSeconds()),
  ].join('');
}

function parseArgs(argv) {
  const args = [...argv];
  const parsed = {
    outDir: 'logs/mcp-reports',
    bundleOutDir: 'logs/mcp-reports/packages',
    policyMode: 'strict',
    profile: 'local-dev',
    requestTimeoutSecs: 8,
    service: 'gateway-edge',
    includePretest: true,
    includeControlledRestart: true,
    controlledRestartDryRun: false,
    controlledRestartSkipRuntimeChecks: false,
  };

  while (args.length > 0) {
    const token = args.shift();
    switch (token) {
      case '--out-dir':
        parsed.outDir = args.shift() ?? parsed.outDir;
        break;
      case '--bundle-out-dir':
        parsed.bundleOutDir = args.shift() ?? parsed.bundleOutDir;
        break;
      case '--mode':
        parsed.policyMode = args.shift() ?? parsed.policyMode;
        break;
      case '--request-timeout':
        parsed.requestTimeoutSecs = Number.parseInt(args.shift() ?? `${parsed.requestTimeoutSecs}`, 10);
        break;
      case '--profile':
        parsed.profile = args.shift() ?? parsed.profile;
        break;
      case '--service':
        parsed.service = args.shift() ?? parsed.service;
        break;
      case '--no-pretest':
        parsed.includePretest = false;
        break;
      case '--no-controlled-restart':
        parsed.includeControlledRestart = false;
        break;
      case '--controlled-restart-dry-run':
        parsed.controlledRestartDryRun = true;
        break;
      case '--controlled-restart-skip-runtime':
        parsed.controlledRestartSkipRuntimeChecks = true;
        break;
      default:
        break;
    }
  }

  return parsed;
}

function runCommand(command, args) {
  const result = spawnSync(command, args, {
    cwd: process.cwd(),
    encoding: 'utf8',
    env: process.env,
  });
  return {
    command: [command, ...args].join(' '),
    exitCode: result.status ?? 1,
    stdout: result.stdout ?? '',
    stderr: result.stderr ?? '',
  };
}

function parseJsonFromStdout(stdout) {
  const text = `${stdout}`.trim();
  if (!text) return null;
  const first = text.indexOf('{');
  const last = text.lastIndexOf('}');
  if (first === -1 || last === -1 || last < first) return null;
  try {
    return JSON.parse(text.slice(first, last + 1));
  } catch {
    return null;
  }
}

function summarizeMarkdown(payload, timestamp) {
  const lines = [
    '# MCP Operator Go/No-Go Report',
    '',
    `- Timestamp (UTC): ${timestamp}`,
    `- Go decision: ${payload.go}`,
    `- Pretest included: ${payload.inputs.includePretest}`,
    `- Controlled restart included: ${payload.inputs.includeControlledRestart}`,
    '',
    '## Step Results',
  ];

  for (const step of payload.steps) {
    lines.push(`- ${step.name}: exitCode=${step.exitCode} pass=${step.pass}`);
  }

  if (payload.releaseGate) {
    lines.push('');
    lines.push('## Release Gate Summary');
    lines.push(`- pass: ${payload.releaseGate.pass}`);
    lines.push(`- runningServices: ${payload.releaseGate.summary?.runningServices ?? 'n/a'}`);
    lines.push(`- tenantPolicyPass: ${payload.releaseGate.summary?.tenantPolicyPass ?? 'n/a'}`);
    lines.push(`- readiness: ${payload.releaseGate.summary?.readiness ?? 'n/a'}`);
  }

  if (payload.controlledRestart) {
    lines.push('');
    lines.push('## Controlled Restart Summary');
    lines.push(`- pass: ${payload.controlledRestart.pass}`);
    lines.push(`- restartOk: ${payload.controlledRestart.restartOk}`);
    lines.push(`- gatewayHealthy: ${payload.controlledRestart.gatewayHealthy}`);
    lines.push(`- dryRun: ${payload.controlledRestart.dryRun}`);
  }

  return `${lines.join('\n')}\n`;
}

function run(argv = process.argv.slice(2)) {
  const input = parseArgs(argv);
  const steps = [];

  if (input.includePretest) {
    const pretest = runCommand('bash', ['scripts/mcp/pre-test-checklist.sh']);
    steps.push({
      name: 'pretest',
      ...pretest,
      pass: pretest.exitCode === 0,
      parsed: null,
    });
  }

  const releaseGateArgs = [
    'scripts/mcp/context-aware-release-gate.mjs',
    '--mode',
    input.policyMode,
    '--profile',
    input.profile,
    '--request-timeout',
    `${input.requestTimeoutSecs}`,
    '--out-dir',
    input.outDir,
  ];
  const releaseGate = runCommand('node', releaseGateArgs);
  const releaseGateParsed = parseJsonFromStdout(releaseGate.stdout);
  steps.push({
    name: 'release-gate',
    ...releaseGate,
    pass: Boolean(releaseGateParsed?.pass),
    parsed: releaseGateParsed,
  });

  const evidencePack = runCommand('node', [
    'scripts/mcp/build-release-evidence-pack.mjs',
    '--report-dir',
    input.outDir,
    '--output-dir',
    input.outDir,
  ]);
  const evidencePackParsed = parseJsonFromStdout(evidencePack.stdout);
  steps.push({
    name: 'evidence-pack',
    ...evidencePack,
    pass: evidencePack.exitCode === 0 && !evidencePackParsed?.error,
    parsed: evidencePackParsed,
  });

  let controlledRestartParsed = null;
  if (input.includeControlledRestart) {
    const controlledArgs = [
      'scripts/mcp/controlled-restart-smoke.mjs',
      '--service',
      input.service,
      '--out-dir',
      input.outDir,
      '--append-to-latest-bundle',
    ];
    if (input.controlledRestartDryRun) controlledArgs.push('--dry-run');
    if (input.controlledRestartSkipRuntimeChecks) controlledArgs.push('--skip-runtime-checks');
    const controlled = runCommand('node', controlledArgs);
    controlledRestartParsed = parseJsonFromStdout(controlled.stdout);
    steps.push({
      name: 'controlled-restart',
      ...controlled,
      pass: Boolean(controlledRestartParsed?.pass),
      parsed: controlledRestartParsed,
    });
  }

  const evidencePackage = runCommand('node', [
    'scripts/mcp/package-deployment-evidence.mjs',
    '--report-dir',
    input.outDir,
    '--out-dir',
    input.bundleOutDir,
  ]);
  const evidencePackageParsed = parseJsonFromStdout(evidencePackage.stdout);
  steps.push({
    name: 'evidence-package',
    ...evidencePackage,
    pass: evidencePackage.exitCode === 0 && !evidencePackageParsed?.error,
    parsed: evidencePackageParsed,
  });

  const go = steps.every((step) => step.pass);
  const timestamp = toTimestamp();
  mkdirSync(input.outDir, { recursive: true });
  const jsonPath = path.join(input.outDir, `operator-go-no-go-${timestamp}.json`);
  const mdPath = path.join(input.outDir, `operator-go-no-go-${timestamp}.md`);

  const payload = {
    go,
    timestamp,
    inputs: input,
    releaseGate: releaseGateParsed,
    controlledRestart: controlledRestartParsed,
    evidencePack: evidencePackParsed,
    evidencePackage: evidencePackageParsed,
    steps: steps.map((step) => ({
      name: step.name,
      pass: step.pass,
      exitCode: step.exitCode,
      command: step.command,
      stdout: step.stdout,
      stderr: step.stderr,
      parsed: step.parsed,
    })),
    jsonReport: jsonPath,
    markdownReport: mdPath,
  };

  writeFileSync(jsonPath, JSON.stringify(payload, null, 2));
  writeFileSync(mdPath, summarizeMarkdown(payload, timestamp));

  process.stdout.write(
    `${JSON.stringify(
      {
        go: payload.go,
        jsonReport: jsonPath,
        markdownReport: mdPath,
      },
      null,
      2,
    )}\n`,
  );

  process.exit(go ? 0 : 2);
}

if (import.meta.url === `file://${process.argv[1]}`) {
  run();
}

export { parseArgs, parseJsonFromStdout, runCommand, summarizeMarkdown, toTimestamp };

import { copyFileSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import process from 'node:process';

import { createServer } from './hdim-docker-mcp.mjs';

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
    composeFile: process.env.HDIM_COMPOSE_FILE ?? 'docker-compose.demo.yml',
    gatewayUrl: process.env.GATEWAY_URL ?? process.env.HDIM_BASE_URL ?? 'http://localhost:18080',
    service: 'gateway-edge',
    outDir: 'logs/mcp-reports',
    requestTimeoutSecs: 5,
    warmupTimeoutSecs: 120,
    pollIntervalMs: 1000,
    stablePasses: 1,
    dryRun: false,
    skipRuntimeChecks: false,
    appendToLatestBundle: false,
    bundleDir: '',
  };

  while (args.length > 0) {
    const token = args.shift();
    switch (token) {
      case '--compose-file':
        parsed.composeFile = args.shift() ?? parsed.composeFile;
        break;
      case '--gateway-url':
        parsed.gatewayUrl = args.shift() ?? parsed.gatewayUrl;
        break;
      case '--service':
        parsed.service = args.shift() ?? parsed.service;
        break;
      case '--out-dir':
        parsed.outDir = args.shift() ?? parsed.outDir;
        break;
      case '--request-timeout':
        parsed.requestTimeoutSecs = Number.parseInt(args.shift() ?? `${parsed.requestTimeoutSecs}`, 10);
        break;
      case '--warmup-timeout':
        parsed.warmupTimeoutSecs = Number.parseInt(args.shift() ?? `${parsed.warmupTimeoutSecs}`, 10);
        break;
      case '--poll-interval-ms':
        parsed.pollIntervalMs = Number.parseInt(args.shift() ?? `${parsed.pollIntervalMs}`, 10);
        break;
      case '--stable-passes':
        parsed.stablePasses = Number.parseInt(args.shift() ?? `${parsed.stablePasses}`, 10);
        break;
      case '--dry-run':
        parsed.dryRun = true;
        break;
      case '--skip-runtime-checks':
        parsed.skipRuntimeChecks = true;
        break;
      case '--append-to-latest-bundle':
        parsed.appendToLatestBundle = true;
        break;
      case '--bundle-dir':
        parsed.bundleDir = args.shift() ?? parsed.bundleDir;
        break;
      default:
        break;
    }
  }

  return parsed;
}

function summarizeResult(result, input, timestamp) {
  const gatewayChecks = result.gatewayValidate?.checks ?? [];
  const checkLines =
    gatewayChecks.length === 0
      ? ['- No gateway checks returned']
      : gatewayChecks.map((item) => `- ${item.endpoint}: ${item.status ?? 'n/a'} (${item.ok ? 'ok' : 'fail'})`);
  return `# MCP Controlled Restart Report

- Timestamp (UTC): ${timestamp}
- Service: ${input.service}
- Compose file: ${input.composeFile}
- Dry run: ${input.dryRun}
- Skip runtime checks: ${input.skipRuntimeChecks}
- Restart ok: ${result.restart?.result?.ok ?? false}
- Restart exit code: ${result.restart?.result?.exitCode ?? 'n/a'}
- Gateway healthy after warmup: ${result.gatewayValidate?.allHealthy ?? false}
- Warmup attempts: ${result.gatewayValidate?.warmup?.attempts ?? 'n/a'}
- Warmup stable achieved: ${result.gatewayValidate?.warmup?.achievedStablePasses ?? false}

## Gateway Checks
${checkLines.join('\n')}
`;
}

function resolveBundleDir(input) {
  if (input.bundleDir && input.bundleDir.trim()) return input.bundleDir.trim();
  if (!input.appendToLatestBundle) return '';
  try {
    const roots = readdirSync('logs/mcp-reports/packages')
      .filter((name) => name.startsWith('deployment-evidence-'))
      .sort()
      .reverse();
    if (roots.length === 0) return '';
    return path.join('logs/mcp-reports/packages', roots[0]);
  } catch {
    return '';
  }
}

function appendRestartArtifactsToBundle({ bundleDir, jsonReport, markdownReport }) {
  if (!bundleDir) return { appended: false, reason: 'no bundle directory resolved' };
  mkdirSync(bundleDir, { recursive: true });
  const targetJson = path.join(bundleDir, path.basename(jsonReport));
  const targetMd = path.join(bundleDir, path.basename(markdownReport));
  copyFileSync(jsonReport, targetJson);
  copyFileSync(markdownReport, targetMd);

  const summaryPath = path.join(bundleDir, 'DEPLOYMENT_EVIDENCE_SUMMARY.md');
  let summary = '';
  try {
    summary = readFileSync(summaryPath, 'utf8');
  } catch {
    summary = '# Deployment Evidence Bundle\n\n';
  }

  const marker = '## Controlled Restart Report (Latest)';
  const newSection = `${marker}

- JSON: \`${path.basename(jsonReport)}\`
- Markdown: \`${path.basename(markdownReport)}\`
`;

  if (summary.includes(marker)) {
    const start = summary.indexOf(marker);
    const after = summary.slice(start + marker.length);
    const nextHeaderIndex = after.indexOf('\n## ');
    const head = summary.slice(0, start).trimEnd();
    const tail = nextHeaderIndex === -1 ? '' : after.slice(nextHeaderIndex + 1).trimStart();
    summary = `${head}\n\n${newSection}${tail ? `\n${tail}` : ''}\n`;
  } else {
    summary = `${summary.trimEnd()}\n\n${newSection}\n`;
  }

  writeFileSync(summaryPath, summary);
  return {
    appended: true,
    bundleDir,
    summaryPath,
    copied: [targetJson, targetMd],
  };
}

async function run() {
  const input = parseArgs(process.argv.slice(2));
  const server = createServer();

  // eslint-disable-next-line no-underscore-dangle
  const psTool = server._registeredTools?.['hdim_docker_ps'];
  // eslint-disable-next-line no-underscore-dangle
  const operateTool = server._registeredTools?.['hdim_service_operate'];
  // eslint-disable-next-line no-underscore-dangle
  const validateTool = server._registeredTools?.['hdim_gateway_validate'];
  if (!psTool || !operateTool || !validateTool) {
    throw new Error('Required MCP tools are not available');
  }

  const prePs = input.skipRuntimeChecks
    ? { skipped: true, reason: 'skipRuntimeChecks=true' }
    : JSON.parse((await psTool.handler({ composeFile: input.composeFile })).content[0].text);
  const restart = JSON.parse(
    (
      await operateTool.handler({
        composeFile: input.composeFile,
        action: 'restart',
        services: [input.service],
        dryRun: input.dryRun,
      })
    ).content[0].text,
  );
  const postPs = input.skipRuntimeChecks
    ? { skipped: true, reason: 'skipRuntimeChecks=true' }
    : JSON.parse((await psTool.handler({ composeFile: input.composeFile })).content[0].text);
  const gatewayValidate = input.skipRuntimeChecks
    ? { skipped: true, reason: 'skipRuntimeChecks=true', allHealthy: true, warmup: {} }
    : JSON.parse(
        (
          await validateTool.handler({
            baseUrl: input.gatewayUrl,
            requestTimeoutSecs: input.requestTimeoutSecs,
            warmupTimeoutSecs: input.warmupTimeoutSecs,
            pollIntervalMs: input.pollIntervalMs,
            stablePasses: input.stablePasses,
          })
        ).content[0].text,
      );

  const payload = {
    pass: Boolean(input.dryRun ? restart?.dryRun === true : restart?.result?.ok) && Boolean(gatewayValidate?.allHealthy),
    input,
    prePs,
    restart,
    postPs,
    gatewayValidate,
  };

  const timestamp = toTimestamp();
  mkdirSync(input.outDir, { recursive: true });
  const jsonReport = path.join(input.outDir, `controlled-restart-${timestamp}.json`);
  const markdownReport = path.join(input.outDir, `controlled-restart-${timestamp}.md`);
  writeFileSync(jsonReport, JSON.stringify(payload, null, 2));
  writeFileSync(markdownReport, summarizeResult(payload, input, timestamp));

  const bundleDir = resolveBundleDir(input);
  const bundleUpdate = appendRestartArtifactsToBundle({
    bundleDir,
    jsonReport,
    markdownReport,
  });

  process.stdout.write(
    `${JSON.stringify(
      {
        pass: payload.pass,
        restartOk: restart?.result?.ok ?? false,
        dryRun: input.dryRun,
        skipRuntimeChecks: input.skipRuntimeChecks,
        gatewayHealthy: gatewayValidate?.allHealthy ?? false,
        warmup: gatewayValidate?.warmup ?? {},
        jsonReport,
        markdownReport,
        bundleUpdate,
      },
      null,
      2,
    )}\n`,
  );

  process.exit(payload.pass ? 0 : 2);
}

if (import.meta.url === `file://${process.argv[1]}`) {
  run().catch((error) => {
    // eslint-disable-next-line no-console
    console.error(error);
    process.exit(1);
  });
}

export { appendRestartArtifactsToBundle, parseArgs, resolveBundleDir, summarizeResult, toTimestamp };

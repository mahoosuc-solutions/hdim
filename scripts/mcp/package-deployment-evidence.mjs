import { copyFileSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import process from 'node:process';

function parseArgs(argv) {
  const args = [...argv];
  const parsed = {
    reportDir: 'logs/mcp-reports',
    outDir: 'logs/mcp-reports/packages',
  };

  while (args.length > 0) {
    const token = args.shift();
    switch (token) {
      case '--report-dir':
        parsed.reportDir = args.shift() ?? parsed.reportDir;
        break;
      case '--out-dir':
        parsed.outDir = args.shift() ?? parsed.outDir;
        break;
      default:
        break;
    }
  }

  return parsed;
}

function listFiles(reportDir, prefix) {
  try {
    return readdirSync(reportDir)
      .filter((file) => file.startsWith(prefix))
      .map((file) => path.join(reportDir, file))
      .sort()
      .reverse();
  } catch {
    return [];
  }
}

function timestampFromName(filePath) {
  const base = path.basename(filePath);
  const match = /(\d{8}-\d{6})/.exec(base);
  return match?.[1] ?? 'unknown';
}

function run() {
  const options = parseArgs(process.argv.slice(2));

  const gateJson = listFiles(options.reportDir, 'release-gate-').find((f) => f.endsWith('.json'));
  const gateMd = listFiles(options.reportDir, 'release-gate-').find((f) => f.endsWith('.md'));
  const packJson = listFiles(options.reportDir, 'release-evidence-pack-').find((f) => f.endsWith('.json'));
  const packMd = listFiles(options.reportDir, 'release-evidence-pack-').find((f) => f.endsWith('.md'));

  if (!gateJson || !packJson) {
    process.stdout.write(
      `${JSON.stringify(
        {
          error: 'Missing required artifacts. Need latest release-gate and release-evidence-pack JSON files.',
          reportDir: options.reportDir,
          found: { gateJson: Boolean(gateJson), packJson: Boolean(packJson) },
        },
        null,
        2,
      )}\n`,
    );
    process.exit(1);
  }

  const ts = timestampFromName(gateJson);
  const bundleDir = path.join(options.outDir, `deployment-evidence-${ts}`);
  mkdirSync(bundleDir, { recursive: true });

  const artifacts = [gateJson, gateMd, packJson, packMd].filter(Boolean);
  const copied = [];
  for (const artifact of artifacts) {
    const target = path.join(bundleDir, path.basename(artifact));
    copyFileSync(artifact, target);
    copied.push(target);
  }

  const gatePayload = JSON.parse(readFileSync(gateJson, 'utf8'));
  const summaryMd = [
    '# Deployment Evidence Bundle',
    '',
    `- Source report dir: ${options.reportDir}`,
    `- Bundle dir: ${bundleDir}`,
    `- Gate pass: ${gatePayload.pass}`,
    `- Tenant policy mode: ${gatePayload.summary?.tenantPolicyMode ?? 'unknown'}`,
    `- Tenant policy pass: ${gatePayload.summary?.tenantPolicyPass ?? 'unknown'}`,
    `- Readiness: ${gatePayload.summary?.readiness ?? 'unknown'}`,
    `- Running services: ${gatePayload.summary?.runningServices ?? 'unknown'}`,
    '',
    '## Included Artifacts',
    ...copied.map((item) => `- ${item}`),
    '',
  ].join('\n');
  const summaryPath = path.join(bundleDir, 'DEPLOYMENT_EVIDENCE_SUMMARY.md');
  writeFileSync(summaryPath, summaryMd);

  process.stdout.write(
    `${JSON.stringify(
      {
        bundleDir,
        summaryPath,
        copiedArtifacts: copied,
      },
      null,
      2,
    )}\n`,
  );
}

if (import.meta.url === `file://${process.argv[1]}`) {
  try {
    run();
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error(error);
    process.exit(1);
  }
}

export { parseArgs };

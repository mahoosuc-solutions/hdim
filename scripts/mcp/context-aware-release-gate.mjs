import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
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
    policyMode: undefined,
    noHeaderAllowlist: [],
    includeLogs: undefined,
    runSystemValidate: undefined,
    skipFrontend: undefined,
    skipFhirQuery: undefined,
    composeFile: process.env.HDIM_COMPOSE_FILE ?? 'docker-compose.demo.yml',
    gatewayUrl: process.env.GATEWAY_URL ?? process.env.HDIM_BASE_URL ?? 'http://localhost:18080',
    demoSeedingUrl: process.env.DEMO_SEEDING_URL ?? 'http://localhost:8098',
    tenantA: process.env.TENANT_A ?? 'acme-health',
    tenantB: process.env.TENANT_B ?? 'beta-health',
    outDir: 'logs/mcp-reports',
    policyFile: 'scripts/mcp/release-gate-policy.json',
    profile: 'local-dev',
    requestTimeoutSecs: 8,
  };

  while (args.length > 0) {
    const token = args.shift();
    switch (token) {
      case '--mode':
        parsed.policyMode = args.shift() ?? parsed.policyMode;
        break;
      case '--allow-no-header':
        parsed.noHeaderAllowlist.push(args.shift() ?? '');
        break;
      case '--include-logs':
        parsed.includeLogs = true;
        break;
      case '--no-system-validate':
        parsed.runSystemValidate = false;
        break;
      case '--skip-frontend':
        parsed.skipFrontend = true;
        break;
      case '--skip-fhir-query':
        parsed.skipFhirQuery = true;
        break;
      case '--compose-file':
        parsed.composeFile = args.shift() ?? parsed.composeFile;
        break;
      case '--gateway-url':
        parsed.gatewayUrl = args.shift() ?? parsed.gatewayUrl;
        break;
      case '--demo-seeding-url':
        parsed.demoSeedingUrl = args.shift() ?? parsed.demoSeedingUrl;
        break;
      case '--tenant-a':
        parsed.tenantA = args.shift() ?? parsed.tenantA;
        break;
      case '--tenant-b':
        parsed.tenantB = args.shift() ?? parsed.tenantB;
        break;
      case '--out-dir':
        parsed.outDir = args.shift() ?? parsed.outDir;
        break;
      case '--policy-file':
        parsed.policyFile = args.shift() ?? parsed.policyFile;
        break;
      case '--profile':
        parsed.profile = args.shift() ?? parsed.profile;
        break;
      case '--request-timeout':
        parsed.requestTimeoutSecs = Number.parseInt(args.shift() ?? `${parsed.requestTimeoutSecs}`, 10);
        break;
      default:
        break;
    }
  }

  parsed.noHeaderAllowlist = parsed.noHeaderAllowlist.filter(Boolean);
  return parsed;
}

function loadPolicyFile(policyFilePath) {
  try {
    const text = readFileSync(policyFilePath, 'utf8');
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function resolvePolicyInput(input, policyConfig) {
  const profile = input.profile ? policyConfig?.profiles?.[input.profile] : null;
  const merged = {
    ...input,
    policyMode: input.policyMode ?? profile?.policyMode ?? 'strict',
    noHeaderAllowlist:
      input.noHeaderAllowlist.length > 0
        ? input.noHeaderAllowlist
        : profile?.noHeaderAllowlist ?? input.noHeaderAllowlist,
    includeLogs: input.includeLogs ?? profile?.includeLogs ?? false,
    runSystemValidate:
      input.runSystemValidate ?? profile?.runSystemValidate ?? true,
    skipFrontend: input.skipFrontend ?? profile?.skipFrontend ?? false,
    skipFhirQuery: input.skipFhirQuery ?? profile?.skipFhirQuery ?? false,
    requestTimeoutSecs: input.requestTimeoutSecs ?? profile?.requestTimeoutSecs ?? 8,
    gatewayUrl: input.gatewayUrl ?? profile?.gatewayUrl ?? input.gatewayUrl,
    demoSeedingUrl: input.demoSeedingUrl ?? profile?.demoSeedingUrl ?? input.demoSeedingUrl,
    tenantA: input.tenantA ?? profile?.tenantA ?? input.tenantA,
    tenantB: input.tenantB ?? profile?.tenantB ?? input.tenantB,
  };

  return merged;
}

function summarizeGate(result, input, timestamp) {
  const warnings = result?.tenantIsolation?.warnings ?? [];
  const violations = result?.tenantIsolation?.violations ?? [];
  return `# MCP Release Gate Knowledge Report

- Timestamp (UTC): ${timestamp}
- Policy mode: ${input.policyMode}
- Gate pass: ${result.pass}
- Running services: ${result.summary?.runningServices ?? 'n/a'}
- Readiness pass: ${result.summary?.readiness ?? 'n/a'}
- Tenant policy pass: ${result.summary?.tenantPolicyPass ?? 'n/a'}
- Missing required config: ${result.summary?.missingRequiredConfig ?? 'n/a'}
- Tenant warnings: ${warnings.length}
- Tenant violations: ${violations.length}

## Tenant Policy Findings
${warnings.length === 0 ? '- No warnings' : warnings.map((item) => `- WARN ${item.endpoint}: ${item.reason}`).join('\n')}
${violations.length === 0 ? '- No violations' : violations.map((item) => `- FAIL ${item.endpoint ?? 'global'}: ${item.reason}`).join('\n')}
`;
}

async function run() {
  const rawInput = parseArgs(process.argv.slice(2));
  const policyConfig = loadPolicyFile(rawInput.policyFile);
  const input = resolvePolicyInput(rawInput, policyConfig);
  const server = createServer();
  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_release_gate'];
  if (!tool) throw new Error('hdim_release_gate tool is not available');

  const response = await tool.handler({
    composeFile: input.composeFile,
    gatewayUrl: input.gatewayUrl,
    demoSeedingUrl: input.demoSeedingUrl,
    tenantA: input.tenantA,
    tenantB: input.tenantB,
    policyMode: input.policyMode,
    noHeaderAllowlist: input.noHeaderAllowlist,
    includeLogs: input.includeLogs,
    runSystemValidate: input.runSystemValidate,
    skipFrontend: input.skipFrontend,
    skipFhirQuery: input.skipFhirQuery,
    requestTimeoutSecs: input.requestTimeoutSecs,
  });

  const payload = JSON.parse(response.content[0].text);
  const timestamp = toTimestamp();

  mkdirSync(input.outDir, { recursive: true });
  const jsonPath = path.join(input.outDir, `release-gate-${timestamp}.json`);
  const mdPath = path.join(input.outDir, `release-gate-${timestamp}.md`);

  writeFileSync(jsonPath, JSON.stringify(payload, null, 2));
  writeFileSync(mdPath, summarizeGate(payload, input, timestamp));

  process.stdout.write(
    JSON.stringify(
      {
        pass: payload.pass,
        summary: payload.summary,
        policy: payload.tenantIsolation?.policyMode,
        profile: rawInput.profile,
        policyFile: rawInput.policyFile,
        jsonReport: jsonPath,
        markdownReport: mdPath,
      },
      null,
      2,
    ) + '\n',
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

export { loadPolicyFile, parseArgs, resolvePolicyInput, summarizeGate, toTimestamp };

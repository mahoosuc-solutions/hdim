import { spawn } from 'node:child_process';
import { mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import process from 'node:process';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { z } from 'zod';

const DEFAULT_COMPOSE_FILE = 'docker-compose.demo.yml';
const DEFAULT_DOCKER_BIN = 'docker';
const DEFAULT_BASE_URL = 'http://localhost:18080';
const DEFAULT_POLICY_FILE = 'scripts/mcp/release-gate-policy.json';

const SERVICE_METADATA = {
  postgres: { category: 'data', owner: 'platform', health: 'pg_isready' },
  redis: { category: 'data', owner: 'platform', health: 'redis ping' },
  kafka: { category: 'messaging', owner: 'platform', health: 'broker api versions' },
  'gateway-edge': { category: 'gateway', owner: 'api-platform', healthEndpoint: '/actuator/health' },
  'gateway-admin-service': { category: 'gateway', owner: 'api-platform' },
  'gateway-clinical-service': { category: 'gateway', owner: 'api-platform' },
  'gateway-fhir-service': { category: 'gateway', owner: 'api-platform' },
  'fhir-service': { category: 'clinical-core', owner: 'clinical-platform', healthEndpoint: '/fhir/metadata' },
  'quality-measure-service': { category: 'quality', owner: 'quality-engineering' },
  'care-gap-service': { category: 'quality', owner: 'quality-engineering' },
  'cql-engine-service': { category: 'quality', owner: 'quality-engineering' },
  'audit-query-service': { category: 'audit', owner: 'security-observability' },
  'demo-seeding-service': { category: 'demo', owner: 'platform-enablement', healthEndpoint: '/demo/actuator/health' },
  'clinical-portal': { category: 'frontend', owner: 'frontend' },
};

const CONFIG_CONTRACTS = {
  global: [
    { name: 'HDIM_BASE_URL', required: false, default: 'http://localhost:18080' },
    { name: 'GATEWAY_URL', required: false, default: 'http://localhost:18080' },
    { name: 'DEMO_SEEDING_URL', required: false, default: 'http://localhost:8098' },
    { name: 'TENANT_ID', required: false, default: 'acme-health' },
  ],
  services: {
    'gateway-edge': [
      { name: 'UPSTREAM_TIMEOUT', required: false },
      { name: 'CLIENT_MAX_BODY_SIZE', required: false },
    ],
    'fhir-service': [
      { name: 'SPRING_PROFILES_ACTIVE', required: true },
      { name: 'SPRING_DATASOURCE_URL', required: true },
    ],
    'quality-measure-service': [
      { name: 'SPRING_PROFILES_ACTIVE', required: true },
      { name: 'SPRING_DATASOURCE_URL', required: true },
      { name: 'KAFKA_BOOTSTRAP_SERVERS', required: true },
    ],
    'demo-seeding-service': [
      { name: 'SPRING_PROFILES_ACTIVE', required: true },
      { name: 'TENANT_ID', required: false, default: 'acme-health' },
    ],
  },
};

const SERVICE_DEPENDENCIES = {
  'gateway-edge': ['gateway-admin-service', 'gateway-clinical-service', 'gateway-fhir-service'],
  'gateway-admin-service': ['postgres', 'redis'],
  'gateway-clinical-service': ['postgres', 'redis'],
  'gateway-fhir-service': ['fhir-service'],
  'fhir-service': ['postgres'],
  'quality-measure-service': ['postgres', 'kafka'],
  'care-gap-service': ['postgres', 'kafka'],
  'cql-engine-service': ['postgres', 'kafka', 'redis'],
  'patient-service': ['postgres'],
  'audit-query-service': ['postgres'],
  'demo-seeding-service': ['postgres', 'kafka', 'redis'],
};

export function normalizeComposeFile(input) {
  const raw = (input ?? process.env.HDIM_COMPOSE_FILE ?? DEFAULT_COMPOSE_FILE).trim();
  if (!raw) throw new Error('composeFile must not be empty');
  return raw;
}

export function normalizeDockerBin(input) {
  const raw = (input ?? process.env.DOCKER_BIN ?? DEFAULT_DOCKER_BIN).trim();
  if (!raw) throw new Error('docker command must not be empty');
  return raw;
}

export function toServiceList(input) {
  if (!input) return [];
  const services = Array.isArray(input) ? input : [input];
  return services.map((service) => `${service}`.trim()).filter(Boolean);
}

export function composeArgs(composeFile, subcommandArgs) {
  return ['compose', '-f', composeFile, ...subcommandArgs];
}

export async function runCommand(command, args, options = {}) {
  const timeoutMs = options.timeoutMs ?? 120_000;
  const env = options.env ?? process.env;
  const cwd = options.cwd ?? process.cwd();

  return await new Promise((resolve) => {
    let stdout = '';
    let stderr = '';
    let timedOut = false;

    const child = spawn(command, args, {
      env,
      cwd,
      stdio: ['ignore', 'pipe', 'pipe'],
    });

    const timeout = setTimeout(() => {
      timedOut = true;
      child.kill('SIGTERM');
    }, timeoutMs);

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString();
    });

    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString();
    });

    child.on('error', (error) => {
      clearTimeout(timeout);
      resolve({
        ok: false,
        exitCode: null,
        timedOut,
        stdout,
        stderr: `${stderr}${error.message}`,
      });
    });

    child.on('close', (code) => {
      clearTimeout(timeout);
      resolve({
        ok: !timedOut && code === 0,
        exitCode: code,
        timedOut,
        stdout,
        stderr,
      });
    });
  });
}

function toJsonText(payload) {
  return {
    content: [
      {
        type: 'text',
        text: JSON.stringify(payload, null, 2),
      },
    ],
  };
}

function truncateText(text, limit = 20_000) {
  if (typeof text !== 'string') return '';
  return text.length > limit ? `${text.slice(0, limit)}\n...[truncated]` : text;
}

function isHeaderEnforcedStatus(status) {
  return status === 400 || status === 401 || status === 403;
}

function isAllowlistedEndpoint(endpoint, allowlist) {
  return allowlist.some((entry) => endpoint === entry || endpoint.startsWith(entry));
}

async function readResponseBodySafe(response, limit = 10_000) {
  try {
    const text = await response.text();
    return truncateText(text, limit);
  } catch {
    return '';
  }
}

export function parseServicesFromConfigOutput(output) {
  return `${output}`
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
}

export function parseComposePsJson(output) {
  const text = `${output}`.trim();
  if (!text) return [];

  if (text.startsWith('[')) {
    try {
      const parsed = JSON.parse(text);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  const entries = [];
  for (const line of text.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed.startsWith('{')) continue;
    try {
      entries.push(JSON.parse(trimmed));
    } catch {
      continue;
    }
  }
  return entries;
}

export function extractComposeEnvVariables(composeText) {
  const matches = composeText.matchAll(/\$\{([A-Z0-9_]+)(?:(:-|-)([^}]*))?\}/g);
  const byName = new Map();

  for (const match of matches) {
    const [, name, defaultMarker, defaultValue] = match;
    if (!name) continue;
    const hasDefault = Boolean(defaultMarker);
    const prev = byName.get(name);
    byName.set(name, {
      name,
      required: prev ? prev.required && !hasDefault : !hasDefault,
      hasAnyDefault: prev ? prev.hasAnyDefault || hasDefault : hasDefault,
      defaults: [
        ...(prev?.defaults ?? []),
        ...(hasDefault ? [`${defaultMarker}${defaultValue ?? ''}`] : []),
      ],
    });
  }

  return [...byName.values()].sort((a, b) => a.name.localeCompare(b.name));
}

function buildServiceCatalogEntries(services, psEntries) {
  const byService = new Map();
  for (const service of services) {
    byService.set(service, {
      service,
      running: false,
      containers: [],
      metadata: SERVICE_METADATA[service] ?? null,
    });
  }

  for (const entry of psEntries) {
    const serviceName = entry.Service ?? entry.service;
    if (!serviceName) continue;
    if (!byService.has(serviceName)) {
      byService.set(serviceName, {
        service: serviceName,
        running: false,
        containers: [],
        metadata: SERVICE_METADATA[serviceName] ?? null,
      });
    }
    const current = byService.get(serviceName);
    current.containers.push({
      name: entry.Name ?? entry.name ?? null,
      state: entry.State ?? entry.state ?? null,
      status: entry.Status ?? entry.status ?? null,
    });
    const statusText = `${entry.Status ?? entry.status ?? ''}`.toLowerCase();
    current.running =
      current.running || statusText.includes('up') || `${entry.State ?? entry.state ?? ''}`.toLowerCase() === 'running';
  }

  return [...byService.values()].sort((a, b) => a.service.localeCompare(b.service));
}

function unique(values) {
  return [...new Set(values)];
}

function parseReportTimestampFromPath(filePath) {
  const match = /release-gate-(\d{8}-\d{6})\.json$/.exec(filePath);
  return match?.[1] ?? null;
}

function listReleaseGateJsonReports(reportDir) {
  try {
    const files = readdirSync(reportDir)
      .filter((file) => file.startsWith('release-gate-') && file.endsWith('.json'))
      .map((file) => path.join(reportDir, file));
    files.sort((a, b) => {
      const aTs = parseReportTimestampFromPath(a) ?? '';
      const bTs = parseReportTimestampFromPath(b) ?? '';
      return bTs.localeCompare(aTs);
    });
    return files;
  } catch {
    return [];
  }
}

function toBool(value) {
  return value === true;
}

function parseReport(pathToReport) {
  try {
    const payload = JSON.parse(readFileSync(pathToReport, 'utf8'));
    return { ok: true, payload };
  } catch (error) {
    return { ok: false, error: error?.message ?? String(error) };
  }
}

function buildGateDiff(previous, current) {
  if (!previous || !current) return null;

  const previousWarnings = previous.tenantIsolation?.warnings ?? [];
  const currentWarnings = current.tenantIsolation?.warnings ?? [];
  const previousViolations = previous.tenantIsolation?.violations ?? [];
  const currentViolations = current.tenantIsolation?.violations ?? [];

  const diff = {
    passChanged: Boolean(previous.pass) !== Boolean(current.pass),
    readinessChanged:
      Boolean(previous.summary?.readiness) !== Boolean(current.summary?.readiness),
    runningServicesDelta:
      (current.summary?.runningServices ?? 0) - (previous.summary?.runningServices ?? 0),
    warningCountDelta: currentWarnings.length - previousWarnings.length,
    violationCountDelta: currentViolations.length - previousViolations.length,
  };

  const regressions = [];
  if (diff.passChanged && current.pass === false) regressions.push('Gate changed from pass to fail');
  if (diff.readinessChanged && current.summary?.readiness === false) {
    regressions.push('Readiness changed from true to false');
  }
  if (diff.runningServicesDelta < 0) regressions.push('Running service count decreased');
  if (diff.warningCountDelta > 0) regressions.push('Warning count increased');
  if (diff.violationCountDelta > 0) regressions.push('Violation count increased');

  return {
    ...diff,
    regressions,
    hasRegression: regressions.length > 0,
  };
}

function buildRestartPlan(targetServices, includeDependencies = true) {
  const visited = new Set();
  const order = [];

  const dfs = (service) => {
    if (visited.has(service)) return;
    visited.add(service);
    if (includeDependencies) {
      for (const dependency of SERVICE_DEPENDENCIES[service] ?? []) {
        dfs(dependency);
      }
    }
    order.push(service);
  };

  for (const service of targetServices) dfs(service);
  return order;
}

export function createServer(options = {}) {
  const server = new McpServer({
    name: 'hdim-docker',
    version: '0.1.0',
  });

  const runner = options.runCommand ?? runCommand;
  const fetchImpl = options.fetchImpl ?? fetch;
  const sleep = options.sleep ?? ((ms) => new Promise((resolve) => setTimeout(resolve, ms)));

  const runCompose = async ({ composeFile, subcommandArgs, timeoutMs }) => {
    const resolvedComposeFile = normalizeComposeFile(composeFile);
    const dockerBin = normalizeDockerBin(options.dockerBin);
    const args = composeArgs(resolvedComposeFile, subcommandArgs);
    const result = await runner(dockerBin, args, { timeoutMs });

    return {
      dockerBin,
      composeFile: resolvedComposeFile,
      command: [dockerBin, ...args].join(' '),
      ...result,
    };
  };

  const runRepoScript = async ({ scriptPath, timeoutMs, env }) => {
    const result = await runner('bash', [scriptPath], {
      timeoutMs,
      env: {
        ...process.env,
        ...(env ?? {}),
      },
      cwd: options.cwd ?? process.cwd(),
    });

    return {
      scriptPath,
      command: `bash ${scriptPath}`,
      ...result,
    };
  };

  server.registerTool(
    'hdim_docker_info',
    {
      title: 'HDIM Docker MCP Info',
      description: 'Returns Docker MCP server defaults for this repo.',
      inputSchema: z.object({}),
    },
    async () =>
      toJsonText({
        name: 'hdim-docker',
        dockerBin: normalizeDockerBin(options.dockerBin),
        composeFile: normalizeComposeFile(options.composeFile),
        baseUrl: (process.env.HDIM_BASE_URL ?? DEFAULT_BASE_URL).trim(),
      }),
  );

  server.registerTool(
    'hdim_service_restart_plan',
    {
      title: 'HDIM Service Restart Plan',
      description: 'Builds a dependency-aware ordered restart plan for one or more services.',
      inputSchema: z.object({
        services: z.array(z.string().min(1)).min(1).max(20),
        includeDependencies: z.boolean().default(true),
      }),
    },
    async ({ services, includeDependencies }) => {
      const normalized = unique(services.map((value) => `${value}`.trim()).filter(Boolean));
      const ordered = buildRestartPlan(normalized, includeDependencies ?? true);

      return toJsonText({
        requestedServices: normalized,
        includeDependencies: includeDependencies ?? true,
        dependencies: SERVICE_DEPENDENCIES,
        restartOrder: ordered,
      });
    },
  );

  server.registerTool(
    'hdim_service_operate',
    {
      title: 'HDIM Service Operate',
      description:
        'Runs centralized compose operations for services (start/stop/restart). Defaults to dryRun for safety.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
        action: z.enum(['start', 'stop', 'restart']),
        services: z.array(z.string().min(1)).min(1).max(20),
        dryRun: z.boolean().default(true),
      }),
    },
    async ({ composeFile, action, services, dryRun }) => {
      const resolvedComposeFile = normalizeComposeFile(composeFile);
      const normalizedServices = unique(services.map((value) => `${value}`.trim()).filter(Boolean));

      let subcommand = [];
      if (action === 'restart') subcommand = ['restart', ...normalizedServices];
      if (action === 'start') subcommand = ['up', '-d', ...normalizedServices];
      if (action === 'stop') subcommand = ['stop', ...normalizedServices];

      const preview = ['docker', ...composeArgs(resolvedComposeFile, subcommand)].join(' ');
      if (dryRun ?? true) {
        return toJsonText({
          composeFile: resolvedComposeFile,
          action,
          services: normalizedServices,
          dryRun: true,
          commandPreview: preview,
        });
      }

      const result = await runCompose({
        composeFile: resolvedComposeFile,
        subcommandArgs: subcommand,
        timeoutMs: action === 'start' ? 300_000 : 120_000,
      });

      return toJsonText({
        composeFile: resolvedComposeFile,
        action,
        services: normalizedServices,
        dryRun: false,
        commandPreview: preview,
        result,
      });
    },
  );

  server.registerTool(
    'hdim_topology_report',
    {
      title: 'HDIM Topology Report',
      description:
        'Builds a runtime topology report from docker compose service inventory, running container states, and gateway checks.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
        gatewayUrl: z.string().url().optional(),
        requestTimeoutSecs: z.number().int().min(2).max(120).default(8),
      }),
    },
    async ({ composeFile, gatewayUrl, requestTimeoutSecs }) => {
      const resolvedGatewayUrl = (gatewayUrl ?? process.env.GATEWAY_URL ?? DEFAULT_BASE_URL)
        .trim()
        .replace(/\/$/, '');
      const timeoutMs = (requestTimeoutSecs ?? 8) * 1000;

      const servicesResult = await runCompose({
        composeFile,
        subcommandArgs: ['config', '--services'],
        timeoutMs: 30_000,
      });

      const psResult = await runCompose({
        composeFile,
        subcommandArgs: ['ps', '--format', 'json'],
        timeoutMs: 30_000,
      });

      const services = servicesResult.ok ? parseServicesFromConfigOutput(servicesResult.stdout) : [];
      const psEntries = psResult.ok ? parseComposePsJson(psResult.stdout) : [];

      const gatewayChecks = await Promise.all(
        ['/actuator/health', '/fhir/metadata'].map(async (endpoint) => {
          const url = `${resolvedGatewayUrl}${endpoint}`;
          const controller = new AbortController();
          const timeout = setTimeout(() => controller.abort(), timeoutMs);
          try {
            const response = await fetchImpl(url, {
              method: 'GET',
              headers: { accept: 'application/json, text/plain;q=0.9, */*;q=0.1' },
              signal: controller.signal,
            });
            return { endpoint, url, ok: response.ok, status: response.status };
          } catch (error) {
            return {
              endpoint,
              url,
              ok: false,
              status: null,
              error: error?.message ?? String(error),
            };
          } finally {
            clearTimeout(timeout);
          }
        }),
      );

      const runningServiceNames = new Set(
        psEntries.map((entry) => entry.Service ?? entry.service).filter(Boolean),
      );

      const inventory = services.map((name) => ({
        service: name,
        running: runningServiceNames.has(name),
      }));

      return toJsonText({
        composeFile: normalizeComposeFile(composeFile),
        gatewayUrl: resolvedGatewayUrl,
        summary: {
          serviceCount: services.length,
          runningCount: inventory.filter((item) => item.running).length,
          gatewayHealthy: gatewayChecks.every((item) => item.ok),
        },
        services: inventory,
        containers: psEntries,
        gatewayChecks,
        diagnostics: {
          servicesCommand: {
            ok: servicesResult.ok,
            exitCode: servicesResult.exitCode,
            stderr: truncateText(servicesResult.stderr, 4000),
          },
          psCommand: {
            ok: psResult.ok,
            exitCode: psResult.exitCode,
            stderr: truncateText(psResult.stderr, 4000),
          },
        },
      });
    },
  );

  server.registerTool(
    'hdim_config_audit',
    {
      title: 'HDIM Config Audit',
      description:
        'Audits compose env variable usage, flags missing required runtime values, and reports key MCP runtime config.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
      }),
    },
    async ({ composeFile }) => {
      const resolvedComposeFile = normalizeComposeFile(composeFile);

      let composeText = '';
      try {
        composeText = readFileSync(resolvedComposeFile, 'utf8');
      } catch (error) {
        return toJsonText({
          composeFile: resolvedComposeFile,
          error: `Unable to read compose file: ${error?.message ?? String(error)}`,
        });
      }

      const vars = extractComposeEnvVariables(composeText);
      const requiredVars = vars.filter((item) => item.required).map((item) => item.name);
      const missingRequired = requiredVars.filter((name) => {
        const value = process.env[name];
        return typeof value === 'undefined' || value === '';
      });
      const optionalUnset = vars
        .filter((item) => !item.required)
        .map((item) => item.name)
        .filter((name) => {
          const value = process.env[name];
          return typeof value === 'undefined' || value === '';
        });

      return toJsonText({
        composeFile: resolvedComposeFile,
        summary: {
          variablesReferenced: vars.length,
          requiredVariables: requiredVars.length,
          missingRequired: missingRequired.length,
          optionalUnset: optionalUnset.length,
        },
        missingRequired,
        optionalUnset,
        variables: vars,
        mcpRuntime: {
          dockerBin: normalizeDockerBin(options.dockerBin),
          hdimComposeFileEnv: process.env.HDIM_COMPOSE_FILE ?? null,
          hdimBaseUrlEnv: process.env.HDIM_BASE_URL ?? null,
          gatewayUrlEnv: process.env.GATEWAY_URL ?? null,
          demoSeedingUrlEnv: process.env.DEMO_SEEDING_URL ?? null,
        },
      });
    },
  );

  server.registerTool(
    'hdim_service_catalog',
    {
      title: 'HDIM Service Catalog',
      description: 'Returns central service inventory with ownership/category metadata and runtime status.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
      }),
    },
    async ({ composeFile }) => {
      const servicesResult = await runCompose({
        composeFile,
        subcommandArgs: ['config', '--services'],
        timeoutMs: 30_000,
      });
      const psResult = await runCompose({
        composeFile,
        subcommandArgs: ['ps', '--format', 'json'],
        timeoutMs: 30_000,
      });

      const services = servicesResult.ok ? parseServicesFromConfigOutput(servicesResult.stdout) : [];
      const psEntries = psResult.ok ? parseComposePsJson(psResult.stdout) : [];
      const catalog = buildServiceCatalogEntries(services, psEntries);

      return toJsonText({
        composeFile: normalizeComposeFile(composeFile),
        summary: {
          totalServices: catalog.length,
          runningServices: catalog.filter((entry) => entry.running).length,
          withMetadata: catalog.filter((entry) => Boolean(entry.metadata)).length,
        },
        catalog,
      });
    },
  );

  server.registerTool(
    'hdim_service_config_contracts',
    {
      title: 'HDIM Service Config Contracts',
      description: 'Returns baseline configuration contracts and current env resolution for central management.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
      }),
    },
    async ({ composeFile }) => {
      const resolvedComposeFile = normalizeComposeFile(composeFile);
      let composeText = '';
      try {
        composeText = readFileSync(resolvedComposeFile, 'utf8');
      } catch (error) {
        return toJsonText({
          composeFile: resolvedComposeFile,
          error: `Unable to read compose file: ${error?.message ?? String(error)}`,
        });
      }

      const discoveredVars = extractComposeEnvVariables(composeText);
      const globalResolved = CONFIG_CONTRACTS.global.map((entry) => ({
        ...entry,
        value: process.env[entry.name] ?? null,
      }));

      const serviceContracts = Object.fromEntries(
        Object.entries(CONFIG_CONTRACTS.services).map(([service, entries]) => [
          service,
          entries.map((entry) => ({
            ...entry,
            value: process.env[entry.name] ?? null,
          })),
        ]),
      );

      return toJsonText({
        composeFile: resolvedComposeFile,
        summary: {
          discoveredVariables: discoveredVars.length,
          contractedServices: Object.keys(CONFIG_CONTRACTS.services).length,
          globalContracts: CONFIG_CONTRACTS.global.length,
        },
        discoveredVariables: discoveredVars,
        contracts: {
          global: globalResolved,
          services: serviceContracts,
        },
      });
    },
  );

  server.registerTool(
    'hdim_policy_registry',
    {
      title: 'HDIM Policy Registry',
      description: 'Returns central policy profiles and active tenant/isolation defaults for release gating.',
      inputSchema: z.object({
        policyFile: z.string().min(1).optional(),
      }),
    },
    async ({ policyFile }) => {
      const resolvedPolicyFile = (policyFile ?? DEFAULT_POLICY_FILE).trim();
      let profiles = {};
      let readError = null;
      try {
        const raw = readFileSync(resolvedPolicyFile, 'utf8');
        const parsed = JSON.parse(raw);
        profiles = parsed.profiles ?? {};
      } catch (error) {
        readError = error?.message ?? String(error);
      }

      return toJsonText({
        policyFile: resolvedPolicyFile,
        readError,
        defaults: {
          policyMode: 'strict',
          tenantCheckEndpoints: ['/api/quality/results?page=0&size=1', '/api/v1/audit/logs/statistics'],
          requireTenantDifferentiation: true,
        },
        profiles,
      });
    },
  );

  server.registerTool(
    'hdim_release_artifact_diff',
    {
      title: 'HDIM Release Artifact Diff',
      description:
        'Compares the two most recent release-gate JSON artifacts and highlights regressions in pass/readiness/warnings/violations.',
      inputSchema: z.object({
        reportDir: z.string().min(1).default('logs/mcp-reports'),
      }),
    },
    async ({ reportDir }) => {
      const resolvedReportDir = `${reportDir}`.trim();
      const reports = listReleaseGateJsonReports(resolvedReportDir);
      if (reports.length < 2) {
        return toJsonText({
          reportDir: resolvedReportDir,
          error: 'At least two release-gate JSON reports are required',
          reports,
        });
      }

      const currentPath = reports[0];
      const previousPath = reports[1];
      const current = parseReport(currentPath);
      const previous = parseReport(previousPath);

      if (!current.ok || !previous.ok) {
        return toJsonText({
          reportDir: resolvedReportDir,
          error: 'Unable to parse one or more reports',
          currentPath,
          previousPath,
          currentError: current.ok ? null : current.error,
          previousError: previous.ok ? null : previous.error,
        });
      }

      return toJsonText({
        reportDir: resolvedReportDir,
        currentPath,
        previousPath,
        currentTimestamp: parseReportTimestampFromPath(currentPath),
        previousTimestamp: parseReportTimestampFromPath(previousPath),
        diff: buildGateDiff(previous.payload, current.payload),
      });
    },
  );

  server.registerTool(
    'hdim_release_evidence_pack',
    {
      title: 'HDIM Release Evidence Pack',
      description:
        'Builds a consolidated release evidence pack from the latest release-gate artifacts and optional regression diff.',
      inputSchema: z.object({
        reportDir: z.string().min(1).default('logs/mcp-reports'),
        outputDir: z.string().min(1).default('logs/mcp-reports'),
        includeDiff: z.boolean().default(true),
      }),
    },
    async ({ reportDir, outputDir, includeDiff }) => {
      const resolvedReportDir = `${reportDir}`.trim();
      const resolvedOutputDir = `${outputDir}`.trim();
      const reports = listReleaseGateJsonReports(resolvedReportDir);
      if (reports.length === 0) {
        return toJsonText({
          reportDir: resolvedReportDir,
          outputDir: resolvedOutputDir,
          error: 'No release-gate JSON reports found',
        });
      }

      const latestPath = reports[0];
      const latest = parseReport(latestPath);
      if (!latest.ok) {
        return toJsonText({
          reportDir: resolvedReportDir,
          outputDir: resolvedOutputDir,
          error: `Unable to parse latest report: ${latest.error}`,
          latestPath,
        });
      }

      let diff = null;
      if (toBool(includeDiff) && reports.length > 1) {
        const previous = parseReport(reports[1]);
        if (previous.ok) {
          diff = buildGateDiff(previous.payload, latest.payload);
        }
      }

      const ts = parseReportTimestampFromPath(latestPath) ?? new Date().toISOString().replace(/[:.]/g, '-');
      const pack = {
        generatedAt: new Date().toISOString(),
        latestReportPath: latestPath,
        latestReportTimestamp: parseReportTimestampFromPath(latestPath),
        latestSummary: latest.payload.summary ?? null,
        latestPass: latest.payload.pass,
        latestTenantPolicy: {
          mode: latest.payload.tenantIsolation?.policyMode ?? null,
          pass: latest.payload.tenantIsolation?.pass ?? null,
          warnings: latest.payload.tenantIsolation?.warnings?.length ?? 0,
          violations: latest.payload.tenantIsolation?.violations?.length ?? 0,
        },
        diff,
      };

      mkdirSync(resolvedOutputDir, { recursive: true });
      const jsonPath = path.join(resolvedOutputDir, `release-evidence-pack-${ts}.json`);
      const mdPath = path.join(resolvedOutputDir, `release-evidence-pack-${ts}.md`);
      writeFileSync(jsonPath, JSON.stringify(pack, null, 2));
      writeFileSync(
        mdPath,
        [
          '# Release Evidence Pack',
          '',
          `- Generated: ${pack.generatedAt}`,
          `- Latest report: ${latestPath}`,
          `- Latest pass: ${pack.latestPass}`,
          `- Tenant policy mode: ${pack.latestTenantPolicy.mode ?? 'unknown'}`,
          `- Tenant policy pass: ${pack.latestTenantPolicy.pass}`,
          `- Tenant warnings: ${pack.latestTenantPolicy.warnings}`,
          `- Tenant violations: ${pack.latestTenantPolicy.violations}`,
          diff
            ? `- Regression detected: ${diff.hasRegression} (${diff.regressions.join('; ') || 'none'})`
            : '- Regression detected: n/a',
          '',
        ].join('\n'),
      );

      return toJsonText({
        reportDir: resolvedReportDir,
        outputDir: resolvedOutputDir,
        latestReportPath: latestPath,
        evidencePackJson: jsonPath,
        evidencePackMarkdown: mdPath,
        includeDiff: toBool(includeDiff),
        diff,
      });
    },
  );

  server.registerTool(
    'hdim_tenant_isolation_check',
    {
      title: 'HDIM Tenant Isolation Check',
      description:
        'Checks tenant header enforcement behavior and compares responses across tenants for core endpoints.',
      inputSchema: z.object({
        gatewayUrl: z.string().url().optional(),
        tenantA: z.string().min(1).default('acme-health'),
        tenantB: z.string().min(1).default('beta-health'),
        policyMode: z.enum(['strict', 'permissive']).default('strict'),
        noHeaderAllowlist: z.array(z.string().min(1)).max(20).default([]),
        requestTimeoutSecs: z.number().int().min(2).max(120).default(8),
        endpoints: z
          .array(z.string().min(1))
          .max(20)
          .default(['/api/quality/results?page=0&size=1', '/api/v1/audit/logs/statistics']),
        requireTenantDifferentiation: z.boolean().default(true),
      }),
    },
    async ({
      gatewayUrl,
      tenantA,
      tenantB,
      policyMode,
      noHeaderAllowlist,
      requestTimeoutSecs,
      endpoints,
      requireTenantDifferentiation,
    }) => {
      const resolvedEndpoints =
        endpoints ?? ['/api/quality/results?page=0&size=1', '/api/v1/audit/logs/statistics'];
      const resolvedNoHeaderAllowlist = noHeaderAllowlist ?? [];
      const resolvedPolicyMode = policyMode ?? 'strict';
      const resolvedRequireTenantDifferentiation = requireTenantDifferentiation ?? true;
      const timeoutMs = (requestTimeoutSecs ?? 8) * 1000;
      const resolvedGatewayUrl = (gatewayUrl ?? process.env.GATEWAY_URL ?? DEFAULT_BASE_URL)
        .trim()
        .replace(/\/$/, '');

      const request = async (pathSuffix, tenantId) => {
        const headers = { accept: 'application/json, text/plain;q=0.9, */*;q=0.1' };
        if (tenantId) headers['x-tenant-id'] = tenantId;
        const url = `${resolvedGatewayUrl}${pathSuffix}`;
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), timeoutMs);
        try {
          const response = await fetchImpl(url, {
            method: 'GET',
            headers,
            signal: controller.signal,
          });
          return {
            path: pathSuffix,
            tenantId: tenantId ?? null,
            status: response.status,
            ok: response.ok,
            body: await readResponseBodySafe(response),
          };
        } catch (error) {
          return {
            path: pathSuffix,
            tenantId: tenantId ?? null,
            status: null,
            ok: false,
            error: error?.message ?? String(error),
          };
        } finally {
          clearTimeout(timeout);
        }
      };

      const checks = [];
      for (const endpoint of resolvedEndpoints) {
        const withoutTenant = await request(endpoint, undefined);
        const withTenantA = await request(endpoint, tenantA);
        const withTenantB = await request(endpoint, tenantB);
        const tenantStatusesDiffer =
          withTenantA.status !== null &&
          withTenantB.status !== null &&
          withTenantA.status !== withTenantB.status;
        const sameStatusDiffBody =
          withTenantA.status === withTenantB.status &&
          (withTenantA.body ?? '') !== (withTenantB.body ?? '');

        checks.push({
          endpoint,
          withoutTenant,
          withTenantA,
          withTenantB,
          heuristics: {
            headerEnforced: isHeaderEnforcedStatus(withoutTenant.status),
            tenantResponsesDiffer: tenantStatusesDiffer || sameStatusDiffBody,
            allowlistedNoHeader: isAllowlistedEndpoint(endpoint, resolvedNoHeaderAllowlist),
          },
        });
      }

      const strictViolations = checks
        .filter((item) => !item.heuristics.allowlistedNoHeader && !item.heuristics.headerEnforced)
        .map((item) => ({
          endpoint: item.endpoint,
          reason: 'Tenant header not enforced for non-allowlisted endpoint',
          statusWithoutTenant: item.withoutTenant.status,
        }));

      const tenantDifferentiationObserved = checks.some((item) => item.heuristics.tenantResponsesDiffer);
      const differentiationViolation =
        resolvedRequireTenantDifferentiation && !tenantDifferentiationObserved
          ? [
              {
                reason: 'No tenant differentiation observed across tested endpoints',
              },
            ]
          : [];

      const warnings =
        resolvedPolicyMode === 'permissive'
          ? strictViolations.map((item) => ({
              ...item,
              reason: `Permissive mode warning: ${item.reason}`,
            }))
          : [];

      const pass =
        (resolvedPolicyMode === 'strict' ? strictViolations.length === 0 : true) &&
        differentiationViolation.length === 0;

      return toJsonText({
        gatewayUrl: resolvedGatewayUrl,
        tenants: { tenantA, tenantB },
        policy: {
          mode: resolvedPolicyMode,
          noHeaderAllowlist: resolvedNoHeaderAllowlist,
          requireTenantDifferentiation: resolvedRequireTenantDifferentiation,
          pass,
          violations: [
            ...(resolvedPolicyMode === 'strict' ? strictViolations : []),
            ...differentiationViolation,
          ],
          warnings,
        },
        checks,
        summary: {
          totalEndpoints: checks.length,
          headerEnforcedCount: checks.filter((item) => item.heuristics.headerEnforced).length,
          tenantDifferentiationCount: checks.filter((item) => item.heuristics.tenantResponsesDiffer)
            .length,
        },
      });
    },
  );

  server.registerTool(
    'hdim_seed_diagnostics',
    {
      title: 'HDIM Seed Diagnostics',
      description:
        'Diagnoses demo seeding issues by checking health, attempting a small seed POST, and collecting service logs.',
      inputSchema: z.object({
        demoSeedingUrl: z.string().url().optional(),
        tenantId: z.string().min(1).optional(),
        count: z.number().int().min(1).max(500).default(5),
        careGapPercentage: z.number().int().min(0).max(100).default(20),
        requestTimeoutSecs: z.number().int().min(3).max(300).default(20),
        composeFile: z.string().min(1).optional(),
        includeLogs: z.boolean().default(true),
        logTail: z.number().int().min(20).max(2000).default(120),
      }),
    },
    async ({
      demoSeedingUrl,
      tenantId,
      count,
      careGapPercentage,
      requestTimeoutSecs,
      composeFile,
      includeLogs,
      logTail,
    }) => {
      const baseUrl = (demoSeedingUrl ?? process.env.DEMO_SEEDING_URL ?? 'http://localhost:8098')
        .trim()
        .replace(/\/$/, '');
      const resolvedTenantId = tenantId ?? process.env.TENANT_ID ?? 'acme-health';
      const timeoutMs = requestTimeoutSecs * 1000;
      const endpointHealth = `${baseUrl}/demo/actuator/health`;
      const endpointSeed = `${baseUrl}/demo/api/v1/demo/patients/generate`;

      const httpResult = async (method, url, body) => {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), timeoutMs);
        try {
          const response = await fetchImpl(url, {
            method,
            signal: controller.signal,
            headers: {
              accept: 'application/json, text/plain;q=0.9, */*;q=0.1',
              'content-type': 'application/json',
              'x-tenant-id': resolvedTenantId,
            },
            ...(body ? { body: JSON.stringify(body) } : {}),
          });
          const text = await response.text();
          return {
            ok: response.ok,
            status: response.status,
            body: truncateText(text, 10_000),
          };
        } catch (error) {
          return {
            ok: false,
            status: null,
            error: error?.message ?? String(error),
          };
        } finally {
          clearTimeout(timeout);
        }
      };

      const health = await httpResult('GET', endpointHealth);
      const seedAttempt = await httpResult('POST', endpointSeed, {
        count,
        tenantId: resolvedTenantId,
        careGapPercentage,
      });

      let logs = null;
      if (includeLogs) {
        logs = await runCompose({
          composeFile,
          subcommandArgs: [
            'logs',
            '--no-color',
            '--tail',
            `${logTail}`,
            'demo-seeding-service',
            'gateway-edge',
          ],
          timeoutMs: 60_000,
        });
      }

      return toJsonText({
        demoSeedingUrl: baseUrl,
        tenantId: resolvedTenantId,
        diagnostics: {
          health,
          seedAttempt,
        },
        logs: logs
          ? {
              ok: logs.ok,
              exitCode: logs.exitCode,
              stderr: truncateText(logs.stderr, 4_000),
              stdout: truncateText(logs.stdout, 20_000),
            }
          : null,
      });
    },
  );

  server.registerTool(
    'hdim_docker_ps',
    {
      title: 'HDIM Docker Compose PS',
      description: 'Shows compose service status for the HDIM stack.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
      }),
    },
    async ({ composeFile }) => {
      const result = await runCompose({
        composeFile,
        subcommandArgs: ['ps'],
        timeoutMs: 30_000,
      });
      return toJsonText(result);
    },
  );

  server.registerTool(
    'hdim_docker_up',
    {
      title: 'HDIM Docker Compose Up',
      description: 'Starts HDIM services using docker compose up -d.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
        services: z.array(z.string().min(1)).max(20).optional(),
      }),
    },
    async ({ composeFile, services }) => {
      const result = await runCompose({
        composeFile,
        subcommandArgs: ['up', '-d', ...toServiceList(services)],
        timeoutMs: 300_000,
      });
      return toJsonText(result);
    },
  );

  server.registerTool(
    'hdim_docker_down',
    {
      title: 'HDIM Docker Compose Down',
      description: 'Stops HDIM services using docker compose down.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
      }),
    },
    async ({ composeFile }) => {
      const result = await runCompose({
        composeFile,
        subcommandArgs: ['down'],
        timeoutMs: 180_000,
      });
      return toJsonText(result);
    },
  );

  server.registerTool(
    'hdim_docker_logs',
    {
      title: 'HDIM Docker Compose Logs',
      description: 'Returns recent logs from HDIM compose services.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
        services: z.array(z.string().min(1)).max(20).optional(),
        tail: z.number().int().min(1).max(5000).default(200),
      }),
    },
    async ({ composeFile, services, tail }) => {
      const result = await runCompose({
        composeFile,
        subcommandArgs: ['logs', '--no-color', '--tail', `${tail}`, ...toServiceList(services)],
        timeoutMs: 60_000,
      });
      return toJsonText(result);
    },
  );

  server.registerTool(
    'hdim_gateway_validate',
    {
      title: 'HDIM Gateway Validate',
      description: 'Validates core gateway endpoints for live demo testing.',
      inputSchema: z.object({
        baseUrl: z.string().url().optional(),
        requestTimeoutSecs: z.number().int().min(2).max(120).default(8),
        warmupTimeoutSecs: z.number().int().min(0).max(600).default(0),
        pollIntervalMs: z.number().int().min(200).max(10_000).default(1_000),
        stablePasses: z.number().int().min(1).max(10).default(1),
      }),
    },
    async ({ baseUrl, requestTimeoutSecs, warmupTimeoutSecs, pollIntervalMs, stablePasses }) => {
      const gatewayBase = (baseUrl ?? process.env.HDIM_BASE_URL ?? DEFAULT_BASE_URL).trim().replace(/\/$/, '');
      const timeoutMs = (requestTimeoutSecs ?? 8) * 1000;
      const resolvedWarmupTimeoutSecs = warmupTimeoutSecs ?? 0;
      const resolvedPollIntervalMs = pollIntervalMs ?? 1_000;
      const resolvedStablePasses = stablePasses ?? 1;
      const deadline = Date.now() + resolvedWarmupTimeoutSecs * 1000;

      const checks = ['/actuator/health', '/fhir/metadata'];
      const runChecks = async () => {
        const results = [];

        for (const endpoint of checks) {
          const url = `${gatewayBase}${endpoint}`;
          const controller = new AbortController();
          const timeout = setTimeout(() => controller.abort(), timeoutMs);
          try {
            const response = await fetchImpl(url, {
              method: 'GET',
              headers: { accept: 'application/json, text/plain;q=0.9, */*;q=0.1' },
              signal: controller.signal,
            });
            results.push({ endpoint, url, status: response.status, ok: response.ok });
          } catch (error) {
            results.push({
              endpoint,
              url,
              status: null,
              ok: false,
              error: error?.message ?? String(error),
            });
          } finally {
            clearTimeout(timeout);
          }
        }

        return results;
      };

      let attempts = 0;
      let consecutiveHealthyPasses = 0;
      let results = [];
      const history = [];

      while (true) {
        attempts += 1;
        results = await runChecks();
        const allHealthy = results.every((item) => item.ok);
        consecutiveHealthyPasses = allHealthy ? consecutiveHealthyPasses + 1 : 0;
        history.push({
          attempt: attempts,
          allHealthy,
          statuses: results.map((item) => ({
            endpoint: item.endpoint,
            status: item.status,
            ok: item.ok,
          })),
        });

        if (consecutiveHealthyPasses >= resolvedStablePasses) break;

        const remainingMs = deadline - Date.now();
        if (remainingMs <= 0) break;
        await sleep(Math.min(resolvedPollIntervalMs, remainingMs));
      }

      return toJsonText({
        baseUrl: gatewayBase,
        allHealthy: results.every((item) => item.ok),
        checks: results,
        warmup: {
          requestTimeoutSecs: requestTimeoutSecs ?? 8,
          warmupTimeoutSecs: resolvedWarmupTimeoutSecs,
          pollIntervalMs: resolvedPollIntervalMs,
          stablePassesRequired: resolvedStablePasses,
          attempts,
          achievedStablePasses: consecutiveHealthyPasses >= resolvedStablePasses,
          consecutiveHealthyPasses,
          history,
        },
      });
    },
  );

  server.registerTool(
    'hdim_demo_seed',
    {
      title: 'HDIM Demo Seed',
      description: 'Runs scripts/seed-all-demo-data.sh with non-interactive defaults.',
      inputSchema: z.object({
        tenantId: z.string().min(1).optional(),
        seedProfile: z.enum(['smoke', 'full']).default('smoke'),
        nonInteractive: z.boolean().default(true),
        demoSeedingUrl: z.string().url().optional(),
        waitTimeoutSecs: z.number().int().min(30).max(1800).default(240),
        curlMaxTimeSecs: z.number().int().min(5).max(1800).default(120),
      }),
    },
    async ({ tenantId, seedProfile, nonInteractive, demoSeedingUrl, waitTimeoutSecs, curlMaxTimeSecs }) => {
      const result = await runRepoScript({
        scriptPath: 'scripts/seed-all-demo-data.sh',
        timeoutMs: Math.max(waitTimeoutSecs * 1000 + curlMaxTimeSecs * 1000 + 30_000, 120_000),
        env: {
          TENANT_ID: tenantId ?? process.env.TENANT_ID ?? 'acme-health',
          SEED_PROFILE: seedProfile,
          NON_INTERACTIVE: nonInteractive ? '1' : '0',
          WAIT_TIMEOUT_SECS: `${waitTimeoutSecs}`,
          CURL_MAX_TIME: `${curlMaxTimeSecs}`,
          ...(demoSeedingUrl ? { DEMO_SEEDING_URL: demoSeedingUrl } : {}),
        },
      });
      return toJsonText(result);
    },
  );

  server.registerTool(
    'hdim_system_validate',
    {
      title: 'HDIM System Validate',
      description: 'Runs validate-system.sh for live platform verification.',
      inputSchema: z.object({
        gatewayUrl: z.string().url().optional(),
        tenantId: z.string().min(1).optional(),
        skipFrontend: z.boolean().default(false),
        skipFhirQuery: z.boolean().default(false),
      }),
    },
    async ({ gatewayUrl, tenantId, skipFrontend, skipFhirQuery }) => {
      const result = await runRepoScript({
        scriptPath: 'validate-system.sh',
        timeoutMs: 180_000,
        env: {
          GATEWAY_URL: gatewayUrl ?? process.env.GATEWAY_URL ?? DEFAULT_BASE_URL,
          TENANT_ID: tenantId ?? process.env.TENANT_ID ?? 'acme-health',
          SKIP_FRONTEND_VALIDATE: skipFrontend ? '1' : '0',
          SKIP_FHIR_QUERY: skipFhirQuery ? '1' : '0',
        },
      });
      return toJsonText(result);
    },
  );

  server.registerTool(
    'hdim_live_readiness',
    {
      title: 'HDIM Live Readiness',
      description:
        'Runs a consolidated readiness check (compose status, gateway checks, seed diagnostics, and optional full system validation).',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
        gatewayUrl: z.string().url().optional(),
        demoSeedingUrl: z.string().url().optional(),
        tenantId: z.string().min(1).optional(),
        includeLogs: z.boolean().default(true),
        logTail: z.number().int().min(20).max(2000).default(80),
        requestTimeoutSecs: z.number().int().min(3).max(120).default(30),
        runSystemValidate: z.boolean().default(true),
        skipFrontend: z.boolean().default(false),
        skipFhirQuery: z.boolean().default(false),
      }),
    },
    async ({
      composeFile,
      gatewayUrl,
      demoSeedingUrl,
      tenantId,
      includeLogs,
      logTail,
      requestTimeoutSecs,
      runSystemValidate,
      skipFrontend,
      skipFhirQuery,
    }) => {
      const resolvedGatewayUrl = (gatewayUrl ?? process.env.GATEWAY_URL ?? DEFAULT_BASE_URL)
        .trim()
        .replace(/\/$/, '');
      const resolvedSeedingUrl = (demoSeedingUrl ?? process.env.DEMO_SEEDING_URL ?? 'http://localhost:8098')
        .trim()
        .replace(/\/$/, '');
      const resolvedTenantId = tenantId ?? process.env.TENANT_ID ?? 'acme-health';

      const composePs = await runCompose({
        composeFile,
        subcommandArgs: ['ps'],
        timeoutMs: 30_000,
      });

      const checkGatewayEndpoint = async (endpoint) => {
        const url = `${resolvedGatewayUrl}${endpoint}`;
        try {
          const response = await fetchImpl(url, {
            method: 'GET',
            headers: { accept: 'application/json, text/plain;q=0.9, */*;q=0.1' },
          });
          return { endpoint, url, status: response.status, ok: response.ok };
        } catch (error) {
          return {
            endpoint,
            url,
            status: null,
            ok: false,
            error: error?.message ?? String(error),
          };
        }
      };

      const gatewayChecks = await Promise.all([
        checkGatewayEndpoint('/actuator/health'),
        checkGatewayEndpoint('/fhir/metadata'),
      ]);

      const timeoutMs = requestTimeoutSecs * 1000;
      const requestSeedEndpoint = async (method, url, body) => {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), timeoutMs);
        try {
          const response = await fetchImpl(url, {
            method,
            signal: controller.signal,
            headers: {
              accept: 'application/json, text/plain;q=0.9, */*;q=0.1',
              'content-type': 'application/json',
              'x-tenant-id': resolvedTenantId,
            },
            ...(body ? { body: JSON.stringify(body) } : {}),
          });
          const text = await response.text();
          return {
            ok: response.ok,
            status: response.status,
            body: truncateText(text, 10_000),
          };
        } catch (error) {
          return {
            ok: false,
            status: null,
            error: error?.message ?? String(error),
          };
        } finally {
          clearTimeout(timeout);
        }
      };

      const seedDiagnostics = {
        health: await requestSeedEndpoint('GET', `${resolvedSeedingUrl}/demo/actuator/health`),
        seedAttempt: await requestSeedEndpoint(
          'POST',
          `${resolvedSeedingUrl}/demo/api/v1/demo/patients/generate`,
          {
            count: 3,
            tenantId: resolvedTenantId,
            careGapPercentage: 20,
          },
        ),
      };

      let logs = null;
      if (includeLogs) {
        logs = await runCompose({
          composeFile,
          subcommandArgs: [
            'logs',
            '--no-color',
            '--tail',
            `${logTail}`,
            'demo-seeding-service',
            'gateway-edge',
          ],
          timeoutMs: 60_000,
        });
      }

      let systemValidate = null;
      if (runSystemValidate) {
        systemValidate = await runRepoScript({
          scriptPath: 'validate-system.sh',
          timeoutMs: 180_000,
          env: {
            GATEWAY_URL: resolvedGatewayUrl,
            TENANT_ID: resolvedTenantId,
            SKIP_FRONTEND_VALIDATE: skipFrontend ? '1' : '0',
            SKIP_FHIR_QUERY: skipFhirQuery ? '1' : '0',
          },
        });
      }

      const ready =
        composePs.ok &&
        gatewayChecks.every((check) => check.ok) &&
        seedDiagnostics.health.ok &&
        seedDiagnostics.seedAttempt.ok &&
        (!runSystemValidate || (systemValidate?.ok ?? false));

      return toJsonText({
        ready,
        composePs: {
          ok: composePs.ok,
          exitCode: composePs.exitCode,
          stderr: truncateText(composePs.stderr, 4_000),
        },
        gateway: {
          baseUrl: resolvedGatewayUrl,
          checks: gatewayChecks,
        },
        seedDiagnostics: {
          demoSeedingUrl: resolvedSeedingUrl,
          tenantId: resolvedTenantId,
          health: seedDiagnostics.health,
          seedAttempt: seedDiagnostics.seedAttempt,
        },
        logs: logs
          ? {
              ok: logs.ok,
              exitCode: logs.exitCode,
              stderr: truncateText(logs.stderr, 4_000),
              stdout: truncateText(logs.stdout, 20_000),
            }
          : null,
        systemValidate: systemValidate
          ? {
              ok: systemValidate.ok,
              exitCode: systemValidate.exitCode,
              stderr: truncateText(systemValidate.stderr, 4_000),
              stdout: truncateText(systemValidate.stdout, 20_000),
            }
          : null,
      });
    },
  );

  server.registerTool(
    'hdim_release_gate',
    {
      title: 'HDIM Release Gate',
      description:
        'Runs a consolidated release gate with topology, config audit, tenant isolation, and live readiness checks.',
      inputSchema: z.object({
        composeFile: z.string().min(1).optional(),
        gatewayUrl: z.string().url().optional(),
        demoSeedingUrl: z.string().url().optional(),
        tenantA: z.string().min(1).default('acme-health'),
        tenantB: z.string().min(1).default('beta-health'),
        requestTimeoutSecs: z.number().int().min(2).max(120).default(12),
        policyMode: z.enum(['strict', 'permissive']).default('strict'),
        noHeaderAllowlist: z.array(z.string().min(1)).max(20).default([]),
        tenantCheckEndpoints: z
          .array(z.string().min(1))
          .max(20)
          .default(['/api/quality/results?page=0&size=1', '/api/v1/audit/logs/statistics']),
        requireTenantDifferentiation: z.boolean().default(true),
        includeLogs: z.boolean().default(false),
        runSystemValidate: z.boolean().default(true),
        skipFrontend: z.boolean().default(false),
        skipFhirQuery: z.boolean().default(false),
      }),
    },
    async ({
      composeFile,
      gatewayUrl,
      demoSeedingUrl,
      tenantA,
      tenantB,
      requestTimeoutSecs,
      policyMode,
      noHeaderAllowlist,
      tenantCheckEndpoints,
      requireTenantDifferentiation,
      includeLogs,
      runSystemValidate,
      skipFrontend,
      skipFhirQuery,
    }) => {
      const resolvedPolicyMode = policyMode ?? 'strict';
      const resolvedNoHeaderAllowlist = noHeaderAllowlist ?? [];
      const resolvedTenantCheckEndpoints =
        tenantCheckEndpoints ?? ['/api/quality/results?page=0&size=1', '/api/v1/audit/logs/statistics'];
      const resolvedRequireTenantDifferentiation = requireTenantDifferentiation ?? true;
      const resolvedGatewayUrl = (gatewayUrl ?? process.env.GATEWAY_URL ?? DEFAULT_BASE_URL)
        .trim()
        .replace(/\/$/, '');
      const resolvedRequestTimeoutSecs = requestTimeoutSecs ?? 12;
      const timeoutMs = resolvedRequestTimeoutSecs * 1000;
      const resolvedComposeFile = normalizeComposeFile(composeFile);
      const resolvedSeedingUrl = (demoSeedingUrl ?? process.env.DEMO_SEEDING_URL ?? 'http://localhost:8098')
        .trim()
        .replace(/\/$/, '');

      const topologyResult = await runCompose({
        composeFile: resolvedComposeFile,
        subcommandArgs: ['ps', '--format', 'json'],
        timeoutMs: 30_000,
      });
      const topologyEntries = topologyResult.ok ? parseComposePsJson(topologyResult.stdout) : [];
      const runningCount = topologyEntries.length;

      const configAudit = (() => {
        try {
          const composeText = readFileSync(resolvedComposeFile, 'utf8');
          const vars = extractComposeEnvVariables(composeText);
          const missingRequired = vars
            .filter((item) => item.required)
            .map((item) => item.name)
            .filter((name) => {
              const value = process.env[name];
              return typeof value === 'undefined' || value === '';
            });
          return {
            ok: true,
            variablesReferenced: vars.length,
            missingRequired,
          };
        } catch (error) {
          return {
            ok: false,
            variablesReferenced: 0,
            missingRequired: [],
            error: error?.message ?? String(error),
          };
        }
      })();

      const tenantCheck = async (pathSuffix, tenantId) => {
        const url = `${resolvedGatewayUrl}${pathSuffix}`;
        const headers = { accept: 'application/json, text/plain;q=0.9, */*;q=0.1' };
        if (tenantId) headers['x-tenant-id'] = tenantId;
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), timeoutMs);
        try {
          const response = await fetchImpl(url, {
            method: 'GET',
            headers,
            signal: controller.signal,
          });
          return { status: response.status, ok: response.ok };
        } catch (error) {
          return { status: null, ok: false, error: error?.message ?? String(error) };
        } finally {
          clearTimeout(timeout);
        }
      };

      const tenantResults = [];
      for (const endpoint of resolvedTenantCheckEndpoints) {
        const withoutTenant = await tenantCheck(endpoint, undefined);
        const tenantAStatus = await tenantCheck(endpoint, tenantA);
        const tenantBStatus = await tenantCheck(endpoint, tenantB);
        const allowlistedNoHeader = isAllowlistedEndpoint(endpoint, resolvedNoHeaderAllowlist);
        const headerEnforced = isHeaderEnforcedStatus(withoutTenant.status);
        const tenantDifferentiated =
          tenantAStatus.status !== null &&
          tenantBStatus.status !== null &&
          tenantAStatus.status !== tenantBStatus.status;
        tenantResults.push({
          endpoint,
          withoutTenant,
          tenantA: tenantAStatus,
          tenantB: tenantBStatus,
          allowlistedNoHeader,
          headerEnforced,
          tenantDifferentiated,
        });
      }

      const strictTenantViolations = tenantResults
        .filter((result) => !result.allowlistedNoHeader && !result.headerEnforced)
        .map((result) => ({
          endpoint: result.endpoint,
          reason: 'Tenant header not enforced for non-allowlisted endpoint',
          statusWithoutTenant: result.withoutTenant.status,
        }));
      const tenantDifferentiationObserved = tenantResults.some((result) => result.tenantDifferentiated);
      const differentiationViolations =
        resolvedRequireTenantDifferentiation && !tenantDifferentiationObserved
          ? [{ reason: 'No tenant differentiation observed across tenantCheckEndpoints' }]
          : [];
      const tenantPolicyPass =
        (resolvedPolicyMode === 'strict' ? strictTenantViolations.length === 0 : true) &&
        differentiationViolations.length === 0;
      const tenantPolicyWarnings =
        resolvedPolicyMode === 'permissive'
          ? strictTenantViolations.map((item) => ({
              ...item,
              reason: `Permissive mode warning: ${item.reason}`,
            }))
          : [];

      const readiness = await (async () => {
        const gatewayChecks = await Promise.all(
          ['/actuator/health', '/fhir/metadata'].map(async (endpoint) => {
            const url = `${resolvedGatewayUrl}${endpoint}`;
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), timeoutMs);
            try {
              const response = await fetchImpl(url, {
                method: 'GET',
                headers: { accept: 'application/json, text/plain;q=0.9, */*;q=0.1' },
                signal: controller.signal,
              });
              return { endpoint, status: response.status, ok: response.ok };
            } catch (error) {
              return {
                endpoint,
                status: null,
                ok: false,
                error: error?.message ?? String(error),
              };
            } finally {
              clearTimeout(timeout);
            }
          }),
        );

        const seedHealth = await (async () => {
          const url = `${resolvedSeedingUrl}/demo/actuator/health`;
          const controller = new AbortController();
          const timeout = setTimeout(() => controller.abort(), timeoutMs);
          try {
            const response = await fetchImpl(url, {
              method: 'GET',
              headers: {
                accept: 'application/json, text/plain;q=0.9, */*;q=0.1',
                'x-tenant-id': tenantA,
              },
              signal: controller.signal,
            });
            return { ok: response.ok, status: response.status };
          } catch (error) {
            return {
              ok: false,
              status: null,
              error: error?.message ?? String(error),
            };
          } finally {
            clearTimeout(timeout);
          }
        })();

        let systemValidate = null;
        if (runSystemValidate) {
          systemValidate = await runRepoScript({
            scriptPath: 'validate-system.sh',
            timeoutMs: 180_000,
            env: {
              GATEWAY_URL: resolvedGatewayUrl,
              TENANT_ID: tenantA,
              SKIP_FRONTEND_VALIDATE: skipFrontend ? '1' : '0',
              SKIP_FHIR_QUERY: skipFhirQuery ? '1' : '0',
            },
          });
        }

        return {
          gatewayChecks,
          seedHealth,
          systemValidate,
          ready:
            gatewayChecks.every((item) => item.ok) &&
            seedHealth.ok &&
            (!runSystemValidate || (systemValidate?.ok ?? false)),
        };
      })();

      let logs = null;
      if (includeLogs) {
        logs = await runCompose({
          composeFile: resolvedComposeFile,
          subcommandArgs: ['logs', '--no-color', '--tail', '120', 'gateway-edge', 'demo-seeding-service'],
          timeoutMs: 60_000,
        });
      }

      const pass =
        topologyResult.ok &&
        runningCount > 0 &&
        configAudit.ok &&
        configAudit.missingRequired.length === 0 &&
        tenantPolicyPass &&
        readiness.ready;

      return toJsonText({
        pass,
        summary: {
          runningServices: runningCount,
          missingRequiredConfig: configAudit.missingRequired.length,
          tenantHeaderEnforcedCount: tenantResults.filter((result) => result.headerEnforced).length,
          tenantPolicyMode: resolvedPolicyMode,
          tenantPolicyPass,
          readiness: readiness.ready,
        },
        topology: {
          ok: topologyResult.ok,
          exitCode: topologyResult.exitCode,
          runningCount,
        },
        configAudit,
        tenantIsolation: {
          policyMode: resolvedPolicyMode,
          noHeaderAllowlist: resolvedNoHeaderAllowlist,
          requireTenantDifferentiation: resolvedRequireTenantDifferentiation,
          tenantCheckEndpoints: resolvedTenantCheckEndpoints,
          pass: tenantPolicyPass,
          violations: [
            ...(resolvedPolicyMode === 'strict' ? strictTenantViolations : []),
            ...differentiationViolations,
          ],
          warnings: tenantPolicyWarnings,
          results: tenantResults,
        },
        readiness: {
          gatewayChecks: readiness.gatewayChecks,
          seedHealth: readiness.seedHealth,
          systemValidate: readiness.systemValidate
            ? {
                ok: readiness.systemValidate.ok,
                exitCode: readiness.systemValidate.exitCode,
                stderr: truncateText(readiness.systemValidate.stderr, 4_000),
              }
            : null,
        },
        logs: logs
          ? {
              ok: logs.ok,
              exitCode: logs.exitCode,
              stderr: truncateText(logs.stderr, 4_000),
            }
          : null,
      });
    },
  );

  return server;
}

async function main() {
  const server = createServer();
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

const entrypointHref = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : null;
const isEntrypoint = entrypointHref === import.meta.url;
if (isEntrypoint) {
  main().catch((error) => {
    // eslint-disable-next-line no-console
    console.error(error);
    process.exit(1);
  });
}

import assert from 'node:assert/strict';
import test from 'node:test';

import {
  composeArgs,
  createServer,
  extractComposeEnvVariables,
  normalizeComposeFile,
  normalizeDockerBin,
  parseComposePsJson,
  toServiceList,
} from './hdim-docker-mcp.mjs';

test('compose helpers normalize defaults', () => {
  assert.equal(normalizeComposeFile(' docker-compose.demo.yml '), 'docker-compose.demo.yml');
  assert.equal(normalizeDockerBin(' docker '), 'docker');
  assert.deepEqual(toServiceList([' gateway-edge ', '']), ['gateway-edge']);
  assert.deepEqual(composeArgs('docker-compose.demo.yml', ['ps']), [
    'compose',
    '-f',
    'docker-compose.demo.yml',
    'ps',
  ]);
});

test('server registers expected docker tools', () => {
  const server = createServer();
  const toolNames = Object.keys(server._registeredTools ?? {});

  assert.ok(toolNames.includes('hdim_docker_info'));
  assert.ok(toolNames.includes('hdim_docker_ps'));
  assert.ok(toolNames.includes('hdim_docker_up'));
  assert.ok(toolNames.includes('hdim_docker_down'));
  assert.ok(toolNames.includes('hdim_docker_logs'));
  assert.ok(toolNames.includes('hdim_gateway_validate'));
  assert.ok(toolNames.includes('hdim_demo_seed'));
  assert.ok(toolNames.includes('hdim_system_validate'));
  assert.ok(toolNames.includes('hdim_seed_diagnostics'));
  assert.ok(toolNames.includes('hdim_live_readiness'));
  assert.ok(toolNames.includes('hdim_topology_report'));
  assert.ok(toolNames.includes('hdim_config_audit'));
  assert.ok(toolNames.includes('hdim_service_catalog'));
  assert.ok(toolNames.includes('hdim_service_config_contracts'));
  assert.ok(toolNames.includes('hdim_policy_registry'));
  assert.ok(toolNames.includes('hdim_service_restart_plan'));
  assert.ok(toolNames.includes('hdim_service_operate'));
  assert.ok(toolNames.includes('hdim_release_artifact_diff'));
  assert.ok(toolNames.includes('hdim_release_evidence_pack'));
  assert.ok(toolNames.includes('hdim_tenant_isolation_check'));
  assert.ok(toolNames.includes('hdim_release_gate'));
});

test('docker up tool invokes compose with target services', async () => {
  const calls = [];
  const server = createServer({
    runCommand: async (command, args) => {
      calls.push({ command, args });
      return { ok: true, exitCode: 0, stdout: 'ok', stderr: '', timedOut: false };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const upTool = server._registeredTools?.['hdim_docker_up'];
  assert.ok(upTool);
  await upTool.handler({
    composeFile: 'docker-compose.demo.yml',
    services: ['gateway-edge', 'clinical-portal'],
  });

  assert.equal(calls.length, 1);
  assert.equal(calls[0].command, 'docker');
  assert.deepEqual(calls[0].args, [
    'compose',
    '-f',
    'docker-compose.demo.yml',
    'up',
    '-d',
    'gateway-edge',
    'clinical-portal',
  ]);
});

test('gateway validate reports healthy when checks pass', async () => {
  const server = createServer({
    fetchImpl: async () => ({ status: 200, ok: true }),
  });

  // eslint-disable-next-line no-underscore-dangle
  const validateTool = server._registeredTools?.['hdim_gateway_validate'];
  assert.ok(validateTool);
  const response = await validateTool.handler({ baseUrl: 'http://localhost:18080' });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.allHealthy, true);
  assert.equal(payload.checks.length, 2);
  assert.equal(payload.checks[0].status, 200);
  assert.equal(payload.warmup.attempts, 1);
  assert.equal(payload.warmup.achievedStablePasses, true);
});

test('gateway validate warmup retries until stable healthy checks', async () => {
  const responses = [
    { status: 200, ok: true },
    { status: 502, ok: false },
    { status: 200, ok: true },
    { status: 200, ok: true },
  ];

  const server = createServer({
    fetchImpl: async () => responses.shift() ?? { status: 200, ok: true },
    sleep: async () => {},
  });

  // eslint-disable-next-line no-underscore-dangle
  const validateTool = server._registeredTools?.['hdim_gateway_validate'];
  assert.ok(validateTool);
  const response = await validateTool.handler({
    baseUrl: 'http://localhost:18080',
    warmupTimeoutSecs: 5,
    pollIntervalMs: 200,
    stablePasses: 1,
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.allHealthy, true);
  assert.equal(payload.warmup.attempts, 2);
  assert.equal(payload.warmup.achievedStablePasses, true);
  assert.equal(payload.warmup.history.length, 2);
  assert.equal(payload.warmup.history[0].allHealthy, false);
  assert.equal(payload.warmup.history[1].allHealthy, true);
});

test('demo seed tool invokes seed script with safe defaults', async () => {
  const calls = [];
  const server = createServer({
    runCommand: async (command, args, options) => {
      calls.push({ command, args, options });
      return { ok: true, exitCode: 0, stdout: 'seeded', stderr: '', timedOut: false };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const seedTool = server._registeredTools?.['hdim_demo_seed'];
  assert.ok(seedTool);
  await seedTool.handler({
    tenantId: 'acme-health',
    seedProfile: 'smoke',
    nonInteractive: true,
    waitTimeoutSecs: 120,
    curlMaxTimeSecs: 30,
  });

  assert.equal(calls.length, 1);
  assert.equal(calls[0].command, 'bash');
  assert.deepEqual(calls[0].args, ['scripts/seed-all-demo-data.sh']);
  assert.equal(calls[0].options.env.NON_INTERACTIVE, '1');
  assert.equal(calls[0].options.env.SEED_PROFILE, 'smoke');
  assert.equal(calls[0].options.env.TENANT_ID, 'acme-health');
  assert.equal(calls[0].options.env.CURL_MAX_TIME, '30');
});

test('system validate tool invokes validate script', async () => {
  const calls = [];
  const server = createServer({
    runCommand: async (command, args, options) => {
      calls.push({ command, args, options });
      return { ok: true, exitCode: 0, stdout: 'valid', stderr: '', timedOut: false };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const validateTool = server._registeredTools?.['hdim_system_validate'];
  assert.ok(validateTool);
  await validateTool.handler({
    gatewayUrl: 'http://localhost:18080',
    tenantId: 'acme-health',
    skipFrontend: true,
    skipFhirQuery: true,
  });

  assert.equal(calls.length, 1);
  assert.equal(calls[0].command, 'bash');
  assert.deepEqual(calls[0].args, ['validate-system.sh']);
  assert.equal(calls[0].options.env.GATEWAY_URL, 'http://localhost:18080');
  assert.equal(calls[0].options.env.SKIP_FRONTEND_VALIDATE, '1');
  assert.equal(calls[0].options.env.SKIP_FHIR_QUERY, '1');
});

test('seed diagnostics checks endpoints and collects logs', async () => {
  const calls = [];
  const fetchCalls = [];
  const server = createServer({
    runCommand: async (command, args) => {
      calls.push({ command, args });
      return { ok: true, exitCode: 0, stdout: 'demo logs', stderr: '', timedOut: false };
    },
    fetchImpl: async (url, init) => {
      fetchCalls.push({ url, init });
      return {
        ok: true,
        status: 200,
        text: async () => '{"status":"ok"}',
      };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const diagnosticsTool = server._registeredTools?.['hdim_seed_diagnostics'];
  assert.ok(diagnosticsTool);
  const response = await diagnosticsTool.handler({
    demoSeedingUrl: 'http://localhost:8098',
    tenantId: 'acme-health',
    includeLogs: true,
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(fetchCalls.length, 2);
  assert.match(fetchCalls[0].url, /\/demo\/actuator\/health$/);
  assert.match(fetchCalls[1].url, /\/demo\/api\/v1\/demo\/patients\/generate$/);
  assert.equal(calls.length, 1);
  assert.equal(calls[0].command, 'docker');
  assert.ok(payload.logs);
  assert.equal(payload.diagnostics.health.status, 200);
  assert.equal(payload.diagnostics.seedAttempt.status, 200);
});

test('live readiness aggregates checks and marks ready on success', async () => {
  const calls = [];
  const server = createServer({
    runCommand: async (command, args) => {
      calls.push({ command, args });
      return { ok: true, exitCode: 0, stdout: 'ok', stderr: '', timedOut: false };
    },
    fetchImpl: async () => ({
      ok: true,
      status: 200,
      text: async () => '{"status":"UP"}',
    }),
  });

  // eslint-disable-next-line no-underscore-dangle
  const readinessTool = server._registeredTools?.['hdim_live_readiness'];
  assert.ok(readinessTool);
  const response = await readinessTool.handler({
    composeFile: 'docker-compose.demo.yml',
    gatewayUrl: 'http://localhost:18080',
    demoSeedingUrl: 'http://localhost:8098',
    tenantId: 'acme-health',
    includeLogs: true,
    runSystemValidate: true,
    requestTimeoutSecs: 30,
    skipFrontend: true,
    skipFhirQuery: true,
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.ready, true);
  assert.equal(payload.composePs.ok, true);
  assert.equal(payload.seedDiagnostics.health.status, 200);
  assert.equal(payload.seedDiagnostics.seedAttempt.status, 200);
  assert.equal(payload.systemValidate.ok, true);
  assert.equal(calls.length, 3);
  assert.deepEqual(calls[0].args, ['compose', '-f', 'docker-compose.demo.yml', 'ps']);
});

test('compose ps json parser handles line-delimited and array forms', () => {
  const lineDelimited = '{"Service":"gateway-edge","State":"running"}\n{"Service":"fhir-service","State":"running"}';
  const arrayJson = '[{"Service":"gateway-edge","State":"running"}]';

  assert.equal(parseComposePsJson(lineDelimited).length, 2);
  assert.equal(parseComposePsJson(arrayJson).length, 1);
});

test('extractComposeEnvVariables identifies required and defaulted vars', () => {
  const vars = extractComposeEnvVariables(`
    services:
      app:
        environment:
          REQUIRED: \${REQUIRED_TOKEN}
          OPTIONAL: \${OPTIONAL_TOKEN:-default}
  `);

  const required = vars.find((entry) => entry.name === 'REQUIRED_TOKEN');
  const optional = vars.find((entry) => entry.name === 'OPTIONAL_TOKEN');
  assert.equal(required?.required, true);
  assert.equal(optional?.required, false);
});

test('topology report aggregates compose and gateway checks', async () => {
  const server = createServer({
    runCommand: async (_command, args) => {
      if (args.includes('config')) {
        return {
          ok: true,
          exitCode: 0,
          stdout: 'gateway-edge\nfhir-service\n',
          stderr: '',
          timedOut: false,
        };
      }
      return {
        ok: true,
        exitCode: 0,
        stdout: '[{"Service":"gateway-edge","State":"running"}]',
        stderr: '',
        timedOut: false,
      };
    },
    fetchImpl: async () => ({ ok: true, status: 200 }),
  });

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_topology_report'];
  assert.ok(tool);
  const response = await tool.handler({
    composeFile: 'docker-compose.demo.yml',
    gatewayUrl: 'http://localhost:18080',
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.summary.serviceCount, 2);
  assert.equal(payload.summary.runningCount, 1);
  assert.equal(payload.summary.gatewayHealthy, true);
});

test('config audit reports missing required variables', async () => {
  const server = createServer();

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_config_audit'];
  assert.ok(tool);
  const response = await tool.handler({
    composeFile: 'docker-compose.demo.yml',
  });
  const payload = JSON.parse(response.content[0].text);

  assert.ok(payload.summary.variablesReferenced >= 1);
  assert.ok(Array.isArray(payload.missingRequired));
});

test('service catalog returns inventory with metadata and runtime states', async () => {
  const server = createServer({
    runCommand: async (_command, args) => {
      if (args.includes('config')) {
        return {
          ok: true,
          exitCode: 0,
          stdout: 'gateway-edge\nquality-measure-service\n',
          stderr: '',
          timedOut: false,
        };
      }
      return {
        ok: true,
        exitCode: 0,
        stdout: '[{"Service":"gateway-edge","Status":"Up 10 seconds"}]',
        stderr: '',
        timedOut: false,
      };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_service_catalog'];
  assert.ok(tool);
  const response = await tool.handler({ composeFile: 'docker-compose.demo.yml' });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.summary.totalServices, 2);
  assert.equal(payload.summary.runningServices, 1);
  assert.ok(Array.isArray(payload.catalog));
  assert.ok(payload.catalog.find((entry) => entry.service === 'gateway-edge')?.metadata);
});

test('service config contracts returns discovered variables and contracts', async () => {
  const server = createServer();

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_service_config_contracts'];
  assert.ok(tool);
  const response = await tool.handler({ composeFile: 'docker-compose.demo.yml' });
  const payload = JSON.parse(response.content[0].text);

  assert.ok(payload.summary.discoveredVariables >= 1);
  assert.ok(payload.summary.globalContracts >= 1);
  assert.ok(payload.contracts?.services?.['quality-measure-service']);
});

test('policy registry loads profiles from policy file', async () => {
  const server = createServer();

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_policy_registry'];
  assert.ok(tool);
  const response = await tool.handler({});
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.readError, null);
  assert.equal(payload.defaults.policyMode, 'strict');
  assert.ok(payload.profiles?.production);
});

test('service restart plan includes dependencies in order', async () => {
  const server = createServer();

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_service_restart_plan'];
  assert.ok(tool);
  const response = await tool.handler({
    services: ['gateway-edge'],
    includeDependencies: true,
  });
  const payload = JSON.parse(response.content[0].text);

  assert.ok(payload.restartOrder.includes('gateway-edge'));
  assert.ok(payload.restartOrder.includes('gateway-admin-service'));
  assert.ok(payload.restartOrder.includes('gateway-fhir-service'));
});

test('service operate dry-run returns command preview', async () => {
  const server = createServer();

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_service_operate'];
  assert.ok(tool);
  const response = await tool.handler({
    composeFile: 'docker-compose.demo.yml',
    action: 'restart',
    services: ['gateway-edge'],
    dryRun: true,
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.dryRun, true);
  assert.match(payload.commandPreview, /docker compose -f docker-compose\.demo\.yml restart gateway-edge/);
});

test('service operate executes compose action when dryRun=false', async () => {
  const calls = [];
  const server = createServer({
    runCommand: async (command, args) => {
      calls.push({ command, args });
      return { ok: true, exitCode: 0, stdout: 'ok', stderr: '', timedOut: false };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_service_operate'];
  assert.ok(tool);
  const response = await tool.handler({
    composeFile: 'docker-compose.demo.yml',
    action: 'stop',
    services: ['gateway-edge', 'gateway-edge'],
    dryRun: false,
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.dryRun, false);
  assert.equal(calls.length, 1);
  assert.deepEqual(calls[0].args, ['compose', '-f', 'docker-compose.demo.yml', 'stop', 'gateway-edge']);
});

test('tenant isolation check evaluates header enforcement heuristics', async () => {
  const server = createServer({
    fetchImpl: async (_url, init) => {
      const tenant = init?.headers?.['x-tenant-id'];
      if (!tenant) {
        return { ok: false, status: 403, text: async () => '{"error":"missing tenant"}' };
      }
      if (tenant === 'acme-health') {
        return { ok: true, status: 200, text: async () => '{"tenant":"acme-health"}' };
      }
      return { ok: true, status: 200, text: async () => '{"tenant":"beta-health"}' };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_tenant_isolation_check'];
  assert.ok(tool);
  const response = await tool.handler({
    gatewayUrl: 'http://localhost:18080',
    tenantA: 'acme-health',
    tenantB: 'beta-health',
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.summary.totalEndpoints, 2);
  assert.equal(payload.summary.headerEnforcedCount, 2);
  assert.equal(payload.summary.tenantDifferentiationCount, 2);
  assert.equal(payload.policy.pass, true);
});

test('release gate passes with healthy mocked dependencies', async () => {
  const server = createServer({
    runCommand: async (_command, args) => {
      if (args.includes('ps')) {
        return {
          ok: true,
          exitCode: 0,
          stdout: '[{"Service":"gateway-edge","State":"running"}]',
          stderr: '',
          timedOut: false,
        };
      }
      return {
        ok: true,
        exitCode: 0,
        stdout: '',
        stderr: '',
        timedOut: false,
      };
    },
    fetchImpl: async (_url, init) => {
      if (_url.includes('/actuator/health') || _url.includes('/fhir/metadata')) {
        return { ok: true, status: 200, text: async () => '' };
      }
      if (_url.includes('8098')) {
        return { ok: true, status: 200, text: async () => '' };
      }
      const tenant = init?.headers?.['x-tenant-id'];
      if (tenant === undefined) return { ok: false, status: 403, text: async () => '' };
      if (tenant === 'beta-health') return { ok: false, status: 403, text: async () => '' };
      return { ok: true, status: 200, text: async () => '' };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_release_gate'];
  assert.ok(tool);
  const response = await tool.handler({
    composeFile: 'docker-compose.mcp.yml',
    gatewayUrl: 'http://localhost:18080',
    demoSeedingUrl: 'http://localhost:8098',
    tenantA: 'acme-health',
    tenantB: 'beta-health',
    includeLogs: false,
    runSystemValidate: false,
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.pass, true);
  assert.equal(payload.summary.runningServices, 1);
  assert.equal(payload.summary.tenantPolicyPass, true);
  assert.equal(payload.summary.readiness, true);
});

test('release gate permissive mode warns but can pass', async () => {
  const server = createServer({
    runCommand: async (_command, args) => {
      if (args.includes('ps')) {
        return {
          ok: true,
          exitCode: 0,
          stdout: '[{"Service":"gateway-edge","State":"running"}]',
          stderr: '',
          timedOut: false,
        };
      }
      return {
        ok: true,
        exitCode: 0,
        stdout: '',
        stderr: '',
        timedOut: false,
      };
    },
    fetchImpl: async (_url, init) => {
      if (_url.includes('/actuator/health') || _url.includes('/fhir/metadata') || _url.includes('8098')) {
        return { ok: true, status: 200, text: async () => '' };
      }
      const tenant = init?.headers?.['x-tenant-id'];
      if (tenant === undefined) return { ok: true, status: 200, text: async () => '' };
      if (tenant === 'acme-health') return { ok: true, status: 200, text: async () => '' };
      return { ok: false, status: 403, text: async () => '' };
    },
  });

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_release_gate'];
  assert.ok(tool);
  const response = await tool.handler({
    composeFile: 'docker-compose.mcp.yml',
    gatewayUrl: 'http://localhost:18080',
    demoSeedingUrl: 'http://localhost:8098',
    tenantA: 'acme-health',
    tenantB: 'beta-health',
    policyMode: 'permissive',
    includeLogs: false,
    runSystemValidate: false,
  });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.pass, true);
  assert.equal(payload.tenantIsolation.pass, true);
  assert.ok(payload.tenantIsolation.warnings.length >= 1);
});

test('release artifact diff returns regression delta from latest two reports', async () => {
  const server = createServer();
  const reportDir = `/tmp/mcp-diff-${Date.now()}`;
  await import('node:fs/promises').then((fs) => fs.mkdir(reportDir, { recursive: true }));
  await import('node:fs/promises').then((fs) =>
    fs.writeFile(
      `${reportDir}/release-gate-20260216-120000.json`,
      JSON.stringify({
        pass: true,
        summary: { readiness: true, runningServices: 20 },
        tenantIsolation: { warnings: [], violations: [] },
      }),
    ),
  );
  await import('node:fs/promises').then((fs) =>
    fs.writeFile(
      `${reportDir}/release-gate-20260216-120100.json`,
      JSON.stringify({
        pass: false,
        summary: { readiness: true, runningServices: 19 },
        tenantIsolation: { warnings: [{ endpoint: '/x' }], violations: [{ endpoint: '/y' }] },
      }),
    ),
  );

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_release_artifact_diff'];
  assert.ok(tool);
  const response = await tool.handler({ reportDir });
  const payload = JSON.parse(response.content[0].text);

  assert.equal(payload.diff.passChanged, true);
  assert.equal(payload.diff.runningServicesDelta, -1);
  assert.equal(payload.diff.warningCountDelta, 1);
  assert.equal(payload.diff.violationCountDelta, 1);
  assert.equal(payload.diff.hasRegression, true);
});

test('release evidence pack writes json and markdown artifacts', async () => {
  const server = createServer();
  const base = `/tmp/mcp-pack-${Date.now()}`;
  const reportDir = `${base}/reports`;
  const outputDir = `${base}/out`;
  const fs = await import('node:fs/promises');
  await fs.mkdir(reportDir, { recursive: true });
  await fs.writeFile(
    `${reportDir}/release-gate-20260216-130000.json`,
    JSON.stringify({
      pass: true,
      summary: { readiness: true, runningServices: 20 },
      tenantIsolation: { policyMode: 'permissive', pass: true, warnings: [], violations: [] },
    }),
  );

  // eslint-disable-next-line no-underscore-dangle
  const tool = server._registeredTools?.['hdim_release_evidence_pack'];
  assert.ok(tool);
  const response = await tool.handler({ reportDir, outputDir, includeDiff: false });
  const payload = JSON.parse(response.content[0].text);

  assert.ok(payload.evidencePackJson.includes('release-evidence-pack-'));
  assert.ok(payload.evidencePackMarkdown.includes('release-evidence-pack-'));
});

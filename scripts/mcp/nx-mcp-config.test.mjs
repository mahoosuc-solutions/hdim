import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(dirname, '..', '..');

test('.mcp.json pins nx-mcp and uses wrapper script', () => {
  const mcpConfigPath = path.join(repoRoot, '.mcp.json');
  const config = JSON.parse(readFileSync(mcpConfigPath, 'utf8'));

  assert.equal(config.mcpServers?.['nx-mcp']?.type, 'stdio');
  assert.equal(config.mcpServers?.['nx-mcp']?.command, 'node');
  assert.deepEqual(config.mcpServers?.['nx-mcp']?.args, ['scripts/mcp/nx-mcp.mjs']);

  assert.equal(config.mcpServers?.['hdim-platform']?.type, 'stdio');
  assert.equal(config.mcpServers?.['hdim-platform']?.command, 'node');
  assert.deepEqual(config.mcpServers?.['hdim-platform']?.args, ['scripts/mcp/hdim-platform-mcp.mjs']);

  assert.equal(config.mcpServers?.['MCP_DOCKER']?.type, 'stdio');
  assert.equal(config.mcpServers?.['MCP_DOCKER']?.command, 'node');
  assert.deepEqual(config.mcpServers?.['MCP_DOCKER']?.args, ['scripts/mcp/hdim-docker-mcp.mjs']);

  const packageJsonPath = path.join(repoRoot, 'package.json');
  const pkg = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
  assert.equal(pkg.devDependencies?.['nx-mcp'], '0.21.0');
  assert.equal(pkg.scripts?.['mcp:release-gate'], 'node scripts/mcp/context-aware-release-gate.mjs');
  assert.equal(pkg.scripts?.['mcp:evidence-pack'], 'node scripts/mcp/build-release-evidence-pack.mjs');
  assert.equal(
    pkg.scripts?.['mcp:evidence-package'],
    'node scripts/mcp/package-deployment-evidence.mjs',
  );
  assert.equal(
    pkg.scripts?.['mcp:controlled-restart'],
    'node scripts/mcp/controlled-restart-smoke.mjs',
  );
  assert.equal(
    pkg.scripts?.['mcp:operator:go-no-go'],
    'node scripts/mcp/operator-go-no-go.mjs',
  );
  assert.equal(
    pkg.scripts?.['mcp:spec:check'],
    'node scripts/mcp/sync-reference-spec-examples.mjs --check',
  );
  assert.equal(
    pkg.scripts?.['mcp:spec:sync'],
    'node scripts/mcp/sync-reference-spec-examples.mjs --write',
  );
  assert.equal(pkg.scripts?.['mcp:pretest'], 'bash scripts/mcp/pre-test-checklist.sh');

  const wrapperPath = path.join(repoRoot, 'scripts', 'mcp', 'nx-mcp.mjs');
  const wrapper = readFileSync(wrapperPath, 'utf8');
  assert.match(wrapper, /--transport/);
  assert.match(wrapper, /stdio/);
  assert.match(wrapper, /--disableTelemetry/);
});

test('workspace and VS Code MCP configs point MCP_DOCKER at HDIM docker server', () => {
  const workspaceMcpPath = path.join(repoRoot, 'mcp.json');
  const workspaceMcp = JSON.parse(readFileSync(workspaceMcpPath, 'utf8'));
  const workspaceArgs =
    workspaceMcp.mcpServers?.['MCP_DOCKER']?.args ??
    workspaceMcp.servers?.['MCP_DOCKER']?.args;
  assert.deepEqual(workspaceArgs, ['scripts/mcp/hdim-docker-mcp.mjs']);

  const vscodeMcpPath = path.join(repoRoot, '.vscode', 'mcp.json');
  const vscodeMcp = JSON.parse(readFileSync(vscodeMcpPath, 'utf8'));
  const vscodeArgs =
    vscodeMcp.mcpServers?.['MCP_DOCKER']?.args ??
    vscodeMcp.servers?.['MCP_DOCKER']?.args;
  assert.deepEqual(vscodeArgs, ['scripts/mcp/hdim-docker-mcp.mjs']);
});

test('docker MCP host compose file exists', () => {
  const composePath = path.join(repoRoot, 'docker-compose.mcp.yml');
  const compose = readFileSync(composePath, 'utf8');
  assert.match(compose, /container_name:\s*hdim-nx-mcp/);
  assert.match(compose, /hdim_nx_mcp_node_modules/);
});

test('docker MCP Toolkit smoke scripts exist', () => {
  const ps1Path = path.join(repoRoot, 'scripts', 'mcp', 'docker-toolkit-smoke.ps1');
  const ps1 = readFileSync(ps1Path, 'utf8');
  assert.match(ps1, /docker mcp gateway run/);
  assert.match(ps1, /--dry-run/);

  const shPath = path.join(repoRoot, 'scripts', 'mcp', 'docker-toolkit-smoke.sh');
  const sh = readFileSync(shPath, 'utf8');
  assert.match(sh, /docker mcp gateway run/);
  assert.match(sh, /--dry-run/);
});

test('HDIM Toolkit gateway scripts exist', () => {
  const ps1Path = path.join(repoRoot, 'scripts', 'mcp', 'docker-toolkit-hdim-platform.ps1');
  const ps1 = readFileSync(ps1Path, 'utf8');
  assert.match(ps1, /"--servers", "hdim-platform"/);
  assert.match(ps1, /mcp", "gateway", "run"|docker mcp gateway run/);

  const shPath = path.join(repoRoot, 'scripts', 'mcp', 'docker-toolkit-hdim-platform.sh');
  const sh = readFileSync(shPath, 'utf8');
  assert.match(sh, /--servers hdim-platform/);
  assert.match(sh, /mcp gateway run/);
  assert.match(sh, /toolkit_bin=/);

  const dockerfilePath = path.join(repoRoot, 'docker', 'mcp', 'hdim-platform-mcp', 'Dockerfile');
  const dockerfile = readFileSync(dockerfilePath, 'utf8');
  assert.match(dockerfile, /hdim-platform-mcp\.mjs/);

  const dockerServerPath = path.join(repoRoot, 'scripts', 'mcp', 'hdim-docker-mcp.mjs');
  const dockerServer = readFileSync(dockerServerPath, 'utf8');
  assert.match(dockerServer, /hdim_docker_up/);
  assert.match(dockerServer, /hdim_docker_logs/);
  assert.match(dockerServer, /hdim_demo_seed/);
  assert.match(dockerServer, /hdim_system_validate/);
  assert.match(dockerServer, /hdim_live_readiness/);
  assert.match(dockerServer, /hdim_topology_report/);
  assert.match(dockerServer, /hdim_config_audit/);
  assert.match(dockerServer, /hdim_service_catalog/);
  assert.match(dockerServer, /hdim_service_config_contracts/);
  assert.match(dockerServer, /hdim_policy_registry/);
  assert.match(dockerServer, /hdim_service_restart_plan/);
  assert.match(dockerServer, /hdim_service_operate/);
  assert.match(dockerServer, /hdim_release_artifact_diff/);
  assert.match(dockerServer, /hdim_release_evidence_pack/);
  assert.match(dockerServer, /hdim_tenant_isolation_check/);
  assert.match(dockerServer, /hdim_release_gate/);

  const runnerPath = path.join(repoRoot, 'scripts', 'mcp', 'context-aware-release-gate.mjs');
  const runner = readFileSync(runnerPath, 'utf8');
  assert.match(runner, /--request-timeout/);

  const policyPath = path.join(repoRoot, 'scripts', 'mcp', 'release-gate-policy.json');
  const policy = JSON.parse(readFileSync(policyPath, 'utf8'));
  assert.equal(typeof policy.profiles?.['production']?.policyMode, 'string');

  const evidencePackRunnerPath = path.join(
    repoRoot,
    'scripts',
    'mcp',
    'build-release-evidence-pack.mjs',
  );
  const evidencePackRunner = readFileSync(evidencePackRunnerPath, 'utf8');
  assert.match(evidencePackRunner, /hdim_release_evidence_pack/);

  const evidencePackageRunnerPath = path.join(
    repoRoot,
    'scripts',
    'mcp',
    'package-deployment-evidence.mjs',
  );
  const evidencePackageRunner = readFileSync(evidencePackageRunnerPath, 'utf8');
  assert.match(evidencePackageRunner, /DEPLOYMENT_EVIDENCE_SUMMARY\.md/);
  assert.match(evidencePackageRunner, /release-gate-/);

  const controlledRestartRunnerPath = path.join(
    repoRoot,
    'scripts',
    'mcp',
    'controlled-restart-smoke.mjs',
  );
  const controlledRestartRunner = readFileSync(controlledRestartRunnerPath, 'utf8');
  assert.match(controlledRestartRunner, /hdim_service_operate/);
  assert.match(controlledRestartRunner, /warmupTimeoutSecs/);
  assert.match(controlledRestartRunner, /appendToLatestBundle/);

  const operatorRunnerPath = path.join(
    repoRoot,
    'scripts',
    'mcp',
    'operator-go-no-go.mjs',
  );
  const operatorRunner = readFileSync(operatorRunnerPath, 'utf8');
  assert.match(operatorRunner, /context-aware-release-gate\.mjs/);
  assert.match(operatorRunner, /controlled-restart-smoke\.mjs/);

  const referenceSpecPath = path.join(
    repoRoot,
    'docs',
    'runbooks',
    'MCP_IMPLEMENTATION_REFERENCE_SPEC.md',
  );
  const referenceSpec = readFileSync(referenceSpecPath, 'utf8');
  assert.match(referenceSpec, /MCP Implementation Reference Specification/);

  const referenceSchemaPath = path.join(
    repoRoot,
    'docs',
    'runbooks',
    'MCP_IMPLEMENTATION_REFERENCE_SPEC.schema.json',
  );
  const referenceSchema = JSON.parse(readFileSync(referenceSchemaPath, 'utf8'));
  assert.equal(referenceSchema.type, 'object');

  const referenceExamplesPath = path.join(
    repoRoot,
    'docs',
    'runbooks',
    'MCP_IMPLEMENTATION_REFERENCE_SPEC.examples.json',
  );
  const referenceExamples = JSON.parse(readFileSync(referenceExamplesPath, 'utf8'));
  assert.equal(typeof referenceExamples.examples, 'object');

  const referenceSyncScriptPath = path.join(
    repoRoot,
    'scripts',
    'mcp',
    'sync-reference-spec-examples.mjs',
  );
  const referenceSyncScript = readFileSync(referenceSyncScriptPath, 'utf8');
  assert.match(referenceSyncScript, /## 21\. Example Payload Appendix/);
  assert.match(referenceSyncScript, /--check/);
});

test('HDIM catalog install scripts and server definition exist', () => {
  const catalogYamlPath = path.join(
    repoRoot,
    'docker',
    'mcp',
    'catalogs',
    'hdim.yaml',
  );
  const catalogYaml = readFileSync(catalogYamlPath, 'utf8');
  assert.match(catalogYaml, /^name:\s*hdim/m);
  assert.match(catalogYaml, /^version:\s*3/m);
  assert.match(catalogYaml, /hdim-platform:/);
  assert.match(catalogYaml, /^\s*image:\s*hdim-platform-mcp:local/m);

  const ps1Path = path.join(repoRoot, 'scripts', 'mcp', 'docker-toolkit-install-hdim-catalog.ps1');
  const ps1 = readFileSync(ps1Path, 'utf8');
  assert.match(ps1, /docker mcp catalog add/);
  assert.match(ps1, /docker mcp server enable/);

  const shPath = path.join(repoRoot, 'scripts', 'mcp', 'docker-toolkit-install-hdim-catalog.sh');
  const sh = readFileSync(shPath, 'utf8');
  assert.match(sh, /mcp catalog add/);
  assert.match(sh, /mcp server enable/);
  assert.match(sh, /toolkit_bin=/);
});

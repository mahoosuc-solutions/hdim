const { runCommand } = require('../docker-client');

function createDefinition() {
  return {
    name: 'build_status',
    description: 'NX/Gradle build status — affected projects and build state',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      try {
        const result = await runCommand('npx', ['nx', 'show', 'projects', '--affected'], {
          cwd: process.cwd(),
          timeoutMs: 30_000
        });
        const projects = result.ok
          ? result.stdout.trim().split('\n').filter(Boolean)
          : [];
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              ok: result.ok,
              affectedProjects: projects,
              count: projects.length,
              stderr: result.stderr || null
            }, null, 2)
          }]
        };
      } catch (err) {
        return {
          content: [{ type: 'text', text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2) }]
        };
      }
    }
  };
}

module.exports = { createDefinition };

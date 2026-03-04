const SERVICE_NAME_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9_.-]*$/;

function createDefinition(dockerClient) {
  return {
    name: 'docker_restart',
    description: 'Restart a Docker compose service',
    inputSchema: {
      type: 'object',
      properties: {
        service: { type: 'string', description: 'Service name to restart (e.g. fhir-service, postgres)' }
      },
      required: ['service'],
      additionalProperties: false
    },
    handler: async (args) => {
      const { service } = args;
      const trimmed = String(service || '').trim();
      if (!trimmed) {
        return { content: [{ type: 'text', text: JSON.stringify({ error: 'service is required' }) }] };
      }
      if (!SERVICE_NAME_PATTERN.test(trimmed)) {
        return { content: [{ type: 'text', text: JSON.stringify({ error: 'Invalid service name: only alphanumeric, hyphens, dots, underscores allowed' }) }] };
      }
      try {
        const result = await dockerClient.restart(trimmed);
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({ ok: result.ok, service, stdout: result.stdout, stderr: result.stderr || null }, null, 2)
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

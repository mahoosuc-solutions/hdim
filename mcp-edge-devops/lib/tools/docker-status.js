function createDefinition(dockerClient) {
  return {
    name: 'docker_status',
    description: 'Docker compose service states (running/stopped/unhealthy)',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    handler: async () => {
      try {
        const result = await dockerClient.ps();
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({
              ok: result.ok,
              services: result.services,
              composeFile: dockerClient.composeFile
            }, null, 2)
          }]
        };
      } catch (err) {
        return {
          content: [{
            type: 'text',
            text: JSON.stringify({ ok: false, error: err?.message || String(err) }, null, 2)
          }]
        };
      }
    }
  };
}

module.exports = { createDefinition };

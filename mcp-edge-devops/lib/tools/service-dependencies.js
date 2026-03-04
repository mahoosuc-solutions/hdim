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
  'demo-seeding-service': ['postgres', 'kafka', 'redis']
};

const definition = {
  name: 'service_dependencies',
  description: 'HDIM service dependency graph — what depends on what',
  inputSchema: {
    type: 'object',
    properties: {
      service: { type: 'string', description: 'Get dependencies for a specific service' }
    },
    additionalProperties: false
  },
  handler: async (args) => {
    if (args?.service) {
      const deps = SERVICE_DEPENDENCIES[args.service];
      if (!deps) {
        return { content: [{ type: 'text', text: JSON.stringify({ error: `Unknown service: ${args.service}`, knownServices: Object.keys(SERVICE_DEPENDENCIES) }, null, 2) }] };
      }
      return { content: [{ type: 'text', text: JSON.stringify({ service: args.service, dependencies: deps }, null, 2) }] };
    }
    return { content: [{ type: 'text', text: JSON.stringify({ dependencies: SERVICE_DEPENDENCIES }, null, 2) }] };
  }
};

module.exports = { definition, SERVICE_DEPENDENCIES };

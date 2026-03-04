const SERVICE_CATALOG = [
  { name: 'gateway-edge', category: 'gateway', port: 18080, healthPath: '/actuator/health' },
  { name: 'fhir-service', category: 'clinical-core', port: 8084, healthPath: '/fhir/metadata' },
  { name: 'quality-measure-service', category: 'quality', port: 8085, healthPath: '/actuator/health' },
  { name: 'care-gap-service', category: 'quality', port: 8086, healthPath: '/actuator/health' },
  { name: 'cql-engine-service', category: 'quality', port: 8081, healthPath: '/actuator/health' },
  { name: 'demo-seeding-service', category: 'demo', port: 8098, healthPath: '/demo/actuator/health' },
  { name: 'audit-query-service', category: 'audit', port: 8089, healthPath: '/actuator/health' },
  { name: 'clinical-workflow-service', category: 'clinical', port: 8090, healthPath: '/actuator/health' },
  { name: 'postgres', category: 'data', port: 5435, healthPath: null },
  { name: 'redis', category: 'data', port: 6380, healthPath: null },
  { name: 'kafka', category: 'messaging', port: 9094, healthPath: null }
];

function createDefinition(platformClient) {
  return {
    name: 'service_catalog',
    description: 'List all HDIM microservices with health status and metadata',
    inputSchema: {
      type: 'object',
      properties: {
        category: { type: 'string', description: 'Filter by category: gateway, clinical-core, quality, data, messaging' }
      },
      additionalProperties: false
    },
    handler: async (args) => {
      let services = SERVICE_CATALOG;
      if (args?.category) {
        services = services.filter((s) => s.category === args.category);
      }
      const results = await Promise.all(
        services.map(async (svc) => {
          if (!svc.healthPath) return { ...svc, status: 'unknown', detail: 'no health endpoint' };
          try {
            const res = await platformClient.get(svc.healthPath);
            return { ...svc, status: res.ok ? 'healthy' : 'unhealthy', httpStatus: res.status };
          } catch (err) {
            return { ...svc, status: 'unreachable', error: err?.message || String(err) };
          }
        })
      );
      return { content: [{ type: 'text', text: JSON.stringify({ services: results, checkedAt: new Date().toISOString() }, null, 2) }] };
    }
  };
}
module.exports = { createDefinition, SERVICE_CATALOG };

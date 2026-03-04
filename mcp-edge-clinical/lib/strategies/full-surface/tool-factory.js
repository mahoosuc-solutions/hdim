// mcp-edge-clinical/lib/strategies/full-surface/tool-factory.js
// Dynamic tool factory — generates read/search/create MCP tool definitions
// from the FHIR resource registry. Produces 52 FHIR tools for 20 resource types.

/**
 * Convert CamelCase FHIR type to snake_case tool prefix.
 * e.g. 'MedicationRequest' -> 'medication_request'
 */
function toSnakeCase(type) {
  return type.replace(/([a-z])([A-Z])/g, '$1_$2').toLowerCase();
}

function createReadTool(resource, client) {
  const snake = toSnakeCase(resource.type);
  return {
    name: `${snake}_read`,
    description: `Read a FHIR ${resource.type} resource by ID.`,
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Resource ID (UUID)' },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['id', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      try {
        const res = await client.get(`/fhir/${resource.type}/${args.id}`, { tenantId: args.tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: resource.type, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: resource.type, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}

function createSearchTool(resource, client) {
  const snake = toSnakeCase(resource.type);
  return {
    name: `${snake}_search`,
    description: `Search FHIR ${resource.type} resources with query parameters.`,
    inputSchema: {
      type: 'object',
      properties: {
        tenantId: { type: 'string', description: 'Tenant identifier' },
        patient: { type: 'string', description: 'Patient ID for scoped search' },
        params: { type: 'object', description: 'Additional search params', additionalProperties: { type: 'string' } }
      },
      required: ['tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      const qp = new URLSearchParams();
      if (args.patient) qp.set('patient', args.patient);
      if (args.params) Object.entries(args.params).forEach(([k, v]) => qp.set(k, v));
      const qs = qp.toString();
      try {
        const res = await client.get(`/fhir/${resource.type}${qs ? '?' + qs : ''}`, { tenantId: args.tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: resource.type, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: resource.type, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}

function createCreateTool(resource, client) {
  const snake = toSnakeCase(resource.type);
  return {
    name: `${snake}_create`,
    description: `Create a new FHIR ${resource.type} resource.`,
    inputSchema: {
      type: 'object',
      properties: {
        resource: { type: 'object', description: `FHIR ${resource.type} JSON` },
        tenantId: { type: 'string', description: 'Tenant identifier' }
      },
      required: ['resource', 'tenantId'],
      additionalProperties: false
    },
    handler: async (args) => {
      try {
        const res = await client.post(`/fhir/${resource.type}`, args.resource, { tenantId: args.tenantId });
        return { content: [{ type: 'text', text: JSON.stringify({ status: res.status, ok: res.ok, resourceType: resource.type, data: res.body }, null, 2) }] };
      } catch (err) {
        return { content: [{ type: 'text', text: JSON.stringify({ ok: false, resourceType: resource.type, error: err?.message || String(err) }, null, 2) }] };
      }
    }
  };
}

/**
 * Generate all FHIR tool definitions from the registry.
 * Every resource gets a read tool; searchable ones get search; creatable ones get create.
 */
function generateFhirTools(registry, client) {
  const tools = [];
  for (const resource of registry) {
    tools.push(createReadTool(resource, client));
    if (resource.searchable) tools.push(createSearchTool(resource, client));
    if (resource.creatable) tools.push(createCreateTool(resource, client));
  }
  return tools;
}

module.exports = { generateFhirTools, createReadTool, createSearchTool, createCreateTool, toSnakeCase };

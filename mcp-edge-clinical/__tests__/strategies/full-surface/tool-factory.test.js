const { toSnakeCase, createReadTool, createSearchTool, createCreateTool, generateFhirTools } = require('../../../lib/strategies/full-surface/tool-factory');
const { RESOURCE_REGISTRY } = require('../../../lib/strategies/full-surface/resource-registry');

const mockClient = {
  get: jest.fn().mockResolvedValue({ status: 200, ok: true, body: {} }),
  post: jest.fn().mockResolvedValue({ status: 201, ok: true, body: {} })
};

describe('full-surface tool-factory', () => {
  describe('toSnakeCase', () => {
    it.each([
      ['Patient', 'patient'],
      ['Observation', 'observation'],
      ['MedicationRequest', 'medication_request'],
      ['AllergyIntolerance', 'allergy_intolerance'],
      ['MedicationAdministration', 'medication_administration'],
      ['DiagnosticReport', 'diagnostic_report'],
      ['DocumentReference', 'document_reference'],
      ['CarePlan', 'care_plan'],
      ['PractitionerRole', 'practitioner_role'],
    ])('%s -> %s', (input, expected) => {
      expect(toSnakeCase(input)).toBe(expected);
    });
  });

  describe('createReadTool', () => {
    const resource = { type: 'Patient', searchable: true, creatable: true };
    const tool = createReadTool(resource, mockClient);

    it('generates correct name', () => {
      expect(tool.name).toBe('patient_read');
    });

    it('has required inputSchema fields', () => {
      expect(tool.inputSchema.required).toEqual(['id', 'tenantId']);
      expect(tool.inputSchema.properties.id).toBeDefined();
      expect(tool.inputSchema.properties.tenantId).toBeDefined();
    });

    it('handler calls client.get', async () => {
      mockClient.get.mockResolvedValueOnce({ status: 200, ok: true, body: { id: '1' } });
      const result = await tool.handler({ id: '1', tenantId: 'acme' });
      expect(mockClient.get).toHaveBeenCalledWith('/fhir/Patient/1', { tenantId: 'acme' });
      const parsed = JSON.parse(result.content[0].text);
      expect(parsed.ok).toBe(true);
    });

    it('handler returns error on failure', async () => {
      mockClient.get.mockRejectedValueOnce(new Error('Network error'));
      const result = await tool.handler({ id: '1', tenantId: 'acme' });
      const parsed = JSON.parse(result.content[0].text);
      expect(parsed.ok).toBe(false);
      expect(parsed.error).toBe('Network error');
    });
  });

  describe('createSearchTool', () => {
    const resource = { type: 'Observation', searchable: true, creatable: true };
    const tool = createSearchTool(resource, mockClient);

    it('generates correct name', () => {
      expect(tool.name).toBe('observation_search');
    });

    it('requires only tenantId', () => {
      expect(tool.inputSchema.required).toEqual(['tenantId']);
    });

    it('handler builds query string', async () => {
      mockClient.get.mockResolvedValueOnce({ status: 200, ok: true, body: {} });
      await tool.handler({ tenantId: 'acme', patient: 'p1', params: { code: '1234' } });
      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/fhir/Observation?'),
        { tenantId: 'acme' }
      );
    });
  });

  describe('createCreateTool', () => {
    const resource = { type: 'Condition', searchable: true, creatable: true };
    const tool = createCreateTool(resource, mockClient);

    it('generates correct name', () => {
      expect(tool.name).toBe('condition_create');
    });

    it('requires resource and tenantId', () => {
      expect(tool.inputSchema.required).toEqual(['resource', 'tenantId']);
    });

    it('handler calls client.post', async () => {
      mockClient.post.mockResolvedValueOnce({ status: 201, ok: true, body: { id: 'new' } });
      const payload = { resourceType: 'Condition', code: {} };
      await tool.handler({ resource: payload, tenantId: 'acme' });
      expect(mockClient.post).toHaveBeenCalledWith('/fhir/Condition', payload, { tenantId: 'acme' });
    });
  });

  describe('generateFhirTools', () => {
    const tools = generateFhirTools(RESOURCE_REGISTRY, mockClient);

    it('returns 52 FHIR tools (20 reads + 19 searches + 13 creates)', () => {
      expect(tools).toHaveLength(52);
    });

    it('all tools have name, description, inputSchema, handler', () => {
      for (const tool of tools) {
        expect(typeof tool.name).toBe('string');
        expect(typeof tool.description).toBe('string');
        expect(tool.inputSchema).toBeDefined();
        expect(typeof tool.handler).toBe('function');
      }
    });

    it('tool names are unique', () => {
      const names = tools.map(t => t.name);
      expect(new Set(names).size).toBe(names.length);
    });

    it('all tool names follow snake_case_{read|search|create} pattern', () => {
      for (const tool of tools) {
        expect(tool.name).toMatch(/^[a-z_]+_(read|search|create)$/);
      }
    });
  });
});

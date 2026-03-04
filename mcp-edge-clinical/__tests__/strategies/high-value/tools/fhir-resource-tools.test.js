/**
 * Tests for all 10 FHIR resource-specific tools in the high-value strategy.
 * Covers: patient, observation, condition, medication (MedicationRequest), encounter
 * Each resource type has a _read and _search tool.
 */

const RESOURCE_TOOLS = [
  { file: 'patient-read', name: 'patient_read', resourceType: 'Patient', mode: 'read' },
  { file: 'patient-search', name: 'patient_search', resourceType: 'Patient', mode: 'search' },
  { file: 'observation-read', name: 'observation_read', resourceType: 'Observation', mode: 'read' },
  { file: 'observation-search', name: 'observation_search', resourceType: 'Observation', mode: 'search' },
  { file: 'condition-read', name: 'condition_read', resourceType: 'Condition', mode: 'read' },
  { file: 'condition-search', name: 'condition_search', resourceType: 'Condition', mode: 'search' },
  { file: 'medication-read', name: 'medication_read', resourceType: 'MedicationRequest', mode: 'read' },
  { file: 'medication-search', name: 'medication_search', resourceType: 'MedicationRequest', mode: 'search' },
  { file: 'encounter-read', name: 'encounter_read', resourceType: 'Encounter', mode: 'read' },
  { file: 'encounter-search', name: 'encounter_search', resourceType: 'Encounter', mode: 'search' },
];

describe.each(RESOURCE_TOOLS)('$name tool', ({ file, name, resourceType, mode }) => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    const { createDefinition } = require(`../../../../lib/strategies/high-value/tools/${file}`);
    definition = createDefinition(mockClient);
  });

  it(`has correct name "${name}"`, () => {
    expect(definition.name).toBe(name);
  });

  it('has a non-empty description', () => {
    expect(typeof definition.description).toBe('string');
    expect(definition.description.length).toBeGreaterThan(10);
  });

  it('requires tenantId', () => {
    expect(definition.inputSchema.required).toContain('tenantId');
  });

  if (mode === 'read') {
    it('requires id and tenantId', () => {
      expect(definition.inputSchema.required).toEqual(['id', 'tenantId']);
    });

    it(`calls GET /fhir/${resourceType}/{id} with tenantId`, async () => {
      mockClient.get.mockResolvedValue({ status: 200, ok: true, body: `{"resourceType":"${resourceType}"}` });

      const result = await definition.handler({ id: 'test-id-1', tenantId: 'acme-health' });
      expect(mockClient.get).toHaveBeenCalledWith(`/fhir/${resourceType}/test-id-1`, { tenantId: 'acme-health' });

      const parsed = JSON.parse(result.content[0].text);
      expect(parsed.status).toBe(200);
      expect(parsed.ok).toBe(true);
      expect(parsed.resourceType).toBe(resourceType);
    });

    it('returns error without leaking the resource ID on failure', async () => {
      mockClient.get.mockRejectedValue(new Error('Connection refused'));

      const result = await definition.handler({ id: 'secret-id-123', tenantId: 't1' });
      const parsed = JSON.parse(result.content[0].text);

      expect(parsed.ok).toBe(false);
      expect(parsed.error).toBe('Connection refused');
      expect(parsed.resourceType).toBe(resourceType);
      expect(parsed.data).toBeUndefined();
      expect(result.content[0].text).not.toContain('secret-id-123');
    });
  }

  if (mode === 'search') {
    it('requires only tenantId', () => {
      expect(definition.inputSchema.required).toEqual(['tenantId']);
    });

    it(`calls GET /fhir/${resourceType} with no params when none provided`, async () => {
      mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"resourceType":"Bundle"}' });

      await definition.handler({ tenantId: 'acme-health' });
      expect(mockClient.get).toHaveBeenCalledWith(`/fhir/${resourceType}`, { tenantId: 'acme-health' });
    });

    it('forwards patient param into query string', async () => {
      mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"resourceType":"Bundle"}' });

      await definition.handler({ tenantId: 't1', patient: 'p1' });
      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining(`/fhir/${resourceType}?patient=p1`),
        { tenantId: 't1' }
      );
    });

    it('forwards additional params into query string', async () => {
      mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"resourceType":"Bundle"}' });

      await definition.handler({ tenantId: 't1', params: { _count: '10', status: 'active' } });
      const call = mockClient.get.mock.calls[0][0];
      expect(call).toContain('_count=10');
      expect(call).toContain('status=active');
    });

    it('returns resourceType in response', async () => {
      mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });

      const result = await definition.handler({ tenantId: 't1' });
      const parsed = JSON.parse(result.content[0].text);
      expect(parsed.resourceType).toBe(resourceType);
    });

    it('returns error on failure', async () => {
      mockClient.get.mockRejectedValue(new Error('Timeout'));

      const result = await definition.handler({ tenantId: 't1' });
      const parsed = JSON.parse(result.content[0].text);
      expect(parsed.ok).toBe(false);
      expect(parsed.error).toBe('Timeout');
      expect(parsed.resourceType).toBe(resourceType);
    });
  }

  it('returns valid MCP content format', async () => {
    if (mode === 'read') {
      mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
      const result = await definition.handler({ id: 'x', tenantId: 't1' });
      expect(result.content).toHaveLength(1);
      expect(result.content[0].type).toBe('text');
      expect(() => JSON.parse(result.content[0].text)).not.toThrow();
    } else {
      mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
      const result = await definition.handler({ tenantId: 't1' });
      expect(result.content).toHaveLength(1);
      expect(result.content[0].type).toBe('text');
      expect(() => JSON.parse(result.content[0].text)).not.toThrow();
    }
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue('string error');

    const args = mode === 'read' ? { id: 'x', tenantId: 't1' } : { tenantId: 't1' };
    const result = await definition.handler(args);
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string error');
  });
});

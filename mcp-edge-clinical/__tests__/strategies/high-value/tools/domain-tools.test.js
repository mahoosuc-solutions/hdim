/**
 * Tests for the 5 domain tools in the high-value strategy:
 * care_gap_list, care_gap_close, care_gap_stats, measure_evaluate, measure_results
 */

describe('care_gap_list tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    const { createDefinition } = require('../../../../lib/strategies/high-value/tools/care-gap-list');
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('care_gap_list');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('calls /care-gap/open by default', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });
    await definition.handler({ patientId: 'p1', tenantId: 'acme' });
    expect(mockClient.get).toHaveBeenCalledWith('/care-gap/open?patient=p1', { tenantId: 'acme' });
  });

  it('maps status to correct endpoint', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });
    await definition.handler({ patientId: 'p1', tenantId: 't1', status: 'high-priority' });
    expect(mockClient.get).toHaveBeenCalledWith('/care-gap/high-priority?patient=p1', { tenantId: 't1' });
  });

  it('returns error without leaking PHI', async () => {
    mockClient.get.mockRejectedValue(new Error('fail'));
    const result = await definition.handler({ patientId: 'secret-id', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(result.content[0].text).not.toContain('secret-id');
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue('string-error');
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('string-error');
  });

  it('falls back to /care-gap/open for unknown status values', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '[]' });
    await definition.handler({ patientId: 'p1', tenantId: 't1', status: 'unknown-status' });
    expect(mockClient.get).toHaveBeenCalledWith('/care-gap/open?patient=p1', { tenantId: 't1' });
  });
});

describe('care_gap_close tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    const { createDefinition } = require('../../../../lib/strategies/high-value/tools/care-gap-close');
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('care_gap_close');
  });

  it('requires gapId, tenantId, closedBy, reason', () => {
    expect(definition.inputSchema.required).toEqual(['gapId', 'tenantId', 'closedBy', 'reason']);
  });

  it('calls POST /care-gap/close with body', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    await definition.handler({ gapId: 'g1', tenantId: 'acme', closedBy: 'dr-smith', reason: 'resolved' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/care-gap/close',
      { careGapId: 'g1', closedBy: 'dr-smith', closureReason: 'resolved', tenantId: 'acme' },
      { tenantId: 'acme' }
    );
  });

  it('returns error on failure', async () => {
    mockClient.post.mockRejectedValue(new Error('forbidden'));
    const result = await definition.handler({ gapId: 'g1', tenantId: 't1', closedBy: 'x', reason: 'y' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('forbidden');
  });

  it('handles non-Error throw values', async () => {
    mockClient.post.mockRejectedValue(null);
    const result = await definition.handler({ gapId: 'g1', tenantId: 't1', closedBy: 'x', reason: 'y' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('null');
  });
});

describe('care_gap_stats tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    const { createDefinition } = require('../../../../lib/strategies/high-value/tools/care-gap-stats');
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('care_gap_stats');
  });

  it('requires only tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['tenantId']);
  });

  it('calls GET /care-gap/stats', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    await definition.handler({ tenantId: 'acme' });
    expect(mockClient.get).toHaveBeenCalledWith('/care-gap/stats', { tenantId: 'acme' });
  });

  it('returns valid MCP content', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{"total":5}' });
    const result = await definition.handler({ tenantId: 't1' });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].type).toBe('text');
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(true);
  });

  it('returns error on failure', async () => {
    mockClient.get.mockRejectedValue(new Error('service unavailable'));
    const result = await definition.handler({ tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('service unavailable');
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue(undefined);
    const result = await definition.handler({ tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('undefined');
  });
});

describe('measure_evaluate tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    const { createDefinition } = require('../../../../lib/strategies/high-value/tools/measure-evaluate');
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('measure_evaluate');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('calls POST /quality-measure/calculate', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    await definition.handler({ patientId: 'p1', tenantId: 'acme' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/quality-measure/calculate',
      { patientId: 'p1' },
      { tenantId: 'acme' }
    );
  });

  it('includes optional measureId when provided', async () => {
    mockClient.post.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    await definition.handler({ patientId: 'p1', tenantId: 'acme', measureId: 'HbA1c' });
    expect(mockClient.post).toHaveBeenCalledWith(
      '/quality-measure/calculate',
      { patientId: 'p1', measureId: 'HbA1c' },
      { tenantId: 'acme' }
    );
  });

  it('returns error on failure', async () => {
    mockClient.post.mockRejectedValue(new Error('timeout'));
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
  });

  it('handles non-Error throw values', async () => {
    mockClient.post.mockRejectedValue(42);
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('42');
  });
});

describe('measure_results tool', () => {
  let mockClient, definition;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    const { createDefinition } = require('../../../../lib/strategies/high-value/tools/measure-results');
    definition = createDefinition(mockClient);
  });

  it('has correct name', () => {
    expect(definition.name).toBe('measure_results');
  });

  it('requires patientId and tenantId', () => {
    expect(definition.inputSchema.required).toEqual(['patientId', 'tenantId']);
  });

  it('calls GET /quality-measure/results?patient=...', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    await definition.handler({ patientId: 'p1', tenantId: 'acme' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/quality-measure/results?patient=p1',
      { tenantId: 'acme' }
    );
  });

  it('encodes patientId in URL', async () => {
    mockClient.get.mockResolvedValue({ status: 200, ok: true, body: '{}' });
    await definition.handler({ patientId: 'id with spaces', tenantId: 't1' });
    expect(mockClient.get).toHaveBeenCalledWith(
      '/quality-measure/results?patient=id%20with%20spaces',
      { tenantId: 't1' }
    );
  });

  it('returns error on failure', async () => {
    mockClient.get.mockRejectedValue(new Error('not found'));
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('not found');
  });

  it('handles non-Error throw values', async () => {
    mockClient.get.mockRejectedValue(false);
    const result = await definition.handler({ patientId: 'p1', tenantId: 't1' });
    const parsed = JSON.parse(result.content[0].text);
    expect(parsed.ok).toBe(false);
    expect(parsed.error).toBe('false');
  });
});

const { validateToolParams } = require('../lib/param-validator');

describe('validateToolParams', () => {
  const schema = {
    type: 'object',
    properties: {
      service: { type: 'string', maxLength: 100 },
      tail: { type: 'integer', minimum: 1, maximum: 10000 }
    },
    required: ['service'],
    additionalProperties: false
  };

  it('returns null for valid params', () => {
    expect(validateToolParams(schema, { service: 'gateway', tail: 50 })).toBeNull();
  });

  it('returns null when only required params provided', () => {
    expect(validateToolParams(schema, { service: 'gateway' })).toBeNull();
  });

  it('returns error string for missing required field', () => {
    const err = validateToolParams(schema, {});
    expect(err).not.toBeNull();
    expect(typeof err).toBe('string');
  });

  it('returns error for extra properties when additionalProperties is false', () => {
    const err = validateToolParams(schema, { service: 'gw', evil: 'code' });
    expect(err).not.toBeNull();
  });

  it('returns error for type mismatch', () => {
    const err = validateToolParams(schema, { service: 123 });
    expect(err).not.toBeNull();
  });

  it('returns error for value out of range', () => {
    const err = validateToolParams(schema, { service: 'gw', tail: 0 });
    expect(err).not.toBeNull();
  });

  it('returns error for value over max', () => {
    const err = validateToolParams(schema, { service: 'gw', tail: 99999 });
    expect(err).not.toBeNull();
  });

  it('returns null for empty/no-properties schema', () => {
    const emptySchema = { type: 'object', properties: {} };
    expect(validateToolParams(emptySchema, {})).toBeNull();
  });

  it('returns null when schema is null', () => {
    expect(validateToolParams(null, {})).toBeNull();
  });

  it('returns null when schema has no properties key', () => {
    expect(validateToolParams({ type: 'object' }, {})).toBeNull();
  });

  it('handles null params (treats as empty object)', () => {
    const err = validateToolParams(schema, null);
    expect(err).not.toBeNull(); // required 'service' missing
  });

  it('handles undefined params (treats as empty object)', () => {
    const err = validateToolParams(schema, undefined);
    expect(err).not.toBeNull(); // required 'service' missing
  });
});

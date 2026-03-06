const { createAuditLogger, scrubSensitive, withTraceContext } = require('../lib/audit-log');
const { Writable } = require('node:stream');

describe('createAuditLogger', () => {
  it('returns a pino logger instance with info and error methods', () => {
    const logger = createAuditLogger({ serviceName: 'test' });
    expect(typeof logger.info).toBe('function');
    expect(typeof logger.error).toBe('function');
    expect(typeof logger.warn).toBe('function');
  });

  it('creates logger without stream and with default serviceName', () => {
    const logger = createAuditLogger();
    expect(logger).toBeDefined();
    expect(typeof logger.info).toBe('function');
  });

  it('logs to provided writable stream with structured output', () => {
    const chunks = [];
    const stream = new Writable({
      write(chunk, enc, cb) { chunks.push(chunk.toString()); cb(); }
    });
    const logger = createAuditLogger({ serviceName: 'test-svc', stream });
    logger.info({ tool: 'edge_health', role: 'admin' }, 'tool_call');
    expect(chunks.length).toBe(1);
    const log = JSON.parse(chunks[0]);
    expect(log.tool).toBe('edge_health');
    expect(log.role).toBe('admin');
    expect(log.service).toBe('test-svc');
    expect(log.level).toBe('info');
    expect(log.msg).toBe('tool_call');
  });

  it('respects LOG_LEVEL env var', () => {
    process.env.LOG_LEVEL = 'warn';
    const chunks = [];
    const stream = new Writable({
      write(chunk, enc, cb) { chunks.push(chunk.toString()); cb(); }
    });
    const logger = createAuditLogger({ serviceName: 'test', stream });
    logger.info('should be suppressed');
    logger.warn('should appear');
    expect(chunks.length).toBe(1);
    expect(JSON.parse(chunks[0]).level).toBe('warn');
    delete process.env.LOG_LEVEL;
  });
});

describe('scrubSensitive', () => {
  it('redacts Bearer tokens from strings', () => {
    expect(scrubSensitive('Bearer abc123xyz')).toBe('Bearer [REDACTED]');
  });

  it('redacts multiple Bearer tokens', () => {
    const input = 'Token: Bearer key1, also Bearer key2';
    const result = scrubSensitive(input);
    expect(result).not.toContain('key1');
    expect(result).not.toContain('key2');
  });

  it('redacts sensitive keys from objects', () => {
    const obj = { patient_id: '12345', ssn: '999-99-9999', status: 'ok' };
    const scrubbed = scrubSensitive(obj);
    expect(scrubbed.patient_id).toBe('[REDACTED]');
    expect(scrubbed.ssn).toBe('[REDACTED]');
    expect(scrubbed.status).toBe('ok');
  });

  it('does not mutate original object', () => {
    const obj = { password: 'secret123', name: 'test' };
    scrubSensitive(obj);
    expect(obj.password).toBe('secret123');
  });

  it('returns primitives unchanged', () => {
    expect(scrubSensitive(42)).toBe(42);
    expect(scrubSensitive(null)).toBe(null);
    expect(scrubSensitive(true)).toBe(true);
    expect(scrubSensitive(undefined)).toBe(undefined);
  });

  it('returns arrays unchanged', () => {
    const arr = [1, 2, 3];
    expect(scrubSensitive(arr)).toEqual([1, 2, 3]);
  });

  it('redacts all known sensitive keys', () => {
    const obj = { patient_id: 'x', ssn: 'x', mrn: 'x', password: 'x', secret: 'x', api_key: 'x', safe: 'y' };
    const scrubbed = scrubSensitive(obj);
    expect(scrubbed.safe).toBe('y');
    for (const key of ['patient_id', 'ssn', 'mrn', 'password', 'secret', 'api_key']) {
      expect(scrubbed[key]).toBe('[REDACTED]');
    }
  });
});

describe('ECS compatibility', () => {
  it('includes ecs.version in log output', () => {
    const chunks = [];
    const stream = new Writable({
      write(chunk, enc, cb) { chunks.push(chunk.toString()); cb(); }
    });
    const logger = createAuditLogger({ serviceName: 'ecs-test', stream });
    logger.info('ecs check');
    const log = JSON.parse(chunks[0]);
    expect(log['ecs.version']).toBe('1.6.0');
  });

  it('includes service.name in log output', () => {
    const chunks = [];
    const stream = new Writable({
      write(chunk, enc, cb) { chunks.push(chunk.toString()); cb(); }
    });
    const logger = createAuditLogger({ serviceName: 'my-svc', stream });
    logger.info('svc check');
    const log = JSON.parse(chunks[0]);
    expect(log['service.name']).toBe('my-svc');
    expect(log.service).toBe('my-svc');
  });
});

describe('withTraceContext', () => {
  it('returns original logger when no traceContext', () => {
    const logger = createAuditLogger({ serviceName: 'test' });
    expect(withTraceContext(logger, null)).toBe(logger);
    expect(withTraceContext(logger, undefined)).toBe(logger);
  });

  it('returns logger unchanged when logger is null', () => {
    expect(withTraceContext(null, { traceId: 'abc' })).toBeNull();
  });

  it('creates child logger with trace fields', () => {
    const chunks = [];
    const stream = new Writable({
      write(chunk, enc, cb) { chunks.push(chunk.toString()); cb(); }
    });
    const logger = createAuditLogger({ serviceName: 'trace-test', stream });
    const child = withTraceContext(logger, {
      traceId: 'aaaa1111bbbb2222cccc3333dddd4444',
      spanId: 'eeee5555ffff6666'
    });
    child.info('traced');
    const log = JSON.parse(chunks[0]);
    expect(log['trace.id']).toBe('aaaa1111bbbb2222cccc3333dddd4444');
    expect(log['span.id']).toBe('eeee5555ffff6666');
    expect(log['parent.id']).toBeUndefined();
  });

  it('includes parent.id when parentSpanId is present', () => {
    const chunks = [];
    const stream = new Writable({
      write(chunk, enc, cb) { chunks.push(chunk.toString()); cb(); }
    });
    const logger = createAuditLogger({ serviceName: 'trace-test', stream });
    const child = withTraceContext(logger, {
      traceId: 'aaaa1111bbbb2222cccc3333dddd4444',
      spanId: 'eeee5555ffff6666',
      parentSpanId: '11112222aaaabbbb'
    });
    child.info('traced with parent');
    const log = JSON.parse(chunks[0]);
    expect(log['parent.id']).toBe('11112222aaaabbbb');
  });
});

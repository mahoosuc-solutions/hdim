const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const { isDemoMode, loadFixture, createDemoInterceptor } = require('../lib/demo-mode');

const FIXTURES_DIR = path.resolve(__dirname, '../../mcp-edge-platform/fixtures');

describe('demo-mode', () => {
  const originalEnv = process.env.HDIM_DEMO_MODE;

  afterEach(() => {
    if (originalEnv === undefined) delete process.env.HDIM_DEMO_MODE;
    else process.env.HDIM_DEMO_MODE = originalEnv;
  });

  describe('isDemoMode', () => {
    it('returns false by default', () => {
      delete process.env.HDIM_DEMO_MODE;
      expect(isDemoMode()).toBe(false);
    });

    it('returns true when env is set', () => {
      process.env.HDIM_DEMO_MODE = 'true';
      expect(isDemoMode()).toBe(true);
    });
  });

  describe('loadFixture', () => {
    it('returns null for unknown fixture', () => {
      expect(loadFixture('/nonexistent', 'no_tool')).toBeNull();
    });
  });

  describe('createDemoInterceptor', () => {
    it('returns a function', () => {
      const interceptor = createDemoInterceptor('/nonexistent');
      expect(typeof interceptor).toBe('function');
    });

    it('passes through when not in demo mode', async () => {
      delete process.env.HDIM_DEMO_MODE;
      const interceptor = createDemoInterceptor('/nonexistent');
      const handler = async () => ({ content: [{ type: 'text', text: 'real' }] });
      const result = await interceptor('some_tool', {}, handler);
      expect(result.content[0].text).toBe('real');
    });
  });
});

describe('isDemoMode — truthy variants', () => {
  afterEach(() => {
    delete process.env.HDIM_DEMO_MODE;
  });

  it.each(['true', 'TRUE', '1', 'yes', 'YES'])('returns true for %s', (val) => {
    process.env.HDIM_DEMO_MODE = val;
    expect(isDemoMode()).toBe(true);
  });
});

describe('isDemoMode — falsy variants', () => {
  afterEach(() => {
    delete process.env.HDIM_DEMO_MODE;
  });

  it.each(['false', '0', 'no', '', 'random'])('returns false for %s', (val) => {
    process.env.HDIM_DEMO_MODE = val;
    expect(isDemoMode()).toBe(false);
  });
});

describe('loadFixture — successful load', () => {
  it('loads a real fixture from the fixtures directory', () => {
    const result = loadFixture(FIXTURES_DIR, 'edge_health');
    expect(result).not.toBeNull();
    expect(typeof result).toBe('object');
  });
});

describe('loadFixture — malformed JSON', () => {
  let tmpDir;
  let tmpFile;

  beforeAll(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'demo-test-'));
    tmpFile = path.join(tmpDir, 'bad_fixture.json');
    fs.writeFileSync(tmpFile, '{ not valid json !!!', 'utf8');
  });

  afterAll(() => {
    fs.unlinkSync(tmpFile);
    fs.rmdirSync(tmpDir);
  });

  it('returns null for malformed JSON fixture', () => {
    expect(loadFixture(tmpDir, 'bad_fixture')).toBeNull();
  });
});

describe('createDemoInterceptor — end-to-end', () => {
  afterEach(() => {
    delete process.env.HDIM_DEMO_MODE;
  });

  it('returns fixture data when demo mode enabled and fixture exists', async () => {
    process.env.HDIM_DEMO_MODE = 'true';
    const interceptor = createDemoInterceptor(FIXTURES_DIR);
    const handler = jest.fn();
    const result = await interceptor('edge_health', {}, handler);
    expect(handler).not.toHaveBeenCalled();
    expect(result.content[0].type).toBe('text');
    const parsed = JSON.parse(result.content[0].text);
    expect(typeof parsed).toBe('object');
  });

  it('falls through to real handler when demo mode disabled', async () => {
    delete process.env.HDIM_DEMO_MODE;
    const interceptor = createDemoInterceptor(FIXTURES_DIR);
    const handler = jest.fn().mockResolvedValue({ content: [{ type: 'text', text: 'real-result' }] });
    const result = await interceptor('edge_health', { foo: 'bar' }, handler);
    expect(handler).toHaveBeenCalledWith({ foo: 'bar' });
    expect(result.content[0].text).toBe('real-result');
  });

  it('falls through to real handler when demo mode enabled but no fixture exists', async () => {
    process.env.HDIM_DEMO_MODE = 'true';
    const interceptor = createDemoInterceptor(FIXTURES_DIR);
    const handler = jest.fn().mockResolvedValue({ content: [{ type: 'text', text: 'fallback' }] });
    const result = await interceptor('nonexistent_tool_xyz', {}, handler);
    expect(handler).toHaveBeenCalledWith({});
    expect(result.content[0].text).toBe('fallback');
  });
});

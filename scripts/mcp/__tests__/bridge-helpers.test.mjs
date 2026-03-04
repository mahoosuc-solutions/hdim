import { toVscodeSafeToolName, createAliasRegistry, mapRequestForEdge, mapResponseForVscode } from '../bridge-helpers.mjs';

describe('toVscodeSafeToolName', () => {
  it('passes through valid lowercase names', () => {
    expect(toVscodeSafeToolName('edge_health')).toBe('edge_health');
  });

  it('replaces uppercase with underscore', () => {
    expect(toVscodeSafeToolName('Edge_Health')).toBe('_dge__ealth');
  });

  it('replaces dots with underscores', () => {
    expect(toVscodeSafeToolName('tool.name.here')).toBe('tool_name_here');
  });

  it('replaces special characters with underscores', () => {
    expect(toVscodeSafeToolName('tool@name!')).toBe('tool_name_');
  });

  it('preserves hyphens', () => {
    expect(toVscodeSafeToolName('tool-name')).toBe('tool-name');
  });

  it('handles empty string', () => {
    expect(toVscodeSafeToolName('')).toBe('');
  });
});

describe('createAliasRegistry', () => {
  it('registers and resolves aliases', () => {
    const { registerAlias, resolveAlias } = createAliasRegistry();
    const alias = registerAlias('edge_health');
    expect(alias).toBe('edge_health');
    expect(resolveAlias('edge_health')).toBe('edge_health');
  });

  it('returns same alias for same original name', () => {
    const { registerAlias } = createAliasRegistry();
    const alias1 = registerAlias('my_tool');
    const alias2 = registerAlias('my_tool');
    expect(alias1).toBe(alias2);
  });

  it('deduplicates collision aliases with suffix', () => {
    const { registerAlias } = createAliasRegistry();
    // Register a tool that will have the same safe name as another
    registerAlias('tool.v1');  // becomes 'tool_v1'
    const alias2 = registerAlias('tool_v1');  // would collide
    // tool_v1 is already taken by 'tool.v1', so this gets 'tool_v1_2'
    expect(alias2).toBe('tool_v1_2');
  });

  it('resolveAlias returns original name for unknown alias', () => {
    const { resolveAlias } = createAliasRegistry();
    expect(resolveAlias('unknown_tool')).toBe('unknown_tool');
  });
});

describe('mapRequestForEdge', () => {
  it('maps aliased tool name back to original in tools/call', () => {
    const { registerAlias, resolveAlias } = createAliasRegistry();
    registerAlias('tool.original');  // alias becomes 'tool_original'

    const request = {
      jsonrpc: '2.0',
      id: 1,
      method: 'tools/call',
      params: { name: 'tool_original', arguments: {} }
    };
    const mapped = mapRequestForEdge(request, resolveAlias);
    expect(mapped.params.name).toBe('tool.original');
  });

  it('passes through non-tools/call methods', () => {
    const { resolveAlias } = createAliasRegistry();
    const request = { jsonrpc: '2.0', id: 1, method: 'initialize', params: {} };
    const mapped = mapRequestForEdge(request, resolveAlias);
    expect(mapped).toEqual(request);
  });

  it('passes through when no alias registered', () => {
    const { resolveAlias } = createAliasRegistry();
    const request = {
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'edge_health', arguments: {} }
    };
    const mapped = mapRequestForEdge(request, resolveAlias);
    expect(mapped.params.name).toBe('edge_health');
  });

  it('does not modify original message object', () => {
    const { resolveAlias } = createAliasRegistry();
    const original = { method: 'tools/call', params: { name: 'test' } };
    mapRequestForEdge(original, resolveAlias);
    expect(original.params.name).toBe('test');
  });
});

describe('mapResponseForVscode', () => {
  it('aliases tool names in tools/list response', () => {
    const { registerAlias } = createAliasRegistry();
    const response = {
      jsonrpc: '2.0',
      id: 1,
      result: {
        tools: [
          { name: 'edge_health', description: 'Health check' },
          { name: 'platform_info', description: 'Platform info' }
        ]
      }
    };
    const mapped = mapResponseForVscode(response, registerAlias);
    expect(mapped.result.tools[0].name).toBe('edge_health');
    expect(mapped.result.tools[1].name).toBe('platform_info');
  });

  it('passes through non-tools responses unchanged', () => {
    const { registerAlias } = createAliasRegistry();
    const response = {
      jsonrpc: '2.0',
      id: 1,
      result: { protocolVersion: '2025-11-25' }
    };
    const mapped = mapResponseForVscode(response, registerAlias);
    expect(mapped).toEqual(response);
  });

  it('handles tool with no name gracefully', () => {
    const { registerAlias } = createAliasRegistry();
    const response = {
      result: { tools: [{ description: 'no name' }] }
    };
    const mapped = mapResponseForVscode(response, registerAlias);
    expect(mapped.result.tools[0].description).toBe('no name');
  });

  it('passes through error responses', () => {
    const { registerAlias } = createAliasRegistry();
    const response = {
      jsonrpc: '2.0',
      id: 1,
      error: { code: -32600, message: 'Invalid' }
    };
    const mapped = mapResponseForVscode(response, registerAlias);
    expect(mapped).toEqual(response);
  });
});

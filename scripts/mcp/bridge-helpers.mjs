/**
 * Pure helper functions for MCP stdio↔HTTP bridges.
 * Shared between platform and devops bridge scripts.
 */

/**
 * Convert a tool name to a VS Code-safe identifier.
 * VS Code MCP clients only accept [a-z0-9_-] in tool names.
 */
export function toVscodeSafeToolName(name) {
  return String(name).replace(/[^a-z0-9_-]/g, '_');
}

/**
 * Create a tool alias registry for bidirectional name mapping.
 * Returns { registerAlias, resolveAlias, getOriginal } functions.
 */
export function createAliasRegistry() {
  const aliasToOriginal = new Map();
  const originalToAlias = new Map();

  function registerAlias(originalName) {
    if (originalToAlias.has(originalName)) {
      return originalToAlias.get(originalName);
    }

    const baseAlias = toVscodeSafeToolName(originalName);
    let alias = baseAlias;
    let suffix = 2;

    while (aliasToOriginal.has(alias) && aliasToOriginal.get(alias) !== originalName) {
      alias = `${baseAlias}_${suffix}`;
      suffix += 1;
    }

    aliasToOriginal.set(alias, originalName);
    originalToAlias.set(originalName, alias);
    return alias;
  }

  function resolveAlias(aliasName) {
    return aliasToOriginal.get(aliasName) || aliasName;
  }

  return { registerAlias, resolveAlias };
}

/**
 * Map an incoming tools/call request from VS Code alias back to original name.
 */
export function mapRequestForEdge(message, resolveAlias) {
  if (message?.method === 'tools/call' && message?.params?.name) {
    const incomingName = message.params.name;
    const mappedName = resolveAlias(incomingName);
    return {
      ...message,
      params: {
        ...message.params,
        name: mappedName
      }
    };
  }
  return message;
}

/**
 * Map a tools/list response to use VS Code-safe aliased names.
 */
export function mapResponseForVscode(response, registerAlias) {
  if (response?.result?.tools && Array.isArray(response.result.tools)) {
    const mappedTools = response.result.tools.map((tool) => {
      const originalName = tool?.name;
      if (!originalName) return tool;

      const alias = registerAlias(originalName);
      return { ...tool, name: alias };
    });

    return {
      ...response,
      result: {
        ...response.result,
        tools: mappedTools
      }
    };
  }
  return response;
}

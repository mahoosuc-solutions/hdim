import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import process from 'node:process';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { z } from 'zod';

const DEFAULT_BASE_URL = 'http://localhost:18080';
const DEFAULT_DOCKER_BASE_URL = 'http://host.docker.internal:18080';

export function normalizeBaseUrl(input) {
  const raw = (input ?? process.env.HDIM_BASE_URL ?? DEFAULT_BASE_URL).trim();
  const parsed = new URL(raw);
  return parsed.toString().replace(/\/$/, '');
}

export function pathToUrl(baseUrl, path) {
  if (typeof path !== 'string' || path.length === 0) throw new Error('path is required');
  if (!path.startsWith('/')) throw new Error('path must start with "/"');
  if (path.startsWith('//')) throw new Error('path must not start with "//"');
  if (path.includes('://')) throw new Error('path must not include "://"');
  return new URL(path, `${baseUrl}/`).toString();
}

export function createServer() {
  const server = new McpServer({
    name: 'hdim-platform',
    version: '0.1.0',
  });

  const httpGet = async ({ baseUrl, path: requestPath }) => {
    const normalizedBaseUrl = normalizeBaseUrl(baseUrl);
    const url = pathToUrl(normalizedBaseUrl, requestPath);

    const response = await fetch(url, {
      method: 'GET',
      headers: { accept: 'application/json, text/plain;q=0.9, */*;q=0.1' },
    });

    const contentType = response.headers.get('content-type') ?? '';
    const bodyText = await response.text();
    const truncatedBody =
      bodyText.length > 20_000 ? `${bodyText.slice(0, 20_000)}\n...[truncated]` : bodyText;

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(
            {
              url,
              status: response.status,
              ok: response.ok,
              contentType,
              body: truncatedBody,
            },
            null,
            2,
          ),
        },
      ],
    };
  };

  server.registerTool(
    'hdim_info',
    {
      title: 'HDIM Platform MCP Info',
      description:
        'Returns MCP server info and recommended base URLs (host + Docker Desktop).',
      inputSchema: z.object({}),
    },
    async () => ({
      content: [
        {
          type: 'text',
          text: JSON.stringify(
            {
              name: 'hdim-platform',
              defaultBaseUrl: DEFAULT_BASE_URL,
              dockerDesktopBaseUrl: DEFAULT_DOCKER_BASE_URL,
              envBaseUrl: process.env.HDIM_BASE_URL ?? null,
            },
            null,
            2,
          ),
        },
      ],
    }),
  );

  server.registerTool(
    'hdim_http_get',
    {
      title: 'HDIM HTTP GET',
      description:
        'Fetches a platform endpoint by path (default baseUrl: HDIM_BASE_URL or http://localhost:18080).',
      inputSchema: z.object({
        baseUrl: z.string().url().optional(),
        path: z.string().min(1).describe('Must start with "/" (example: "/actuator/health")'),
      }),
    },
    httpGet,
  );

  server.registerTool(
    'hdim_health_check',
    {
      title: 'HDIM Health Check',
      description: 'Calls /actuator/health on the platform gateway.',
      inputSchema: z.object({
        baseUrl: z.string().url().optional(),
      }),
    },
    async ({ baseUrl }) => {
      return await httpGet({ baseUrl, path: '/actuator/health' });
    },
  );

  server.registerTool(
    'hdim_fhir_metadata',
    {
      title: 'HDIM FHIR Metadata',
      description: 'Calls /fhir/metadata (FHIR capability statement).',
      inputSchema: z.object({
        baseUrl: z.string().url().optional(),
      }),
    },
    async ({ baseUrl }) => {
      return await httpGet({ baseUrl, path: '/fhir/metadata' });
    },
  );

  return server;
}

async function main() {
  const server = createServer();
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

const entrypointHref = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : null;
const isEntrypoint = entrypointHref === import.meta.url;
if (isEntrypoint) {
  main().catch((error) => {
    // eslint-disable-next-line no-console
    console.error(error);
    process.exit(1);
  });
}

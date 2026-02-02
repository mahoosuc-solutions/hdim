/**
 * Parse request body for Vercel serverless functions.
 * Reads body from stream since Vercel doesn't auto-parse for Node.js functions.
 */

import type { VercelRequest } from '@vercel/node';

export async function parseBody<T = Record<string, unknown>>(req: VercelRequest): Promise<T | null> {
  try {
    // Read body from stream
    const chunks: Buffer[] = [];
    for await (const chunk of req) {
      chunks.push(Buffer.from(chunk));
    }
    const rawBody = Buffer.concat(chunks).toString('utf-8');

    if (!rawBody.trim()) {
      return null;
    }

    return JSON.parse(rawBody) as T;
  } catch {
    return null;
  }
}

/**
 * POST /api/auth/debug-body
 *
 * Debug endpoint to test request body parsing.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  res.setHeader('Access-Control-Allow-Origin', '*');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }

  try {
    // Step 1: Check raw body
    const bodyType = typeof req.body;
    const bodyIsNull = req.body === null;
    const bodyIsUndefined = req.body === undefined;

    // Step 2: Try to access properties
    let email, password;
    let accessError = null;
    try {
      const body = req.body || {};
      email = body.email;
      password = body.password;
    } catch (e) {
      accessError = e instanceof Error ? e.message : String(e);
    }

    res.status(200).json({
      bodyType,
      bodyIsNull,
      bodyIsUndefined,
      body: req.body,
      accessError,
      email,
      hasPassword: !!password,
    });

  } catch (error) {
    res.status(500).json({
      error: 'Error',
      message: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined,
    });
  }
}

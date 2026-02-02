/**
 * POST /api/auth/debug-auth-module
 *
 * Debug endpoint to test auth module import.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { hashPassword, comparePassword, generateTokens } from '../../lib/auth';

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
    const { password } = req.body || {};

    if (!password) {
      res.status(400).json({ error: 'password required' });
      return;
    }

    // Test hash
    const hash = await hashPassword(password);

    // Test compare
    const isValid = await comparePassword(password, hash);

    // Test tokens
    const tokens = generateTokens({
      id: 'test-id',
      email: 'test@test.com',
      firstName: 'Test',
      lastName: 'User',
      role: 'USER',
    });

    res.status(200).json({
      message: 'auth module working',
      hash,
      isValid,
      accessToken: tokens.accessToken.substring(0, 50) + '...',
    });

  } catch (error) {
    res.status(500).json({
      error: 'auth module error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

/**
 * POST /api/auth/debug-jwt
 *
 * Debug endpoint to test JWT specifically.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import jwt, { SignOptions } from 'jsonwebtoken';

const JWT_SECRET = process.env.JWT_SECRET || 'test-secret';

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
    const { userId, email } = req.body || {};

    if (!userId || !email) {
      res.status(400).json({ error: 'userId and email required' });
      return;
    }

    // Test signing
    const payload = { userId, email, type: 'access' };
    const token = jwt.sign(payload, JWT_SECRET, {
      expiresIn: '24h',
    } as SignOptions);

    // Test verifying
    const decoded = jwt.verify(token, JWT_SECRET);

    res.status(200).json({
      message: 'JWT working',
      token,
      decoded,
    });

  } catch (error) {
    res.status(500).json({
      error: 'JWT error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

/**
 * POST /api/auth/debug-bcrypt
 *
 * Debug endpoint to test bcrypt specifically.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import bcrypt from 'bcryptjs';

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
    const { password, hash } = req.body || {};

    if (!password) {
      res.status(400).json({ error: 'Password required' });
      return;
    }

    // Test hashing
    const testHash = await bcrypt.hash(password, 10);

    // Test comparison
    const isValid = hash
      ? await bcrypt.compare(password, hash)
      : await bcrypt.compare(password, testHash);

    res.status(200).json({
      message: 'bcrypt working',
      testHash,
      isValid,
    });

  } catch (error) {
    res.status(500).json({
      error: 'bcrypt error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

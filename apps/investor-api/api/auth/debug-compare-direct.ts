/**
 * POST /api/auth/debug-compare-direct
 *
 * Debug: compare against hardcoded hash
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import bcrypt from 'bcryptjs';

// The actual hash from the database
const STORED_HASH = '$2b$10$A/3zkA.LlzL5N/C0ZZJwJOj.P4p/w2tmsueYXwXTcwD/CvwYOVFoe';

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
      res.status(400).json({ error: 'Password required' });
      return;
    }

    // Compare against hardcoded hash
    const isValid = await bcrypt.compare(password, STORED_HASH);

    res.status(200).json({
      success: true,
      isValid,
      password,
      hashUsed: STORED_HASH,
    });

  } catch (error) {
    res.status(500).json({
      error: 'Error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

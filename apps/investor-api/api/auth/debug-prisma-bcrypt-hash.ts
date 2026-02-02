/**
 * POST /api/auth/debug-prisma-bcrypt-hash
 *
 * Debug: test prisma query + bcrypt.hash (not compare)
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
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
    const body = req.body || {};
    const { email, password } = body;

    if (!email || !password) {
      res.status(400).json({ error: 'Email and password required' });
      return;
    }

    // Query user
    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase() },
      select: { id: true, email: true, passwordHash: true },
    });

    if (!user) {
      res.status(401).json({ error: 'User not found' });
      return;
    }

    // Just hash, don't compare
    const newHash = await bcrypt.hash(password, 10);

    res.status(200).json({
      success: true,
      userId: user.id,
      storedHashLength: user.passwordHash.length,
      newHashLength: newHash.length,
    });

  } catch (error) {
    res.status(500).json({
      error: 'Error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

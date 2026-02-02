/**
 * POST /api/auth/debug-compare-only
 *
 * Debug: test bcrypt.compare specifically with stored hash
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

  const steps: { step: string; success: boolean; data?: any; error?: string }[] = [];

  try {
    const body = req.body || {};
    const { email, password } = body;
    steps.push({ step: '1-body', success: true, data: { email, hasPassword: !!password } });

    if (!email || !password) {
      res.status(400).json({ steps, error: 'Email and password required' });
      return;
    }

    // Step 2: Query user
    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase() },
      select: { id: true, email: true, passwordHash: true },
    });
    steps.push({ step: '2-query', success: !!user, data: { userId: user?.id, hashLength: user?.passwordHash?.length } });

    if (!user) {
      res.status(401).json({ steps, error: 'User not found' });
      return;
    }

    // Step 3: Log hash info
    const storedHash = user.passwordHash;
    steps.push({
      step: '3-hash-info',
      success: true,
      data: {
        hashLength: storedHash.length,
        hashStart: storedHash.substring(0, 10),
        hashType: storedHash.startsWith('$2a$') ? 'bcrypt-2a' : storedHash.startsWith('$2b$') ? 'bcrypt-2b' : 'unknown',
      }
    });

    // Step 4: Call bcrypt.compare
    let isValid;
    try {
      isValid = await bcrypt.compare(password, storedHash);
      steps.push({ step: '4-compare', success: true, data: { isValid } });
    } catch (bcryptError) {
      steps.push({ step: '4-compare', success: false, error: bcryptError instanceof Error ? bcryptError.message : String(bcryptError) });
      res.status(500).json({ steps, error: 'bcrypt.compare failed' });
      return;
    }

    res.status(200).json({
      steps,
      success: true,
      isValid,
    });

  } catch (error) {
    steps.push({ step: 'error', success: false, error: error instanceof Error ? error.message : String(error) });
    res.status(500).json({ steps, error: error instanceof Error ? error.message : String(error) });
  }
}

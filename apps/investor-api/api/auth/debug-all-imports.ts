/**
 * POST /api/auth/debug-all-imports
 *
 * Debug: test with BOTH prisma and auth imports
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import { comparePassword, generateTokens } from '../../lib/auth';

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
    // Step 1: Access body
    const body = req.body || {};
    const { email, password } = body;

    if (!email || !password) {
      res.status(400).json({ error: 'Email and password required', body });
      return;
    }

    // Step 2: Query user
    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase() },
    });

    if (!user) {
      res.status(401).json({ error: 'User not found' });
      return;
    }

    // Step 3: Compare password
    const isValid = await comparePassword(password, user.passwordHash);

    if (!isValid) {
      res.status(401).json({ error: 'Invalid password' });
      return;
    }

    // Step 4: Generate tokens
    const tokens = generateTokens({
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
    });

    res.status(200).json({
      success: true,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      user: {
        id: user.id,
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        role: user.role,
      }
    });

  } catch (error) {
    res.status(500).json({
      error: 'Error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

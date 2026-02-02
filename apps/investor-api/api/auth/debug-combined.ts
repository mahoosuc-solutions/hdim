/**
 * POST /api/auth/debug-combined
 *
 * Debug endpoint combining prisma + auth.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';

// Import db first
import prisma from '../../lib/db';

// Then auth
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

  const steps: { step: string; success: boolean; data?: any; error?: string }[] = [];

  try {
    steps.push({ step: '1-imports-loaded', success: true });

    const { email, password } = req.body || {};
    steps.push({ step: '2-body-parsed', success: true, data: { email, hasPassword: !!password } });

    if (!email || !password) {
      res.status(400).json({ steps, error: 'Email and password required' });
      return;
    }

    // Find user
    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase() },
    });
    steps.push({ step: '3-user-found', success: !!user, data: { userId: user?.id } });

    if (!user) {
      res.status(401).json({ steps, error: 'User not found' });
      return;
    }

    // Compare password
    const isValid = await comparePassword(password, user.passwordHash);
    steps.push({ step: '4-password-compared', success: isValid });

    if (!isValid) {
      res.status(401).json({ steps, error: 'Invalid password' });
      return;
    }

    // Generate tokens
    const tokens = generateTokens({
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
    });
    steps.push({ step: '5-tokens-generated', success: true });

    res.status(200).json({
      steps,
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
    steps.push({
      step: 'error',
      success: false,
      error: error instanceof Error ? error.message : String(error),
    });
    res.status(500).json({ steps, error: error instanceof Error ? error.message : String(error) });
  }
}

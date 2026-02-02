/**
 * POST /api/auth/debug-simple
 *
 * Simplest debug endpoint - only uses prisma, no auth imports.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';

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
    const { email } = req.body || {};

    if (!email) {
      res.status(400).json({ error: 'Email required', body: req.body });
      return;
    }

    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase() },
      select: {
        id: true,
        email: true,
        firstName: true,
        lastName: true,
        role: true,
        passwordHash: true,
      }
    });

    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    res.status(200).json({
      message: 'User found',
      user: {
        id: user.id,
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        role: user.role,
        passwordHashLength: user.passwordHash?.length || 0,
      }
    });

  } catch (error) {
    res.status(500).json({
      error: 'Error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

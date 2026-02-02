/**
 * POST /api/auth/debug-prisma-body
 *
 * Debug: test body parsing WITH prisma import
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
    // Step 1: Access body
    const body = req.body || {};
    const { email, password } = body;

    // Step 2: Query user
    const user = email
      ? await prisma.user.findUnique({ where: { email: email.toLowerCase() } })
      : null;

    res.status(200).json({
      body,
      email,
      hasPassword: !!password,
      userFound: !!user,
      userId: user?.id,
    });

  } catch (error) {
    res.status(500).json({
      error: 'Error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

/**
 * GET /api/auth/debug-show-hash
 *
 * Debug: show the actual stored hash
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  res.setHeader('Access-Control-Allow-Origin', '*');

  try {
    const email = (req.query.email as string) || 'aaron@mahoosuc.solutions';

    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase() },
      select: { id: true, email: true, passwordHash: true },
    });

    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    res.status(200).json({
      userId: user.id,
      email: user.email,
      passwordHash: user.passwordHash,
      hashLength: user.passwordHash.length,
      hashBytes: Buffer.from(user.passwordHash).toString('hex'),
    });

  } catch (error) {
    res.status(500).json({
      error: 'Error',
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

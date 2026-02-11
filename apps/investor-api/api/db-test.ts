/**
 * GET /api/db-test
 *
 * Test database connection.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import prisma from '../lib/db';

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  try {
    // Try to count users
    const userCount = await prisma.user.count();
    const taskCount = await prisma.task.count();

    res.status(200).json({
      status: 'connected',
      counts: {
        users: userCount,
        tasks: taskCount,
      },
    });
  } catch (error) {
    res.status(500).json({
      status: 'error',
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined,
    });
  }
}

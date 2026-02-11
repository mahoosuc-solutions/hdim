/**
 * GET /api/health
 *
 * Health check endpoint - verifies API is running.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';

export default function handler(
  req: VercelRequest,
  res: VercelResponse
): void {
  res.status(200).json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    env: {
      DATABASE_URL_EXISTS: !!process.env.DATABASE_URL,
      JWT_SECRET_EXISTS: !!process.env.JWT_SECRET,
      NODE_ENV: process.env.NODE_ENV,
    },
  });
}

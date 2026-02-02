/**
 * GET /api/auth/debug-env
 *
 * Debug endpoint to check JWT environment variables.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';

export default function handler(
  req: VercelRequest,
  res: VercelResponse
): void {
  res.setHeader('Access-Control-Allow-Origin', '*');

  res.status(200).json({
    JWT_SECRET_SET: !!process.env.JWT_SECRET,
    JWT_SECRET_LENGTH: process.env.JWT_SECRET?.length || 0,
    JWT_ACCESS_EXPIRY: process.env.JWT_ACCESS_EXPIRY || 'NOT SET (will use default 24h)',
    JWT_ACCESS_EXPIRY_TYPE: typeof process.env.JWT_ACCESS_EXPIRY,
    JWT_REFRESH_EXPIRY: process.env.JWT_REFRESH_EXPIRY || 'NOT SET (will use default 7d)',
    JWT_REFRESH_EXPIRY_TYPE: typeof process.env.JWT_REFRESH_EXPIRY,
    DATABASE_URL_SET: !!process.env.DATABASE_URL,
    NODE_ENV: process.env.NODE_ENV,
  });
}

/**
 * POST /api/auth/refresh
 *
 * Refresh access token using a valid refresh token.
 * Returns new access and refresh tokens.
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import { generateTokens, verifyRefreshToken } from '../../lib/auth';
import {
  handleCors,
  withErrorHandler,
  sendSuccess,
  sendError,
} from '../../lib/middleware';
import type { RefreshRequest, LoginResponse } from '../../lib/types';

async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  // Handle CORS
  if (handleCors(req as any, res)) return;

  // Only allow POST
  if (req.method !== 'POST') {
    sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
    return;
  }

  const { refreshToken } = req.body as RefreshRequest;

  // Validate input
  if (!refreshToken) {
    sendError(res, 'Refresh token is required', 400, 'VALIDATION_ERROR');
    return;
  }

  try {
    // Verify refresh token
    const payload = verifyRefreshToken(refreshToken);

    // Get user from database (to ensure they still exist and are active)
    const user = await prisma.user.findUnique({
      where: { id: payload.userId },
    });

    if (!user) {
      sendError(res, 'User not found', 401, 'USER_NOT_FOUND');
      return;
    }

    if (!user.active) {
      sendError(res, 'Account is deactivated', 403, 'ACCOUNT_DEACTIVATED');
      return;
    }

    // Generate new tokens
    const userInfo = {
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
    };

    const tokens = generateTokens(userInfo);

    const response: LoginResponse = {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      tokenType: 'Bearer',
      expiresIn: tokens.expiresIn,
      user: userInfo,
    };

    sendSuccess(res, response);
  } catch (error) {
    const message =
      error instanceof Error ? error.message : 'Invalid refresh token';
    sendError(res, message, 401, 'INVALID_TOKEN');
  }
}

export default withErrorHandler(handler as any);

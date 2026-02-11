/**
 * GET /api/auth/me
 *
 * Get current authenticated user information.
 * Requires valid access token.
 */

import type { VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import {
  withAuth,
  withErrorHandler,
  sendSuccess,
  sendError,
} from '../../lib/middleware';
import type { AuthenticatedRequest, UserInfo } from '../../lib/types';

async function handler(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  // Only allow GET
  if (req.method !== 'GET') {
    sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
    return;
  }

  // User is guaranteed to exist due to withAuth middleware
  const userId = req.user!.userId;

  // Get fresh user data from database
  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: {
      id: true,
      email: true,
      firstName: true,
      lastName: true,
      role: true,
      active: true,
      lastLogin: true,
      linkedInConnection: {
        select: {
          connected: true,
          profileUrl: true,
          lastSync: true,
        },
      },
    },
  });

  if (!user) {
    sendError(res, 'User not found', 404, 'USER_NOT_FOUND');
    return;
  }

  if (!user.active) {
    sendError(res, 'Account is deactivated', 403, 'ACCOUNT_DEACTIVATED');
    return;
  }

  const response: UserInfo & { linkedIn?: { connected: boolean; profileUrl: string | null } } = {
    id: user.id,
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    role: user.role,
  };

  // Include LinkedIn status if connected
  if (user.linkedInConnection?.connected) {
    response.linkedIn = {
      connected: true,
      profileUrl: user.linkedInConnection.profileUrl,
    };
  }

  sendSuccess(res, response);
}

export default withErrorHandler(withAuth(handler));

/**
 * User authentication models and types
 * Re-exported from auth.service for convenience
 */

import type { User } from '../services/auth.service';

export type { User, Role, Permission, LoginRequest, LoginResponse, MfaRequiredResponse } from '../services/auth.service';

/**
 * User utility extensions
 */
export interface UserDisplay extends User {
  fullName?: string;
  displayName: string;
}

/**
 * Get user's display name (full name or username fallback)
 */
export function getUserDisplayName(user: User): string {
  if (user.firstName && user.lastName) {
    return `${user.firstName} ${user.lastName}`;
  }
  if (user.firstName) {
    return user.firstName;
  }
  return user.username;
}

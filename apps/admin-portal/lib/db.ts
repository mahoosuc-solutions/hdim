/**
 * Prisma Database Client
 *
 * Singleton pattern for Prisma client to prevent connection exhaustion
 * in serverless environments like Vercel.
 */

import { PrismaClient } from '@prisma/client';

// Declare global type for Prisma client caching
declare global {
  // eslint-disable-next-line no-var
  var prisma: PrismaClient | undefined;
}

// Prevent multiple instances of Prisma Client in development
export const prisma =
  global.prisma ||
  new PrismaClient({
    log:
      process.env.NODE_ENV === 'development'
        ? ['query', 'error', 'warn']
        : ['error'],
  });

if (process.env.NODE_ENV !== 'production') {
  global.prisma = prisma;
}

export default prisma;

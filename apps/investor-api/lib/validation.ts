/**
 * Zod validation schemas for API request bodies.
 *
 * Provides type-safe input validation with helpful error messages.
 * Uses Zod 4 API.
 */

import { z } from 'zod';

// ============================================================================
// Auth Schemas
// ============================================================================

export const loginSchema = z.object({
  email: z
    .string()
    .email('Invalid email format')
    .max(255, 'Email must be less than 255 characters')
    .transform((val) => val.toLowerCase().trim()),
  password: z
    .string()
    .min(1, 'Password is required')
    .max(128, 'Password must be less than 128 characters'),
});

export const refreshTokenSchema = z.object({
  refreshToken: z
    .string()
    .min(1, 'Refresh token is required'),
});

// ============================================================================
// Task Schemas
// ============================================================================

export const createTaskSchema = z.object({
  subject: z
    .string()
    .min(1, 'Subject is required')
    .max(255, 'Subject must be less than 255 characters'),
  description: z
    .string()
    .max(10000, 'Description must be less than 10000 characters')
    .optional()
    .nullable(),
  status: z
    .enum(['pending', 'in_progress', 'completed', 'blocked'])
    .default('pending'),
  category: z
    .string()
    .min(1, 'Category is required')
    .max(100, 'Category must be less than 100 characters'),
  week: z
    .number()
    .int('Week must be an integer')
    .min(1, 'Week must be at least 1')
    .max(52, 'Week must be at most 52'),
  deliverable: z
    .string()
    .max(500, 'Deliverable must be less than 500 characters')
    .optional()
    .nullable(),
  owner: z
    .string()
    .max(100, 'Owner must be less than 100 characters')
    .optional()
    .nullable(),
  dueDate: z
    .string()
    .datetime('Due date must be a valid ISO date')
    .optional()
    .nullable(),
  notes: z
    .string()
    .max(10000, 'Notes must be less than 10000 characters')
    .optional()
    .nullable(),
});

export const updateTaskSchema = createTaskSchema.partial();

// ============================================================================
// Contact Schemas
// ============================================================================

export const createContactSchema = z.object({
  name: z
    .string()
    .min(1, 'Name is required')
    .max(255, 'Name must be less than 255 characters'),
  title: z
    .string()
    .max(255, 'Title must be less than 255 characters')
    .optional()
    .nullable(),
  organization: z
    .string()
    .max(255, 'Organization must be less than 255 characters')
    .optional()
    .nullable(),
  email: z
    .string()
    .email('Invalid email format')
    .max(255, 'Email must be less than 255 characters')
    .optional()
    .nullable(),
  phone: z
    .string()
    .max(50, 'Phone must be less than 50 characters')
    .optional()
    .nullable(),
  linkedinUrl: z
    .string()
    .url('Invalid LinkedIn URL')
    .max(500, 'LinkedIn URL must be less than 500 characters')
    .optional()
    .nullable(),
  category: z
    .string()
    .min(1, 'Category is required')
    .max(100, 'Category must be less than 100 characters'),
  status: z
    .enum(['identified', 'researching', 'outreach', 'engaged', 'meeting_scheduled', 'follow_up', 'closed'])
    .default('identified'),
  tier: z
    .enum(['A', 'B', 'C'])
    .default('B'),
  investmentThesis: z
    .string()
    .max(5000, 'Investment thesis must be less than 5000 characters')
    .optional()
    .nullable(),
  notes: z
    .string()
    .max(10000, 'Notes must be less than 10000 characters')
    .optional()
    .nullable(),
  nextFollowUp: z
    .string()
    .datetime('Next follow up must be a valid ISO date')
    .optional()
    .nullable(),
});

export const updateContactSchema = createContactSchema.partial();

// ============================================================================
// Activity Schemas
// ============================================================================

export const createActivitySchema = z.object({
  contactId: z
    .string()
    .uuid('Invalid contact ID format'),
  activityType: z
    .enum(['email', 'linkedin_message', 'linkedin_connection', 'call', 'meeting', 'note']),
  status: z
    .enum(['pending', 'sent', 'delivered', 'opened', 'replied', 'completed', 'failed'])
    .default('pending'),
  subject: z
    .string()
    .max(500, 'Subject must be less than 500 characters')
    .optional()
    .nullable(),
  content: z
    .string()
    .max(50000, 'Content must be less than 50000 characters')
    .optional()
    .nullable(),
  activityDate: z
    .string()
    .datetime('Activity date must be a valid ISO date'),
  scheduledTime: z
    .string()
    .datetime('Scheduled time must be a valid ISO date')
    .optional()
    .nullable(),
  notes: z
    .string()
    .max(10000, 'Notes must be less than 10000 characters')
    .optional()
    .nullable(),
});

export const updateActivitySchema = createActivitySchema.partial().omit({ contactId: true });

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Format Zod validation errors into a user-friendly message.
 */
export function formatZodError(error: z.ZodError): string {
  return error.issues
    .map((e) => {
      const path = e.path.join('.');
      return path ? `${path}: ${e.message}` : e.message;
    })
    .join('; ');
}

/**
 * Validate request body against a schema.
 * Returns the validated data or throws with formatted error message.
 */
export function validateBody<T>(schema: z.ZodSchema<T>, body: unknown): T {
  const result = schema.safeParse(body);
  if (!result.success) {
    const error = new Error(formatZodError(result.error));
    (error as any).code = 'VALIDATION_ERROR';
    (error as any).details = result.error.issues;
    throw error;
  }
  return result.data;
}

// Type exports for use in handlers
export type LoginInput = z.infer<typeof loginSchema>;
export type RefreshTokenInput = z.infer<typeof refreshTokenSchema>;
export type CreateTaskInput = z.infer<typeof createTaskSchema>;
export type UpdateTaskInput = z.infer<typeof updateTaskSchema>;
export type CreateContactInput = z.infer<typeof createContactSchema>;
export type UpdateContactInput = z.infer<typeof updateContactSchema>;
export type CreateActivityInput = z.infer<typeof createActivitySchema>;
export type UpdateActivityInput = z.infer<typeof updateActivitySchema>;

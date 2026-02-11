/**
 * Shared TypeScript types for Investor Dashboard API
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';

// ============================================
// Request/Response Types
// ============================================

export interface AuthenticatedRequest extends VercelRequest {
  user?: {
    userId: string;
    email: string;
    role: string;
  };
}

export type ApiHandler = (
  req: AuthenticatedRequest,
  res: VercelResponse
) => Promise<void> | void;

// ============================================
// Auth Types
// ============================================

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface UserInfo {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface JwtPayload {
  userId: string;
  email: string;
  role: string;
  type: 'access' | 'refresh';
  iat?: number;
  exp?: number;
}

// ============================================
// Task Types
// ============================================

export interface CreateTaskRequest {
  subject: string;
  description?: string;
  category: string;
  week: number;
  deliverable?: string;
  owner?: string;
  dueDate?: string;
  notes?: string;
}

export interface UpdateTaskRequest {
  subject?: string;
  description?: string;
  status?: TaskStatus;
  category?: string;
  week?: number;
  deliverable?: string;
  owner?: string;
  dueDate?: string;
  notes?: string;
}

export type TaskStatus = 'pending' | 'in_progress' | 'completed' | 'blocked';

export interface TaskResponse {
  id: string;
  subject: string;
  description: string | null;
  status: string;
  category: string;
  week: number;
  deliverable: string | null;
  owner: string | null;
  dueDate: string | null;
  completedAt: string | null;
  notes: string | null;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

// ============================================
// Contact Types
// ============================================

export interface CreateContactRequest {
  name: string;
  title?: string;
  organization?: string;
  email?: string;
  phone?: string;
  linkedInUrl?: string;
  category: string;
  tier?: string;
  notes?: string;
}

export interface UpdateContactRequest {
  name?: string;
  title?: string;
  organization?: string;
  email?: string;
  phone?: string;
  linkedInUrl?: string;
  category?: string;
  status?: ContactStatus;
  tier?: string;
  notes?: string;
  nextFollowUp?: string;
}

export type ContactStatus =
  | 'identified'
  | 'contacted'
  | 'engaged'
  | 'meeting_scheduled'
  | 'follow_up'
  | 'declined'
  | 'invested';

export interface ContactResponse {
  id: string;
  name: string;
  title: string | null;
  organization: string | null;
  email: string | null;
  phone: string | null;
  linkedInUrl: string | null;
  category: string;
  status: string;
  tier: string;
  notes: string | null;
  lastContacted: string | null;
  nextFollowUp: string | null;
  createdAt: string;
  updatedAt: string;
}

// ============================================
// Activity Types
// ============================================

export interface CreateActivityRequest {
  contactId: string;
  activityType: ActivityType;
  subject?: string;
  content?: string;
  activityDate: string;
  scheduledTime?: string;
  notes?: string;
}

export interface UpdateActivityRequest {
  activityType?: ActivityType;
  status?: ActivityStatus;
  subject?: string;
  content?: string;
  activityDate?: string;
  scheduledTime?: string;
  responseContent?: string;
  notes?: string;
}

export type ActivityType =
  | 'linkedin_connect'
  | 'linkedin_message'
  | 'email'
  | 'call'
  | 'meeting'
  | 'intro_request';

export type ActivityStatus =
  | 'pending'
  | 'sent'
  | 'responded'
  | 'completed'
  | 'no_response';

export interface ActivityResponse {
  id: string;
  contactId: string;
  contactName?: string;
  activityType: string;
  status: string;
  subject: string | null;
  content: string | null;
  activityDate: string;
  scheduledTime: string | null;
  responseReceived: string | null;
  responseContent: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

// ============================================
// Dashboard Stats Types
// ============================================

export interface DashboardStats {
  tasks: {
    total: number;
    completed: number;
    inProgress: number;
    pending: number;
    blocked: number;
  };
  contacts: {
    total: number;
    byTier: Record<string, number>;
    byStatus: Record<string, number>;
  };
  activities: {
    total: number;
    thisWeek: number;
    byType: Record<string, number>;
    recentResponses: number;
  };
  weeklyProgress: {
    week: number;
    tasksCompleted: number;
    tasksTotal: number;
    outreachSent: number;
    responsesReceived: number;
  }[];
}

// ============================================
// Error Types
// ============================================

export interface ApiError {
  message: string;
  code?: string;
  details?: Record<string, unknown>;
}

export class HttpError extends Error {
  constructor(
    public statusCode: number,
    message: string,
    public code?: string
  ) {
    super(message);
    this.name = 'HttpError';
  }
}

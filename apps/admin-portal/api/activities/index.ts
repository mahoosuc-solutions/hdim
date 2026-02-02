/**
 * /api/activities
 *
 * GET  - List activities (filter by contactId)
 * POST - Log a new activity
 */

import type { VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import {
  withAuth,
  withErrorHandler,
  sendSuccess,
  sendError,
  getQueryString,
  getQueryNumber,
} from '../../lib/middleware';
import type {
  AuthenticatedRequest,
  CreateActivityRequest,
  ActivityResponse,
} from '../../lib/types';

async function handler(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  switch (req.method) {
    case 'GET':
      await listActivities(req, res);
      break;
    case 'POST':
      await createActivity(req, res);
      break;
    default:
      sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
  }
}

/**
 * GET /api/activities
 * Query params: contactId, activityType, status, limit
 */
async function listActivities(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const contactId = getQueryString(req.query.contactId);
  const activityType = getQueryString(req.query.activityType);
  const status = getQueryString(req.query.status);
  const limit = getQueryNumber(req.query.limit) || 50;

  // Build where clause
  const where: Record<string, unknown> = {};
  if (contactId) where.contactId = contactId;
  if (activityType) where.activityType = activityType;
  if (status) where.status = status;

  const activities = await prisma.outreachActivity.findMany({
    where,
    include: {
      contact: {
        select: { name: true },
      },
    },
    orderBy: { activityDate: 'desc' },
    take: limit,
  });

  const response: ActivityResponse[] = activities.map((a) =>
    formatActivityResponse(a, a.contact?.name)
  );

  sendSuccess(res, response);
}

/**
 * POST /api/activities
 * Log a new outreach activity
 */
async function createActivity(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const data = req.body as CreateActivityRequest;

  // Validate required fields
  if (!data.contactId || !data.activityType || !data.activityDate) {
    sendError(
      res,
      'contactId, activityType, and activityDate are required',
      400,
      'VALIDATION_ERROR'
    );
    return;
  }

  // Validate activity type
  const validTypes = [
    'linkedin_connect',
    'linkedin_message',
    'email',
    'call',
    'meeting',
    'intro_request',
  ];
  if (!validTypes.includes(data.activityType)) {
    sendError(
      res,
      `Invalid activityType. Must be one of: ${validTypes.join(', ')}`,
      400,
      'VALIDATION_ERROR'
    );
    return;
  }

  // Check if contact exists
  const contact = await prisma.contact.findUnique({
    where: { id: data.contactId },
  });
  if (!contact) {
    sendError(res, 'Contact not found', 404, 'CONTACT_NOT_FOUND');
    return;
  }

  const activity = await prisma.outreachActivity.create({
    data: {
      contactId: data.contactId,
      activityType: data.activityType,
      subject: data.subject,
      content: data.content,
      activityDate: new Date(data.activityDate),
      scheduledTime: data.scheduledTime ? new Date(data.scheduledTime) : null,
      notes: data.notes,
      createdBy: req.user?.userId,
    },
    include: {
      contact: {
        select: { name: true },
      },
    },
  });

  // Update contact's lastContacted
  await prisma.contact.update({
    where: { id: data.contactId },
    data: { lastContacted: new Date() },
  });

  sendSuccess(res, formatActivityResponse(activity, activity.contact?.name), 201);
}

/**
 * Format activity for API response.
 */
function formatActivityResponse(
  activity: any,
  contactName?: string
): ActivityResponse {
  return {
    id: activity.id,
    contactId: activity.contactId,
    contactName: contactName || undefined,
    activityType: activity.activityType,
    status: activity.status,
    subject: activity.subject,
    content: activity.content,
    activityDate: activity.activityDate.toISOString(),
    scheduledTime: activity.scheduledTime?.toISOString() || null,
    responseReceived: activity.responseReceived?.toISOString() || null,
    responseContent: activity.responseContent,
    notes: activity.notes,
    createdAt: activity.createdAt.toISOString(),
    updatedAt: activity.updatedAt.toISOString(),
  };
}

export default withErrorHandler(withAuth(handler));

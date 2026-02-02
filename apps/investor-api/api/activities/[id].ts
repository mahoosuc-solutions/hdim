/**
 * /api/activities/:id
 *
 * GET    - Get a single activity
 * PUT    - Update an activity
 * DELETE - Delete an activity
 */

import type { VercelResponse } from '@vercel/node';
import prisma from '../../lib/db';
import {
  withAuth,
  withErrorHandler,
  sendSuccess,
  sendError,
  getQueryString,
} from '../../lib/middleware';
import type {
  AuthenticatedRequest,
  UpdateActivityRequest,
  ActivityResponse,
} from '../../lib/types';

async function handler(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const id = getQueryString(req.query.id);

  if (!id) {
    sendError(res, 'Activity ID is required', 400, 'VALIDATION_ERROR');
    return;
  }

  switch (req.method) {
    case 'GET':
      await getActivity(id, res);
      break;
    case 'PUT':
      await updateActivity(id, req, res);
      break;
    case 'DELETE':
      await deleteActivity(id, res);
      break;
    default:
      sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
  }
}

/**
 * GET /api/activities/:id
 */
async function getActivity(id: string, res: VercelResponse): Promise<void> {
  const activity = await prisma.outreachActivity.findUnique({
    where: { id },
    include: {
      contact: {
        select: { name: true },
      },
    },
  });

  if (!activity) {
    sendError(res, 'Activity not found', 404, 'NOT_FOUND');
    return;
  }

  sendSuccess(res, formatActivityResponse(activity, activity.contact?.name));
}

/**
 * PUT /api/activities/:id
 * Update an activity
 */
async function updateActivity(
  id: string,
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const data = req.body as UpdateActivityRequest;

  // Check if activity exists
  const existing = await prisma.outreachActivity.findUnique({ where: { id } });
  if (!existing) {
    sendError(res, 'Activity not found', 404, 'NOT_FOUND');
    return;
  }

  // Build update data
  const updateData: Record<string, unknown> = {};

  if (data.activityType !== undefined) {
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
    updateData.activityType = data.activityType;
  }

  if (data.status !== undefined) {
    const validStatuses = ['pending', 'sent', 'responded', 'completed', 'no_response'];
    if (!validStatuses.includes(data.status)) {
      sendError(
        res,
        `Invalid status. Must be one of: ${validStatuses.join(', ')}`,
        400,
        'VALIDATION_ERROR'
      );
      return;
    }
    updateData.status = data.status;

    // Set responseReceived when status is 'responded'
    if (data.status === 'responded' && !existing.responseReceived) {
      updateData.responseReceived = new Date();
    }
  }

  if (data.subject !== undefined) updateData.subject = data.subject;
  if (data.content !== undefined) updateData.content = data.content;
  if (data.activityDate !== undefined) {
    updateData.activityDate = new Date(data.activityDate);
  }
  if (data.scheduledTime !== undefined) {
    updateData.scheduledTime = data.scheduledTime
      ? new Date(data.scheduledTime)
      : null;
  }
  if (data.responseContent !== undefined) {
    updateData.responseContent = data.responseContent;
  }
  if (data.notes !== undefined) updateData.notes = data.notes;

  const activity = await prisma.outreachActivity.update({
    where: { id },
    data: updateData,
    include: {
      contact: {
        select: { name: true },
      },
    },
  });

  sendSuccess(res, formatActivityResponse(activity, activity.contact?.name));
}

/**
 * DELETE /api/activities/:id
 */
async function deleteActivity(id: string, res: VercelResponse): Promise<void> {
  // Check if activity exists
  const existing = await prisma.outreachActivity.findUnique({ where: { id } });
  if (!existing) {
    sendError(res, 'Activity not found', 404, 'NOT_FOUND');
    return;
  }

  await prisma.outreachActivity.delete({ where: { id } });

  res.status(204).end();
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

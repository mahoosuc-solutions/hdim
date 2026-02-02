/**
 * /api/contacts/:id
 *
 * GET    - Get a single contact with activities
 * PUT    - Update a contact
 * PATCH  - Update contact status
 * DELETE - Delete a contact
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
  UpdateContactRequest,
  ContactResponse,
} from '../../lib/types';

async function handler(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const id = getQueryString(req.query.id);

  if (!id) {
    sendError(res, 'Contact ID is required', 400, 'VALIDATION_ERROR');
    return;
  }

  switch (req.method) {
    case 'GET':
      await getContact(id, res);
      break;
    case 'PUT':
      await updateContact(id, req, res);
      break;
    case 'PATCH':
      await patchContactStatus(id, req, res);
      break;
    case 'DELETE':
      await deleteContact(id, res);
      break;
    default:
      sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
  }
}

/**
 * GET /api/contacts/:id
 * Returns contact with recent activities
 */
async function getContact(id: string, res: VercelResponse): Promise<void> {
  const contact = await prisma.contact.findUnique({
    where: { id },
    include: {
      activities: {
        orderBy: { activityDate: 'desc' },
        take: 10,
      },
    },
  });

  if (!contact) {
    sendError(res, 'Contact not found', 404, 'NOT_FOUND');
    return;
  }

  const response = {
    ...formatContactResponse(contact),
    activities: contact.activities.map((a) => ({
      id: a.id,
      activityType: a.activityType,
      status: a.status,
      subject: a.subject,
      activityDate: a.activityDate.toISOString(),
      notes: a.notes,
    })),
  };

  sendSuccess(res, response);
}

/**
 * PUT /api/contacts/:id
 * Full update of a contact
 */
async function updateContact(
  id: string,
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const data = req.body as UpdateContactRequest;

  // Check if contact exists
  const existing = await prisma.contact.findUnique({ where: { id } });
  if (!existing) {
    sendError(res, 'Contact not found', 404, 'NOT_FOUND');
    return;
  }

  // Build update data
  const updateData: Record<string, unknown> = {};

  if (data.name !== undefined) updateData.name = data.name;
  if (data.title !== undefined) updateData.title = data.title;
  if (data.organization !== undefined) updateData.organization = data.organization;
  if (data.email !== undefined) updateData.email = data.email;
  if (data.phone !== undefined) updateData.phone = data.phone;
  if (data.linkedInUrl !== undefined) updateData.linkedinUrl = data.linkedInUrl;
  if (data.category !== undefined) updateData.category = data.category;
  if (data.status !== undefined) {
    updateData.status = data.status;
    // Update lastContacted when status changes to contacted or beyond
    if (data.status !== 'identified') {
      updateData.lastContacted = new Date();
    }
  }
  if (data.tier !== undefined) updateData.tier = data.tier;
  if (data.notes !== undefined) updateData.notes = data.notes;
  if (data.nextFollowUp !== undefined) {
    updateData.nextFollowUp = data.nextFollowUp ? new Date(data.nextFollowUp) : null;
  }

  const contact = await prisma.contact.update({
    where: { id },
    data: updateData,
  });

  sendSuccess(res, formatContactResponse(contact));
}

/**
 * PATCH /api/contacts/:id/status
 * Update only the status field
 * Query param: status
 */
async function patchContactStatus(
  id: string,
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const status = getQueryString(req.query.status) || req.body?.status;

  if (!status) {
    sendError(res, 'Status is required', 400, 'VALIDATION_ERROR');
    return;
  }

  const validStatuses = [
    'identified',
    'contacted',
    'engaged',
    'meeting_scheduled',
    'follow_up',
    'declined',
    'invested',
  ];
  if (!validStatuses.includes(status)) {
    sendError(
      res,
      `Invalid status. Must be one of: ${validStatuses.join(', ')}`,
      400,
      'VALIDATION_ERROR'
    );
    return;
  }

  // Check if contact exists
  const existing = await prisma.contact.findUnique({ where: { id } });
  if (!existing) {
    sendError(res, 'Contact not found', 404, 'NOT_FOUND');
    return;
  }

  const updateData: Record<string, unknown> = { status };

  // Update lastContacted when status changes
  if (status !== 'identified') {
    updateData.lastContacted = new Date();
  }

  const contact = await prisma.contact.update({
    where: { id },
    data: updateData,
  });

  sendSuccess(res, formatContactResponse(contact));
}

/**
 * DELETE /api/contacts/:id
 * Also deletes all associated activities (cascade)
 */
async function deleteContact(id: string, res: VercelResponse): Promise<void> {
  // Check if contact exists
  const existing = await prisma.contact.findUnique({ where: { id } });
  if (!existing) {
    sendError(res, 'Contact not found', 404, 'NOT_FOUND');
    return;
  }

  await prisma.contact.delete({ where: { id } });

  res.status(204).end();
}

/**
 * Format contact for API response.
 */
function formatContactResponse(contact: any): ContactResponse {
  return {
    id: contact.id,
    name: contact.name,
    title: contact.title,
    organization: contact.organization,
    email: contact.email,
    phone: contact.phone,
    linkedInUrl: contact.linkedinUrl,
    category: contact.category,
    status: contact.status,
    tier: contact.tier,
    notes: contact.notes,
    lastContacted: contact.lastContacted?.toISOString() || null,
    nextFollowUp: contact.nextFollowUp?.toISOString() || null,
    createdAt: contact.createdAt.toISOString(),
    updatedAt: contact.updatedAt.toISOString(),
  };
}

export default withErrorHandler(withAuth(handler));

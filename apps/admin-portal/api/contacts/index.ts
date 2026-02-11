/**
 * /api/contacts
 *
 * GET  - List all contacts (with optional filters)
 * POST - Create a new contact
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
  CreateContactRequest,
  ContactResponse,
} from '../../lib/types';

async function handler(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  switch (req.method) {
    case 'GET':
      await listContacts(req, res);
      break;
    case 'POST':
      await createContact(req, res);
      break;
    default:
      sendError(res, 'Method not allowed', 405, 'METHOD_NOT_ALLOWED');
  }
}

/**
 * GET /api/contacts
 * Query params: category, status, tier, search
 */
async function listContacts(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const category = getQueryString(req.query.category);
  const status = getQueryString(req.query.status);
  const tier = getQueryString(req.query.tier);
  const search = getQueryString(req.query.search);

  // Build where clause
  const where: Record<string, unknown> = {};
  if (category) where.category = category;
  if (status) where.status = status;
  if (tier) where.tier = tier;

  // Add search filter
  if (search) {
    where.OR = [
      { name: { contains: search, mode: 'insensitive' } },
      { organization: { contains: search, mode: 'insensitive' } },
      { email: { contains: search, mode: 'insensitive' } },
    ];
  }

  const contacts = await prisma.contact.findMany({
    where,
    orderBy: [{ tier: 'asc' }, { name: 'asc' }],
  });

  const response: ContactResponse[] = contacts.map(formatContactResponse);

  sendSuccess(res, response);
}

/**
 * POST /api/contacts
 * Create a new contact
 */
async function createContact(
  req: AuthenticatedRequest,
  res: VercelResponse
): Promise<void> {
  const data = req.body as CreateContactRequest;

  // Validate required fields
  if (!data.name || !data.category) {
    sendError(res, 'name and category are required', 400, 'VALIDATION_ERROR');
    return;
  }

  // Validate category
  const validCategories = ['VC', 'Angel', 'Strategic', 'Advisor', 'Partner'];
  if (!validCategories.includes(data.category)) {
    sendError(
      res,
      `Invalid category. Must be one of: ${validCategories.join(', ')}`,
      400,
      'VALIDATION_ERROR'
    );
    return;
  }

  // Validate tier if provided
  if (data.tier && !['A', 'B', 'C'].includes(data.tier)) {
    sendError(res, 'Invalid tier. Must be A, B, or C', 400, 'VALIDATION_ERROR');
    return;
  }

  const contact = await prisma.contact.create({
    data: {
      name: data.name,
      title: data.title,
      organization: data.organization,
      email: data.email,
      phone: data.phone,
      linkedinUrl: data.linkedInUrl,
      category: data.category,
      tier: data.tier || 'B',
      notes: data.notes,
    },
  });

  sendSuccess(res, formatContactResponse(contact), 201);
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

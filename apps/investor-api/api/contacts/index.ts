/**
 * /api/contacts
 *
 * GET  - List all contacts (with optional filters)
 * POST - Create a new contact
 */

import type { VercelRequest, VercelResponse } from '@vercel/node';
import { Pool } from 'pg';
import jwt from 'jsonwebtoken';
import { parseBody } from '../../lib/parse-body';

const JWT_SECRET = (process.env.JWT_SECRET || 'dev-secret-change-in-production').trim();

const ALLOWED_ORIGINS = [
  'https://admin-portal-jet-ten.vercel.app',
  'http://localhost:4200',
  'http://localhost:3000',
];

interface JwtPayload {
  userId: string;
  email: string;
  role: string;
  type: 'access' | 'refresh';
}

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  // CORS headers
  const origin = req.headers.origin || '';
  if (ALLOWED_ORIGINS.includes(origin) || process.env.NODE_ENV === 'development') {
    res.setHeader('Access-Control-Allow-Origin', origin || '*');
  }
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type,Authorization');
  res.setHeader('Access-Control-Allow-Credentials', 'true');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }

  // Verify auth
  const authHeader = req.headers.authorization;
  if (!authHeader) {
    res.status(401).json({ message: 'Authentication required', code: 'UNAUTHORIZED' });
    return;
  }

  const parts = authHeader.split(' ');
  if (parts.length !== 2 || parts[0].toLowerCase() !== 'bearer') {
    res.status(401).json({ message: 'Invalid authorization header', code: 'INVALID_AUTH_HEADER' });
    return;
  }

  try {
    const payload = jwt.verify(parts[1], JWT_SECRET) as JwtPayload;
    if (payload.type !== 'access') {
      throw new Error('Invalid token type');
    }
  } catch {
    res.status(401).json({ message: 'Invalid token', code: 'INVALID_TOKEN' });
    return;
  }

  let pool: Pool | null = null;

  try {
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      ssl: { rejectUnauthorized: false },
      max: 3,
    });

    switch (req.method) {
      case 'GET':
        await listContacts(req, res, pool);
        break;
      case 'POST':
        await createContact(req, res, pool);
        break;
      default:
        res.status(405).json({ message: 'Method not allowed', code: 'METHOD_NOT_ALLOWED' });
    }
  } catch (error) {
    console.error('Contacts endpoint error:', error);
    res.status(500).json({ message: 'Internal server error', code: 'INTERNAL_ERROR' });
  } finally {
    if (pool) {
      await pool.end();
    }
  }
}

/**
 * GET /api/contacts
 * Query params: category, status, tier, search
 */
async function listContacts(
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  const category = req.query.category as string | undefined;
  const status = req.query.status as string | undefined;
  const tier = req.query.tier as string | undefined;
  const search = req.query.search as string | undefined;

  // Build WHERE clause dynamically
  const conditions: string[] = [];
  const params: (string | number)[] = [];
  let paramIndex = 1;

  if (category) {
    conditions.push(`category = $${paramIndex++}`);
    params.push(category);
  }
  if (status) {
    conditions.push(`status = $${paramIndex++}`);
    params.push(status);
  }
  if (tier) {
    conditions.push(`tier = $${paramIndex++}`);
    params.push(tier);
  }
  if (search) {
    conditions.push(`(name ILIKE $${paramIndex} OR organization ILIKE $${paramIndex} OR email ILIKE $${paramIndex})`);
    params.push(`%${search}%`);
    paramIndex++;
  }

  const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

  const result = await pool.query(
    `SELECT id, name, title, organization, email, phone, linkedin_url, linkedin_profile_id,
            category, status, tier, investment_thesis, notes, last_contacted, next_follow_up,
            created_at, updated_at
     FROM investor_contacts
     ${whereClause}
     ORDER BY tier ASC, name ASC`,
    params
  );

  const contacts = result.rows.map(formatContactResponse);
  res.status(200).json(contacts);
}

/**
 * POST /api/contacts
 * Create a new contact
 */
async function createContact(
  req: VercelRequest,
  res: VercelResponse,
  pool: Pool
): Promise<void> {
  const data = await parseBody(req);
  if (!data) {
    res.status(400).json({ message: 'Invalid request body', code: 'INVALID_BODY' });
    return;
  }

  // Validate required fields
  if (!data.name || !data.category) {
    res.status(400).json({
      message: 'name and category are required',
      code: 'VALIDATION_ERROR',
    });
    return;
  }

  // Validate category
  const validCategories = ['VC', 'Angel', 'Strategic', 'Advisor', 'Partner'];
  if (!validCategories.includes(data.category)) {
    res.status(400).json({
      message: `Invalid category. Must be one of: ${validCategories.join(', ')}`,
      code: 'VALIDATION_ERROR',
    });
    return;
  }

  // Validate tier if provided
  if (data.tier && !['A', 'B', 'C'].includes(data.tier)) {
    res.status(400).json({
      message: 'Invalid tier. Must be A, B, or C',
      code: 'VALIDATION_ERROR',
    });
    return;
  }

  const result = await pool.query(
    `INSERT INTO investor_contacts (id, name, title, organization, email, phone, linkedin_url, category, tier, notes, created_at, updated_at)
     VALUES (gen_random_uuid(), $1, $2, $3, $4, $5, $6, $7, $8, $9, NOW(), NOW())
     RETURNING id, name, title, organization, email, phone, linkedin_url, linkedin_profile_id,
               category, status, tier, investment_thesis, notes, last_contacted, next_follow_up,
               created_at, updated_at`,
    [
      data.name,
      data.title || null,
      data.organization || null,
      data.email || null,
      data.phone || null,
      data.linkedInUrl || null,
      data.category,
      data.tier || 'B',
      data.notes || null,
    ]
  );

  res.status(201).json(formatContactResponse(result.rows[0]));
}

/**
 * Format contact for API response (snake_case → camelCase)
 */
function formatContactResponse(contact: any): any {
  return {
    id: contact.id,
    name: contact.name,
    title: contact.title,
    organization: contact.organization,
    email: contact.email,
    phone: contact.phone,
    linkedInUrl: contact.linkedin_url,
    category: contact.category,
    status: contact.status,
    tier: contact.tier,
    investmentThesis: contact.investment_thesis,
    notes: contact.notes,
    lastContacted: contact.last_contacted?.toISOString() || null,
    nextFollowUp: contact.next_follow_up?.toISOString() || null,
    createdAt: contact.created_at?.toISOString() || new Date().toISOString(),
    updatedAt: contact.updated_at?.toISOString() || new Date().toISOString(),
  };
}

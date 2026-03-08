import { NextResponse } from 'next/server';
import { issueEvidenceToken } from '@/lib/server/evidenceGate';

type EvidenceRequestBody = {
  name: string;
  email: string;
  role: string;
  organization: string;
  useCase: string;
  packet: string;
};

function normalizeDomains(raw?: string): string[] {
  if (!raw) return [];
  return raw
    .split(',')
    .map((domain) => domain.trim().toLowerCase())
    .filter(Boolean);
}

function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function getEmailDomain(email: string): string {
  return email.split('@')[1]?.toLowerCase() ?? '';
}

export async function POST(request: Request) {
  let body: Partial<EvidenceRequestBody>;

  try {
    body = (await request.json()) as Partial<EvidenceRequestBody>;
  } catch {
    return NextResponse.json({ ok: false, error: 'Invalid JSON body.' }, { status: 400 });
  }

  const requiredFields: Array<keyof EvidenceRequestBody> = ['name', 'email', 'role', 'organization', 'useCase', 'packet'];
  for (const field of requiredFields) {
    if (!body[field] || String(body[field]).trim().length === 0) {
      return NextResponse.json({ ok: false, error: `Missing required field: ${field}` }, { status: 400 });
    }
  }

  if (!isEmail(body.email!)) {
    return NextResponse.json({ ok: false, error: 'A valid business email is required.' }, { status: 400 });
  }

  const requestId = `evr_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const emailDomain = getEmailDomain(body.email!);
  const allowedDomains = normalizeDomains(process.env.EVIDENCE_ROOM_AUTO_APPROVE_DOMAINS);
  const isAutoApproved = allowedDomains.includes(emailDomain);

  const webhookUrl = process.env.EVIDENCE_REQUEST_WEBHOOK_URL;
  if (webhookUrl) {
    try {
      await fetch(webhookUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          requestId,
          status: isAutoApproved ? 'approved' : 'pending_manual_review',
          submittedAt: new Date().toISOString(),
          approvalConsoleUrl: `${process.env.NEXT_PUBLIC_SITE_URL ?? 'https://healthdatainmotion.com'}/resources/evidence-room/review`,
          approvalApiPath: '/api/evidence-request/approve',
          ...body,
        }),
      });
    } catch (error) {
      console.error('Failed to forward evidence request to webhook', { requestId, error });
    }
  }

  if (!isAutoApproved) {
    return NextResponse.json({
      ok: true,
      status: 'pending_review',
      requestId,
      message: 'Request received. Security review SLA is 1 business day.',
    });
  }

  const secret = process.env.EVIDENCE_ROOM_TOKEN_SECRET;
  if (!secret) {
    return NextResponse.json({
      ok: true,
      status: 'pending_review',
      requestId,
      message: 'Request received. Token secret is not configured for instant access.',
    });
  }

  const ttlHours = Number(process.env.EVIDENCE_ROOM_ACCESS_TTL_HOURS ?? '72');
  const { token, expiresAt } = issueEvidenceToken(
    {
      requestId,
      email: body.email!,
      organization: body.organization!,
      role: body.role!,
      packet: body.packet!,
    },
    secret,
    Number.isFinite(ttlHours) && ttlHours > 0 ? ttlHours : 72,
  );

  const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://healthdatainmotion.com';
  const accessUrl = `${siteUrl}/resources/evidence-room/access?token=${encodeURIComponent(token)}`;

  return NextResponse.json({
    ok: true,
    status: 'approved',
    requestId,
    accessUrl,
    expiresAt,
    message: 'Request approved. Access link generated.',
  });
}

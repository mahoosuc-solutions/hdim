import { NextResponse } from 'next/server';
import { issueEvidenceToken } from '@/lib/server/evidenceGate';

type ManualApprovalBody = {
  requestId: string;
  email: string;
  organization: string;
  role: string;
  packet: string;
  ttlHours?: number;
};

function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export async function POST(request: Request) {
  const approverKey = process.env.EVIDENCE_APPROVER_API_KEY;
  if (!approverKey) {
    return NextResponse.json({ ok: false, error: 'Approval API key not configured.' }, { status: 500 });
  }

  const providedKey = request.headers.get('x-evidence-approver-key');
  if (providedKey !== approverKey) {
    return NextResponse.json({ ok: false, error: 'Unauthorized approver key.' }, { status: 401 });
  }

  let body: Partial<ManualApprovalBody>;
  try {
    body = (await request.json()) as Partial<ManualApprovalBody>;
  } catch {
    return NextResponse.json({ ok: false, error: 'Invalid JSON body.' }, { status: 400 });
  }

  const requiredFields: Array<keyof ManualApprovalBody> = ['requestId', 'email', 'organization', 'role', 'packet'];
  for (const field of requiredFields) {
    if (!body[field] || String(body[field]).trim().length === 0) {
      return NextResponse.json({ ok: false, error: `Missing required field: ${field}` }, { status: 400 });
    }
  }

  if (!isEmail(body.email!)) {
    return NextResponse.json({ ok: false, error: 'A valid email is required.' }, { status: 400 });
  }

  const secret = process.env.EVIDENCE_ROOM_TOKEN_SECRET;
  if (!secret) {
    return NextResponse.json({ ok: false, error: 'Token secret is not configured.' }, { status: 500 });
  }

  const ttlHours = Number.isFinite(body.ttlHours) && body.ttlHours! > 0
    ? body.ttlHours!
    : Number(process.env.EVIDENCE_ROOM_ACCESS_TTL_HOURS ?? '72');

  const { token, expiresAt } = issueEvidenceToken(
    {
      requestId: body.requestId!,
      email: body.email!,
      organization: body.organization!,
      role: body.role!,
      packet: body.packet!,
    },
    secret,
    ttlHours,
  );

  const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://healthdatainmotion.com';
  const accessUrl = `${siteUrl}/resources/evidence-room/access?token=${encodeURIComponent(token)}`;

  return NextResponse.json({
    ok: true,
    status: 'approved',
    accessUrl,
    expiresAt,
  });
}

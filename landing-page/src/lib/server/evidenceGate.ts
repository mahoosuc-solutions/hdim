import crypto from 'crypto';

export type EvidenceAccessPayload = {
  requestId: string;
  email: string;
  organization: string;
  role: string;
  packet: string;
  exp: number;
};

function toBase64Url(value: string): string {
  return Buffer.from(value).toString('base64url');
}

function fromBase64Url(value: string): string {
  return Buffer.from(value, 'base64url').toString('utf8');
}

function sign(input: string, secret: string): string {
  return crypto.createHmac('sha256', secret).update(input).digest('base64url');
}

export function issueEvidenceToken(
  payload: Omit<EvidenceAccessPayload, 'exp'>,
  secret: string,
  ttlHours: number,
): { token: string; expiresAt: string } {
  const exp = Math.floor(Date.now() / 1000) + ttlHours * 60 * 60;
  const fullPayload: EvidenceAccessPayload = { ...payload, exp };
  const encodedPayload = toBase64Url(JSON.stringify(fullPayload));
  const signature = sign(encodedPayload, secret);
  return {
    token: `${encodedPayload}.${signature}`,
    expiresAt: new Date(exp * 1000).toISOString(),
  };
}

export function verifyEvidenceToken(token: string, secret: string): {
  valid: boolean;
  payload?: EvidenceAccessPayload;
  reason?: string;
} {
  const [encodedPayload, providedSignature] = token.split('.');
  if (!encodedPayload || !providedSignature) {
    return { valid: false, reason: 'Malformed token' };
  }

  const expectedSignature = sign(encodedPayload, secret);
  if (providedSignature !== expectedSignature) {
    return { valid: false, reason: 'Invalid signature' };
  }

  try {
    const payload = JSON.parse(fromBase64Url(encodedPayload)) as EvidenceAccessPayload;
    if (!payload.exp || Date.now() / 1000 > payload.exp) {
      return { valid: false, reason: 'Token expired' };
    }
    return { valid: true, payload };
  } catch {
    return { valid: false, reason: 'Invalid payload' };
  }
}

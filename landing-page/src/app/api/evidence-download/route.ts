import { NextResponse } from 'next/server';
import { verifyEvidenceToken } from '@/lib/server/evidenceGate';
import { getAssetForPacket } from '@/lib/server/evidenceAssets';

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const token = searchParams.get('token');
  const assetKey = searchParams.get('asset');

  if (!token || !assetKey) {
    return NextResponse.json({ ok: false, error: 'Missing token or asset parameter.' }, { status: 400 });
  }

  const secret = process.env.EVIDENCE_ROOM_TOKEN_SECRET;
  if (!secret) {
    return NextResponse.json({ ok: false, error: 'Evidence token secret is not configured.' }, { status: 500 });
  }

  const verified = verifyEvidenceToken(token, secret);
  if (!verified.valid || !verified.payload) {
    return NextResponse.json({ ok: false, error: verified.reason ?? 'Invalid or expired token.' }, { status: 401 });
  }

  const asset = getAssetForPacket(verified.payload.packet, assetKey);
  if (!asset) {
    return NextResponse.json({ ok: false, error: 'Requested asset not available for this packet.' }, { status: 404 });
  }

  return NextResponse.redirect(asset.url, 302);
}

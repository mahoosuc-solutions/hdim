# Evidence Room Operations Runbook

## Purpose
Operational workflow for secure evidence-room intake, manual approval, and artifact delivery for enterprise buyers.

## Required environment variables
Set these in Vercel production before enabling flows:

- `NEXT_PUBLIC_SITE_URL` = public site URL
- `EVIDENCE_REQUEST_WEBHOOK_URL` = CRM/automation endpoint for intake
- `EVIDENCE_ROOM_TOKEN_SECRET` = long random signing secret
- `EVIDENCE_ROOM_ACCESS_TTL_HOURS` = default token TTL (recommended `72`)
- `EVIDENCE_ROOM_AUTO_APPROVE_DOMAINS` = comma-separated allowlist for auto-approval
- `EVIDENCE_APPROVER_API_KEY` = key required for manual approvals

Packet asset URLs (private object storage signed or controlled links):
- `EVIDENCE_PACKET_SECURITY_SOC2_URL`
- `EVIDENCE_PACKET_SECURITY_IR_URL`
- `EVIDENCE_PACKET_RELIABILITY_SCORECARD_URL`
- `EVIDENCE_PACKET_RELIABILITY_VALIDATION_URL`
- `EVIDENCE_PACKET_PROCUREMENT_SOW_URL`
- `EVIDENCE_PACKET_PROCUREMENT_SLA_URL`

## Intake flow
1. Buyer submits request at `/resources/evidence-room`.
2. API posts payload to `EVIDENCE_REQUEST_WEBHOOK_URL`.
3. If email domain is allowlisted, request is auto-approved and access URL is returned immediately.
4. Otherwise status is `pending_review` and request is queued in CRM/workflow.

## Manual approval flow
1. Open `/resources/evidence-room/review`.
2. Enter approver key and request metadata from CRM.
3. Issue access link with explicit TTL.
4. Send link to requestor over approved channel.

Alternative API call:

```bash
curl -X POST "https://healthdatainmotion.com/api/evidence-request/approve" \
  -H "Content-Type: application/json" \
  -H "x-evidence-approver-key: $EVIDENCE_APPROVER_API_KEY" \
  -d '{
    "requestId":"evr_example123",
    "email":"buyer@healthsystem.org",
    "organization":"Example Health",
    "role":"CIO/CISO",
    "packet":"security",
    "ttlHours":72
  }'
```

## Artifact delivery
1. Approved users open tokenized access URL.
2. Access page presents packet resources plus private download links.
3. Downloads are validated through `/api/evidence-download` with token + packet-level asset authorization.

## Analytics events
Track in Plausible:

- `evidence_request_start`
- `evidence_role_selected`
- `evidence_packet_requested`
- `evidence_request_submitted`
- `evidence_access_opened`
- `evidence_request_manually_approved`
- `evidence_request_manual_approval_failed`

## Daily operating checklist
1. Review pending request queue age (SLA target: < 1 business day).
2. Process manual approvals and confirm link delivery.
3. Validate packet URLs are current and downloadable.
4. Review analytics conversion funnel by role and packet.
5. Export summary for sales leadership and investor-readiness log.

## Pre-customer or investor demo checklist
1. Execute one end-to-end request in staging.
2. Confirm webhook receipt in CRM and manual approval path.
3. Confirm each packet has at least one valid download artifact.
4. Verify link expiry behavior and invalid token rejection.
5. Capture screenshots of request, approval, access, and download steps.

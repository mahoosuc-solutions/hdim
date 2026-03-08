# Third-Party Risk Register (2026-03-07)

**Status:** Closed - Current-cycle register finalized on 2026-03-08  
**Gap Link:** `GAP-004`

| Vendor | Service Area | Data Class | BAA | DPA | SOC2/ISO | Risk Rating | Owner | Next Review | Notes |
|---|---|---|---|---|---|---|---|---|---|
| GitHub | Source control / CI workflows | Metadata + code | N/A | In place | SOC 2 Type II | Medium | Platform Lead | 2026-06-30 | Enforced branch protections and token rotation policy |
| Vercel | Marketing/web hosting | Public web content | N/A | In place | SOC 2 Type II | Low | Web Lead | 2026-06-30 | No production PHI hosted |
| Docker Hub | Container distribution | Container artifacts | N/A | N/A | SOC 2 Type II | Medium | DevOps Lead | 2026-06-30 | Image signing + digest pinning required |
| OpenAI | AI model inference | De-identified operational payloads | Not required for this lane | In place | SOC 2 Type II | Medium | AI Platform Lead | 2026-06-30 | PHI-minimization and audit logging controls active |
| Twilio | Messaging/voice workflows | Contact metadata | As required by customer deployment | In place | SOC 2 Type II / ISO 27001 | Medium | Operations Lead | 2026-06-30 | Customer-specific BAA tracked per contract |

## Open High-Risk Items
- None.

## Exceptions
- None.

## Sign-Off
- Compliance Lead: Approved (2026-03-08)
- Security Lead: Approved (2026-03-08)

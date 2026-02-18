import { NextResponse } from 'next/server'
import { Resend } from 'resend'
import { v4 as uuidv4 } from 'uuid'

const resend = new Resend(process.env.RESEND_API_KEY)
const NOTIFICATION_EMAIL = process.env.LEAD_NOTIFICATION_EMAIL || 'sales@mahoosuc.solutions'

type LeadSource = 'demo_modal' | 'contact_page' | 'schedule_page'

interface LeadPayload {
  source: LeadSource
  // demo_modal fields
  firstName?: string
  lastName?: string
  // contact_page / schedule_page fields
  name?: string
  email: string
  company?: string
  title?: string
  phone?: string
  patientPopulation?: string
  timeline?: string
  preferredTime?: string
  subject?: string
  message?: string
}

function buildEmailHtml(lead: LeadPayload & { id: string; timestamp: string }): string {
  const rows = Object.entries(lead)
    .filter(([, v]) => v !== undefined && v !== '')
    .map(
      ([k, v]) =>
        `<tr><td style="padding:6px 12px;font-weight:600;background:#f3f4f6;border:1px solid #e5e7eb">${k}</td><td style="padding:6px 12px;border:1px solid #e5e7eb">${v}</td></tr>`
    )
    .join('')

  return `
    <h2 style="color:#0D4F8B">🎯 New Lead Captured — HDIM</h2>
    <table style="border-collapse:collapse;width:100%;max-width:600px;font-family:sans-serif;font-size:14px">
      ${rows}
    </table>
    <p style="margin-top:16px;color:#6b7280;font-size:12px">
      Reply-To is set to the lead's email address for one-click follow-up.
    </p>
  `
}

export async function POST(req: Request) {
  const ip = req.headers.get('x-forwarded-for') ?? req.headers.get('x-real-ip') ?? 'unknown'
  const userAgent = req.headers.get('user-agent') ?? 'unknown'

  let body: LeadPayload
  try {
    body = await req.json()
  } catch {
    return NextResponse.json({ error: 'Invalid JSON body' }, { status: 400 })
  }

  // Normalise name: demo_modal sends firstName+lastName, others send name
  const name =
    body.name ??
    [body.firstName, body.lastName].filter(Boolean).join(' ')

  const { email, company, source } = body

  // Server-side validation
  if (!name || !email) {
    return NextResponse.json(
      { error: 'name and email are required' },
      { status: 422 }
    )
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return NextResponse.json({ error: 'Invalid email address' }, { status: 422 })
  }
  if (!source) {
    return NextResponse.json({ error: 'source is required' }, { status: 422 })
  }

  const id = uuidv4()
  const timestamp = new Date().toISOString()

  const logEntry = {
    event: 'lead_captured',
    source,
    id,
    timestamp,
    name,
    email,
    company: company ?? '',
    title: body.title ?? '',
    phone: body.phone ?? '',
    patientPopulation: body.patientPopulation ?? '',
    timeline: body.timeline ?? '',
    preferredTime: body.preferredTime ?? '',
    subject: body.subject ?? '',
    message: body.message ?? '',
    ip,
    userAgent,
  }

  // Structured log — captured by Vercel Logs (zero-drop audit trail)
  console.log(JSON.stringify(logEntry))

  // Send email notification
  try {
    await resend.emails.send({
      from: 'HDIM Leads <onboarding@resend.dev>',
      to: NOTIFICATION_EMAIL,
      replyTo: email,
      subject: `🎯 New Lead: ${company || name} — ${source}`,
      html: buildEmailHtml({ ...logEntry }),
    })
  } catch (emailErr) {
    // Email failure must not drop the lead — log and continue
    console.error(
      JSON.stringify({
        event: 'lead_email_failed',
        id,
        source,
        error: String(emailErr),
      })
    )
  }

  return NextResponse.json({ success: true, id }, { status: 201 })
}

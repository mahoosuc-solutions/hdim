import { NextResponse } from 'next/server'

const BACKEND_URL = process.env.HDIM_BACKEND_URL || 'http://localhost:8098'

/**
 * Proxy ROI PDF download to the HDIM backend payer-workflows-service.
 * GET /api/roi/pdf?id=XXX → backend GET /api/v1/payer/roi/{id}/pdf
 */
export async function GET(req: Request) {
  const url = new URL(req.url)
  const id = url.searchParams.get('id')

  if (!id) {
    return NextResponse.json({ error: 'id parameter required' }, { status: 400 })
  }

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/payer/roi/${id}/pdf`, {
      signal: AbortSignal.timeout(15000),
    })

    if (backendRes.status === 404) {
      return NextResponse.json({ error: 'ROI calculation not found' }, { status: 404 })
    }

    if (!backendRes.ok) {
      const errorText = await backendRes.text()
      console.error(JSON.stringify({
        event: 'roi_pdf_backend_error',
        status: backendRes.status,
        body: errorText.slice(0, 500),
      }))
      return NextResponse.json(
        { error: 'PDF generation failed', details: errorText },
        { status: backendRes.status }
      )
    }

    const pdfBytes = await backendRes.arrayBuffer()

    return new NextResponse(pdfBytes, {
      status: 200,
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename="roi-report-${id}.pdf"`,
        'Content-Length': String(pdfBytes.byteLength),
      },
    })
  } catch (err) {
    console.warn(JSON.stringify({
      event: 'roi_pdf_backend_unavailable',
      error: String(err),
    }))
    return NextResponse.json(
      { error: 'ROI service temporarily unavailable' },
      { status: 503 }
    )
  }
}

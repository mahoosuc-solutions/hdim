import { NextResponse } from 'next/server'

const BACKEND_URL = process.env.HDIM_BACKEND_URL || 'http://localhost:8098'

/**
 * Proxy ROI calculations to the HDIM backend payer-workflows-service.
 * Falls back to client-side calculation if backend is unavailable.
 */
export async function POST(req: Request) {
  let body: Record<string, unknown>
  try {
    body = await req.json()
  } catch {
    return NextResponse.json({ error: 'Invalid JSON body' }, { status: 400 })
  }

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/payer/roi/calculate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(10000),
    })

    if (!backendRes.ok) {
      const errorText = await backendRes.text()
      console.error(JSON.stringify({
        event: 'roi_backend_error',
        status: backendRes.status,
        body: errorText.slice(0, 500),
      }))
      return NextResponse.json(
        { error: 'ROI calculation failed', details: errorText },
        { status: backendRes.status }
      )
    }

    const data = await backendRes.json()
    return NextResponse.json(data)
  } catch (err) {
    // Backend unavailable — log and return 503
    console.warn(JSON.stringify({
      event: 'roi_backend_unavailable',
      error: String(err),
    }))
    return NextResponse.json(
      { error: 'ROI service temporarily unavailable' },
      { status: 503 }
    )
  }
}

/**
 * Retrieve a saved ROI calculation by ID.
 */
export async function GET(req: Request) {
  const url = new URL(req.url)
  const id = url.searchParams.get('id')

  if (!id) {
    return NextResponse.json({ error: 'id parameter required' }, { status: 400 })
  }

  try {
    const backendRes = await fetch(`${BACKEND_URL}/api/v1/payer/roi/${id}`, {
      signal: AbortSignal.timeout(5000),
    })

    if (!backendRes.ok) {
      return NextResponse.json(
        { error: 'Calculation not found' },
        { status: backendRes.status }
      )
    }

    const data = await backendRes.json()
    return NextResponse.json(data)
  } catch (err) {
    return NextResponse.json(
      { error: 'ROI service temporarily unavailable' },
      { status: 503 }
    )
  }
}

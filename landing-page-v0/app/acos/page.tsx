import { Metadata } from 'next'
import { SegmentPage } from '../components/SegmentPage'
import { SEGMENTS } from '../../lib/constants'

export const metadata: Metadata = {
  title: 'HDIM for ACOs - Population Health Visibility for Shared Savings',
  description: 'Real-time care gap detection and quality measurement for Accountable Care Organizations, physician groups, and IPAs.',
  alternates: { canonical: '/acos' },
}

export default function ACOsPage() {
  return <SegmentPage segment={SEGMENTS.acos} />
}

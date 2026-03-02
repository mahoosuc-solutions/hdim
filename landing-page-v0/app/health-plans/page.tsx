import { Metadata } from 'next'
import { SegmentPage } from '../components/SegmentPage'
import { SEGMENTS } from '../../lib/constants'

export const metadata: Metadata = {
  title: 'HDIM for Health Plans - Close Care Gaps Before They Cost You',
  description: 'HEDIS quality measurement, care gap detection, and revenue cycle automation for Medicare Advantage, Commercial, and Medicaid managed care plans.',
  alternates: { canonical: '/health-plans' },
}

export default function HealthPlansPage() {
  return <SegmentPage segment={SEGMENTS.healthPlans} />
}

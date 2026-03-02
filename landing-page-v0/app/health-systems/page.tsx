import { Metadata } from 'next'
import { SegmentPage } from '../components/SegmentPage'
import { SEGMENTS } from '../../lib/constants'

export const metadata: Metadata = {
  title: 'HDIM for Health Systems - Quality Measurement in Weeks, Not Years',
  description: 'FHIR-native quality measurement platform for hospitals, IDNs, and academic medical centers. 90-day deployment with Epic, Cerner, and Athena integration.',
  alternates: { canonical: '/health-systems' },
}

export default function HealthSystemsPage() {
  return <SegmentPage segment={SEGMENTS.healthSystems} />
}

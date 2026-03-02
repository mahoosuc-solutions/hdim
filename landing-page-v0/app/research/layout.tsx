import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Research & Evidence | HDIM - Healthcare Data in Motion',
  description: 'Evidence-based research on HEDIS quality measures, care gap interventions, and the ROI of real-time quality measurement in value-based care.',
  alternates: { canonical: '/research' },
}

export default function ResearchLayout({ children }: { children: React.ReactNode }) {
  return children
}

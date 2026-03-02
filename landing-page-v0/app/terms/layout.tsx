import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Terms of Service | HDIM - Healthcare Data in Motion',
  description: 'Terms of service for the HDIM healthcare quality platform.',
  alternates: { canonical: '/terms' },
}

export default function TermsLayout({ children }: { children: React.ReactNode }) {
  return children
}

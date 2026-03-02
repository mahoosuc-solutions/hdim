import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Privacy Policy | HDIM - Healthcare Data in Motion',
  description: 'HDIM privacy policy. Learn how we protect your data with HIPAA-compliant controls, encryption, and privacy-by-design architecture.',
  alternates: { canonical: '/privacy' },
}

export default function PrivacyLayout({ children }: { children: React.ReactNode }) {
  return children
}

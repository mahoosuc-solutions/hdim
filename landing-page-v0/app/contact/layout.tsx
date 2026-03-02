import { Metadata } from 'next'
import RecaptchaScript from '../components/RecaptchaScript'

export const metadata: Metadata = {
  title: 'Contact Us | HDIM - Healthcare Data in Motion',
  description: 'Get in touch with the HDIM team. Schedule a demo, ask about pricing, or learn how our FHIR-native quality platform can help your organization.',
  alternates: { canonical: '/contact' },
}

export default function ContactLayout({ children }: { children: React.ReactNode }) {
  return <>{children}<RecaptchaScript /></>
}

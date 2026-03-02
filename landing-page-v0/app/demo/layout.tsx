import { Metadata } from 'next'
import RecaptchaScript from '../components/RecaptchaScript'

export const metadata: Metadata = {
  title: 'Request a Demo | HDIM - Healthcare Data in Motion',
  description: 'See HDIM in action. Request a personalized demo of our FHIR-native quality measurement platform with live care gap detection and HEDIS measure execution.',
  alternates: { canonical: '/demo' },
}

export default function DemoLayout({ children }: { children: React.ReactNode }) {
  return <>{children}<RecaptchaScript /></>
}

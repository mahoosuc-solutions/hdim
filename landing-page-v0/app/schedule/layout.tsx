import { Metadata } from 'next'
import RecaptchaScript from '../components/RecaptchaScript'

export const metadata: Metadata = {
  title: 'Schedule a Demo | HDIM - Healthcare Data in Motion',
  description: 'Book a 30-minute demo or 60-minute deep dive to see HDIM\'s FHIR-native quality measurement platform in action. Live care gap detection, HEDIS measures, and Star Rating optimization.',
  alternates: { canonical: '/schedule' },
}

export default function ScheduleLayout({ children }: { children: React.ReactNode }) {
  return <>{children}<RecaptchaScript /></>
}

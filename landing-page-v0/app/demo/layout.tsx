import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Request a Demo | HDIM - Healthcare Data in Motion',
  description: 'See HDIM in action. Request a personalized demo of our FHIR-native quality measurement platform with live care gap detection and HEDIS measure execution.',
}

export default function DemoLayout({ children }: { children: React.ReactNode }) {
  return children
}

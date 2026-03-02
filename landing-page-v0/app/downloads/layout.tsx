import { Metadata } from 'next'
import RecaptchaScript from '../components/RecaptchaScript'

export const metadata: Metadata = {
  title: 'Downloads | HDIM - Healthcare Data in Motion',
  description: 'Download HDIM resources including product briefs, technical documentation, and integration guides for healthcare quality measurement.',
  alternates: { canonical: '/downloads' },
}

export default function DownloadsLayout({ children }: { children: React.ReactNode }) {
  return <>{children}<RecaptchaScript /></>
}

import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Downloads | HDIM - Healthcare Data in Motion',
  description: 'Download HDIM resources including product briefs, technical documentation, and integration guides for healthcare quality measurement.',
}

export default function DownloadsLayout({ children }: { children: React.ReactNode }) {
  return children
}

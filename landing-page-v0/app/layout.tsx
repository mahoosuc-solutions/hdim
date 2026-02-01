import type { Metadata } from 'next'
import localFont from 'next/font/local'
import { Analytics } from '@vercel/analytics/react'
import './globals.css'

const inter = localFont({
  variable: '--font-inter',
  display: 'swap',
  src: [
    { path: './fonts/inter/Inter-400.ttf', weight: '400', style: 'normal' },
    { path: './fonts/inter/Inter-500.ttf', weight: '500', style: 'normal' },
    { path: './fonts/inter/Inter-600.ttf', weight: '600', style: 'normal' },
    { path: './fonts/inter/Inter-700.ttf', weight: '700', style: 'normal' },
  ],
})

export const metadata: Metadata = {
  metadataBase: new URL('https://hdim-landing-page.vercel.app'),
  title: 'HDIM - FHIR-Native Healthcare Quality Platform',
  description: 'The FHIR-native platform for HEDIS and quality programs. Automate measure evaluation, detect care gaps, and improve quality performance with interoperable workflows.',
  keywords: 'HEDIS, FHIR, healthcare interoperability, care gaps, quality measures, CQL, value-based care, Star Ratings, Medicare Advantage',
  alternates: {
    canonical: '/',
  },
  openGraph: {
    title: 'HDIM - Healthcare Quality Measurement Platform',
    description: 'Automate quality measure evaluation and close care gaps with a FHIR-native platform.',
    url: 'https://hdim-landing-page.vercel.app',
    siteName: 'HDIM',
    images: [
      {
        url: '/images/social/linkedin.png',
        width: 1200,
        height: 630,
        alt: 'HDIM Healthcare Quality Platform',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'HDIM - FHIR-Native Healthcare Quality Platform',
    description: 'Automate quality measure evaluation and close care gaps with a FHIR-native platform.',
    images: ['/images/social/linkedin.png'],
  },
  robots: {
    index: true,
    follow: true,
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" className={inter.variable}>
      <body className="min-h-screen bg-white font-sans">
        {children}
        <Analytics />
      </body>
    </html>
  )
}

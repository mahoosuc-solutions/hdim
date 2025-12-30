import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import { Analytics } from '@vercel/analytics/react'
import './globals.css'

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
})

export const metadata: Metadata = {
  title: 'HDIM - Close Care Gaps 40% Faster | Healthcare Quality Platform',
  description: 'The FHIR-native platform for HEDIS excellence. Automate quality measure evaluation, detect care gaps, and improve Star Ratings with AI-powered healthcare interoperability.',
  keywords: 'HEDIS, FHIR, healthcare interoperability, care gaps, quality measures, CQL, value-based care, Star Ratings, Medicare Advantage',
  openGraph: {
    title: 'HDIM - Healthcare Quality Measurement Platform',
    description: 'Close care gaps 40% faster with AI-powered quality measure evaluation.',
    url: 'https://hdim.io',
    siteName: 'HDIM',
    images: [
      {
        url: '/og-image.png',
        width: 1200,
        height: 630,
        alt: 'HDIM Healthcare Platform',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'HDIM - Close Care Gaps 40% Faster',
    description: 'The FHIR-native platform for HEDIS excellence.',
    images: ['/og-image.png'],
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

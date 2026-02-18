import type { Metadata } from 'next'
import localFont from 'next/font/local'
import Script from 'next/script'
import { Analytics } from '@vercel/analytics/react'
import './globals.css'

const RECAPTCHA_SITE_KEY = process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY
const GA_ID = process.env.NEXT_PUBLIC_GA_MEASUREMENT_ID

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
  metadataBase: new URL('https://www.healthdatainmotion.com'),
  title: 'HDIM - FHIR-Native Healthcare Quality Platform',
  description: 'The FHIR-native platform for HEDIS and quality programs. Automate measure evaluation, detect care gaps, and improve quality performance with interoperable workflows.',
  keywords: 'HEDIS, FHIR, healthcare interoperability, care gaps, quality measures, CQL, value-based care, Star Ratings, Medicare Advantage',
  alternates: {
    canonical: '/',
  },
  openGraph: {
    title: 'HDIM - Healthcare Quality Measurement Platform',
    description: 'Automate quality measure evaluation and close care gaps with a FHIR-native platform.',
    url: 'https://www.healthdatainmotion.com',
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
        {GA_ID && (
          <>
            <Script
              src={`https://www.googletagmanager.com/gtag/js?id=${GA_ID}`}
              strategy="afterInteractive"
            />
            <Script id="ga4-init" strategy="afterInteractive">{`
              window.dataLayer = window.dataLayer || [];
              function gtag(){dataLayer.push(arguments);}
              gtag('js', new Date());
              gtag('config', '${GA_ID}');
            `}</Script>
          </>
        )}
        {RECAPTCHA_SITE_KEY && (
          <Script
            src={`https://www.google.com/recaptcha/api.js?render=${RECAPTCHA_SITE_KEY}`}
            strategy="afterInteractive"
          />
        )}
      </body>
    </html>
  )
}

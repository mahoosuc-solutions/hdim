import type { Metadata, Viewport } from 'next';
import { Inter } from 'next/font/google';
import CookieConsent from '@/components/consent/CookieConsent';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
};

export const metadata: Metadata = {
  title: 'HDIM - Healthcare Quality Measurement Platform',
  description:
    'Deploy HDIM your way: Single-node to enterprise, Epic to Athena, 52 HEDIS measures to unlimited customization. Real-time clinical insights without moving your data.',
  keywords: [
    'healthcare quality measurement',
    'HEDIS measures',
    'FHIR',
    'care gaps',
    'clinical quality',
    'EHR integration',
  ],
  authors: [{ name: 'HDIM' }],
  robots: 'index, follow',
  icons: {
    icon: [
      { url: '/favicon.ico' },
      { url: '/icon-32.png', sizes: '32x32', type: 'image/png' },
    ],
    apple: [{ url: '/apple-touch-icon.png', sizes: '180x180' }],
  },
  openGraph: {
    title: 'HDIM - Healthcare Quality Measurement Platform',
    description:
      'Deploy HDIM your way: Single-node to enterprise, Epic to Athena, 52 HEDIS measures to unlimited customization. Real-time clinical insights without moving your data.',
    images: [{ url: '/og-image.png', width: 1200, height: 630, alt: 'HDIM — Healthcare Quality Measurement Platform' }],
    type: 'website',
    siteName: 'HDIM',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'HDIM - Healthcare Quality Measurement Platform',
    description:
      'Real-time HEDIS quality measurement and care gap detection for health plans and ACOs.',
    images: ['/og-image.png'],
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <head>
        <script
          defer
          data-domain="healthdatainmotion.com"
          src="https://plausible.io/js/script.js"
        />
      </head>
      <body className={inter.className}>
        <a
          href="#main-content"
          className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-[100] focus:bg-white focus:px-4 focus:py-2 focus:rounded focus:shadow-lg focus:text-blue-600 focus:font-semibold"
        >
          Skip to main content
        </a>
        <div id="root">{children}</div>
        <CookieConsent />
      </body>
    </html>
  );
}

import type { Metadata, Viewport } from 'next';
import { Inter } from 'next/font/google';
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
    images: [{ url: '/og-image.png', width: 512, height: 512, alt: 'HDIM' }],
    type: 'website',
  },
  twitter: {
    card: 'summary',
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
      <body className={inter.className}>
        <div id="root">{children}</div>
      </body>
    </html>
  );
}

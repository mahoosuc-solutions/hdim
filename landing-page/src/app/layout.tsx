import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';

const inter = Inter({ subsets: ['latin'] });

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
  viewport: 'width=device-width, initial-scale=1',
  robots: 'index, follow',
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

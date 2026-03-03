import type { Metadata } from 'next';
import Header from '@/components/layout/Header';
import PricingPage from '@/components/pricing/PricingPage';

export const metadata: Metadata = {
  title: 'Pricing — HDIM Healthcare Quality Platform',
  description:
    'HDIM pricing: Pilot ($2,500/mo), Annual ($8,500/mo), Enterprise (custom). Typical ROI 50–150×. Go live in 4 weeks. HIPAA BAA included.',
  keywords: [
    'HEDIS quality measurement pricing',
    'healthcare quality platform cost',
    'care gap detection pricing',
    'FHIR quality measures',
    'health plan quality ROI',
  ],
  openGraph: {
    title: 'Pricing — HDIM Healthcare Quality Platform',
    description:
      'Pilot to enterprise pricing. 35–40% gap closure improvement. $1M+ quality bonus capture in Month 1.',
    type: 'website',
  },
};

export default function PricingRoute() {
  return (
    <>
      <Header />
      <PricingPage />
    </>
  );
}

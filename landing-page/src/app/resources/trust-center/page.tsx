import type { Metadata } from 'next';
import TrustCenterHub from './TrustCenterHub';

export const metadata: Metadata = {
  title: 'Trust Center | HDIM Resources',
  description:
    'Public trust center with claim-to-proof mapping, evidence freshness, and role-based diligence paths.',
  alternates: {
    canonical: 'https://hdim-himss.vercel.app/resources/trust-center',
  },
};

export default function TrustCenterPage() {
  return <TrustCenterHub />;
}

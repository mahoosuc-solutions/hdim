import type { Metadata } from 'next';
import LicensingTransparencyHub from './LicensingTransparencyHub';

export const metadata: Metadata = {
  title: 'Licensing Transparency | HDIM Resources',
  description:
    'Public licensing and compliance transparency for HDIM, including BSL release planning and controlled-content boundaries.',
  alternates: {
    canonical: 'https://hdim-himss.vercel.app/resources/licensing',
  },
};

export default function LicensingTransparencyPage() {
  return <LicensingTransparencyHub />;
}

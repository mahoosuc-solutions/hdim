import type { Metadata } from 'next';
import CioCisoHub from './CioCisoHub';

export const metadata: Metadata = {
  title: 'CIO/CISO Evaluation Path | HDIM Resources',
  description:
    'Role-based diligence path for CIO and CISO buyers reviewing security, architecture, and release governance.',
  alternates: {
    canonical: 'https://healthdatainmotion.com/resources/cio-ciso',
  },
};

export default function CioCisoPage() {
  return <CioCisoHub />;
}

import type { Metadata } from 'next';
import HimssLaunchPage from './HimssLaunchPage';

export const metadata: Metadata = {
  title: 'HIMSS 2026 — HealthData-in-Motion BSL Launch',
  description:
    'HDIM is now open source under BSL 1.1. Built by an experienced healthcare architect to fast-forward the improvement of healthcare outcomes and reduce costs.',
  alternates: {
    canonical: 'https://healthdatainmotion.com/himss',
  },
  openGraph: {
    title: 'HIMSS 2026 — HealthData-in-Motion BSL Launch',
    description:
      'Enterprise healthcare quality platform — now open source. Free to evaluate, built to partner.',
    url: 'https://healthdatainmotion.com/himss',
    siteName: 'HealthData-in-Motion',
    type: 'website',
  },
};

export default function HimssPage() {
  return <HimssLaunchPage />;
}

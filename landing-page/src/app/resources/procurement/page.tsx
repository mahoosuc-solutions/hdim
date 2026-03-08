import type { Metadata } from 'next';
import ProcurementHub from './ProcurementHub';

export const metadata: Metadata = {
  title: 'Procurement Evaluation Path | HDIM Resources',
  description:
    'Role-based diligence path for procurement stakeholders covering commercial, delivery, and evidence requirements.',
  alternates: {
    canonical: 'https://healthdatainmotion.com/resources/procurement',
  },
};

export default function ProcurementPage() {
  return <ProcurementHub />;
}

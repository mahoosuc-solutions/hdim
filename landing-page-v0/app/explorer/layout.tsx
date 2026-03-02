import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'HEDIS Measure Explorer | HDIM - Healthcare Data in Motion',
  description: 'Explore HEDIS quality measures, clinical criteria, and care gap definitions. Interactive reference for healthcare quality professionals.',
}

export default function ExplorerLayout({ children }: { children: React.ReactNode }) {
  return children
}

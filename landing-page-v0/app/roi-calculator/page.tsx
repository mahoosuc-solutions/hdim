import type { Metadata } from 'next'
import ROICalculator from '../components/ROICalculator'

export const metadata: Metadata = {
  title: 'ROI Calculator | HDIM - HealthData-in-Motion',
  description:
    'Calculate your projected quality improvement, Star Rating impact, and financial ROI with the HDIM healthcare quality platform. Free, instant results.',
  keywords: [
    'healthcare ROI calculator',
    'HEDIS quality improvement',
    'Star Rating improvement',
    'quality bonus calculator',
    'healthcare analytics ROI',
    'care gap closure value',
  ],
  openGraph: {
    title: 'ROI Calculator | HDIM - HealthData-in-Motion',
    description:
      'See your projected quality improvement and financial impact. Free ROI calculator for health plans, ACOs, and health systems.',
    type: 'website',
  },
}

export default function ROICalculatorPage() {
  return (
    <main className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
      {/* Header */}
      <div className="bg-gradient-to-r from-[#0D4F8B] to-[#0F766E] text-white py-16">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <h1 className="text-4xl md:text-5xl font-bold mb-4">
            HDIM ROI Calculator
          </h1>
          <p className="text-xl text-blue-100 max-w-2xl mx-auto">
            Estimate your quality improvement, Star Rating impact, and
            financial return with real industry benchmarks.
          </p>
        </div>
      </div>

      {/* Calculator */}
      <div className="max-w-4xl mx-auto px-4 -mt-8 pb-16">
        <ROICalculator />
      </div>

      {/* Methodology note */}
      <div className="max-w-4xl mx-auto px-4 pb-16">
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">
            Methodology
          </h2>
          <div className="text-sm text-gray-600 space-y-2">
            <p>
              Projections are based on published CMS Star Rating bonus tables,
              NCQA HEDIS benchmark data, and care gap closure rates from peer-reviewed studies.
            </p>
            <p>
              The quality improvement model uses organization-type-specific base improvement
              rates calibrated against industry benchmarks. Star Rating improvements are
              projected using the CMS 5-star quality bonus payment methodology.
            </p>
            <p>
              Financial projections use a standard 8% discount rate for NPV calculations.
              Actual results may vary based on patient population characteristics,
              current operational maturity, and implementation engagement.
            </p>
          </div>
        </div>
      </div>
    </main>
  )
}

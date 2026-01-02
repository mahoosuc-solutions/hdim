'use client'

import { useState, useEffect } from 'react'
import { Calculator, DollarSign, TrendingUp, Clock, Star } from 'lucide-react'

interface ROIInputs {
  orgType: 'ACO' | 'Health System' | 'HIE' | 'Payer' | 'FQHC'
  patientPopulation: number
  currentQualityScore: number
  currentStarRating: number
  manualReportingHours: number
}

interface ROIResults {
  qualityImprovement: number
  projectedScore: number
  starImprovement: number
  projectedStarRating: number
  qualityBonuses: number
  adminSavings: number
  gapClosureValue: number
  totalYear1Value: number
  year1Investment: number
  year1ROI: number
  paybackDays: number
  threeYearNPV: number
}

const ORG_TYPES = [
  { value: 'ACO', label: 'ACO / MSSP' },
  { value: 'Health System', label: 'Health System' },
  { value: 'Payer', label: 'Health Plan / Payer' },
  { value: 'FQHC', label: 'FQHC / Community Health' },
  { value: 'HIE', label: 'Health Information Exchange' },
]

const BASE_IMPROVEMENT: Record<string, number> = {
  'ACO': 0.25,
  'Health System': 0.23,
  'HIE': 0.20,
  'Payer': 0.28,
  'FQHC': 0.22
}

export default function ROICalculator() {
  const [inputs, setInputs] = useState<ROIInputs>({
    orgType: 'ACO',
    patientPopulation: 25000,
    currentQualityScore: 70,
    currentStarRating: 3.5,
    manualReportingHours: 40,
  })

  const [results, setResults] = useState<ROIResults | null>(null)
  const [showResults, setShowResults] = useState(false)

  const calculateROI = () => {
    const { orgType, patientPopulation, currentQualityScore, currentStarRating, manualReportingHours } = inputs

    // Quality improvement calculation
    const baseImprovement = BASE_IMPROVEMENT[orgType]
    const baselineGapFactor = (100 - currentQualityScore) / 30
    const projectedImprovement = baseImprovement * baselineGapFactor
    const projectedScore = Math.min(currentQualityScore * (1 + projectedImprovement), 95)
    const qualityImprovement = projectedScore - currentQualityScore

    // Star rating improvement
    const starImprovement = (qualityImprovement / 10) * 0.5
    const projectedStarRating = Math.min(Math.round((currentStarRating + starImprovement) * 2) / 2, 5.0)

    // Star bonus calculation (for MA plans)
    const starBonusPerMember: Record<number, number> = {
      2.0: 0, 2.5: 0, 3.0: 0, 3.5: 0,
      4.0: 850, 4.5: 1100, 5.0: 1350
    }
    const currentBonus = starBonusPerMember[Math.round(currentStarRating * 2) / 2] || 0
    const projectedBonus = starBonusPerMember[projectedStarRating] || 0
    const qualityBonuses = (projectedBonus - currentBonus) * patientPopulation * 0.3 // 30% MA attribution

    // Shared savings calculation
    const sharedSavingsPerPoint = patientPopulation < 10000 ? 25000 :
                                   patientPopulation < 50000 ? 75000 : 150000
    const sharedSavings = qualityImprovement * sharedSavingsPerPoint

    // Admin savings (67% reduction in manual effort)
    const hourlyRate = 75
    const hoursReduction = manualReportingHours * 0.67
    const adminSavings = hoursReduction * hourlyRate * 12

    // Care gap closure value
    const gapsPerPatient = 0.3
    const closureRateImprovement = 0.35
    const avgGapValue = 105 // weighted average
    const gapClosureValue = patientPopulation * gapsPerPatient * closureRateImprovement * avgGapValue

    // Total value and investment
    const totalYear1Value = qualityBonuses + sharedSavings + adminSavings + gapClosureValue

    // Investment calculation
    let baseFee = 24000
    if (patientPopulation > 100000) baseFee = 60000
    else if (patientPopulation > 50000) baseFee = 48000
    else if (patientPopulation > 20000) baseFee = 36000

    const year1Investment = baseFee

    // ROI metrics
    const year1ROI = year1Investment > 0 ? ((totalYear1Value - year1Investment) / year1Investment) * 100 : 0
    const paybackDays = totalYear1Value > 0 ? (year1Investment / totalYear1Value) * 365 : 0

    // 3-year NPV (8% discount rate)
    const year2Value = totalYear1Value * 1.1
    const year3Value = totalYear1Value * 1.2
    const year2Investment = baseFee * 0.6
    const year3Investment = baseFee * 0.6
    const discountRate = 0.08
    const threeYearNPV =
      (totalYear1Value - year1Investment) / (1 + discountRate) +
      (year2Value - year2Investment) / Math.pow(1 + discountRate, 2) +
      (year3Value - year3Investment) / Math.pow(1 + discountRate, 3)

    setResults({
      qualityImprovement: Math.round(qualityImprovement * 10) / 10,
      projectedScore: Math.round(projectedScore * 10) / 10,
      starImprovement: Math.round(starImprovement * 10) / 10,
      projectedStarRating,
      qualityBonuses: Math.round(qualityBonuses + sharedSavings),
      adminSavings: Math.round(adminSavings),
      gapClosureValue: Math.round(gapClosureValue),
      totalYear1Value: Math.round(totalYear1Value),
      year1Investment: Math.round(year1Investment),
      year1ROI: Math.round(year1ROI),
      paybackDays: Math.round(paybackDays),
      threeYearNPV: Math.round(threeYearNPV),
    })
    setShowResults(true)

    // Track event
    if (typeof window !== 'undefined' && (window as any).gtag) {
      (window as any).gtag('event', 'roi_calculation', {
        org_type: orgType,
        patient_population: patientPopulation,
        quality_score: currentQualityScore,
      })
    }
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value)
  }

  const formatNumber = (value: number) => {
    return new Intl.NumberFormat('en-US').format(value)
  }

  return (
    <div className="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
      {/* Header */}
      <div className="bg-gradient-to-r from-[#0D4F8B] to-[#0F766E] p-6 text-white">
        <div className="flex items-center mb-2">
          <Calculator className="mr-3" size={28} />
          <h3 className="text-2xl font-bold">ROI Calculator</h3>
        </div>
        <p className="text-blue-100">
          Estimate your quality improvement and financial impact with HDIM
        </p>
      </div>

      <div className="p-6 md:p-8">
        {/* Input Form */}
        <div className="grid md:grid-cols-2 gap-6 mb-8">
          {/* Organization Type */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Organization Type
            </label>
            <select
              value={inputs.orgType}
              onChange={(e) => setInputs({ ...inputs, orgType: e.target.value as ROIInputs['orgType'] })}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#0D4F8B] focus:border-transparent"
            >
              {ORG_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>
          </div>

          {/* Patient Population */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Patient Population
            </label>
            <input
              type="number"
              value={inputs.patientPopulation}
              onChange={(e) => setInputs({ ...inputs, patientPopulation: parseInt(e.target.value) || 0 })}
              min="1000"
              max="1000000"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#0D4F8B] focus:border-transparent"
            />
            <p className="text-xs text-gray-500 mt-1">
              {formatNumber(inputs.patientPopulation)} members
            </p>
          </div>

          {/* Current Quality Score */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Current HEDIS Composite Score
            </label>
            <input
              type="range"
              value={inputs.currentQualityScore}
              onChange={(e) => setInputs({ ...inputs, currentQualityScore: parseInt(e.target.value) })}
              min="40"
              max="90"
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
            />
            <div className="flex justify-between text-sm text-gray-600 mt-1">
              <span>40%</span>
              <span className="font-semibold text-[#0D4F8B]">{inputs.currentQualityScore}%</span>
              <span>90%</span>
            </div>
          </div>

          {/* Current Star Rating */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Current Star Rating (Medicare)
            </label>
            <select
              value={inputs.currentStarRating}
              onChange={(e) => setInputs({ ...inputs, currentStarRating: parseFloat(e.target.value) })}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#0D4F8B] focus:border-transparent"
            >
              {[2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0].map((rating) => (
                <option key={rating} value={rating}>
                  {rating} Stars
                </option>
              ))}
            </select>
          </div>

          {/* Manual Reporting Hours */}
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Monthly Manual Reporting Hours
            </label>
            <input
              type="range"
              value={inputs.manualReportingHours}
              onChange={(e) => setInputs({ ...inputs, manualReportingHours: parseInt(e.target.value) })}
              min="10"
              max="200"
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
            />
            <div className="flex justify-between text-sm text-gray-600 mt-1">
              <span>10 hrs</span>
              <span className="font-semibold text-[#0D4F8B]">{inputs.manualReportingHours} hours/month</span>
              <span>200 hrs</span>
            </div>
          </div>
        </div>

        {/* Calculate Button */}
        <button
          onClick={calculateROI}
          className="w-full bg-[#0D4F8B] text-white py-4 rounded-lg font-semibold text-lg hover:bg-[#0A3D6E] transition flex items-center justify-center"
        >
          <Calculator className="mr-2" size={20} />
          Calculate Your ROI
        </button>

        {/* Results */}
        {showResults && results && (
          <div className="mt-8 pt-8 border-t border-gray-200">
            <h4 className="text-xl font-bold text-gray-900 mb-6">Your Projected Results</h4>

            {/* Quality Improvement */}
            <div className="grid md:grid-cols-2 gap-4 mb-6">
              <div className="bg-blue-50 p-4 rounded-xl">
                <div className="flex items-center mb-2">
                  <TrendingUp className="text-[#0D4F8B] mr-2" size={20} />
                  <span className="text-sm font-medium text-gray-600">Quality Score Improvement</span>
                </div>
                <div className="text-3xl font-bold text-[#0D4F8B]">
                  +{results.qualityImprovement} pts
                </div>
                <p className="text-sm text-gray-600">
                  {inputs.currentQualityScore}% → {results.projectedScore}%
                </p>
              </div>

              <div className="bg-teal-50 p-4 rounded-xl">
                <div className="flex items-center mb-2">
                  <Star className="text-[#0F766E] mr-2" size={20} />
                  <span className="text-sm font-medium text-gray-600">Star Rating Impact</span>
                </div>
                <div className="text-3xl font-bold text-[#0F766E]">
                  +{results.starImprovement} Stars
                </div>
                <p className="text-sm text-gray-600">
                  {inputs.currentStarRating} → {results.projectedStarRating} Stars
                </p>
              </div>
            </div>

            {/* Financial Impact */}
            <div className="bg-gray-50 p-6 rounded-xl mb-6">
              <h5 className="text-lg font-semibold text-gray-900 mb-4">Year 1 Financial Impact</h5>
              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-gray-600">Quality Bonuses & Shared Savings</span>
                  <span className="font-semibold text-green-600">{formatCurrency(results.qualityBonuses)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Administrative Savings</span>
                  <span className="font-semibold text-green-600">{formatCurrency(results.adminSavings)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Care Gap Closure Value</span>
                  <span className="font-semibold text-green-600">{formatCurrency(results.gapClosureValue)}</span>
                </div>
                <div className="border-t pt-3 flex justify-between">
                  <span className="font-semibold text-gray-900">Total Year 1 Value</span>
                  <span className="font-bold text-xl text-[#0D4F8B]">{formatCurrency(results.totalYear1Value)}</span>
                </div>
                <div className="flex justify-between text-gray-600">
                  <span>Platform Investment</span>
                  <span>{formatCurrency(results.year1Investment)}</span>
                </div>
              </div>
            </div>

            {/* ROI Metrics */}
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-green-50 p-4 rounded-xl text-center">
                <DollarSign className="text-green-600 mx-auto mb-2" size={24} />
                <div className="text-2xl font-bold text-green-600">{results.year1ROI}%</div>
                <p className="text-sm text-gray-600">Year 1 ROI</p>
              </div>
              <div className="bg-blue-50 p-4 rounded-xl text-center">
                <Clock className="text-[#0D4F8B] mx-auto mb-2" size={24} />
                <div className="text-2xl font-bold text-[#0D4F8B]">{results.paybackDays}</div>
                <p className="text-sm text-gray-600">Days to Payback</p>
              </div>
              <div className="bg-purple-50 p-4 rounded-xl text-center">
                <TrendingUp className="text-purple-600 mx-auto mb-2" size={24} />
                <div className="text-2xl font-bold text-purple-600">{formatCurrency(results.threeYearNPV)}</div>
                <p className="text-sm text-gray-600">3-Year NPV</p>
              </div>
            </div>

            <p className="text-xs text-gray-500 mt-6 text-center">
              * Projections based on industry benchmarks. Actual results may vary based on implementation and engagement.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

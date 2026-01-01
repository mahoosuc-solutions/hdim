'use client';

import React, { useState, useMemo } from 'react';
import { ROIInputs, calculateROI, formatCurrency, ROIResults } from '@/lib/calculations';

export default function ROICalculator() {
  const [inputs, setInputs] = useState<ROIInputs>({
    organizationType: 'health-system',
    patientPopulation: 100000,
    currentFTE: 5,
    expectedQualityImprovement: 2,
    deploymentModel: 'growth',
    numberOfEHRs: 2,
  });

  const results = useMemo(() => calculateROI(inputs), [inputs]);

  const handleChange = (field: keyof ROIInputs, value: any) => {
    setInputs((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  return (
    <section id="roi" className="section bg-gradient-to-b from-white to-blue-50">
      <div className="container-lg">
        <div className="max-w-4xl mx-auto">
          {/* Header */}
          <div className="text-center mb-12">
            <h2 className="section-title">See Your Financial Impact</h2>
            <p className="section-subtitle">
              Calculate your specific ROI based on your organization type, patient volume, and improvement targets.
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-8">
            {/* Input Panel */}
            <div className="card p-8">
              <h3 className="text-2xl font-bold text-gray-900 mb-6">Your Organization</h3>

              {/* Organization Type */}
              <div className="mb-6">
                <label className="block text-sm font-semibold text-gray-900 mb-2">
                  Organization Type
                </label>
                <select
                  value={inputs.organizationType}
                  onChange={(e) => handleChange('organizationType', e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="solo-practice">Solo Practice</option>
                  <option value="health-system">Health System</option>
                  <option value="aco">ACO Network</option>
                  <option value="payer">Payer/Insurance</option>
                  <option value="other">Other</option>
                </select>
              </div>

              {/* Patient Population */}
              <div className="mb-6">
                <label className="block text-sm font-semibold text-gray-900 mb-2">
                  Patient Population
                </label>
                <div className="flex items-center gap-4">
                  <input
                    type="range"
                    min="10000"
                    max="2000000"
                    step="10000"
                    value={inputs.patientPopulation}
                    onChange={(e) => handleChange('patientPopulation', parseInt(e.target.value))}
                    className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                  />
                  <span className="text-lg font-bold text-blue-600 min-w-max">
                    {(inputs.patientPopulation / 1000).toFixed(0)}K
                  </span>
                </div>
              </div>

              {/* Current FTE */}
              <div className="mb-6">
                <label className="block text-sm font-semibold text-gray-900 mb-2">
                  Current Quality Management FTE
                </label>
                <input
                  type="number"
                  min="1"
                  max="100"
                  value={inputs.currentFTE}
                  onChange={(e) => handleChange('currentFTE', parseInt(e.target.value))}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Quality Improvement */}
              <div className="mb-6">
                <label className="block text-sm font-semibold text-gray-900 mb-2">
                  Expected Quality Improvement (points)
                </label>
                <div className="flex items-center gap-4">
                  <input
                    type="range"
                    min="0"
                    max="10"
                    step="0.5"
                    value={inputs.expectedQualityImprovement}
                    onChange={(e) => handleChange('expectedQualityImprovement', parseFloat(e.target.value))}
                    className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                  />
                  <span className="text-lg font-bold text-blue-600 min-w-max">
                    +{inputs.expectedQualityImprovement.toFixed(1)}
                  </span>
                </div>
              </div>

              {/* Deployment Model */}
              <div className="mb-6">
                <label className="block text-sm font-semibold text-gray-900 mb-2">
                  Deployment Model
                </label>
                <select
                  value={inputs.deploymentModel}
                  onChange={(e) => handleChange('deploymentModel', e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="pilot">Pilot ($500/mo)</option>
                  <option value="growth">Growth ($2,500/mo)</option>
                  <option value="enterprise">Enterprise ($7,500/mo)</option>
                  <option value="hybrid">Hybrid ($12,500/mo)</option>
                </select>
              </div>

              {/* Number of EHRs */}
              <div className="mb-6">
                <label className="block text-sm font-semibold text-gray-900 mb-2">
                  Number of EHRs / FHIR Servers
                </label>
                <input
                  type="number"
                  min="1"
                  max="20"
                  value={inputs.numberOfEHRs}
                  onChange={(e) => handleChange('numberOfEHRs', parseInt(e.target.value))}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            {/* Results Panel */}
            <div className="card p-8 bg-gradient-to-br from-blue-50 to-indigo-50 border-2 border-blue-200">
              <h3 className="text-2xl font-bold text-gray-900 mb-6">Your Year 1 Impact</h3>

              {/* Key Metrics */}
              <div className="space-y-6 mb-8">
                {/* Quality Bonus */}
                <div>
                  <p className="text-sm font-semibold text-gray-600 mb-1">Quality Bonus Revenue</p>
                  <p className="text-3xl font-bold text-green-600">
                    {formatCurrency(results.qualityBonus)}
                  </p>
                </div>

                {/* Labor Savings */}
                <div>
                  <p className="text-sm font-semibold text-gray-600 mb-1">Labor Savings</p>
                  <p className="text-3xl font-bold text-blue-600">
                    {formatCurrency(results.laborSavings)}
                  </p>
                </div>

                {/* Member Engagement */}
                {results.memberEngagementBenefit > 0 && (
                  <div>
                    <p className="text-sm font-semibold text-gray-600 mb-1">Member Engagement Value</p>
                    <p className="text-3xl font-bold text-purple-600">
                      {formatCurrency(results.memberEngagementBenefit)}
                    </p>
                  </div>
                )}

                {/* Total Cost */}
                <div className="pt-4 border-t-2 border-gray-300">
                  <p className="text-sm font-semibold text-gray-600 mb-1">Total Year 1 Cost</p>
                  <p className="text-xl font-bold text-gray-900">
                    {formatCurrency(results.totalCost)}
                  </p>
                </div>

                {/* Net ROI */}
                <div className="bg-white rounded-lg p-4 border-2 border-green-200">
                  <p className="text-sm font-semibold text-gray-600 mb-2">NET ROI</p>
                  <p className="text-4xl font-bold text-green-600 mb-2">
                    {formatCurrency(results.netROI)}
                  </p>
                  <p className="text-lg font-semibold text-green-600">
                    {results.roiPercent.toFixed(0)}% ROI
                  </p>
                </div>

                {/* Payback Period */}
                <div className="bg-blue-100 rounded-lg p-4 border-2 border-blue-300">
                  <p className="text-sm font-semibold text-gray-600 mb-2">Break-Even</p>
                  <p className="text-2xl font-bold text-blue-600">
                    {results.paybackMonths < 1
                      ? '< 1 month'
                      : `${results.paybackMonths.toFixed(1)} months`}
                  </p>
                </div>
              </div>

              {/* CTA */}
              <button className="w-full btn-primary py-3">
                Let's Talk About Your ROI
              </button>
            </div>
          </div>

          {/* Breakdown Table */}
          <div className="mt-12 card p-8">
            <h3 className="text-xl font-bold text-gray-900 mb-6">Detailed Breakdown</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <tbody>
                  <tr className="border-b">
                    <td className="py-3 text-gray-600">Labor Savings (FTE × cost × savings %)</td>
                    <td className="py-3 text-right font-semibold text-gray-900">
                      {formatCurrency(results.laborSavings)}
                    </td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3 text-gray-600">Quality Improvement Bonus</td>
                    <td className="py-3 text-right font-semibold text-gray-900">
                      {formatCurrency(results.qualityBonus)}
                    </td>
                  </tr>
                  {results.memberEngagementBenefit > 0 && (
                    <tr className="border-b">
                      <td className="py-3 text-gray-600">Member Engagement/Retention</td>
                      <td className="py-3 text-right font-semibold text-gray-900">
                        {formatCurrency(results.memberEngagementBenefit)}
                      </td>
                    </tr>
                  )}
                  <tr className="bg-blue-50">
                    <td className="py-3 font-semibold text-gray-900">Total Benefits</td>
                    <td className="py-3 text-right font-bold text-blue-600">
                      {formatCurrency(results.laborSavings + results.qualityBonus + results.memberEngagementBenefit)}
                    </td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3 text-gray-600">Implementation Cost</td>
                    <td className="py-3 text-right font-semibold text-gray-900">
                      -{formatCurrency(results.implementationCost)}
                    </td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3 text-gray-600">Year 1 Monthly Costs (12 months)</td>
                    <td className="py-3 text-right font-semibold text-gray-900">
                      -{formatCurrency(results.yearlyMonthlyCost)}
                    </td>
                  </tr>
                  <tr className="bg-green-50">
                    <td className="py-3 font-bold text-gray-900">NET ROI</td>
                    <td className="py-3 text-right font-bold text-green-600 text-lg">
                      {formatCurrency(results.netROI)}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

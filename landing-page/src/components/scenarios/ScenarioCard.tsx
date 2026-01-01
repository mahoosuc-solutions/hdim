'use client';

import React from 'react';
import { CUSTOMER_SCENARIOS } from '@/lib/constants';

interface ScenarioCardProps {
  scenarioId: string;
}

export default function ScenarioCard({ scenarioId }: ScenarioCardProps) {
  const scenario = CUSTOMER_SCENARIOS.find((s) => s.id === scenarioId);

  if (!scenario) return null;

  const roi = scenario.roi.year1;

  return (
    <div className="card p-8 h-full flex flex-col">
      {/* Header */}
      <div className="mb-6">
        <h3 className="text-xl font-bold text-gray-900 mb-2">{scenario.name}</h3>
        <p className="text-sm text-gray-600">{scenario.description}</p>
        <p className="text-sm text-blue-600 font-semibold mt-2">EHR: {scenario.ehr}</p>
      </div>

      {/* Current State */}
      <div className="mb-6 pb-6 border-b">
        <h4 className="font-semibold text-gray-900 mb-3">Current State</h4>
        <div className="space-y-2 text-sm text-gray-600">
          <p>
            <span className="font-medium text-gray-900">{scenario.currentState.patients.toLocaleString()}</span> patients
          </p>
          {scenario.currentState.manualHours && (
            <p>
              <span className="font-medium text-gray-900">{scenario.currentState.manualHours} hrs/week</span> manual work
            </p>
          )}
          {scenario.currentState.clinics && (
            <p>
              <span className="font-medium text-gray-900">{scenario.currentState.clinics}</span> clinics
            </p>
          )}
          {scenario.currentState.members && (
            <p>
              <span className="font-medium text-gray-900">{scenario.currentState.members.toLocaleString()}</span> members
            </p>
          )}
          <p>
            <span className="font-medium text-gray-900">{scenario.currentState.gap_closure}%</span> gap closure rate
          </p>
        </div>
      </div>

      {/* HDIM Outcome */}
      <div className="mb-6 pb-6 border-b">
        <h4 className="font-semibold text-gray-900 mb-3">With HDIM</h4>
        <div className="space-y-2 text-sm">
          <p>
            <span className="text-gray-600">Timeline:</span>{' '}
            <span className="font-semibold text-gray-900">{scenario.hdimOutcome.timeline}</span>
          </p>
          <p>
            <span className="text-gray-600">Deployment:</span>{' '}
            <span className="font-semibold text-gray-900">{scenario.hdimOutcome.deployment}</span>
          </p>
          <p>
            <span className="text-gray-600">Measures:</span>{' '}
            <span className="font-semibold text-gray-900">{scenario.hdimOutcome.measures}</span>
          </p>
          <p>
            <span className="text-gray-600">Benefit:</span>{' '}
            <span className="font-semibold text-gray-900">{scenario.hdimOutcome.automation}</span>
          </p>
        </div>
      </div>

      {/* ROI Highlights */}
      <div className="mb-8 pb-8 border-b">
        <h4 className="font-semibold text-gray-900 mb-4">Year 1 Financial Impact</h4>
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-green-50 rounded-lg p-3">
            <p className="text-xs text-gray-600 mb-1">Quality Bonus</p>
            <p className="text-lg font-bold text-green-600">
              ${(roi.qualityBonus / 1000000).toFixed(1)}M
            </p>
          </div>
          <div className="bg-blue-50 rounded-lg p-3">
            <p className="text-xs text-gray-600 mb-1">Labor Savings</p>
            <p className="text-lg font-bold text-blue-600">
              ${(roi.laborSavings / 1000).toFixed(0)}K
            </p>
          </div>
          <div className="bg-indigo-50 rounded-lg p-3 col-span-2">
            <p className="text-xs text-gray-600 mb-1">Payback Period</p>
            <p className="text-lg font-bold text-indigo-600">{roi.payback}</p>
          </div>
        </div>
      </div>

      {/* Key Benefits */}
      <div className="flex-1">
        <h4 className="font-semibold text-gray-900 mb-3">Key Benefits</h4>
        <ul className="space-y-2">
          {scenario.keyBenefits.map((benefit, idx) => (
            <li key={idx} className="flex items-start gap-2 text-sm">
              <svg
                className="w-4 h-4 text-green-500 flex-shrink-0 mt-0.5"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path
                  fillRule="evenodd"
                  d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                  clipRule="evenodd"
                />
              </svg>
              <span className="text-gray-700">{benefit}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* CTA */}
      <button className="w-full btn-primary mt-6 py-2 text-sm">
        Explore This Scenario
      </button>
    </div>
  );
}

'use client';

import React from 'react';

interface CaseStudyCardProps {
  organization: string;
  metric: string;
  challenge: string;
  solution: string;
  results: string[];
  quote: string;
  quoteAuthor: string;
}

export default function CaseStudyCard({
  organization,
  metric,
  challenge,
  solution,
  results,
  quote,
  quoteAuthor,
}: CaseStudyCardProps) {
  return (
    <div className="card p-8 hover:shadow-xl transition-all duration-300">
      {/* Header */}
      <div className="mb-6">
        <h3 className="text-lg font-bold text-gray-900 mb-2">{organization}</h3>
        <p className="text-2xl font-bold text-blue-600">{metric}</p>
      </div>

      {/* Challenge */}
      <div className="mb-6">
        <h4 className="font-semibold text-gray-900 mb-2">Challenge</h4>
        <p className="text-gray-600 text-sm">{challenge}</p>
      </div>

      {/* Solution */}
      <div className="mb-6">
        <h4 className="font-semibold text-gray-900 mb-2">Solution</h4>
        <p className="text-gray-600 text-sm">{solution}</p>
      </div>

      {/* Results */}
      <div className="mb-8">
        <h4 className="font-semibold text-gray-900 mb-3">Results</h4>
        <ul className="space-y-2">
          {results.map((result, idx) => (
            <li key={idx} className="flex items-start gap-2">
              <svg
                className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path
                  fillRule="evenodd"
                  d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                  clipRule="evenodd"
                />
              </svg>
              <span className="text-gray-700 text-sm font-medium">{result}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* Quote */}
      <div className="border-l-4 border-blue-600 pl-4 pt-4 border-t">
        <p className="text-gray-700 italic mb-3 text-sm">"{quote}"</p>
        <p className="text-gray-600 text-sm font-medium">— {quoteAuthor}</p>
      </div>
    </div>
  );
}

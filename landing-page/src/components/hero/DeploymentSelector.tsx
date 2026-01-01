'use client';

import React, { useState } from 'react';
import { DEPLOYMENT_MODELS } from '@/lib/constants';
import clsx from 'clsx';

interface DeploymentSelectorProps {
  onSelect?: (model: string) => void;
}

export default function DeploymentSelector({ onSelect }: DeploymentSelectorProps) {
  const [selected, setSelected] = useState('pilot');

  const handleSelect = (modelId: string) => {
    setSelected(modelId);
    onSelect?.(modelId);
  };

  const selectedModel = DEPLOYMENT_MODELS.find((m) => m.id === selected);

  return (
    <div className="w-full mt-12">
      {/* Selector Buttons */}
      <div className="flex flex-wrap gap-3 mb-8 justify-center">
        {DEPLOYMENT_MODELS.map((model) => (
          <button
            key={model.id}
            onClick={() => handleSelect(model.id)}
            className={clsx(
              'px-4 py-2 rounded-lg font-semibold transition-all',
              selected === model.id
                ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            )}
          >
            {model.name}
          </button>
        ))}
      </div>

      {/* Selected Model Details */}
      {selectedModel && (
        <div className="bg-white rounded-xl shadow-lg p-8 border border-blue-100">
          <div className="grid md:grid-cols-3 gap-8">
            {/* Left Column - Description */}
            <div className="md:col-span-1">
              <h3 className="text-2xl font-bold text-gray-900 mb-2">{selectedModel.name}</h3>
              <p className="text-gray-600 mb-6">{selectedModel.description}</p>
              <div className="space-y-4">
                <div>
                  <p className="text-sm text-gray-500 font-medium">PATIENT VOLUME</p>
                  <p className="text-lg font-semibold text-gray-900">{selectedModel.patients}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 font-medium">DEPLOYMENT TIME</p>
                  <p className="text-lg font-semibold text-gray-900">{selectedModel.timeline}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 font-medium">MONTHLY COST</p>
                  <p className="text-lg font-semibold text-blue-600">{selectedModel.cost}</p>
                </div>
              </div>
            </div>

            {/* Middle Column - Infrastructure */}
            <div className="md:col-span-1">
              <h4 className="font-bold text-gray-900 mb-4">Infrastructure</h4>
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-gray-500">Servers</p>
                  <p className="text-gray-900 font-medium">{selectedModel.infrastructure.servers}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">CPU</p>
                  <p className="text-gray-900 font-medium">{selectedModel.infrastructure.cpu}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Memory</p>
                  <p className="text-gray-900 font-medium">{selectedModel.infrastructure.memory}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Storage</p>
                  <p className="text-gray-900 font-medium">{selectedModel.infrastructure.storage}</p>
                </div>
              </div>
            </div>

            {/* Right Column - Features */}
            <div className="md:col-span-1">
              <h4 className="font-bold text-gray-900 mb-4">Included Features</h4>
              <ul className="space-y-2">
                {selectedModel.features.map((feature, idx) => (
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
                    <span className="text-gray-700 text-sm">{feature}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Use Cases */}
          <div className="mt-8 pt-8 border-t border-gray-200">
            <h4 className="font-bold text-gray-900 mb-4">Best For</h4>
            <div className="flex flex-wrap gap-2">
              {selectedModel.useCases.map((useCase, idx) => (
                <span key={idx} className="inline-block px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-sm font-medium">
                  {useCase}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

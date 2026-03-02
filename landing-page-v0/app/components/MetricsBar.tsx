'use client'

import { AnimatedCounter } from './AnimatedCounter'

const METRIC_ITEMS = [
  { value: 59, suffix: '', label: 'Microservices' },
  { value: 1171, suffix: '', label: 'Test Classes' },
  { value: 80, suffix: '+', label: 'HEDIS Measures' },
  { value: 62, suffix: '', label: 'API Endpoints' },
  { value: 2, prefix: '<', suffix: 's', label: 'Evaluation Speed' },
  { value: 90, suffix: '-day', label: 'Deployment' },
]

export function MetricsBar() {
  return (
    <section className="py-12 bg-gray-50 border-y border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-8 text-center">
          {METRIC_ITEMS.map((item) => (
            <div key={item.label}>
              <div className="text-3xl font-bold text-primary">
                <AnimatedCounter
                  end={item.value}
                  suffix={item.suffix}
                  prefix={item.prefix}
                />
              </div>
              <div className="text-sm text-gray-600 mt-1">{item.label}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

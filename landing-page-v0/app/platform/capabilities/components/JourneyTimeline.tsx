'use client'

import { useState } from 'react'
import { Clock, CheckCircle2, ChevronDown, ChevronUp, Zap, Sparkles } from 'lucide-react'
import type { PersonaJourney, PersonaId, Persona } from '../data'

const FREQUENCY_COLORS: Record<string, string> = {
  'Day 1–5': 'bg-purple-100 text-purple-700',
  'Daily AM': 'bg-blue-100 text-blue-700',
  'Daily (5 min)': 'bg-blue-100 text-blue-700',
  'Daily (10 min)': 'bg-blue-100 text-blue-700',
  Daily: 'bg-blue-100 text-blue-700',
  'Daily / Weekly': 'bg-cyan-100 text-cyan-700',
  'Per patient': 'bg-green-100 text-green-700',
  'Per visit': 'bg-green-100 text-green-700',
  'Per request': 'bg-green-100 text-green-700',
  'Per evaluation': 'bg-green-100 text-green-700',
  'As needed': 'bg-gray-100 text-gray-600',
  Weekly: 'bg-amber-100 text-amber-700',
  'Weekly (5 min)': 'bg-amber-100 text-amber-700',
  Monthly: 'bg-orange-100 text-orange-700',
  'Weekly / Monthly': 'bg-orange-100 text-orange-700',
  Quarterly: 'bg-red-100 text-red-700',
  'Quarterly / Annual': 'bg-red-100 text-red-700',
  Onboarding: 'bg-purple-100 text-purple-700',
}

interface JourneyTimelineProps {
  journey: PersonaJourney | undefined
  persona: Persona | undefined
}

export default function JourneyTimeline({ journey, persona }: JourneyTimelineProps) {
  const [expandedStage, setExpandedStage] = useState<number | null>(null)

  if (!journey || !persona) {
    return (
      <section id="journey" className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 tracking-tight">User Journey</h2>
          <p className="mt-4 text-lg text-gray-500">Select a persona above to see their daily-to-quarterly lifecycle.</p>
        </div>
      </section>
    )
  }

  return (
    <section id="journey" className="py-20 bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 tracking-tight">
            <span style={{ color: persona.color }}>{persona.title}</span> Journey
          </h2>
          <p className="mt-3 text-lg text-gray-600 max-w-2xl mx-auto">{journey.goal}</p>
        </div>

        {/* Timeline */}
        <div className="relative">
          {/* Vertical line */}
          <div className="absolute left-8 md:left-12 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary via-accent to-primary/20" />

          <div className="space-y-6">
            {journey.stages.map((stage, index) => {
              const isExpanded = expandedStage === index
              const freqColor = FREQUENCY_COLORS[stage.frequency] || 'bg-gray-100 text-gray-600'

              return (
                <div
                  key={index}
                  className="relative pl-20 md:pl-28"
                  style={{ animationDelay: `${index * 100}ms` }}
                >
                  {/* Timeline node */}
                  <div
                    className="absolute left-5 md:left-9 top-6 w-6 h-6 rounded-full border-4 border-white shadow-md flex items-center justify-center"
                    style={{ backgroundColor: persona.color }}
                  >
                    <CheckCircle2 className="w-3 h-3 text-white" />
                  </div>

                  {/* Stage card */}
                  <button
                    onClick={() => setExpandedStage(isExpanded ? null : index)}
                    className={`w-full text-left bg-white rounded-xl border transition-all duration-300 ${
                      isExpanded ? 'border-primary/30 shadow-lg' : 'border-gray-200 shadow-sm hover:shadow-md hover:border-gray-300'
                    }`}
                  >
                    <div className="p-5">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <h3 className="text-lg font-semibold text-gray-900">{stage.stage}</h3>
                          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${freqColor}`}>
                            <Clock className="w-3 h-3" />
                            {stage.frequency}
                          </span>
                        </div>
                        {isExpanded ? (
                          <ChevronUp className="w-5 h-5 text-gray-400" />
                        ) : (
                          <ChevronDown className="w-5 h-5 text-gray-400" />
                        )}
                      </div>
                      <p className="mt-2 text-sm text-gray-600">{stage.whatTheyDo}</p>
                    </div>

                    {/* Expanded details */}
                    {isExpanded && (
                      <div className="px-5 pb-5 pt-2 border-t border-gray-100 space-y-4">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                          <div className="bg-blue-50 rounded-lg p-4">
                            <p className="text-xs font-semibold text-blue-600 uppercase tracking-wider mb-1.5">Features Used</p>
                            <p className="text-sm text-blue-900">{stage.featuresUsed}</p>
                          </div>
                          <div className="bg-green-50 rounded-lg p-4">
                            <p className="text-xs font-semibold text-green-600 uppercase tracking-wider mb-1.5">Success Criteria</p>
                            <p className="text-sm text-green-900">{stage.successCriteria}</p>
                          </div>
                          <div className="bg-amber-50 rounded-lg p-4">
                            <p className="text-xs font-semibold text-amber-600 uppercase tracking-wider mb-1.5">Value Delivered</p>
                            <p className="text-sm text-amber-900">{stage.valueDelivered}</p>
                          </div>
                        </div>
                      </div>
                    )}
                  </button>
                </div>
              )
            })}
          </div>
        </div>

        {/* Differentiators */}
        {journey.differentiators.length > 0 && (
          <div className="mt-12 bg-white rounded-2xl border border-gray-200 p-8">
            <div className="flex items-center gap-2 mb-5">
              <Sparkles className="w-5 h-5 text-amber-500" />
              <h3 className="text-lg font-semibold text-gray-900">Differentiating Experience</h3>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {journey.differentiators.map((diff, i) => (
                <div key={i} className="flex items-start gap-3">
                  <Zap
                    className="w-4 h-4 mt-0.5 flex-shrink-0"
                    style={{ color: persona.color }}
                  />
                  <p className="text-sm text-gray-700">{diff}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  )
}

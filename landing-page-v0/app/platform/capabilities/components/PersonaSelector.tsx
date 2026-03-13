'use client'

import { Crown, Target, HeartHandshake, Stethoscope, UserCheck, ClipboardCheck, BarChart3, Settings, ShieldCheck } from 'lucide-react'
import type { Persona, PersonaId } from '../data'

const ICON_MAP: Record<string, React.ElementType> = {
  Crown,
  Target,
  HeartHandshake,
  Stethoscope,
  UserCheck,
  ClipboardCheck,
  BarChart3,
  Settings,
  ShieldCheck,
}

interface PersonaSelectorProps {
  personas: Persona[]
  selected: PersonaId | null
  onSelect: (id: PersonaId) => void
}

export default function PersonaSelector({ personas, selected, onSelect }: PersonaSelectorProps) {
  return (
    <section id="personas" className="py-20 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 tracking-tight">
            Choose Your <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary to-accent">Role</span>
          </h2>
          <p className="mt-4 text-lg text-gray-600 max-w-2xl mx-auto">
            HDIM delivers a purpose-built experience for every user type. Select a persona to explore their journey, features, and value.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {personas.map((persona, index) => {
            const Icon = ICON_MAP[persona.icon] || BarChart3
            const isSelected = selected === persona.id

            return (
              <button
                key={persona.id}
                onClick={() => onSelect(persona.id)}
                className={`group relative text-left p-6 rounded-2xl border-2 transition-all duration-300 ${
                  isSelected
                    ? 'border-primary bg-primary-50 shadow-lg shadow-primary/10 scale-[1.02]'
                    : 'border-gray-200 bg-white hover:border-primary/40 hover:shadow-md hover:-translate-y-0.5'
                }`}
                style={{ animationDelay: `${index * 60}ms` }}
                aria-pressed={isSelected}
              >
                {/* Selected indicator */}
                {isSelected && (
                  <div className="absolute -top-1 -right-1 w-6 h-6 bg-primary rounded-full flex items-center justify-center">
                    <svg className="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                )}

                <div className="flex items-start gap-4">
                  {/* Icon */}
                  <div
                    className={`flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center transition-colors duration-300 ${
                      isSelected ? 'bg-primary text-white' : 'bg-gray-100 text-gray-500 group-hover:bg-primary/10 group-hover:text-primary'
                    }`}
                    style={isSelected ? { backgroundColor: persona.color } : undefined}
                  >
                    <Icon className="w-6 h-6" />
                  </div>

                  {/* Content */}
                  <div className="min-w-0 flex-1">
                    <h3
                      className={`font-semibold text-base transition-colors duration-300 ${
                        isSelected ? 'text-primary-700' : 'text-gray-900'
                      }`}
                    >
                      {persona.title}
                    </h3>
                    <p className="mt-0.5 text-xs text-gray-500">{persona.titleExamples}</p>
                    <p className="mt-2 text-sm text-gray-600 line-clamp-2">{persona.description}</p>

                    {/* Role badges */}
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      {persona.systemRoles.map((role) => (
                        <span
                          key={role}
                          className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium ${
                            isSelected ? 'bg-primary/10 text-primary-700' : 'bg-gray-100 text-gray-600'
                          }`}
                        >
                          {role}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Key metric */}
                <div
                  className={`mt-4 pt-3 border-t text-xs ${
                    isSelected ? 'border-primary/20 text-primary-600' : 'border-gray-100 text-gray-500'
                  }`}
                >
                  <span className="font-medium">Key Metric:</span> {persona.keyMetric}
                </div>
              </button>
            )
          })}
        </div>
      </div>
    </section>
  )
}

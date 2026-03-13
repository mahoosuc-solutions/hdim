'use client'

import { useState, useMemo } from 'react'
import { CheckCircle2, Eye, XCircle, Filter } from 'lucide-react'
import type { FeaturePage, PersonaId, Persona, AccessLevel } from '../data'

const CATEGORY_LABELS: Record<string, string> = {
  clinical: 'Clinical',
  quality: 'Quality',
  analytics: 'Analytics',
  admin: 'Administration',
  platform: 'Platform',
}

const CATEGORY_COLORS: Record<string, string> = {
  clinical: 'bg-green-100 text-green-700',
  quality: 'bg-blue-100 text-blue-700',
  analytics: 'bg-purple-100 text-purple-700',
  admin: 'bg-gray-100 text-gray-700',
  platform: 'bg-cyan-100 text-cyan-700',
}

const ACCESS_CELLS: Record<AccessLevel, { icon: React.ReactNode; label: string; className: string }> = {
  full: {
    icon: <CheckCircle2 className="w-4 h-4" />,
    label: 'Full Access',
    className: 'bg-green-50 text-green-600',
  },
  read: {
    icon: <Eye className="w-4 h-4" />,
    label: 'Read Only',
    className: 'bg-amber-50 text-amber-600',
  },
  none: {
    icon: <XCircle className="w-3.5 h-3.5" />,
    label: 'No Access',
    className: 'bg-gray-50 text-gray-300',
  },
}

interface FeatureMatrixProps {
  features: FeaturePage[]
  personas: Persona[]
  selectedPersona: PersonaId | null
}

export default function FeatureMatrix({ features, personas, selectedPersona }: FeatureMatrixProps) {
  const [categoryFilter, setCategoryFilter] = useState<string>('all')
  const [hoveredCell, setHoveredCell] = useState<{ feature: string; persona: string } | null>(null)

  const categories = useMemo(() => {
    const cats = new Set(features.map((f) => f.category))
    return Array.from(cats)
  }, [features])

  const filteredFeatures = useMemo(() => {
    if (categoryFilter === 'all') return features
    return features.filter((f) => f.category === categoryFilter)
  }, [features, categoryFilter])

  // Short labels for persona columns
  const personaLabels: Record<PersonaId, string> = {
    cmo: 'CMO',
    'quality-director': 'Qual Dir',
    'care-coordinator': 'Care Coord',
    provider: 'Provider',
    rn: 'RN',
    ma: 'MA',
    analyst: 'Analyst',
    'it-admin': 'IT Admin',
    auditor: 'Auditor',
  }

  // Compute persona stats
  const personaStats = useMemo(() => {
    return personas.map((p) => {
      const fullCount = features.filter((f) => f.access[p.id] === 'full').length
      const readCount = features.filter((f) => f.access[p.id] === 'read').length
      return { id: p.id, full: fullCount, read: readCount, total: fullCount + readCount }
    })
  }, [personas, features])

  return (
    <section id="features" className="py-20 bg-white">
      <div className="max-w-[90rem] mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-10">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 tracking-tight">
            Feature Access <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary to-accent">Matrix</span>
          </h2>
          <p className="mt-4 text-lg text-gray-600 max-w-2xl mx-auto">
            Every feature is role-gated. Hover to see access details. {features.length} pages across {categories.length} categories.
          </p>
        </div>

        {/* Category filter */}
        <div className="flex flex-wrap items-center justify-center gap-2 mb-8">
          <Filter className="w-4 h-4 text-gray-400" />
          <button
            onClick={() => setCategoryFilter('all')}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
              categoryFilter === 'all'
                ? 'bg-primary text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            All ({features.length})
          </button>
          {categories.map((cat) => {
            const count = features.filter((f) => f.category === cat).length
            return (
              <button
                key={cat}
                onClick={() => setCategoryFilter(cat)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  categoryFilter === cat
                    ? 'bg-primary text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {CATEGORY_LABELS[cat]} ({count})
              </button>
            )
          })}
        </div>

        {/* Matrix table */}
        <div className="overflow-x-auto rounded-xl border border-gray-200 shadow-sm">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50">
                <th className="text-left px-4 py-3 font-semibold text-gray-700 sticky left-0 bg-gray-50 z-10 min-w-[200px]">
                  Feature
                </th>
                <th className="text-left px-3 py-3 font-medium text-gray-500 text-xs min-w-[80px]">
                  Category
                </th>
                {personas.map((p) => (
                  <th
                    key={p.id}
                    className={`text-center px-2 py-3 font-medium text-xs min-w-[70px] transition-colors ${
                      selectedPersona === p.id ? 'bg-primary/10 text-primary-700' : 'text-gray-500'
                    }`}
                  >
                    <div className="flex flex-col items-center gap-1">
                      <span>{personaLabels[p.id]}</span>
                      <span className="text-[10px] font-normal text-gray-400">
                        {personaStats.find((s) => s.id === p.id)?.total ?? 0} pages
                      </span>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredFeatures.map((feature) => (
                <tr
                  key={feature.name}
                  className="hover:bg-gray-50/50 transition-colors"
                >
                  <td className="px-4 py-3 font-medium text-gray-900 sticky left-0 bg-white z-10">
                    {feature.name}
                  </td>
                  <td className="px-3 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${CATEGORY_COLORS[feature.category]}`}>
                      {CATEGORY_LABELS[feature.category]}
                    </span>
                  </td>
                  {personas.map((p) => {
                    const access = feature.access[p.id]
                    const cell = ACCESS_CELLS[access]
                    const isHighlighted = selectedPersona === p.id
                    const isHovered =
                      hoveredCell?.feature === feature.name && hoveredCell?.persona === p.id

                    return (
                      <td
                        key={p.id}
                        className={`text-center px-2 py-3 transition-all duration-200 ${
                          isHighlighted ? 'bg-primary/5' : ''
                        }`}
                        onMouseEnter={() => setHoveredCell({ feature: feature.name, persona: p.id })}
                        onMouseLeave={() => setHoveredCell(null)}
                      >
                        <div
                          className={`relative inline-flex items-center justify-center w-8 h-8 rounded-lg transition-transform ${
                            cell.className
                          } ${isHovered ? 'scale-125' : ''}`}
                          title={`${personaLabels[p.id]}: ${cell.label} for ${feature.name}`}
                        >
                          {cell.icon}
                        </div>
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Legend */}
        <div className="mt-6 flex items-center justify-center gap-6 text-sm text-gray-500">
          <div className="flex items-center gap-2">
            <div className="inline-flex items-center justify-center w-6 h-6 rounded bg-green-50 text-green-600">
              <CheckCircle2 className="w-3.5 h-3.5" />
            </div>
            Full Access
          </div>
          <div className="flex items-center gap-2">
            <div className="inline-flex items-center justify-center w-6 h-6 rounded bg-amber-50 text-amber-600">
              <Eye className="w-3.5 h-3.5" />
            </div>
            Read Only
          </div>
          <div className="flex items-center gap-2">
            <div className="inline-flex items-center justify-center w-6 h-6 rounded bg-gray-50 text-gray-300">
              <XCircle className="w-3 h-3" />
            </div>
            No Access
          </div>
        </div>
      </div>
    </section>
  )
}

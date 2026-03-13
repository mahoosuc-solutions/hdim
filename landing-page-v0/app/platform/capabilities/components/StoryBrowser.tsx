'use client'

import { useState, useMemo } from 'react'
import { Search, CheckCircle2, Code2, ChevronDown, ChevronUp, Layers } from 'lucide-react'
import type { UserStory, PersonaId, Persona } from '../data'

const PHASE_LABELS: Record<string, { label: string; color: string }> = {
  daily: { label: 'Daily', color: 'bg-blue-100 text-blue-700' },
  weekly: { label: 'Weekly', color: 'bg-amber-100 text-amber-700' },
  monthly: { label: 'Monthly', color: 'bg-orange-100 text-orange-700' },
  quarterly: { label: 'Quarterly', color: 'bg-red-100 text-red-700' },
  'cross-role': { label: 'Cross-Role', color: 'bg-purple-100 text-purple-700' },
}

interface StoryBrowserProps {
  stories: UserStory[]
  personas: Persona[]
  selectedPersona: PersonaId | null
}

export default function StoryBrowser({ stories, personas, selectedPersona }: StoryBrowserProps) {
  const [searchQuery, setSearchQuery] = useState('')
  const [phaseFilter, setPhaseFilter] = useState<string>('all')
  const [expandedStory, setExpandedStory] = useState<string | null>(null)

  const phases = ['daily', 'weekly', 'monthly', 'quarterly', 'cross-role']

  const filteredStories = useMemo(() => {
    return stories.filter((story) => {
      // Persona filter
      if (selectedPersona && story.personaId !== selectedPersona && story.phase !== 'cross-role') {
        return false
      }
      // Phase filter
      if (phaseFilter !== 'all' && story.phase !== phaseFilter) {
        return false
      }
      // Search filter
      if (searchQuery) {
        const q = searchQuery.toLowerCase()
        return (
          story.story.toLowerCase().includes(q) ||
          story.id.toLowerCase().includes(q) ||
          story.role.toLowerCase().includes(q) ||
          story.component.toLowerCase().includes(q) ||
          story.acceptanceCriteria.toLowerCase().includes(q)
        )
      }
      return true
    })
  }, [stories, selectedPersona, phaseFilter, searchQuery])

  // Group stories by role
  const groupedStories = useMemo(() => {
    const groups: Record<string, UserStory[]> = {}
    for (const story of filteredStories) {
      const key = story.role
      if (!groups[key]) groups[key] = []
      groups[key].push(story)
    }
    return groups
  }, [filteredStories])

  const personaMap = useMemo(() => {
    const map: Record<string, Persona> = {}
    for (const p of personas) map[p.id] = p
    return map
  }, [personas])

  return (
    <section id="stories" className="py-20 bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-10">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 tracking-tight">
            User <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary to-accent">Stories</span>
          </h2>
          <p className="mt-4 text-lg text-gray-600 max-w-2xl mx-auto">
            {stories.length} user stories, all implemented. Every story is traced to its source component with acceptance criteria.
          </p>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-3 mb-8">
          {/* Search */}
          <div className="relative flex-1 min-w-[250px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search stories, IDs, components..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 bg-white text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary transition-shadow"
            />
          </div>

          {/* Phase filter pills */}
          <div className="flex items-center gap-1.5">
            <Layers className="w-4 h-4 text-gray-400" />
            <button
              onClick={() => setPhaseFilter('all')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                phaseFilter === 'all'
                  ? 'bg-primary text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              All
            </button>
            {phases.map((phase) => {
              const config = PHASE_LABELS[phase]
              const count = stories.filter(
                (s) =>
                  s.phase === phase &&
                  (!selectedPersona || s.personaId === selectedPersona || s.phase === 'cross-role')
              ).length
              if (count === 0) return null
              return (
                <button
                  key={phase}
                  onClick={() => setPhaseFilter(phase)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                    phaseFilter === phase
                      ? 'bg-primary text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  {config.label} ({count})
                </button>
              )
            })}
          </div>
        </div>

        {/* Results count */}
        <div className="mb-6 flex items-center gap-2 text-sm text-gray-500">
          <CheckCircle2 className="w-4 h-4 text-green-500" />
          <span>
            Showing <strong className="text-gray-900">{filteredStories.length}</strong> of{' '}
            <strong className="text-gray-900">{stories.length}</strong> stories
            {selectedPersona && (
              <>
                {' '}for{' '}
                <strong className="text-gray-900">
                  {personaMap[selectedPersona]?.title ?? selectedPersona}
                </strong>
              </>
            )}
          </span>
        </div>

        {/* Grouped stories */}
        <div className="space-y-8">
          {Object.entries(groupedStories).map(([role, roleStories]) => (
            <div key={role}>
              <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">
                {role} ({roleStories.length})
              </h3>
              <div className="space-y-2">
                {roleStories.map((story) => {
                  const isExpanded = expandedStory === story.id
                  const phaseConfig = PHASE_LABELS[story.phase]
                  const persona = personaMap[story.personaId]

                  return (
                    <div
                      key={story.id}
                      className={`bg-white rounded-xl border transition-all duration-200 ${
                        isExpanded
                          ? 'border-primary/30 shadow-md'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                    >
                      <button
                        onClick={() => setExpandedStory(isExpanded ? null : story.id)}
                        className="w-full text-left px-5 py-4 flex items-center gap-3"
                      >
                        {/* Status badge */}
                        <CheckCircle2 className="w-5 h-5 text-green-500 flex-shrink-0" />

                        {/* Story content */}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="text-xs font-mono text-gray-400">{story.id}</span>
                            <span className={`inline-flex px-2 py-0.5 rounded text-[10px] font-medium ${phaseConfig.color}`}>
                              {phaseConfig.label}
                            </span>
                          </div>
                          <p className="mt-1 text-sm text-gray-900">
                            As a <strong>{story.role}</strong>, I want to {story.story}
                          </p>
                        </div>

                        {/* Expand toggle */}
                        {isExpanded ? (
                          <ChevronUp className="w-4 h-4 text-gray-400 flex-shrink-0" />
                        ) : (
                          <ChevronDown className="w-4 h-4 text-gray-400 flex-shrink-0" />
                        )}
                      </button>

                      {/* Expanded details */}
                      {isExpanded && (
                        <div className="px-5 pb-5 pt-1 border-t border-gray-100">
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3">
                            <div className="bg-green-50 rounded-lg p-4">
                              <p className="text-xs font-semibold text-green-600 uppercase tracking-wider mb-2">
                                Acceptance Criteria
                              </p>
                              <p className="text-sm text-green-900">{story.acceptanceCriteria}</p>
                            </div>
                            <div className="bg-gray-50 rounded-lg p-4">
                              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                                <Code2 className="w-3 h-3 inline-block mr-1" />
                                Implementing Component
                              </p>
                              <p className="text-sm text-gray-800 font-mono">{story.component}</p>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>
          ))}
        </div>

        {filteredStories.length === 0 && (
          <div className="text-center py-16">
            <Search className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">No stories match your filters. Try adjusting the search or persona selection.</p>
          </div>
        )}
      </div>
    </section>
  )
}

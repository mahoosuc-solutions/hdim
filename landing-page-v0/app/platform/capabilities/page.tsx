'use client'

import { useState, useRef, useEffect } from 'react'
import Link from 'next/link'
import {
  CheckCircle2,
  Users,
  Layers,
  Database,
  ArrowRight,
  ChevronDown,
  TrendingUp,
  ArrowUpRight,
} from 'lucide-react'
import {
  PERSONAS,
  JOURNEYS,
  FEATURE_PAGES,
  USER_STORIES,
  VALUE_METRICS,
  PLATFORM_STATS,
  DIFFERENTIATORS,
} from './data'
import type { PersonaId } from './data'
import PersonaSelector from './components/PersonaSelector'
import JourneyTimeline from './components/JourneyTimeline'
import FeatureMatrix from './components/FeatureMatrix'
import StoryBrowser from './components/StoryBrowser'

// ── Animated Counter Hook ────────────────────────────────────────────────────

function useCounter(target: number, duration = 1500) {
  const [count, setCount] = useState(0)
  const [hasStarted, setHasStarted] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const node = ref.current
    if (!node) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !hasStarted) {
          setHasStarted(true)
        }
      },
      { threshold: 0.3 }
    )

    observer.observe(node)
    return () => observer.disconnect()
  }, [hasStarted])

  useEffect(() => {
    if (!hasStarted) return
    let raf: number
    const start = performance.now()

    function step(now: number) {
      const elapsed = now - start
      const progress = Math.min(elapsed / duration, 1)
      // Ease out cubic
      const eased = 1 - Math.pow(1 - progress, 3)
      setCount(Math.round(eased * target))
      if (progress < 1) raf = requestAnimationFrame(step)
    }

    raf = requestAnimationFrame(step)
    return () => cancelAnimationFrame(raf)
  }, [hasStarted, target, duration])

  return { count, ref }
}

// ── Stat Card ────────────────────────────────────────────────────────────────

function StatCard({
  value,
  label,
  icon: Icon,
  suffix = '',
}: {
  value: number
  label: string
  icon: React.ElementType
  suffix?: string
}) {
  const { count, ref } = useCounter(value)

  return (
    <div
      ref={ref}
      className="relative bg-white/10 backdrop-blur-sm rounded-2xl border border-white/20 p-6 text-center group hover:bg-white/15 transition-colors duration-300"
    >
      <Icon className="w-6 h-6 text-white/60 mx-auto mb-3" />
      <div className="text-4xl md:text-5xl font-bold text-white tabular-nums">
        {count}
        {suffix}
      </div>
      <div className="mt-2 text-sm text-white/70 font-medium">{label}</div>
    </div>
  )
}

// ── Value Comparison ────────────────────────────────────────────────────────

function ValueComparison({ selectedPersona }: { selectedPersona: PersonaId | null }) {
  const metrics = selectedPersona
    ? VALUE_METRICS.filter((m) => m.personaId === selectedPersona)
    : VALUE_METRICS

  return (
    <section id="value" className="py-20 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 tracking-tight">
            Quantified <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary to-accent">Impact</span>
          </h2>
          <p className="mt-4 text-lg text-gray-600 max-w-2xl mx-auto">
            What changes for each role when they move from legacy workflows to HDIM.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {metrics.map((m) => {
            const persona = PERSONAS.find((p) => p.id === m.personaId)
            return (
              <div
                key={m.personaId}
                className="group bg-gradient-to-br from-gray-50 to-white rounded-2xl border border-gray-200 p-6 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-300"
              >
                <div className="flex items-center gap-2 mb-4">
                  <div
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: persona?.color }}
                  />
                  <h3 className="font-semibold text-gray-900 text-sm">{persona?.title}</h3>
                </div>

                {/* Before / After */}
                <div className="space-y-3">
                  <div className="bg-red-50 rounded-lg p-3">
                    <p className="text-[10px] uppercase tracking-wider text-red-400 font-semibold mb-1">Without HDIM</p>
                    <p className="text-sm text-red-800">{m.without}</p>
                  </div>
                  <div className="flex items-center justify-center">
                    <ArrowRight className="w-4 h-4 text-gray-400" />
                  </div>
                  <div className="bg-green-50 rounded-lg p-3">
                    <p className="text-[10px] uppercase tracking-wider text-green-400 font-semibold mb-1">With HDIM</p>
                    <p className="text-sm text-green-800">{m.with}</p>
                  </div>
                </div>

                {/* Improvement */}
                <div className="mt-4 pt-4 border-t border-gray-100 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-gray-500">Improvement</p>
                    <p className="text-sm font-bold text-primary">{m.improvement}</p>
                  </div>
                  <TrendingUp className="w-5 h-5 text-green-500" />
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}

// ── Differentiators Section ─────────────────────────────────────────────────

function DifferentiatorsSection() {
  return (
    <section className="py-20 bg-gradient-to-br from-primary via-primary-600 to-primary-800 text-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-bold tracking-tight">
            Why HDIM Is Different
          </h2>
          <p className="mt-4 text-lg text-white/70 max-w-2xl mx-auto">
            Platform-level capabilities that set HDIM apart from legacy quality measurement tools.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          {DIFFERENTIATORS.map((d, i) => (
            <div
              key={i}
              className="bg-white/10 backdrop-blur-sm rounded-2xl border border-white/20 p-6 hover:bg-white/15 transition-colors duration-300"
              style={{ animationDelay: `${i * 100}ms` }}
            >
              <h3 className="font-semibold text-white text-sm mb-3">{d.title}</h3>
              <p className="text-sm text-white/70 leading-relaxed">{d.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

// ── Main Page ───────────────────────────────────────────────────────────────

export default function CapabilitiesPage() {
  const [selectedPersona, setSelectedPersona] = useState<PersonaId | null>(null)

  const selectedJourney = JOURNEYS.find((j) => j.personaId === selectedPersona)
  const selectedPersonaData = PERSONAS.find((p) => p.id === selectedPersona)

  // Smooth scroll to journey section when persona selected
  const handlePersonaSelect = (id: PersonaId) => {
    const next = selectedPersona === id ? null : id
    setSelectedPersona(next)
    if (next) {
      setTimeout(() => {
        document.getElementById('journey')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }, 100)
    }
  }

  return (
    <main className="min-h-screen">
      {/* ── Hero ─────────────────────────────────────────────────────────── */}
      <section className="relative bg-gradient-to-br from-primary via-primary-600 to-primary-800 overflow-hidden">
        {/* Decorative elements */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-accent rounded-full filter blur-3xl -translate-y-1/2 translate-x-1/4" />
          <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-white rounded-full filter blur-3xl translate-y-1/2 -translate-x-1/4" />
        </div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-24">
          {/* Breadcrumb */}
          <nav className="flex items-center gap-2 text-sm text-white/50 mb-8">
            <Link href="/" className="hover:text-white/80 transition-colors">Home</Link>
            <span>/</span>
            <Link href="/platform" className="hover:text-white/80 transition-colors">Platform</Link>
            <span>/</span>
            <span className="text-white/80">Capabilities</span>
          </nav>

          <div className="text-center">
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/10 text-white/80 text-sm font-medium mb-6 border border-white/20">
              <CheckCircle2 className="w-4 h-4 text-green-400" />
              100% Implemented — 80 of 80 Stories
            </div>

            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-white leading-tight text-balance max-w-4xl mx-auto">
              What&apos;s Actually <span className="text-accent-200">Built</span>
            </h1>
            <p className="mt-6 text-lg md:text-xl text-white/70 max-w-3xl mx-auto">
              Interactive exploration of every user story, journey map, and feature across 9 healthcare personas.
              Not a roadmap — everything here is implemented and production-ready.
            </p>

            {/* Scroll hint */}
            <div className="mt-12">
              <a
                href="#stats"
                className="inline-flex flex-col items-center text-white/40 hover:text-white/70 transition-colors"
              >
                <span className="text-xs font-medium mb-2">Explore</span>
                <ChevronDown className="w-5 h-5 animate-bounce" />
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* ── Stats Bar ────────────────────────────────────────────────────── */}
      <section id="stats" className="bg-gradient-to-r from-primary-700 to-primary-800 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6">
            <StatCard value={PLATFORM_STATS.userStories} label="User Stories" icon={CheckCircle2} />
            <StatCard value={PLATFORM_STATS.roles} label="User Roles" icon={Users} />
            <StatCard value={PLATFORM_STATS.pages} label="Portal Pages" icon={Layers} suffix="+" />
            <StatCard value={PLATFORM_STATS.microservices} label="Microservices" icon={Database} />
          </div>
        </div>
      </section>

      {/* ── Persona Selector ─────────────────────────────────────────────── */}
      <PersonaSelector
        personas={PERSONAS}
        selected={selectedPersona}
        onSelect={handlePersonaSelect}
      />

      {/* ── Journey Timeline ─────────────────────────────────────────────── */}
      <JourneyTimeline journey={selectedJourney} persona={selectedPersonaData} />

      {/* ── Value Comparison ─────────────────────────────────────────────── */}
      <ValueComparison selectedPersona={selectedPersona} />

      {/* ── Feature Matrix ───────────────────────────────────────────────── */}
      <FeatureMatrix
        features={FEATURE_PAGES}
        personas={PERSONAS}
        selectedPersona={selectedPersona}
      />

      {/* ── Story Browser ────────────────────────────────────────────────── */}
      <StoryBrowser
        stories={USER_STORIES}
        personas={PERSONAS}
        selectedPersona={selectedPersona}
      />

      {/* ── Differentiators ──────────────────────────────────────────────── */}
      <DifferentiatorsSection />

      {/* ── CTA ──────────────────────────────────────────────────────────── */}
      <section className="py-20 bg-white">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 tracking-tight">
            Ready to See It <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary to-accent">Live</span>?
          </h2>
          <p className="mt-4 text-lg text-gray-600">
            Everything on this page is implemented and running. Schedule a personalized demo for your role.
          </p>
          <div className="mt-8 flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/schedule"
              className="btn-primary text-lg px-8 py-4"
            >
              Schedule Demo
              <ArrowUpRight className="w-5 h-5 ml-2 inline-block" />
            </Link>
            <Link
              href="/platform"
              className="btn-secondary text-lg px-8 py-4"
            >
              Platform Overview
            </Link>
          </div>
        </div>
      </section>
    </main>
  )
}

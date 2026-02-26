'use client'

import { useEffect, useRef, useState } from 'react'
import {
  Monitor,
  Cpu,
  Search,
  FileBarChart,
  Server,
  ShieldCheck,
  Building2,
  Database,
  FlaskConical,
  Warehouse,
  Network,
  Ban,
  Eye,
  Lock,
} from 'lucide-react'

const hdimServices = [
  { icon: Monitor, label: 'Clinical Portal', subtitle: 'Provider-facing UI' },
  { icon: Cpu, label: 'CQL Engine', subtitle: 'Measure evaluation' },
  { icon: Search, label: 'Care Gap Detection', subtitle: 'Real-time analysis' },
  { icon: FileBarChart, label: 'Quality Reports', subtitle: 'HEDIS / CMS' },
]

const customerSystems = [
  { icon: Building2, label: 'EHR Systems', subtitle: 'Epic, Cerner, etc.' },
  { icon: Server, label: 'Customer FHIR Server', subtitle: 'Existing R4 endpoint' },
  { icon: FlaskConical, label: 'Claims & Labs', subtitle: '837/835, HL7, LIS' },
  { icon: Warehouse, label: 'Data Warehouse', subtitle: 'Analytics / EDW' },
  { icon: Network, label: 'Network Infra', subtitle: 'VPN, firewall, DNS' },
]

const callouts = [
  {
    icon: Ban,
    title: 'Zero Disruption',
    description: 'Your systems remain untouched',
    color: 'from-emerald-500/20 to-emerald-500/5',
    border: 'border-emerald-500/30',
    iconColor: 'text-emerald-400',
  },
  {
    icon: Eye,
    title: 'Read-Only Access',
    description: 'We never write to your infrastructure',
    color: 'from-blue-500/20 to-blue-500/5',
    border: 'border-blue-500/30',
    iconColor: 'text-blue-400',
  },
  {
    icon: Lock,
    title: 'Your Data Stays Put',
    description: 'PHI never leaves your environment',
    color: 'from-violet-500/20 to-violet-500/5',
    border: 'border-violet-500/30',
    iconColor: 'text-violet-400',
  },
]

export default function IntegrationArchitectureDiagram() {
  const sectionRef = useRef<HTMLElement>(null)
  const [isVisible, setIsVisible] = useState(false)

  useEffect(() => {
    const el = sectionRef.current
    if (!el) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true)
          observer.disconnect()
        }
      },
      { threshold: 0.15 }
    )

    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  return (
    <section
      ref={sectionRef}
      className="relative py-24 md:py-32 bg-gray-950 overflow-hidden"
    >
      {/* Subtle grid texture */}
      <div
        className="absolute inset-0 opacity-[0.03]"
        style={{
          backgroundImage:
            'linear-gradient(rgba(255,255,255,.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.1) 1px, transparent 1px)',
          backgroundSize: '40px 40px',
        }}
      />

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div
          className={`text-center mb-16 md:mb-20 transition-all duration-700 ${
            isVisible
              ? 'opacity-100 translate-y-0'
              : 'opacity-0 translate-y-6'
          }`}
        >
          <p className="text-sm font-semibold tracking-widest uppercase text-blue-400 mb-3">
            Integration Architecture
          </p>
          <h2 className="text-4xl md:text-5xl font-bold text-white mb-5 tracking-tight">
            Hovers Above Your Infrastructure
          </h2>
          <p className="text-lg text-gray-400 max-w-2xl mx-auto leading-relaxed">
            HDIM connects to your existing FHIR endpoints through a read-only gateway.
            Nothing installed, nothing changed, nothing disrupted.
          </p>
        </div>

        {/* ─── Diagram ──────────────────────────────────── */}
        <div
          className={`transition-all duration-1000 delay-200 ${
            isVisible
              ? 'opacity-100 translate-y-0'
              : 'opacity-0 translate-y-8'
          }`}
        >
          {/* ── HDIM Platform Layer ── */}
          <div className="relative rounded-2xl border border-indigo-500/30 bg-gradient-to-b from-indigo-950/60 to-gray-900/80 p-6 md:p-8">
            {/* Label */}
            <div className="absolute -top-3.5 left-6 md:left-8">
              <span className="px-3 py-1 text-xs font-bold tracking-wider uppercase bg-indigo-600 text-white rounded-full">
                HDIM Platform
              </span>
            </div>

            {/* Row 1: Services */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 md:gap-4 mt-2">
              {hdimServices.map((svc, i) => (
                <div
                  key={svc.label}
                  className={`group relative rounded-xl bg-white/[0.04] border border-white/[0.06] p-4 md:p-5 hover:bg-white/[0.07] transition-all duration-300 ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'
                  }`}
                  style={{ transitionDelay: `${400 + i * 100}ms` }}
                >
                  <svc.icon className="w-6 h-6 text-indigo-400 mb-2.5 group-hover:text-indigo-300 transition-colors" />
                  <p className="text-sm font-semibold text-white">{svc.label}</p>
                  <p className="text-xs text-gray-500 mt-0.5">{svc.subtitle}</p>
                </div>
              ))}
            </div>

            {/* Row 2: Internal FHIR Server */}
            <div
              className={`mt-4 rounded-xl bg-blue-950/40 border border-blue-500/20 px-5 py-4 flex items-center gap-4 transition-all duration-700 ${
                isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'
              }`}
              style={{ transitionDelay: '800ms' }}
            >
              <Server className="w-5 h-5 text-blue-400 flex-shrink-0" />
              <div>
                <p className="text-sm font-semibold text-blue-200">
                  Internal FHIR Server (R4)
                </p>
                <p className="text-xs text-blue-400/70">
                  Generated content &middot; CDR migrated data &middot; Normalized resources
                </p>
              </div>
            </div>

            {/* Row 3: Kong Gateway */}
            <div
              className={`mt-4 rounded-xl bg-amber-950/30 border border-amber-500/20 px-5 py-4 flex items-center gap-4 transition-all duration-700 ${
                isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'
              }`}
              style={{ transitionDelay: '900ms' }}
            >
              <ShieldCheck className="w-5 h-5 text-amber-400 flex-shrink-0" />
              <div>
                <p className="text-sm font-semibold text-amber-200">
                  Kong API Gateway
                </p>
                <p className="text-xs text-amber-400/70">
                  Auth &middot; Routing &middot; Rate limiting &middot; FHIR proxy &middot; Audit logging
                </p>
              </div>
            </div>
          </div>

          {/* ── Connection Zone ── */}
          <div className="relative flex flex-col items-center py-8 md:py-10">
            {/* Animated connection lines — desktop shows 3, mobile shows 1 */}
            <div className="flex items-center justify-center gap-16 md:gap-24">
              {[0, 1, 2].map((i) => (
                <div
                  key={i}
                  className={`relative h-16 md:h-20 w-px ${
                    i === 0 ? 'hidden md:block' : i === 2 ? 'hidden md:block' : ''
                  }`}
                >
                  {/* Dashed line */}
                  <div className="absolute inset-0 border-l-2 border-dashed border-blue-500/20" />
                  {/* Glow behind dot */}
                  <div
                    className="absolute left-1/2 -translate-x-1/2 w-3 h-3 rounded-full bg-blue-400/30 blur-sm animate-flow-down"
                    style={{ animationDelay: `${i * 0.4}s` }}
                  />
                  {/* Dot */}
                  <div
                    className="absolute left-1/2 -translate-x-1/2 w-2 h-2 rounded-full bg-blue-400 animate-flow-down"
                    style={{ animationDelay: `${i * 0.4}s` }}
                  />
                </div>
              ))}
            </div>

            {/* Label pill */}
            <div className="absolute top-1/2 -translate-y-1/2 px-4 py-1.5 rounded-full bg-gray-900 border border-blue-500/30 shadow-lg shadow-blue-500/5">
              <span className="text-xs font-medium text-blue-300 tracking-wide">
                FHIR R4 API&ensp;&middot;&ensp;Read-Only Access
              </span>
            </div>
          </div>

          {/* ── Customer Infrastructure Layer ── */}
          <div className="relative rounded-2xl border border-gray-700/50 bg-gradient-to-b from-gray-900/60 to-gray-950/80 p-6 md:p-8">
            {/* Label */}
            <div className="absolute -top-3.5 left-6 md:left-8">
              <span className="px-3 py-1 text-xs font-bold tracking-wider uppercase bg-gray-700 text-gray-300 rounded-full">
                Your Infrastructure&ensp;—&ensp;Unchanged
              </span>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 md:gap-4 mt-2">
              {customerSystems.map((sys, i) => (
                <div
                  key={sys.label}
                  className={`group rounded-xl bg-white/[0.02] border border-white/[0.05] p-4 md:p-5 transition-all duration-500 ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'
                  }`}
                  style={{ transitionDelay: `${1100 + i * 80}ms` }}
                >
                  <sys.icon className="w-5 h-5 text-gray-500 mb-2 group-hover:text-gray-400 transition-colors" />
                  <p className="text-sm font-medium text-gray-300">{sys.label}</p>
                  <p className="text-xs text-gray-600 mt-0.5">{sys.subtitle}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ─── Callout Badges ─────────────────────────── */}
        <div
          className={`grid sm:grid-cols-3 gap-4 mt-12 md:mt-16 transition-all duration-700 delay-[1400ms] ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'
          }`}
        >
          {callouts.map((c) => (
            <div
              key={c.title}
              className={`rounded-xl bg-gradient-to-br ${c.color} border ${c.border} p-5 md:p-6`}
            >
              <c.icon className={`w-6 h-6 ${c.iconColor} mb-3`} />
              <p className="text-sm font-bold text-white mb-1">{c.title}</p>
              <p className="text-xs text-gray-400 leading-relaxed">
                {c.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

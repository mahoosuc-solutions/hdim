'use client'

import { useState, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import {
  Building2,
  Hospital,
  Users,
  ChevronLeft,
  Target,
  UserCheck,
} from 'lucide-react'
import { WIZARD_OPTIONS } from '../../lib/constants'

type SegmentId = (typeof WIZARD_OPTIONS.segments)[number]['id']

const segmentIcons: Record<string, typeof Building2> = {
  'health-plan': Building2,
  'health-system': Hospital,
  aco: Users,
}

export function JourneyWizard() {
  const router = useRouter()
  const [currentStep, setCurrentStep] = useState(0)
  const [segment, setSegment] = useState('')
  const [transitioning, setTransitioning] = useState(false)

  const goForward = useCallback((nextStep: number) => {
    setTransitioning(true)
    setTimeout(() => {
      setCurrentStep(nextStep)
      setTransitioning(false)
    }, 200)
  }, [])

  const goBack = useCallback(() => {
    setTransitioning(true)
    setTimeout(() => {
      setSegment('')
      setCurrentStep(0)
      setTransitioning(false)
    }, 200)
  }, [])

  const handleSegmentSelect = (id: string) => {
    setSegment(id)
    goForward(1)
  }

  const handleRoleSelect = () => {
    const selectedSegment = WIZARD_OPTIONS.segments.find((s) => s.id === segment)
    if (selectedSegment) {
      router.push(`/${selectedSegment.slug}`)
    }
  }

  const roles = segment
    ? WIZARD_OPTIONS.roles[segment as SegmentId] || []
    : []

  const stepTitles = [
    'What type of organization are you?',
    'What is your primary role?',
  ]

  const stepIcons = [Target, UserCheck]
  const StepIcon = stepIcons[currentStep]

  const transitionClass = transitioning
    ? 'opacity-0 translate-y-2'
    : 'opacity-100 translate-y-0'

  return (
    <section className="py-16 md:py-20 bg-gray-50" id="journey">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-primary-50 text-primary mb-4">
            <StepIcon className="w-6 h-6" />
          </div>
          <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">
            {stepTitles[currentStep]}
          </h2>
          <p className="text-gray-500 text-sm">
            Step {currentStep + 1} of 2 &mdash; we&apos;ll show you the most relevant solution
          </p>
        </div>

        <div
          className={`transition-all duration-200 ease-in-out ${transitionClass}`}
          style={{ minHeight: '220px' }}
        >
          {currentStep === 0 && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {WIZARD_OPTIONS.segments.map((seg) => {
                const Icon = segmentIcons[seg.id] || Building2
                return (
                  <button
                    key={seg.id}
                    onClick={() => handleSegmentSelect(seg.id)}
                    className={`group relative p-6 rounded-xl border-2 text-left transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5 ${
                      segment === seg.id
                        ? 'border-primary-600 bg-primary-50'
                        : 'border-gray-200 hover:border-primary-300 bg-white'
                    }`}
                  >
                    <div className="flex items-start gap-4">
                      <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center group-hover:bg-primary-200 transition-colors">
                        <Icon className="w-5 h-5 text-primary" />
                      </div>
                      <div>
                        <h3 className="font-semibold text-gray-900 mb-1">
                          {seg.label}
                        </h3>
                        <p className="text-sm text-gray-500">
                          {seg.description}
                        </p>
                      </div>
                    </div>
                  </button>
                )
              })}
            </div>
          )}

          {currentStep === 1 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {roles.map((r) => (
                <button
                  key={r.id}
                  onClick={handleRoleSelect}
                  className="group p-5 rounded-xl border-2 border-gray-200 hover:border-primary-300 bg-white text-left transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center group-hover:bg-primary-200 transition-colors">
                      <UserCheck className="w-4 h-4 text-primary" />
                    </div>
                    <h3 className="font-semibold text-gray-900">{r.label}</h3>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="mt-8 flex items-center justify-between">
          <div>
            {currentStep > 0 && (
              <button
                onClick={goBack}
                className="inline-flex items-center gap-1.5 text-sm font-medium text-gray-500 hover:text-primary transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
                Back
              </button>
            )}
          </div>

          <div className="flex items-center gap-2">
            {[0, 1].map((step) => (
              <div
                key={step}
                className={`h-2 rounded-full transition-all duration-300 ${
                  step === currentStep
                    ? 'w-6 bg-primary'
                    : step < currentStep
                    ? 'w-2 bg-primary-300'
                    : 'w-2 bg-gray-200'
                }`}
              />
            ))}
          </div>

          <div className="w-16" />
        </div>
      </div>
    </section>
  )
}

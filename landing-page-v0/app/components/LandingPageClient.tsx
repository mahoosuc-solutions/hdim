'use client'

import Link from 'next/link'
import { useState, useEffect, useRef, ReactNode } from 'react'
import {
  HeartPulse,
  Menu,
  X,
  Play,
  ChevronDown,
} from 'lucide-react'

interface LandingPageClientProps {
  children: ReactNode
}

export function LandingPageClient({ children }: LandingPageClientProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const [showStickyCTA, setShowStickyCTA] = useState(false)
  const [solutionsOpen, setSolutionsOpen] = useState(false)
  const solutionsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
      setShowStickyCTA(window.scrollY > 600)
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (solutionsRef.current && !solutionsRef.current.contains(event.target as Node)) {
        setSolutionsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const navLinkClass = `text-sm font-medium transition-colors ${
    scrolled ? 'text-gray-600 hover:text-primary' : 'text-white/80 hover:text-white'
  }`

  return (
    <div className="min-h-screen bg-white">
      {/* Skip to main content link for accessibility */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-[60] focus:px-4 focus:py-2 focus:bg-white focus:text-primary focus:rounded"
      >
        Skip to main content
      </a>

      {/* Season Banner */}
      <div className="bg-gradient-to-r from-red-600 to-red-700 text-white py-2 px-4 text-center text-sm fixed top-0 left-0 right-0 z-50">
        <span className="font-semibold">HEDIS 2026 Season:</span> Measurement year is underway — align your quality program now.
        <Link href="/schedule" className="underline ml-2 hover:text-white/90">Get started &rarr;</Link>
      </div>

      {/* Navigation */}
      <nav
        className={`fixed top-8 left-0 right-0 z-50 transition-all duration-300 ${
          scrolled ? 'bg-white/95 backdrop-blur-md shadow-sm' : 'bg-transparent'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16 md:h-20">
            {/* Logo */}
            <div className="flex items-center">
              <Link href="/" className="flex items-center space-x-2">
                <div className="w-10 h-10 bg-gradient-to-br from-primary to-accent rounded-lg flex items-center justify-center">
                  <HeartPulse className="w-6 h-6 text-white" />
                </div>
                <span className={`text-xl font-bold ${scrolled ? 'text-gray-900' : 'text-white'}`}>
                  HDIM
                </span>
              </Link>
            </div>

            {/* Desktop Navigation */}
            <div className="hidden md:flex items-center space-x-8">
              <Link href="/" className={navLinkClass}>
                Home
              </Link>

              {/* Solutions Dropdown */}
              <div className="relative" ref={solutionsRef}>
                <button
                  onClick={() => setSolutionsOpen(!solutionsOpen)}
                  className={`flex items-center gap-1 ${navLinkClass}`}
                  aria-expanded={solutionsOpen}
                  aria-haspopup="true"
                >
                  Solutions
                  <ChevronDown className={`w-4 h-4 transition-transform ${solutionsOpen ? 'rotate-180' : ''}`} />
                </button>
                {solutionsOpen && (
                  <div className="absolute top-full left-0 mt-2 w-56 bg-white rounded-lg shadow-xl border border-gray-200 overflow-hidden z-50">
                    <Link
                      href="/health-plans"
                      className="block px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                      onClick={() => setSolutionsOpen(false)}
                    >
                      <div className="font-medium">Health Plans</div>
                      <div className="text-xs text-gray-500">MA, Commercial, Medicaid</div>
                    </Link>
                    <Link
                      href="/health-systems"
                      className="block px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors border-t border-gray-100"
                      onClick={() => setSolutionsOpen(false)}
                    >
                      <div className="font-medium">Health Systems</div>
                      <div className="text-xs text-gray-500">Hospitals, IDNs, AMCs</div>
                    </Link>
                    <Link
                      href="/acos"
                      className="block px-4 py-3 text-sm text-gray-700 hover:bg-gray-50 transition-colors border-t border-gray-100"
                      onClick={() => setSolutionsOpen(false)}
                    >
                      <div className="font-medium">ACOs & Provider Groups</div>
                      <div className="text-xs text-gray-500">ACOs, physician groups, IPAs</div>
                    </Link>
                  </div>
                )}
              </div>

              <Link href="/platform" className={navLinkClass}>
                Platform
              </Link>
              <Link href="/security" className={navLinkClass}>
                Security
              </Link>
              <Link href="/pricing" className={navLinkClass}>
                Pricing
              </Link>
              <Link href="/about" className={navLinkClass}>
                About
              </Link>
              <Link href="/contact" className={navLinkClass}>
                Contact
              </Link>
            </div>

            {/* CTA Buttons */}
            <div className="hidden md:flex items-center space-x-4">
              <Link
                href="/schedule"
                className="btn-primary text-sm"
              >
                Request Demo
              </Link>
            </div>

            {/* Mobile menu button */}
            <button
              className="md:hidden p-2"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label={mobileMenuOpen ? "Close menu" : "Open menu"}
              aria-expanded={mobileMenuOpen}
              aria-controls="mobile-menu"
            >
              {mobileMenuOpen ? (
                <X className={scrolled ? 'text-gray-900' : 'text-white'} />
              ) : (
                <Menu className={scrolled ? 'text-gray-900' : 'text-white'} />
              )}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div id="mobile-menu" className="md:hidden bg-white border-t">
            <div className="px-4 py-4 space-y-3">
              <Link href="/" className="block text-gray-600 hover:text-primary" onClick={() => setMobileMenuOpen(false)}>Home</Link>
              <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider pt-2">Solutions</div>
              <Link href="/health-plans" className="block text-gray-600 hover:text-primary pl-3" onClick={() => setMobileMenuOpen(false)}>Health Plans</Link>
              <Link href="/health-systems" className="block text-gray-600 hover:text-primary pl-3" onClick={() => setMobileMenuOpen(false)}>Health Systems</Link>
              <Link href="/acos" className="block text-gray-600 hover:text-primary pl-3" onClick={() => setMobileMenuOpen(false)}>ACOs & Provider Groups</Link>
              <div className="border-t border-gray-100 pt-2" />
              <Link href="/platform" className="block text-gray-600 hover:text-primary" onClick={() => setMobileMenuOpen(false)}>Platform</Link>
              <Link href="/security" className="block text-gray-600 hover:text-primary" onClick={() => setMobileMenuOpen(false)}>Security</Link>
              <Link href="/pricing" className="block text-gray-600 hover:text-primary" onClick={() => setMobileMenuOpen(false)}>Pricing</Link>
              <Link href="/about" className="block text-gray-600 hover:text-primary" onClick={() => setMobileMenuOpen(false)}>About</Link>
              <Link href="/contact" className="block text-gray-600 hover:text-primary" onClick={() => setMobileMenuOpen(false)}>Contact</Link>
              <Link href="/schedule" className="btn-primary w-full text-center mt-4" onClick={() => setMobileMenuOpen(false)}>Request Demo</Link>
            </div>
          </div>
        )}
      </nav>

      {children}

      {/* Sticky Mobile CTA */}
      {showStickyCTA && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t shadow-lg py-3 px-4 z-40 md:hidden">
          <Link
            href="/schedule"
            className="flex items-center justify-center gap-2 w-full bg-primary text-white text-center py-3 rounded-lg font-semibold hover:bg-primary-600 transition-colors"
          >
            <Play className="w-4 h-4" />
            Request Demo
          </Link>
        </div>
      )}
    </div>
  )
}

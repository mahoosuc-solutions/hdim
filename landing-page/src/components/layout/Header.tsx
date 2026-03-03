'use client';

import React, { useState } from 'react';
import Link from 'next/link';

export default function Header() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const closeMenu = () => setMobileMenuOpen(false);

  return (
    <header className="sticky top-0 z-50 bg-white shadow-sm">
      <nav className="container-lg py-4" aria-label="Main navigation">
        <div className="flex justify-between items-center">
          <Link href="/" className="flex items-center gap-2" aria-label="HDIM home">
            <img src="/hdim-icon.svg" alt="" className="w-8 h-8" aria-hidden="true" />
            <span className="text-xl font-bold text-gray-900">HDIM</span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center gap-8">
            <a href="#solution" className="text-gray-600 hover:text-gray-900 font-medium transition">
              Solution
            </a>
            <a href="#deployment" className="text-gray-600 hover:text-gray-900 font-medium transition">
              Deployment
            </a>
            <a href="#scenarios" className="text-gray-600 hover:text-gray-900 font-medium transition">
              Scenarios
            </a>
            <a href="#roi" className="text-gray-600 hover:text-gray-900 font-medium transition">
              ROI
            </a>
            <Link href="/integrations" className="text-gray-600 hover:text-gray-900 font-medium transition">
              Integrations
            </Link>
            <Link href="/pricing" className="text-gray-600 hover:text-gray-900 font-medium transition">
              Pricing
            </Link>
          </div>

          {/* CTA Button */}
          <a href="https://calendar.app.google/zKDs6ZdXW7V61c7i7" target="_blank" rel="noopener noreferrer" className="btn-primary hidden md:block">Schedule Demo</a>

          {/* Mobile Menu Toggle */}
          <button
            className="md:hidden p-2"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-expanded={mobileMenuOpen}
            aria-controls="mobile-menu"
            aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d={mobileMenuOpen ? 'M6 18L18 6M6 6l12 12' : 'M4 6h16M4 12h16M4 18h16'}
              />
            </svg>
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div id="mobile-menu" className="md:hidden mt-4 pb-4 border-t" role="menu">
            <a href="#solution" className="block py-2 text-gray-600 hover:text-gray-900" onClick={closeMenu} role="menuitem">
              Solution
            </a>
            <a href="#deployment" className="block py-2 text-gray-600 hover:text-gray-900" onClick={closeMenu} role="menuitem">
              Deployment
            </a>
            <a href="#scenarios" className="block py-2 text-gray-600 hover:text-gray-900" onClick={closeMenu} role="menuitem">
              Scenarios
            </a>
            <a href="#roi" className="block py-2 text-gray-600 hover:text-gray-900" onClick={closeMenu} role="menuitem">
              ROI
            </a>
            <Link href="/integrations" className="block py-2 text-gray-600 hover:text-gray-900" onClick={closeMenu} role="menuitem">
              Integrations
            </Link>
            <Link href="/pricing" className="block py-2 text-gray-600 hover:text-gray-900" onClick={closeMenu} role="menuitem">
              Pricing
            </Link>
            <a href="https://calendar.app.google/zKDs6ZdXW7V61c7i7" target="_blank" rel="noopener noreferrer" className="btn-primary block w-full mt-4 text-center" onClick={closeMenu}>Schedule Demo</a>
          </div>
        )}
      </nav>
    </header>
  );
}

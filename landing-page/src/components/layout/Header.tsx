'use client';

import React, { useState } from 'react';
import Link from 'next/link';

export default function Header() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 bg-white shadow-sm">
      <nav className="container-lg py-4">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-lg"></div>
            <span className="text-xl font-bold text-gray-900">HDIM</span>
          </div>

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
            <a href="#pricing" className="text-gray-600 hover:text-gray-900 font-medium transition">
              Pricing
            </a>
          </div>

          {/* CTA Button */}
          <button className="btn-primary hidden md:block">Schedule Demo</button>

          {/* Mobile Menu Toggle */}
          <button
            className="md:hidden p-2"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 6h16M4 12h16M4 18h16"
              />
            </svg>
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden mt-4 pb-4 border-t">
            <a href="#solution" className="block py-2 text-gray-600 hover:text-gray-900">
              Solution
            </a>
            <a href="#deployment" className="block py-2 text-gray-600 hover:text-gray-900">
              Deployment
            </a>
            <a href="#scenarios" className="block py-2 text-gray-600 hover:text-gray-900">
              Scenarios
            </a>
            <a href="#roi" className="block py-2 text-gray-600 hover:text-gray-900">
              ROI
            </a>
            <a href="#pricing" className="block py-2 text-gray-600 hover:text-gray-900">
              Pricing
            </a>
            <button className="btn-primary w-full mt-4">Schedule Demo</button>
          </div>
        )}
      </nav>
    </header>
  );
}

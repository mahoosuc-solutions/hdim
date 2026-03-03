import React from 'react';
import Link from 'next/link';
import Header from '@/components/layout/Header';

export const metadata = {
  title: '404 — Page Not Found | HDIM',
};

export default function NotFound() {
  return (
    <>
      <Header />
      <main id="main-content" className="min-h-[60vh] flex items-center justify-center bg-white">
        <div className="text-center px-6">
          <div className="flex justify-center mb-6">
            <img src="/hdim-icon.svg" alt="HDIM" className="w-16 h-16 opacity-40" />
          </div>
          <h1 className="text-6xl font-bold text-gray-900 mb-4">404</h1>
          <p className="text-xl text-gray-600 mb-8 max-w-md mx-auto">
            This page doesn&apos;t exist. Let&apos;s get you back on track.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/"
              className="btn-primary px-8 py-3 text-center"
            >
              Back to Home
            </Link>
            <Link
              href="/pricing"
              className="btn-secondary px-8 py-3 text-center"
            >
              View Pricing
            </Link>
          </div>
        </div>
      </main>
    </>
  );
}

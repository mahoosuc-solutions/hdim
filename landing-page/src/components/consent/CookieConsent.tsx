'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';

const STORAGE_KEY = 'hdim-cookie-consent';

type ConsentLevel = 'necessary' | 'all';

interface ConsentRecord {
  consent: ConsentLevel;
  timestamp: string;
}

declare global {
  interface Window {
    hdimConsent: ConsentLevel | null;
  }
}

function getStoredConsent(): ConsentRecord | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as ConsentRecord;
  } catch {
    return null;
  }
}

function saveConsent(level: ConsentLevel) {
  const record: ConsentRecord = { consent: level, timestamp: new Date().toISOString() };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(record));
  window.hdimConsent = level;
  window.dispatchEvent(new CustomEvent('hdim-consent-changed', { detail: record }));
}

export default function CookieConsent() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const stored = getStoredConsent();
    window.hdimConsent = stored?.consent ?? null;

    if (!stored) {
      setVisible(true);
    }

    const handleReset = () => setVisible(true);
    window.addEventListener('hdim-consent-changed', (e) => {
      if (!(e as CustomEvent).detail) handleReset();
    });
    window.addEventListener('hdim-consent-reset', handleReset);

    return () => {
      window.removeEventListener('hdim-consent-reset', handleReset);
    };
  }, []);

  function handleConsent(level: ConsentLevel) {
    saveConsent(level);
    setVisible(false);
  }

  if (!visible) return null;

  return (
    <div
      role="dialog"
      aria-label="Cookie consent"
      className="fixed bottom-0 left-0 right-0 z-50 animate-fadeInUp"
    >
      <div className="bg-gray-900 border-t border-gray-700 px-4 py-4 sm:px-6">
        <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-start sm:items-center gap-4">
          <p className="text-sm text-gray-300 flex-1">
            We use essential cookies for site functionality. Optional cookies help us improve
            your experience.{' '}
            <Link href="/privacy" className="text-blue-400 hover:underline">
              Privacy Policy
            </Link>
          </p>
          <div className="flex gap-3 flex-shrink-0">
            <button
              onClick={() => handleConsent('necessary')}
              className="px-4 py-2 text-sm font-medium text-gray-300 border border-gray-600 rounded-lg hover:bg-gray-800 transition"
            >
              Necessary Only
            </button>
            <button
              onClick={() => handleConsent('all')}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition"
            >
              Accept All
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

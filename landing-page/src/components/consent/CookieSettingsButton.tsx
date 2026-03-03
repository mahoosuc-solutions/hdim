'use client';

const STORAGE_KEY = 'hdim-cookie-consent';

export default function CookieSettingsButton() {
  function openSettings() {
    localStorage.removeItem(STORAGE_KEY);
    window.hdimConsent = null;
    window.dispatchEvent(new CustomEvent('hdim-consent-reset'));
  }

  return (
    <button onClick={openSettings} className="hover:text-white text-left">
      Cookie Settings
    </button>
  );
}

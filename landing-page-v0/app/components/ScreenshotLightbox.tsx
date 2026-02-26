"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";

interface Screenshot {
  src: string;
  alt: string;
  width: number;
  height: number;
  title: string;
  description: string;
}

interface ScreenshotCardProps {
  screenshot: Screenshot;
  onClick: () => void;
  className?: string;
}

function ScreenshotCard({ screenshot, onClick, className = "" }: ScreenshotCardProps) {
  return (
    <div
      className={`group rounded-xl overflow-hidden shadow-lg bg-white border border-gray-100 hover:shadow-xl transition-shadow cursor-pointer ${className}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); onClick(); } }}
      aria-label={`View full size: ${screenshot.title}`}
    >
      <div className="relative overflow-hidden">
        <Image
          src={screenshot.src}
          alt={screenshot.alt}
          width={screenshot.width}
          height={screenshot.height}
          className="w-full h-auto transition-transform duration-300 group-hover:scale-[1.02]"
          loading="lazy"
        />
        <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors flex items-center justify-center">
          <div className="opacity-0 group-hover:opacity-100 transition-opacity bg-white/90 backdrop-blur-sm rounded-full p-3 shadow-lg">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-700">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
              <line x1="11" y1="8" x2="11" y2="14" />
              <line x1="8" y1="11" x2="14" y2="11" />
            </svg>
          </div>
        </div>
      </div>
      <div className="p-4">
        <h3 className="font-semibold text-gray-900 text-sm">{screenshot.title}</h3>
        <p className="text-gray-500 text-xs mt-1">{screenshot.description}</p>
      </div>
    </div>
  );
}

interface LightboxProps {
  screenshots: Screenshot[];
  currentIndex: number;
  onClose: () => void;
  onNavigate: (index: number) => void;
}

function Lightbox({ screenshots, currentIndex, onClose, onNavigate }: LightboxProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const current = screenshots[currentIndex];

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) {
      dialog.showModal();
    }
  }, []);

  const handlePrev = useCallback(() => {
    onNavigate(currentIndex > 0 ? currentIndex - 1 : screenshots.length - 1);
  }, [currentIndex, screenshots.length, onNavigate]);

  const handleNext = useCallback(() => {
    onNavigate(currentIndex < screenshots.length - 1 ? currentIndex + 1 : 0);
  }, [currentIndex, screenshots.length, onNavigate]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "ArrowLeft") handlePrev();
      if (e.key === "ArrowRight") handleNext();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handlePrev, handleNext]);

  return (
    <dialog
      ref={dialogRef}
      className="fixed inset-0 w-full h-full max-w-none max-h-none m-0 p-0 bg-black/90 backdrop-blur-sm z-50 open:flex open:items-center open:justify-center"
      onClick={(e) => { if (e.target === dialogRef.current) onClose(); }}
      onClose={onClose}
    >
      <div className="relative w-full h-full flex flex-col items-center justify-center p-4 sm:p-8">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-10 bg-white/10 hover:bg-white/20 text-white rounded-full p-2 transition-colors"
          aria-label="Close lightbox"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>

        {/* Counter */}
        <div className="absolute top-4 left-4 text-white/70 text-sm font-medium">
          {currentIndex + 1} / {screenshots.length}
        </div>

        {/* Navigation: Previous */}
        <button
          onClick={handlePrev}
          className="absolute left-2 sm:left-6 top-1/2 -translate-y-1/2 z-10 bg-white/10 hover:bg-white/20 text-white rounded-full p-3 transition-colors"
          aria-label="Previous screenshot"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>

        {/* Image */}
        <div className="max-w-[90vw] max-h-[80vh] flex items-center justify-center">
          <Image
            src={current.src}
            alt={current.alt}
            width={current.width}
            height={current.height}
            className="max-w-full max-h-[80vh] w-auto h-auto rounded-lg shadow-2xl object-contain"
            priority
          />
        </div>

        {/* Navigation: Next */}
        <button
          onClick={handleNext}
          className="absolute right-2 sm:right-6 top-1/2 -translate-y-1/2 z-10 bg-white/10 hover:bg-white/20 text-white rounded-full p-3 transition-colors"
          aria-label="Next screenshot"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>

        {/* Caption */}
        <div className="mt-4 text-center max-w-2xl">
          <h3 className="text-white font-semibold text-lg">{current.title}</h3>
          <p className="text-white/70 text-sm mt-1">{current.description}</p>
        </div>
      </div>
    </dialog>
  );
}

export interface ScreenshotGalleryProps {
  heroScreenshot: Screenshot & { badge?: string };
  rows: {
    screenshots: Screenshot[];
    columns: 2 | 3;
  }[];
}

export default function ScreenshotGallery({ heroScreenshot, rows }: ScreenshotGalleryProps) {
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

  // Flatten all screenshots into a single array for lightbox navigation
  const allScreenshots = [heroScreenshot, ...rows.flatMap((r) => r.screenshots)];

  // Map from flat index back to which screenshot it is
  const openLightbox = (flatIndex: number) => setLightboxIndex(flatIndex);
  const closeLightbox = () => setLightboxIndex(null);

  let flatIndex = 1; // hero is index 0

  return (
    <>
      <div className="space-y-8">
        {/* Hero */}
        <div
          className="relative rounded-2xl overflow-hidden shadow-2xl cursor-pointer group"
          onClick={() => openLightbox(0)}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openLightbox(0); } }}
          aria-label={`View full size: ${heroScreenshot.title}`}
        >
          <Image
            src={heroScreenshot.src}
            alt={heroScreenshot.alt}
            width={heroScreenshot.width}
            height={heroScreenshot.height}
            className="w-full h-auto transition-transform duration-300 group-hover:scale-[1.01]"
            loading="lazy"
          />
          {heroScreenshot.badge && (
            <div className="absolute top-4 right-4 bg-green-500 text-white px-3 py-1 rounded-full text-sm font-medium flex items-center">
              <span className="w-2 h-2 bg-white rounded-full mr-2 animate-pulse" />
              {heroScreenshot.badge}
            </div>
          )}
          <div className="absolute inset-0 bg-black/0 group-hover:bg-black/5 transition-colors flex items-center justify-center">
            <div className="opacity-0 group-hover:opacity-100 transition-opacity bg-white/90 backdrop-blur-sm rounded-full p-4 shadow-lg">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-700">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                <line x1="11" y1="8" x2="11" y2="14" />
                <line x1="8" y1="11" x2="14" y2="11" />
              </svg>
            </div>
          </div>
        </div>

        {/* Rows */}
        {/* Tailwind safelist: md:grid-cols-2 md:grid-cols-3 */}
        {rows.map((row, rowIdx) => (
          <div key={rowIdx} className={`grid gap-6 ${row.columns === 2 ? "md:grid-cols-2" : "md:grid-cols-3"}`}>
            {row.screenshots.map((screenshot, colIdx) => {
              const idx = flatIndex++;
              return (
                <ScreenshotCard
                  key={idx}
                  screenshot={screenshot}
                  onClick={() => openLightbox(idx)}
                />
              );
            })}
          </div>
        ))}
      </div>

      {/* Lightbox overlay */}
      {lightboxIndex !== null && (
        <Lightbox
          screenshots={allScreenshots}
          currentIndex={lightboxIndex}
          onClose={closeLightbox}
          onNavigate={setLightboxIndex}
        />
      )}
    </>
  );
}

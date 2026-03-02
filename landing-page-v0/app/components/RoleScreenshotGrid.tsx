"use client";

import { useState } from "react";
import Image from "next/image";
import type { SegmentScreenshots } from "../../lib/constants";

interface LightboxState {
  roleIdx: number;
  shotIdx: number;
}

export function RoleScreenshotGrid({ roles }: { roles: SegmentScreenshots[] }) {
  const [lightbox, setLightbox] = useState<LightboxState | null>(null);

  const allShots = roles.flatMap((r) =>
    r.screenshots.map((s) => ({ ...s, role: r.role }))
  );
  const flatIndex = (roleIdx: number, shotIdx: number) =>
    roles.slice(0, roleIdx).reduce((n, r) => n + r.screenshots.length, 0) + shotIdx;
  const flatTotal = allShots.length;

  const currentFlat = lightbox
    ? flatIndex(lightbox.roleIdx, lightbox.shotIdx)
    : 0;

  const navigate = (dir: 1 | -1) => {
    const next = (currentFlat + dir + flatTotal) % flatTotal;
    let count = 0;
    for (let ri = 0; ri < roles.length; ri++) {
      for (let si = 0; si < roles[ri].screenshots.length; si++) {
        if (count === next) {
          setLightbox({ roleIdx: ri, shotIdx: si });
          return;
        }
        count++;
      }
    }
  };

  return (
    <>
      <div className="space-y-12">
        {roles.map((role, roleIdx) => (
          <div key={role.role}>
            <h3 className="text-lg font-bold text-gray-900 mb-4">{role.role}</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              {role.screenshots.map((shot, shotIdx) => (
                <div
                  key={shot.src}
                  className="group rounded-xl overflow-hidden shadow-lg bg-white border border-gray-100 hover:shadow-xl transition-shadow cursor-pointer"
                  onClick={() => setLightbox({ roleIdx, shotIdx })}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      setLightbox({ roleIdx, shotIdx });
                    }
                  }}
                  aria-label={`View full size: ${shot.title}`}
                >
                  <div className="relative overflow-hidden">
                    <Image
                      src={shot.src}
                      alt={shot.alt}
                      width={960}
                      height={540}
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
                    <h4 className="font-semibold text-gray-900 text-sm">{shot.title}</h4>
                    <p className="text-gray-500 text-xs mt-1">{shot.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Lightbox */}
      {lightbox && (() => {
        const shot = roles[lightbox.roleIdx].screenshots[lightbox.shotIdx];
        return (
          <div
            className="fixed inset-0 z-50 bg-black/90 backdrop-blur-sm flex items-center justify-center p-4 sm:p-8"
            onClick={() => setLightbox(null)}
          >
            <div className="relative max-w-[90vw] max-h-[90vh] flex flex-col items-center" onClick={(e) => e.stopPropagation()}>
              <button
                onClick={() => setLightbox(null)}
                className="absolute -top-2 -right-2 z-10 bg-white/10 hover:bg-white/20 text-white rounded-full p-2 transition-colors"
                aria-label="Close"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
              <div className="absolute top-2 left-2 text-white/70 text-sm">
                {currentFlat + 1} / {flatTotal}
              </div>
              <button onClick={() => navigate(-1)} className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-12 bg-white/10 hover:bg-white/20 text-white rounded-full p-3" aria-label="Previous">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
              </button>
              <Image
                src={shot.src}
                alt={shot.alt}
                width={1920}
                height={1080}
                className="max-w-full max-h-[80vh] w-auto h-auto rounded-lg shadow-2xl object-contain"
                priority
              />
              <button onClick={() => navigate(1)} className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-12 bg-white/10 hover:bg-white/20 text-white rounded-full p-3" aria-label="Next">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 18 15 12 9 6" /></svg>
              </button>
              <div className="mt-4 text-center">
                <h3 className="text-white font-semibold text-lg">{shot.title}</h3>
                <p className="text-white/70 text-sm mt-1">{shot.description}</p>
              </div>
            </div>
          </div>
        );
      })()}
    </>
  );
}

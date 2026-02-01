'use client';

import { useState } from 'react';
import { Play, X } from 'lucide-react';
import Image from 'next/image';

interface VideoPlayerProps {
  videoSrc: string;
  thumbnailSrc: string;
  title: string;
  description: string;
  youtubeId?: string; // Optional YouTube video ID
  preferYouTube?: boolean; // If true, use YouTube by default
}

export default function VideoPlayer({
  videoSrc,
  thumbnailSrc,
  title,
  description,
  youtubeId,
  preferYouTube = false
}: VideoPlayerProps) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [useYouTube, setUseYouTube] = useState(preferYouTube && !!youtubeId);

  // Determine which video source to use
  const effectiveVideoSrc = useYouTube && youtubeId
    ? `https://www.youtube.com/embed/${youtubeId}?autoplay=1&rel=0&modestbranding=1&playsinline=1`
    : videoSrc;

  // Modal overlay when video is playing
  if (isPlaying) {
    return (
      <div
        className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4"
        onClick={() => setIsPlaying(false)} // Click outside to close
      >
        <div
          className="relative w-full max-w-6xl"
          onClick={(e) => e.stopPropagation()} // Prevent close when clicking video
        >
          {/* Close button */}
          <button
            onClick={() => setIsPlaying(false)}
            className="absolute -top-12 right-0 text-white hover:text-gray-300 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-black rounded"
            aria-label="Close video player"
          >
            <X className="w-8 h-8" />
          </button>

          {/* Video element - Self-hosted or YouTube */}
          {useYouTube && youtubeId ? (
            <iframe
              src={effectiveVideoSrc}
              className="w-full aspect-video rounded-lg shadow-2xl"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowFullScreen
              title={title}
            />
          ) : (
            <video
              src={videoSrc}
              controls
              autoPlay
              className="w-full rounded-lg shadow-2xl"
              aria-label={`${title} - ${description}`}
            >
              <track kind="captions" />
              Your browser doesn't support video playback.
            </video>
          )}
        </div>
      </div>
    );
  }

  // Thumbnail with play button overlay
  return (
    <div
      className="relative rounded-2xl overflow-hidden shadow-2xl cursor-pointer group"
      onClick={() => setIsPlaying(true)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          setIsPlaying(true);
        }
      }}
      aria-label={`Play video: ${title}`}
    >
      {/* Thumbnail image */}
      <Image
        src={thumbnailSrc}
        alt={title}
        width={1408}
        height={768}
        loading="lazy"
        className="w-full"
      />

      {/* Play button overlay */}
      <div className="absolute inset-0 flex items-center justify-center bg-black/30 group-hover:bg-black/40 transition-colors">
        <div
          className="w-20 h-20 bg-white rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform"
          aria-hidden="true"
        >
          <Play className="w-8 h-8 text-primary ml-1" fill="currentColor" />
        </div>
      </div>

      {/* Video source indicator (optional) */}
      {youtubeId && (
        <div className="absolute top-4 right-4 bg-black/60 backdrop-blur-sm px-3 py-1 rounded-full text-xs text-white">
          {useYouTube ? 'YouTube' : 'Direct'}
        </div>
      )}

      {/* Accessibility: Screen reader description */}
      <span className="sr-only">{description}</span>
    </div>
  );
}

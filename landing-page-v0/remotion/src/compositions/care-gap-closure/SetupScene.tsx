import React from 'react';
import { AbsoluteFill, interpolate, staticFile, useCurrentFrame } from 'remotion';
import { ScreenshotWithOverlay, Overlay } from '../../components/ScreenshotWithOverlay';

interface SetupSceneProps {
  variant?: 'short' | 'default';
}

export const SetupScene: React.FC<SetupSceneProps> = ({ variant = 'default' }) => {
  const frame = useCurrentFrame();

  // Text overlay: "Clinical Portal - Care Gap Manager" at 2s
  const titleOverlay: Overlay = {
    type: 'text',
    startFrame: 60,
    duration: 180, // Show for 6 seconds
    position: { x: 30, y: 10 },
    props: {
      text: 'Clinical Portal - Care Gap Manager',
      fontSize: '2rem',
      fontWeight: 700,
    },
  };

  // Highlight summary stats at 4s
  const summaryHighlight: Overlay = {
    type: 'glow-highlight',
    startFrame: 120,
    duration: 120,
    position: { x: 10, y: 18 },
    props: {
      width: 1600,
      height: 120,
      borderColor: 'blue',
      pulseCount: 2,
      borderRadius: 12,
    },
  };

  // Show total gaps metric at 5s
  const totalGapsMetric: Overlay = {
    type: 'text',
    startFrame: 150,
    duration: 90,
    position: { x: 20, y: 22 },
    props: {
      text: '45 total gaps • 9 high urgency',
      fontSize: '1.75rem',
      fontWeight: 700,
    },
  };

  const overlays: Overlay[] = [titleOverlay, summaryHighlight, totalGapsMetric];

  return (
    <ScreenshotWithOverlay
      screenshot={staticFile('screenshots/care-gap-dashboard.png')}
      overlays={overlays}
      zoomLevel={1.05}
      panDirection="none"
    />
  );
};

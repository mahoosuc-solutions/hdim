import React from 'react';
import { AbsoluteFill, staticFile } from 'remotion';
import { ScreenshotWithOverlay, Overlay } from '../../components/ScreenshotWithOverlay';

interface ImpactSceneProps {
  variant?: 'short' | 'default';
}

export const ImpactScene: React.FC<ImpactSceneProps> = ({ variant = 'default' }) => {
  // Success notification at 1s
  const successNotification: Overlay = {
    type: 'badge',
    startFrame: 30,
    duration: 150,
    position: { x: 40, y: 10 },
    props: {
      text: '✓ Care gap closed successfully',
      backgroundColor: 'rgba(34, 197, 94, 0.95)',
      color: 'white',
      fontSize: '2rem',
    },
  };

  // Closure time badge at 2s
  const closureTimeBadge: Overlay = {
    type: 'badge',
    startFrame: 60,
    duration: 240,
    position: { x: 50, y: 20 },
    props: {
      text: '✓ Closed in 8 seconds',
      backgroundColor: 'rgba(34, 197, 94, 0.9)',
      color: 'white',
      fontSize: '2rem',
    },
  };

  // Total gaps counter (45 → 44) at 4s
  const totalGapsMetric: Overlay = {
    type: 'metric',
    startFrame: 120,
    duration: 60,
    position: { x: 20, y: 35 },
    props: {
      from: 45,
      to: 44,
      suffix: ' Total Gaps',
      fontSize: '2.25rem',
      glowOnComplete: true,
      duration: 45,
    },
  };

  // High urgency counter (9 → 8) at 5s
  const urgencyMetric: Overlay = {
    type: 'metric',
    startFrame: 150,
    duration: 60,
    position: { x: 50, y: 35 },
    props: {
      from: 9,
      to: 8,
      suffix: ' High Urgency',
      fontSize: '2.25rem',
      glowOnComplete: true,
      duration: 45,
    },
  };

  // Highlight updated summary stats at 7s
  const summaryHighlight: Overlay = {
    type: 'glow-highlight',
    startFrame: 210,
    duration: 120,
    position: { x: 10, y: 18 },
    props: {
      width: 1600,
      height: 120,
      borderColor: 'green',
      pulseCount: 2,
      borderRadius: 12,
    },
  };

  // Eleanor's row removed callout at 9s
  const rowRemovedOverlay: Overlay = {
    type: 'text',
    startFrame: 270,
    duration: 120,
    position: { x: 30, y: 55 },
    props: {
      text: 'Eleanor\'s gap removed from list',
      fontSize: '1.75rem',
      fontWeight: 700,
    },
  };

  // Next gap highlighted at 11s
  const nextGapHighlight: Overlay = {
    type: 'glow-highlight',
    startFrame: 330,
    duration: 120,
    position: { x: 10, y: 65 },
    props: {
      width: 1600,
      height: 80,
      borderColor: 'blue',
      pulseCount: 1,
      borderRadius: 8,
    },
  };

  const overlays: Overlay[] = [
    successNotification,
    closureTimeBadge,
    totalGapsMetric,
    urgencyMetric,
    summaryHighlight,
    rowRemovedOverlay,
    nextGapHighlight,
  ];

  return (
    <ScreenshotWithOverlay
      screenshot={staticFile('screenshots/care-gap-dashboard-updated.png')}
      overlays={overlays}
      zoomLevel={1.05}
      panDirection="none"
    />
  );
};

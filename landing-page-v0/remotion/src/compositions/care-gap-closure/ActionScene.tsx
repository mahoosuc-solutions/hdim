import React from 'react';
import { AbsoluteFill, staticFile } from 'remotion';
import { ScreenshotWithOverlay, Overlay } from '../../components/ScreenshotWithOverlay';

interface ActionSceneProps {
  variant?: 'short' | 'default';
}

export const ActionScene: React.FC<ActionSceneProps> = ({ variant = 'default' }) => {
  // Dialog title overlay at 1s
  const dialogTitleOverlay: Overlay = {
    type: 'text',
    startFrame: 30,
    duration: 450,
    position: { x: 30, y: 15 },
    props: {
      text: 'Quick Actions - Close Care Gap',
      fontSize: '2rem',
      fontWeight: 700,
    },
  };

  // Highlight "Schedule Screening" button at 3s
  const scheduleButtonHighlight: Overlay = {
    type: 'glow-highlight',
    startFrame: 90,
    duration: 240,
    position: { x: 35, y: 38 },
    props: {
      width: 500,
      height: 80,
      borderColor: 'green',
      pulseCount: 3,
      borderRadius: 12,
    },
  };

  // Button stagger animation: "Schedule Screening" badge at 4s
  const scheduleButtonBadge: Overlay = {
    type: 'badge',
    startFrame: 120,
    duration: 300,
    position: { x: 40, y: 42 },
    props: {
      text: '✓ Schedule Screening',
      backgroundColor: 'rgba(139, 92, 246, 0.95)',
      color: 'white',
      fontSize: '1.75rem',
    },
  };

  // "Already Done" button appears at 4.5s (15 frame delay)
  const alreadyDoneBadge: Overlay = {
    type: 'badge',
    startFrame: 135,
    duration: 285,
    position: { x: 40, y: 52 },
    props: {
      text: 'Already Done',
      backgroundColor: 'rgba(34, 197, 94, 0.9)',
      color: 'white',
      fontSize: '1.5rem',
    },
  };

  // "Patient Declined" button appears at 5s (15 frame delay)
  const declinedBadge: Overlay = {
    type: 'badge',
    startFrame: 150,
    duration: 270,
    position: { x: 40, y: 62 },
    props: {
      text: 'Patient Declined',
      backgroundColor: 'rgba(239, 68, 68, 0.9)',
      color: 'white',
      fontSize: '1.5rem',
    },
  };

  // Form filled in text at 8s
  const formFilledOverlay: Overlay = {
    type: 'text',
    startFrame: 240,
    duration: 180,
    position: { x: 25, y: 72 },
    props: {
      text: 'Closure Reason: Screening appointment scheduled',
      fontSize: '1.5rem',
      fontWeight: 600,
    },
  };

  // Intervention badge at 10s
  const interventionBadge: Overlay = {
    type: 'badge',
    startFrame: 300,
    duration: 120,
    position: { x: 35, y: 80 },
    props: {
      text: 'APPOINTMENT_SCHEDULED',
      backgroundColor: 'rgba(59, 130, 246, 0.9)',
      color: 'white',
      fontSize: '1.25rem',
    },
  };

  // Click animation callout at 12s
  const clickCallout: Overlay = {
    type: 'text',
    startFrame: 360,
    duration: 120,
    position: { x: 60, y: 88 },
    props: {
      text: '→ Clicking "Close Gap"',
      fontSize: '1.75rem',
      fontWeight: 700,
    },
  };

  const overlays: Overlay[] = [
    dialogTitleOverlay,
    scheduleButtonHighlight,
    scheduleButtonBadge,
    alreadyDoneBadge,
    declinedBadge,
    formFilledOverlay,
    interventionBadge,
    clickCallout,
  ];

  return (
    <ScreenshotWithOverlay
      screenshot={staticFile('screenshots/care-gap-closure-dialog.png')}
      overlays={overlays}
      zoomLevel={1.06}
      panDirection="none"
    />
  );
};

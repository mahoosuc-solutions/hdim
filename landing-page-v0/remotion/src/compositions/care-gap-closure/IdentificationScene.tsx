import React from 'react';
import { AbsoluteFill, staticFile } from 'remotion';
import { ScreenshotWithOverlay, Overlay } from '../../components/ScreenshotWithOverlay';

interface IdentificationSceneProps {
  variant?: 'short' | 'default';
}

export const IdentificationScene: React.FC<IdentificationSceneProps> = ({ variant = 'default' }) => {
  // Glow highlight around Eleanor's table row at 2s (frame 60)
  const eleanorRowHighlight: Overlay = {
    type: 'glow-highlight',
    startFrame: 60,
    duration: 300, // Show for 10 seconds
    position: { x: 10, y: 45 },
    props: {
      width: 1600,
      height: 80,
      borderColor: 'red',
      pulseCount: 3,
      borderRadius: 8,
    },
  };

  // Patient name overlay at 3s
  const patientNameOverlay: Overlay = {
    type: 'text',
    startFrame: 90,
    duration: 240,
    position: { x: 30, y: 15 },
    props: {
      text: 'Eleanor Anderson, 63 - Mammogram Overdue',
      fontSize: '2rem',
      fontWeight: 700,
    },
  };

  // Urgency badge highlight at 4s
  const urgencyBadge: Overlay = {
    type: 'badge',
    startFrame: 120,
    duration: 210,
    position: { x: 75, y: 48 },
    props: {
      text: 'HIGH',
      backgroundColor: 'rgba(239, 68, 68, 0.95)',
      color: 'white',
      fontSize: '1.5rem',
    },
  };

  // Days overdue metric at 5s
  const daysOverdueMetric: Overlay = {
    type: 'metric',
    startFrame: 150,
    duration: 90,
    position: { x: 45, y: 35 },
    props: {
      from: 0,
      to: 60,
      suffix: ' days overdue',
      fontSize: '2.5rem',
      glowOnComplete: true,
      duration: 60,
    },
  };

  // Measure type callout at 7s
  const measureCallout: Overlay = {
    type: 'text',
    startFrame: 210,
    duration: 120,
    position: { x: 25, y: 60 },
    props: {
      text: 'Breast Cancer Screening (BCS) - HEDIS Measure',
      fontSize: '1.5rem',
      fontWeight: 600,
    },
  };

  const overlays: Overlay[] = [
    eleanorRowHighlight,
    patientNameOverlay,
    urgencyBadge,
    daysOverdueMetric,
    measureCallout,
  ];

  return (
    <ScreenshotWithOverlay
      screenshot={staticFile('screenshots/care-gap-table-eleanor.png')}
      overlays={overlays}
      zoomLevel={1.08}
      panDirection="none"
    />
  );
};

import React from 'react';
import { AbsoluteFill, interpolate, useCurrentFrame } from 'remotion';

interface NarrativeTextProps {
  text: string;
  accentColor?: string;
}

/**
 * Semi-transparent dark bar at the bottom of the screen
 * showing clinical context for what the viewer is seeing.
 *
 * Fades in over 20 frames, fades out 20 frames before the
 * parent Sequence ends (the Sequence controls total duration).
 */
export const NarrativeText: React.FC<NarrativeTextProps> = ({
  text,
  accentColor = '#00CC88',
}) => {
  const frame = useCurrentFrame();

  const opacity = interpolate(frame, [0, 20], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const slideUp = interpolate(frame, [0, 20], [15, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill style={{ pointerEvents: 'none' }}>
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          height: '8%',
          minHeight: '60px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'rgba(0, 0, 0, 0.75)',
          backdropFilter: 'blur(12px)',
          borderTop: `2px solid ${accentColor}40`,
          opacity,
          transform: `translateY(${slideUp}px)`,
        }}
      >
        {/* Accent dot */}
        <div
          style={{
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            backgroundColor: accentColor,
            marginRight: '1rem',
            flexShrink: 0,
            boxShadow: `0 0 8px ${accentColor}80`,
          }}
        />

        <p
          style={{
            fontSize: '1.4rem',
            fontWeight: 500,
            color: 'rgba(255, 255, 255, 0.92)',
            margin: 0,
            letterSpacing: '0.02em',
            fontFamily:
              '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
          }}
        >
          {text}
        </p>
      </div>
    </AbsoluteFill>
  );
};

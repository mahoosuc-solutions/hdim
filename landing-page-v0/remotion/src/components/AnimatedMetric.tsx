import React from 'react';
import { interpolate, useCurrentFrame, useVideoConfig } from 'remotion';

interface AnimatedMetricProps {
  from: number;
  to: number;
  suffix?: string;
  prefix?: string;
  duration?: number; // in frames
  delay?: number; // in frames
  decimals?: number;
  fontSize?: string;
  glowOnComplete?: boolean;
}

export const AnimatedMetric: React.FC<AnimatedMetricProps> = ({
  from,
  to,
  suffix = '',
  prefix = '',
  duration = 45, // 1.5s at 30fps
  delay = 0,
  decimals = 0,
  fontSize = '4rem',
  glowOnComplete = true,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  // Calculate current value
  const value = interpolate(
    frame,
    [delay, delay + duration],
    [from, to],
    {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
      easing: (t) => {
        // Ease-out cubic
        return 1 - Math.pow(1 - t, 3);
      },
    }
  );

  // Format number with commas and decimals
  const formattedValue = value.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });

  // Check if animation complete
  const isComplete = frame >= delay + duration;

  return (
    <div
      style={{
        padding: '1rem 2rem',
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        backdropFilter: 'blur(10px)',
        borderRadius: '12px',
        border: '3px solid rgba(0, 204, 136, 0.7)',
        boxShadow: isComplete && glowOnComplete
          ? '0 0 30px rgba(0, 204, 136, 0.8)'
          : '0 4px 12px rgba(0, 0, 0, 0.5)',
        transition: 'box-shadow 0.3s ease',
        display: 'inline-block',
      }}
    >
      <div
        style={{
          fontSize,
          fontWeight: 700,
          color: 'white',
          margin: 0,
          fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        }}
      >
        {prefix}{formattedValue}{suffix}
      </div>
    </div>
  );
};

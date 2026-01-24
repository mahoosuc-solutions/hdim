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
        fontSize,
        fontWeight: 700,
        color: 'white',
        textShadow: isComplete && glowOnComplete
          ? '0 0 20px rgba(0, 204, 136, 0.8)'
          : 'none',
        transition: 'text-shadow 0.3s ease',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      }}
    >
      {prefix}{formattedValue}{suffix}
    </div>
  );
};

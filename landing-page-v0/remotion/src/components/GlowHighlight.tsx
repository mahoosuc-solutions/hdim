import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';

interface GlowHighlightProps {
  width: number;
  height: number;
  borderColor?: 'green' | 'blue' | 'red';
  pulseCount?: number;
  borderRadius?: number;
  startFrame?: number;
}

export const GlowHighlight: React.FC<GlowHighlightProps> = ({
  width,
  height,
  borderColor = 'green',
  pulseCount = 2,
  borderRadius = 8,
  startFrame = 0,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const colors = {
    green: {
      border: 'linear-gradient(135deg, #0066CC, #00CC88)',
      shadow: 'rgba(0, 204, 136, 0.5)',
    },
    blue: {
      border: 'linear-gradient(135deg, #0066CC, #0052A3)',
      shadow: 'rgba(0, 102, 204, 0.5)',
    },
    red: {
      border: 'linear-gradient(135deg, #EF4444, #DC2626)',
      shadow: 'rgba(239, 68, 68, 0.5)',
    },
  };

  // Fade in animation
  const opacity = spring({
    frame: frame - startFrame,
    fps,
    config: {
      damping: 200,
    },
  });

  // Pulse animation
  const pulseDuration = 30; // 1 second at 30fps
  const totalPulseDuration = pulseDuration * pulseCount;
  const pulseProgress = (frame - startFrame) % pulseDuration;

  const scale = interpolate(
    pulseProgress,
    [0, pulseDuration / 2, pulseDuration],
    [1, 1.05, 1],
    {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    }
  );

  const isAnimating = frame >= startFrame && frame < startFrame + totalPulseDuration;

  return (
    <div
      style={{
        position: 'absolute',
        width,
        height,
        border: '3px solid transparent',
        backgroundImage: colors[borderColor].border,
        backgroundOrigin: 'border-box',
        backgroundClip: 'border-box',
        WebkitMaskImage: 'linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0)',
        WebkitMaskComposite: 'xor',
        maskComposite: 'exclude',
        boxShadow: `0 0 20px ${colors[borderColor].shadow}`,
        borderRadius,
        opacity,
        transform: isAnimating ? `scale(${scale})` : 'scale(1)',
        transition: 'transform 0.1s ease-out',
      }}
    />
  );
};

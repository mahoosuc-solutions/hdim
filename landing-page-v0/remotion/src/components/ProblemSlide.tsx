import React from 'react';
import { AbsoluteFill, interpolate, useCurrentFrame } from 'remotion';

interface ProblemSlideProps {
  statement: string;
  metric: string;
  accentColor: string;
}

export const ProblemSlide: React.FC<ProblemSlideProps> = ({
  statement,
  metric,
  accentColor,
}) => {
  const frame = useCurrentFrame();

  const bgOpacity = interpolate(frame, [0, 20], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const statementOpacity = interpolate(frame, [15, 40], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const statementY = interpolate(frame, [15, 40], [20, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const metricOpacity = interpolate(frame, [45, 70], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const metricScale = interpolate(frame, [45, 60, 70], [0.8, 1.05, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill
      style={{
        background: 'linear-gradient(135deg, #1a0a0a 0%, #2d0a0a 40%, #1a0a0a 100%)',
        justifyContent: 'center',
        alignItems: 'center',
        textAlign: 'center',
        opacity: bgOpacity,
      }}
    >
      {/* Red-tinted glow for "problem" tone */}
      <div
        style={{
          position: 'absolute',
          width: '500px',
          height: '500px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(239,68,68,0.12) 0%, transparent 70%)',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          filter: 'blur(80px)',
        }}
      />

      <div style={{ maxWidth: '75%', position: 'relative' }}>
        {/* Problem icon */}
        <div
          style={{
            fontSize: '3rem',
            marginBottom: '2rem',
            opacity: statementOpacity,
          }}
        >
          &#9888;
        </div>

        {/* Problem statement */}
        {statementOpacity > 0 && (
          <div
            style={{
              fontSize: '3rem',
              fontWeight: 600,
              color: 'rgba(255,255,255,0.9)',
              lineHeight: 1.3,
              marginBottom: '3rem',
              opacity: statementOpacity,
              transform: `translateY(${statementY}px)`,
              fontFamily:
                '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            }}
          >
            {statement}
          </div>
        )}

        {/* Key metric */}
        {metricOpacity > 0 && (
          <div
            style={{
              display: 'inline-block',
              padding: '1.2rem 3rem',
              backgroundColor: 'rgba(239, 68, 68, 0.15)',
              borderRadius: '16px',
              border: '2px solid rgba(239, 68, 68, 0.4)',
              opacity: metricOpacity,
              transform: `scale(${metricScale})`,
            }}
          >
            <span
              style={{
                fontSize: '2.2rem',
                fontWeight: 700,
                color: '#FCA5A5',
                fontFamily:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
              }}
            >
              {metric}
            </span>
          </div>
        )}
      </div>

      {/* Watermark */}
      <div
        style={{
          position: 'absolute',
          bottom: '40px',
          right: '50px',
          fontSize: '1.2rem',
          fontWeight: 600,
          color: 'rgba(255,255,255,0.15)',
          letterSpacing: '0.2em',
          fontFamily:
            '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        }}
      >
        HEALTHDATA IN MOTION
      </div>
    </AbsoluteFill>
  );
};

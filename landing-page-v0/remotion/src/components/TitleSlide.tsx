import React from 'react';
import { AbsoluteFill, interpolate, useCurrentFrame } from 'remotion';

interface TitleSlideProps {
  roleTitle: string;
  roleSubtitle: string;
  headline: string;
  subheadline: string;
  accentColor: string;
}

export const TitleSlide: React.FC<TitleSlideProps> = ({
  roleTitle,
  roleSubtitle,
  headline,
  subheadline,
  accentColor,
}) => {
  const frame = useCurrentFrame();

  const bgOpacity = interpolate(frame, [0, 20], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const roleBadgeOpacity = interpolate(frame, [15, 35], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const headlineOpacity = interpolate(frame, [30, 55], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const headlineY = interpolate(frame, [30, 55], [30, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const subheadlineOpacity = interpolate(frame, [50, 70], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const lineWidth = interpolate(frame, [40, 65], [0, 200], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill
      style={{
        background: `linear-gradient(135deg, #0a0f1e 0%, #0d1b3e 40%, #0a0f1e 100%)`,
        justifyContent: 'center',
        alignItems: 'center',
        textAlign: 'center',
        opacity: bgOpacity,
      }}
    >
      {/* Subtle gradient accent glow */}
      <div
        style={{
          position: 'absolute',
          width: '600px',
          height: '600px',
          borderRadius: '50%',
          background: `radial-gradient(circle, ${accentColor}20 0%, transparent 70%)`,
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          filter: 'blur(80px)',
        }}
      />

      <div style={{ maxWidth: '80%', position: 'relative' }}>
        {/* Role badge */}
        {roleBadgeOpacity > 0 && (
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '0.6rem 1.8rem',
              backgroundColor: `${accentColor}18`,
              borderRadius: '28px',
              border: `2px solid ${accentColor}50`,
              marginBottom: '2.5rem',
              opacity: roleBadgeOpacity,
              fontFamily:
                '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            }}
          >
            <span
              style={{
                fontSize: '1.3rem',
                fontWeight: 700,
                color: accentColor,
                textTransform: 'uppercase',
                letterSpacing: '0.15em',
              }}
            >
              {roleTitle}
            </span>
            <span
              style={{
                fontSize: '1.1rem',
                fontWeight: 400,
                color: 'rgba(255,255,255,0.6)',
              }}
            >
              {roleSubtitle}
            </span>
          </div>
        )}

        {/* Headline */}
        {headlineOpacity > 0 && (
          <div
            style={{
              fontSize: '4.5rem',
              fontWeight: 700,
              color: 'white',
              lineHeight: 1.15,
              marginBottom: '1.5rem',
              opacity: headlineOpacity,
              transform: `translateY(${headlineY}px)`,
              textShadow: '0 4px 30px rgba(0,0,0,0.5)',
              fontFamily:
                '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            }}
          >
            {headline}
          </div>
        )}

        {/* Accent line */}
        <div
          style={{
            width: `${lineWidth}px`,
            height: '4px',
            background: `linear-gradient(90deg, ${accentColor}, ${accentColor}00)`,
            borderRadius: '2px',
            margin: '0 auto 1.5rem',
          }}
        />

        {/* Subheadline */}
        {subheadlineOpacity > 0 && (
          <div
            style={{
              fontSize: '2rem',
              fontWeight: 400,
              color: 'rgba(255,255,255,0.7)',
              opacity: subheadlineOpacity,
              fontFamily:
                '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            }}
          >
            {subheadline}
          </div>
        )}
      </div>

      {/* HDIM logo watermark bottom-right */}
      <div
        style={{
          position: 'absolute',
          bottom: '40px',
          right: '50px',
          fontSize: '1.2rem',
          fontWeight: 600,
          color: 'rgba(255,255,255,0.25)',
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

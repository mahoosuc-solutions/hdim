import React from 'react';
import { AbsoluteFill, interpolate, useCurrentFrame } from 'remotion';
import { BrandedContainer } from './BrandedContainer';
import { CTASlideConfig } from '../types/role-story.types';

interface CTASlideProps {
  config: CTASlideConfig;
  accentColor: string;
}

export const CTASlide: React.FC<CTASlideProps> = ({ config, accentColor }) => {
  const frame = useCurrentFrame();

  const containerOpacity = interpolate(frame, [0, 30], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const headlineOpacity = interpolate(frame, [30, 60], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const ctaOpacity = interpolate(frame, [150, 180], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const urlOpacity = interpolate(frame, [180, 210], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <BrandedContainer variant="blue-teal" opacity={containerOpacity}>
      <AbsoluteFill
        style={{
          justifyContent: 'center',
          alignItems: 'center',
          textAlign: 'center',
        }}
      >
        <div style={{ maxWidth: '85%' }}>
          {/* Headline */}
          {headlineOpacity > 0 && (
            <div
              style={{
                fontSize: '4.5rem',
                fontWeight: 700,
                color: 'white',
                marginBottom: '3.5rem',
                opacity: headlineOpacity,
                textShadow:
                  '0 4px 20px rgba(0,0,0,0.8), 0 2px 8px rgba(0,0,0,0.9)',
                fontFamily:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                lineHeight: 1.2,
              }}
            >
              {config.headline}
              <br />
              <span
                style={{
                  fontSize: '3.8rem',
                  color: accentColor,
                  textShadow: `0 4px 20px rgba(0,0,0,0.8), 0 2px 8px ${accentColor}60`,
                }}
              >
                {config.highlightText}
              </span>
            </div>
          )}

          {/* Stats badges */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'center',
              gap: '2.5rem',
              marginBottom: '3.5rem',
              flexWrap: 'wrap',
            }}
          >
            {config.stats.map((stat, i) => {
              const badgeOpacity = interpolate(
                frame,
                [60 + i * 15, 75 + i * 15],
                [0, 1],
                { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' }
              );

              if (badgeOpacity <= 0) return null;

              return (
                <div
                  key={i}
                  style={{
                    padding: '1.3rem 2.5rem',
                    backgroundColor: stat.backgroundColor,
                    borderRadius: '32px',
                    border: `3px solid ${stat.borderColor}`,
                    boxShadow: `0 0 30px ${stat.glowColor}`,
                    opacity: badgeOpacity,
                    fontFamily:
                      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                  }}
                >
                  <p
                    style={{
                      fontSize: '2.25rem',
                      fontWeight: 700,
                      color: stat.textColor,
                      margin: 0,
                    }}
                  >
                    {stat.value}
                  </p>
                </div>
              );
            })}
          </div>

          {/* Call to action text */}
          {ctaOpacity > 0 && (
            <div
              style={{
                fontSize: '3.2rem',
                fontWeight: 700,
                color: 'white',
                marginBottom: '2rem',
                opacity: ctaOpacity,
                textShadow:
                  '0 4px 20px rgba(0,0,0,0.8), 0 2px 8px rgba(0,0,0,0.9)',
                fontFamily:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
              }}
            >
              {config.ctaText}
            </div>
          )}

          {/* URL */}
          {urlOpacity > 0 && (
            <div
              style={{
                padding: '0.75rem 2rem',
                backgroundColor: 'rgba(0, 0, 0, 0.6)',
                backdropFilter: 'blur(10px)',
                borderRadius: '12px',
                border: '2px solid rgba(255, 255, 255, 0.3)',
                display: 'inline-block',
                opacity: urlOpacity,
              }}
            >
              <div
                style={{
                  fontSize: '2.2rem',
                  fontWeight: 600,
                  color: 'white',
                  fontFamily:
                    '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                {config.ctaUrl}
              </div>
            </div>
          )}
        </div>
      </AbsoluteFill>
    </BrandedContainer>
  );
};

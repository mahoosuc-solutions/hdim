import React from 'react';
import { AbsoluteFill, interpolate, useCurrentFrame } from 'remotion';
import { BrandedContainer } from '../../components/BrandedContainer';

interface OutcomeSceneProps {
  variant?: 'short' | 'default';
}

export const OutcomeScene: React.FC<OutcomeSceneProps> = ({ variant = 'default' }) => {
  const frame = useCurrentFrame();

  // Fade in container
  const containerOpacity = interpolate(frame, [0, 30], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Main headline fade in at 1s
  const headlineOpacity = interpolate(frame, [30, 60], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Statistics badges stagger in at 2s, 2.5s, 3s
  const badge1Opacity = interpolate(frame, [60, 75], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const badge2Opacity = interpolate(frame, [75, 90], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const badge3Opacity = interpolate(frame, [90, 105], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // CTA fade in at 5s
  const ctaOpacity = interpolate(frame, [150, 180], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // URL fade in at 7s
  const urlOpacity = interpolate(frame, [210, 240], [0, 1], {
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
          {/* Main headline */}
          {headlineOpacity > 0 && (
            <div
              style={{
                fontSize: '5rem',
                fontWeight: 700,
                color: 'white',
                marginBottom: '4rem',
                opacity: headlineOpacity,
                textShadow: '0 4px 20px rgba(0, 0, 0, 0.8), 0 2px 8px rgba(0, 0, 0, 0.9)',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                lineHeight: 1.2,
              }}
            >
              Close Care Gaps in Seconds
              <br />
              <span style={{
                fontSize: '4rem',
                color: '#10B981',
                textShadow: '0 4px 20px rgba(0, 0, 0, 0.8), 0 2px 8px rgba(16, 185, 129, 0.6)',
              }}>Not Weeks</span>
            </div>
          )}

          {/* Statistics badges */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'center',
              gap: '3rem',
              marginBottom: '4rem',
              flexWrap: 'wrap',
            }}
          >
            {badge1Opacity > 0 && (
              <div
                style={{
                  padding: '1.5rem 3rem',
                  backgroundColor: 'rgba(251, 191, 36, 0.95)',
                  borderRadius: '32px',
                  border: '3px solid rgba(251, 191, 36, 1)',
                  boxShadow: '0 0 30px rgba(251, 191, 36, 0.6)',
                  opacity: badge1Opacity,
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                <p
                  style={{
                    fontSize: '2.5rem',
                    fontWeight: 700,
                    color: '#78350F',
                    margin: 0,
                  }}
                >
                  8.2x ROI
                </p>
              </div>
            )}

            {badge2Opacity > 0 && (
              <div
                style={{
                  padding: '1.5rem 3rem',
                  backgroundColor: 'rgba(34, 197, 94, 0.95)',
                  borderRadius: '32px',
                  border: '3px solid rgba(34, 197, 94, 1)',
                  boxShadow: '0 0 30px rgba(34, 197, 94, 0.6)',
                  opacity: badge2Opacity,
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                <p
                  style={{
                    fontSize: '2.5rem',
                    fontWeight: 700,
                    color: 'white',
                    margin: 0,
                  }}
                >
                  48% Success Rate
                </p>
              </div>
            )}

            {badge3Opacity > 0 && (
              <div
                style={{
                  padding: '1.5rem 3rem',
                  backgroundColor: 'rgba(59, 130, 246, 0.95)',
                  borderRadius: '32px',
                  border: '3px solid rgba(59, 130, 246, 1)',
                  boxShadow: '0 0 30px rgba(59, 130, 246, 0.6)',
                  opacity: badge3Opacity,
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                <p
                  style={{
                    fontSize: '2.5rem',
                    fontWeight: 700,
                    color: 'white',
                    margin: 0,
                  }}
                >
                  30-Day Avg Closure
                </p>
              </div>
            )}
          </div>

          {/* Call to action */}
          {ctaOpacity > 0 && (
            <div
              style={{
                fontSize: '3.5rem',
                fontWeight: 700,
                color: 'white',
                marginBottom: '2rem',
                opacity: ctaOpacity,
                textShadow: '0 4px 20px rgba(0, 0, 0, 0.8), 0 2px 8px rgba(0, 0, 0, 0.9)',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
              }}
            >
              Try the Interactive Demo
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
                  fontSize: '2.5rem',
                  fontWeight: 600,
                  color: 'white',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                www.healthdatainmotion.com
              </div>
            </div>
          )}
        </div>
      </AbsoluteFill>
    </BrandedContainer>
  );
};

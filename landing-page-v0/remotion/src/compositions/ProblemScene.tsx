import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { BrandedContainer } from '../components/BrandedContainer';

interface ProblemSceneProps {
  variant?: 'short' | 'default' | 'long';
}

export const ProblemScene: React.FC<ProblemSceneProps> = ({ variant = 'default' }) => {
  const frame = useCurrentFrame();
  const { fps, width, height } = useVideoConfig();

  // Frame 1: Hook (0-5s = 0-150 frames at 30fps)
  const hookOpacity = interpolate(frame, [0, 30, 120, 150], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Frame 2: Pain Point 1 (5-10s = 150-300 frames)
  const pain1Opacity = interpolate(frame, [150, 180, 270, 300], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Frame 3: Pain Point 2 (10-15s = 300-450 frames)
  const pain2Opacity = interpolate(frame, [300, 330, 420, 450], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Frame 4: Pain Point 3 (15-20s = 450-600 frames)
  const pain3Opacity = interpolate(frame, [450, 480, 570, 600], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Frame 5: Transition (20-25s = 600-750 frames)
  const transitionOpacity = interpolate(frame, [600, 630], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <BrandedContainer variant="dark-blue">
      {/* Frame 1: Hook */}
      {hookOpacity > 0 && (
        <AbsoluteFill
          style={{
            justifyContent: 'center',
            alignItems: 'center',
            opacity: hookOpacity,
          }}
        >
          <h1
            style={{
              fontSize: '5rem',
              fontWeight: 700,
              color: 'white',
              textAlign: 'center',
              maxWidth: '80%',
              fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
              textShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
            }}
          >
            Managing healthcare quality in 2026?
          </h1>
        </AbsoluteFill>
      )}

      {/* Frame 2: Pain Point 1 - Data Silos */}
      {pain1Opacity > 0 && (
        <AbsoluteFill
          style={{
            justifyContent: 'center',
            alignItems: 'center',
            opacity: pain1Opacity,
            padding: '0 10%',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '4rem', maxWidth: '80%' }}>
            <div style={{ flex: 1, textAlign: 'right' }}>
              <div
                style={{
                  fontSize: '8rem',
                  fontWeight: 700,
                  color: '#EF4444',
                  marginBottom: '1rem',
                }}
              >
                📊
              </div>
            </div>
            <div style={{ flex: 2 }}>
              <h2
                style={{
                  fontSize: '3.5rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '1rem',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Data scattered across 15+ systems
              </h2>
              <p
                style={{
                  fontSize: '2rem',
                  color: 'rgba(255, 255, 255, 0.8)',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                EHRs, claims, labs, pharmacies...
              </p>
            </div>
          </div>
        </AbsoluteFill>
      )}

      {/* Frame 3: Pain Point 2 - Manual Work */}
      {pain2Opacity > 0 && (
        <AbsoluteFill
          style={{
            justifyContent: 'center',
            alignItems: 'center',
            opacity: pain2Opacity,
            padding: '0 10%',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '4rem', maxWidth: '80%' }}>
            <div style={{ flex: 1, textAlign: 'right' }}>
              <div
                style={{
                  fontSize: '8rem',
                  fontWeight: 700,
                  color: '#F59E0B',
                  marginBottom: '1rem',
                }}
              >
                ⏱️
              </div>
            </div>
            <div style={{ flex: 2 }}>
              <h2
                style={{
                  fontSize: '3.5rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '1rem',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Weeks of manual HEDIS calculations
              </h2>
              <p
                style={{
                  fontSize: '2rem',
                  color: 'rgba(255, 255, 255, 0.8)',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Quality teams drowning in spreadsheets
              </p>
            </div>
          </div>
        </AbsoluteFill>
      )}

      {/* Frame 4: Pain Point 3 - Missed Revenue */}
      {pain3Opacity > 0 && (
        <AbsoluteFill
          style={{
            justifyContent: 'center',
            alignItems: 'center',
            opacity: pain3Opacity,
            padding: '0 10%',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '4rem', maxWidth: '80%' }}>
            <div style={{ flex: 1, textAlign: 'right' }}>
              <div
                style={{
                  fontSize: '8rem',
                  fontWeight: 700,
                  color: '#EF4444',
                  marginBottom: '1rem',
                }}
              >
                💸
              </div>
            </div>
            <div style={{ flex: 2 }}>
              <h2
                style={{
                  fontSize: '3.5rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '1rem',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Millions in quality bonuses... missed
              </h2>
              <p
                style={{
                  fontSize: '2rem',
                  color: 'rgba(255, 255, 255, 0.8)',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Care gaps undetected until too late
              </p>
            </div>
          </div>
        </AbsoluteFill>
      )}

      {/* Frame 5: Transition */}
      {transitionOpacity > 0 && (
        <AbsoluteFill
          style={{
            justifyContent: 'center',
            alignItems: 'center',
            opacity: transitionOpacity,
          }}
        >
          <h2
            style={{
              fontSize: '4rem',
              fontWeight: 600,
              color: 'white',
              textAlign: 'center',
              fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            }}
          >
            There&apos;s a better way...
          </h2>
        </AbsoluteFill>
      )}
    </BrandedContainer>
  );
};

import React from 'react';
import { AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig } from 'remotion';
import { BrandedContainer } from '../components/BrandedContainer';
import { AnimatedMetric } from '../components/AnimatedMetric';

interface SolutionSceneProps {
  variant?: 'short' | 'default' | 'long';
}

export const SolutionScene: React.FC<SolutionSceneProps> = ({ variant = 'default' }) => {
  const frame = useCurrentFrame();
  const { fps, width, height } = useVideoConfig();

  // Frame 1: Introduce HDIM (0-5s = 0-150 frames)
  const logoOpacity = interpolate(frame, [0, 30, 120, 150], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const logoScale = interpolate(frame, [0, 30], [0.8, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Frame 2: Three Pillars (5-13s = 150-390 frames)
  const pillarsVisible = frame >= 150 && frame < 390;

  // Stagger card entrance
  const card1Opacity = interpolate(frame, [150, 180], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const card2Opacity = interpolate(frame, [165, 195], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const card3Opacity = interpolate(frame, [180, 210], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Frame 3: Before/After (13-20s = 390-600 frames)
  const comparisonOpacity = interpolate(frame, [390, 420, 570, 600], [0, 1, 1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Progress bar animations
  const beforeProgress = interpolate(frame, [420, 570], [0, 10], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const afterProgress = interpolate(frame, [435, 450], [0, 100], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: (t) => 1 - Math.pow(1 - t, 3), // ease-out cubic
  });

  // Frame 4: Transition (20-25s = 600-750 frames)
  const transitionOpacity = interpolate(frame, [600, 630], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <BrandedContainer variant="light">
      {/* Frame 1: HDIM Logo Introduction */}
      {logoOpacity > 0 && (
        <AbsoluteFill
          style={{
            justifyContent: 'center',
            alignItems: 'center',
            opacity: logoOpacity,
          }}
        >
          <div
            style={{
              textAlign: 'center',
              transform: `scale(${logoScale})`,
            }}
          >
            <div
              style={{
                fontSize: '10rem',
                fontWeight: 700,
                color: 'white',
                marginBottom: '2rem',
                textShadow: '0 0 30px rgba(0, 204, 136, 0.8)',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
              }}
            >
              HDIM
            </div>
            <h2
              style={{
                fontSize: '3rem',
                fontWeight: 600,
                color: 'white',
                marginBottom: '1rem',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
              }}
            >
              The FHIR-Native Quality Platform
            </h2>
            {frame >= 120 && (
              <p
                style={{
                  fontSize: '2rem',
                  color: 'rgba(255, 255, 255, 0.9)',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                From fragmented to connected in seconds
              </p>
            )}
          </div>
        </AbsoluteFill>
      )}

      {/* Frame 2: Three Pillars */}
      {pillarsVisible && (
        <AbsoluteFill
          style={{
            justifyContent: 'center',
            alignItems: 'center',
            padding: '0 5%',
          }}
        >
          {/* Shrunken logo in corner */}
          <div
            style={{
              position: 'absolute',
              top: '5%',
              left: '5%',
              fontSize: '3rem',
              fontWeight: 700,
              color: 'white',
              fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            }}
          >
            HDIM
          </div>

          {/* Three cards */}
          <div
            style={{
              display: 'flex',
              gap: '3rem',
              maxWidth: '90%',
              justifyContent: 'center',
            }}
          >
            {/* Card 1: FHIR Integration */}
            <div
              style={{
                flex: 1,
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                backdropFilter: 'blur(10px)',
                borderRadius: '16px',
                padding: '3rem',
                opacity: card1Opacity,
                transform: `translateX(${interpolate(card1Opacity, [0, 1], [50, 0])}px)`,
                border: '2px solid rgba(255, 255, 255, 0.3)',
              }}
            >
              <div
                style={{
                  fontSize: '5rem',
                  marginBottom: '1.5rem',
                  textAlign: 'center',
                }}
              >
                🔗
              </div>
              <h3
                style={{
                  fontSize: '2rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '1rem',
                  textAlign: 'center',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                FHIR-Native Architecture
              </h3>
              <p
                style={{
                  fontSize: '1.5rem',
                  color: 'rgba(255, 255, 255, 0.9)',
                  textAlign: 'center',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Connect 47 systems automatically
              </p>
            </div>

            {/* Card 2: CQL Engine */}
            <div
              style={{
                flex: 1,
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                backdropFilter: 'blur(10px)',
                borderRadius: '16px',
                padding: '3rem',
                opacity: card2Opacity,
                transform: `translateX(${interpolate(card2Opacity, [0, 1], [50, 0])}px)`,
                border: '2px solid rgba(255, 255, 255, 0.3)',
              }}
            >
              <div
                style={{
                  fontSize: '5rem',
                  marginBottom: '1.5rem',
                  textAlign: 'center',
                }}
              >
                ⚡
              </div>
              <h3
                style={{
                  fontSize: '2rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '1rem',
                  textAlign: 'center',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Real-Time CQL Execution
              </h3>
              <p
                style={{
                  fontSize: '1.5rem',
                  color: 'rgba(255, 255, 255, 0.9)',
                  textAlign: 'center',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Evaluate quality measures instantly
              </p>
            </div>

            {/* Card 3: Care Gap Detection */}
            <div
              style={{
                flex: 1,
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                backdropFilter: 'blur(10px)',
                borderRadius: '16px',
                padding: '3rem',
                opacity: card3Opacity,
                transform: `translateX(${interpolate(card3Opacity, [0, 1], [50, 0])}px)`,
                border: '2px solid rgba(255, 255, 255, 0.3)',
              }}
            >
              <div
                style={{
                  fontSize: '5rem',
                  marginBottom: '1.5rem',
                  textAlign: 'center',
                }}
              >
                💚
              </div>
              <h3
                style={{
                  fontSize: '2rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '1rem',
                  textAlign: 'center',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                AI-Powered Gap Detection
              </h3>
              <p
                style={{
                  fontSize: '1.5rem',
                  color: 'rgba(255, 255, 255, 0.9)',
                  textAlign: 'center',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Identify interventions with ROI
              </p>
            </div>
          </div>
        </AbsoluteFill>
      )}

      {/* Frame 3: Before/After Comparison */}
      {comparisonOpacity > 0 && (
        <AbsoluteFill
          style={{
            justifyContent: 'center',
            alignItems: 'center',
            opacity: comparisonOpacity,
          }}
        >
          <div
            style={{
              display: 'flex',
              width: '90%',
              height: '70%',
              gap: '2rem',
            }}
          >
            {/* Left: Traditional (RED) */}
            <div
              style={{
                flex: 1,
                backgroundColor: 'rgba(239, 68, 68, 0.2)',
                borderRadius: '16px',
                padding: '3rem',
                border: '3px solid #EF4444',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
              }}
            >
              <div
                style={{
                  fontSize: '2.5rem',
                  fontWeight: 700,
                  color: '#EF4444',
                  marginBottom: '2rem',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                Traditional Approach
              </div>
              <div
                style={{
                  fontSize: '6rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '2rem',
                }}
              >
                3 months
              </div>
              <p
                style={{
                  fontSize: '2rem',
                  color: 'rgba(255, 255, 255, 0.9)',
                  textAlign: 'center',
                  marginBottom: '3rem',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                to identify gaps
              </p>
              {/* Slow progress bar */}
              <div
                style={{
                  width: '80%',
                  height: '30px',
                  backgroundColor: 'rgba(255, 255, 255, 0.2)',
                  borderRadius: '15px',
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    width: `${beforeProgress}%`,
                    height: '100%',
                    backgroundColor: '#EF4444',
                    transition: 'width 0.3s ease-out',
                  }}
                />
              </div>
              {frame >= 450 && (
                <div
                  style={{
                    marginTop: '2rem',
                    fontSize: '3rem',
                  }}
                >
                  ⏳
                </div>
              )}
            </div>

            {/* Right: HDIM (GREEN) */}
            <div
              style={{
                flex: 1,
                backgroundColor: 'rgba(16, 185, 129, 0.2)',
                borderRadius: '16px',
                padding: '3rem',
                border: '3px solid #10B981',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
              }}
            >
              <div
                style={{
                  fontSize: '2.5rem',
                  fontWeight: 700,
                  color: '#10B981',
                  marginBottom: '2rem',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                With HDIM
              </div>
              <div
                style={{
                  fontSize: '6rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '2rem',
                }}
              >
                2 seconds
              </div>
              <p
                style={{
                  fontSize: '2rem',
                  color: 'rgba(255, 255, 255, 0.9)',
                  textAlign: 'center',
                  marginBottom: '3rem',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                for real-time alerts
              </p>
              {/* Fast progress bar */}
              <div
                style={{
                  width: '80%',
                  height: '30px',
                  backgroundColor: 'rgba(255, 255, 255, 0.2)',
                  borderRadius: '15px',
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    width: `${afterProgress}%`,
                    height: '100%',
                    backgroundColor: '#10B981',
                    transition: 'width 0.3s ease-out',
                  }}
                />
              </div>
              {afterProgress >= 100 && (
                <div
                  style={{
                    marginTop: '2rem',
                    fontSize: '3rem',
                  }}
                >
                  ✅
                </div>
              )}
            </div>
          </div>
        </AbsoluteFill>
      )}

      {/* Frame 4: Transition to Demo */}
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
            See it in action...
          </h2>
        </AbsoluteFill>
      )}
    </BrandedContainer>
  );
};

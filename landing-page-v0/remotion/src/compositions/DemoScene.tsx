import React from 'react';
import { AbsoluteFill, interpolate, Sequence, useCurrentFrame, useVideoConfig } from 'remotion';
import { ScreenshotWithOverlay, Overlay } from '../components/ScreenshotWithOverlay';
import { BrandedContainer } from '../components/BrandedContainer';

interface DemoSceneProps {
  variant?: 'short' | 'default' | 'long';
}

export const DemoScene: React.FC<DemoSceneProps> = ({ variant = 'default' }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();

  // Frame 1: Provider Dashboard (0-8s = 0-240 frames)
  const mainDashboardOverlays: Overlay[] = [
    // Highlight "20 PATIENTS TODAY" at 2s
    {
      type: 'glow-highlight',
      startFrame: 60,
      duration: 120,
      position: { x: 15, y: 20 },
      props: {
        width: 400,
        height: 120,
        borderColor: 'green',
        pulseCount: 2,
      },
    },
    // Count up animation at 4s
    {
      type: 'metric',
      startFrame: 120,
      duration: 60,
      position: { x: 20, y: 23 },
      props: {
        from: 0,
        to: 20,
        suffix: '',
        fontSize: '2.5rem',
        duration: 30,
      },
    },
    // Highlight "76% QUALITY SCORE" at 5s
    {
      type: 'glow-highlight',
      startFrame: 150,
      duration: 90,
      position: { x: 55, y: 20 },
      props: {
        width: 400,
        height: 120,
        borderColor: 'green',
        pulseCount: 1,
      },
    },
    // Percentage count up at 6s
    {
      type: 'metric',
      startFrame: 180,
      duration: 45,
      position: { x: 62, y: 23 },
      props: {
        from: 0,
        to: 76,
        suffix: '%',
        fontSize: '2.5rem',
        duration: 45,
      },
    },
    // Text annotation at 7s
    {
      type: 'text',
      startFrame: 210,
      duration: 30,
      position: { x: 30, y: 70 },
      props: {
        text: 'One-click workflows',
        fontSize: '1.5rem',
      },
    },
  ];

  // Frame 2: Care Gap Management (8-18s = 240-540 frames)
  const careGapsOverlays: Overlay[] = [
    // Highlight total gaps at 10s (frame 300)
    {
      type: 'glow-highlight',
      startFrame: 60, // Relative to this sequence
      duration: 60,
      position: { x: 15, y: 15 },
      props: {
        width: 450,
        height: 100,
        borderColor: 'green',
        pulseCount: 2,
      },
    },
    // Breakdown badge at 11s
    {
      type: 'text',
      startFrame: 90,
      duration: 150,
      position: { x: 35, y: 22 },
      props: {
        text: '6 HIGH • 5 MEDIUM • 2 LOW',
        fontSize: '1.25rem',
        fontWeight: 700,
      },
    },
    // ROI Badge 8.2x at 14s
    {
      type: 'badge',
      startFrame: 180,
      duration: 120,
      position: { x: 20, y: 45 },
      props: {
        text: '8.2x ROI',
        backgroundColor: 'rgba(251, 191, 36, 0.9)',
        color: '#78350F',
        fontSize: '1.75rem',
      },
    },
    // ROI Badge 5.8x at 15s
    {
      type: 'badge',
      startFrame: 210,
      duration: 90,
      position: { x: 45, y: 52 },
      props: {
        text: '5.8x ROI',
        backgroundColor: 'rgba(251, 191, 36, 0.9)',
        color: '#78350F',
        fontSize: '1.75rem',
      },
    },
    // ROI Badge 12.5x at 16s
    {
      type: 'badge',
      startFrame: 240,
      duration: 60,
      position: { x: 70, y: 58 },
      props: {
        text: '12.5x ROI',
        backgroundColor: 'rgba(251, 191, 36, 0.9)',
        color: '#78350F',
        fontSize: '1.75rem',
      },
    },
    // Bottom text at 17s
    {
      type: 'text',
      startFrame: 270,
      duration: 30,
      position: { x: 25, y: 75 },
      props: {
        text: 'Prioritized by impact & cost-effectiveness',
        fontSize: '1.5rem',
      },
    },
  ];

  // Frame 3: HEDIS Measures (18-28s = 540-840 frames)
  const hedisOverlays: Overlay[] = [
    // Highlight BCS card at 20s
    {
      type: 'glow-highlight',
      startFrame: 60,
      duration: 90,
      position: { x: 10, y: 30 },
      props: {
        width: 500,
        height: 150,
        borderColor: 'blue',
        pulseCount: 1,
      },
    },
    // BCS metric at 21s
    {
      type: 'metric',
      startFrame: 90,
      duration: 30,
      position: { x: 18, y: 36 },
      props: {
        from: 0,
        to: 74.2,
        suffix: '%',
        decimals: 1,
        fontSize: '2rem',
        duration: 30,
      },
    },
    // Text overlay at 25s
    {
      type: 'text',
      startFrame: 210,
      duration: 90,
      position: { x: 30, y: 15 },
      props: {
        text: '6 active measures tracked in real-time',
        fontSize: '1.75rem',
        fontWeight: 700,
      },
    },
    // CMS Star badge at 26s
    {
      type: 'badge',
      startFrame: 240,
      duration: 60,
      position: { x: 70, y: 20 },
      props: {
        text: 'CMS Star Ratings Impact',
        backgroundColor: 'rgba(251, 191, 36, 0.9)',
        color: '#78350F',
        fontSize: '1.5rem',
      },
    },
  ];

  // Frame 4: Mobile Care Gaps (28-35s = 840-1050 frames)
  const mobileOverlays: Overlay[] = [
    // HIGH urgency pulse at 30s
    {
      type: 'badge',
      startFrame: 60,
      duration: 90,
      position: { x: 42, y: 35 },
      props: {
        text: 'HIGH',
        backgroundColor: 'rgba(239, 68, 68, 0.9)',
        color: 'white',
        fontSize: '1.5rem',
      },
    },
    // Text callout at 31s
    {
      type: 'text',
      startFrame: 90,
      duration: 60,
      position: { x: 35, y: 50 },
      props: {
        text: 'Depression screening overdue',
        fontSize: '1.5rem',
      },
    },
    // Bottom text at 33s
    {
      type: 'text',
      startFrame: 150,
      duration: 60,
      position: { x: 28, y: 75 },
      props: {
        text: 'Full functionality on mobile devices',
        fontSize: '1.5rem',
      },
    },
  ];

  // Frame 5: CTA (35-40s = 1050-1200 frames)
  const ctaOpacity = interpolate(frame, [1050, 1080], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const text1Opacity = interpolate(frame, [1080, 1110], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const text2Opacity = interpolate(frame, [1110, 1140], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const text3Opacity = interpolate(frame, [1140, 1170], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  const urlOpacity = interpolate(frame, [1170, 1200], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill>
      {/* Frame 1: Provider Dashboard (0-8s) */}
      <Sequence from={0} durationInFrames={240}>
        <ScreenshotWithOverlay
          screenshot="/assets/screenshots/main.png"
          overlays={mainDashboardOverlays}
          zoomLevel={1.05}
          panDirection="none"
        />
      </Sequence>

      {/* Frame 2: Care Gap Management (8-18s) */}
      <Sequence from={240} durationInFrames={300}>
        <ScreenshotWithOverlay
          screenshot="/assets/screenshots/care-gaps.png"
          overlays={careGapsOverlays}
          zoomLevel={1.08}
          panDirection="left"
        />
      </Sequence>

      {/* Frame 3: HEDIS Measures (18-28s) */}
      <Sequence from={540} durationInFrames={300}>
        <ScreenshotWithOverlay
          screenshot="/assets/screenshots/measures.png"
          overlays={hedisOverlays}
          zoomLevel={1.06}
          panDirection="none"
        />
      </Sequence>

      {/* Frame 4: Mobile Care Gaps (28-35s) */}
      <Sequence from={840} durationInFrames={210}>
        <ScreenshotWithOverlay
          screenshot="/assets/screenshots/mobile.png"
          overlays={mobileOverlays}
          zoomLevel={1.1}
          panDirection="none"
        />
      </Sequence>

      {/* Frame 5: Call to Action (35-40s) */}
      {frame >= 1050 && (
        <BrandedContainer variant="blue-teal" opacity={ctaOpacity}>
          <AbsoluteFill
            style={{
              justifyContent: 'center',
              alignItems: 'center',
              textAlign: 'center',
            }}
          >
            <div style={{ maxWidth: '80%' }}>
              {/* Logo */}
              <div
                style={{
                  fontSize: '8rem',
                  fontWeight: 700,
                  color: 'white',
                  marginBottom: '3rem',
                  textShadow: '0 0 30px rgba(0, 204, 136, 0.8)',
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                }}
              >
                HDIM
              </div>

              {/* Benefit 1 */}
              {text1Opacity > 0 && (
                <div
                  style={{
                    fontSize: '2.5rem',
                    fontWeight: 600,
                    color: 'white',
                    marginBottom: '1.5rem',
                    opacity: text1Opacity,
                    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                  }}
                >
                  ✓ Close care gaps 40% faster
                </div>
              )}

              {/* Benefit 2 */}
              {text2Opacity > 0 && (
                <div
                  style={{
                    fontSize: '2.5rem',
                    fontWeight: 600,
                    color: 'white',
                    marginBottom: '1.5rem',
                    opacity: text2Opacity,
                    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                  }}
                >
                  ✓ Improve HEDIS scores 12+ points
                </div>
              )}

              {/* CTA */}
              {text3Opacity > 0 && (
                <div
                  style={{
                    fontSize: '3rem',
                    fontWeight: 700,
                    color: 'white',
                    marginTop: '3rem',
                    marginBottom: '2rem',
                    opacity: text3Opacity,
                    textShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
                    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                  }}
                >
                  Try the interactive demo
                </div>
              )}

              {/* URL */}
              {urlOpacity > 0 && (
                <div
                  style={{
                    fontSize: '2rem',
                    fontWeight: 600,
                    color: '#00CC88',
                    opacity: urlOpacity,
                    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                  }}
                >
                  hdim-landing-page.vercel.app
                </div>
              )}
            </div>
          </AbsoluteFill>
        </BrandedContainer>
      )}
    </AbsoluteFill>
  );
};

import React from 'react';
import { AbsoluteFill, Img, interpolate, useCurrentFrame, useVideoConfig } from 'remotion';
import { GlowHighlight } from './GlowHighlight';
import { AnimatedMetric } from './AnimatedMetric';

export interface Overlay {
  type: 'glow-highlight' | 'metric' | 'text' | 'badge';
  startFrame: number;
  duration: number;
  position: { x: number; y: number }; // Percentage based
  props: any;
}

interface ScreenshotWithOverlayProps {
  screenshot: string;
  overlays?: Overlay[];
  zoomLevel?: number;
  panDirection?: 'left' | 'right' | 'none';
  blurBackground?: boolean;
  startFrame?: number;
}

export const ScreenshotWithOverlay: React.FC<ScreenshotWithOverlayProps> = ({
  screenshot,
  overlays = [],
  zoomLevel = 1.05,
  panDirection = 'none',
  blurBackground = false,
  startFrame = 0,
}) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();

  // Fade in animation
  const opacity = interpolate(
    frame - startFrame,
    [0, 30],
    [0, 1],
    {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    }
  );

  // Zoom animation
  const scale = interpolate(
    frame - startFrame,
    [0, 60],
    [1, zoomLevel],
    {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    }
  );

  // Pan animation (Ken Burns effect)
  let translateX = 0;
  if (panDirection === 'left') {
    translateX = interpolate(
      frame - startFrame,
      [0, 300],
      [0, -50],
      {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
      }
    );
  } else if (panDirection === 'right') {
    translateX = interpolate(
      frame - startFrame,
      [0, 300],
      [0, 50],
      {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
      }
    );
  }

  return (
    <AbsoluteFill
      style={{
        backgroundColor: '#111827',
        opacity,
      }}
    >
      {/* Screenshot */}
      <div
        style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          overflow: 'hidden',
        }}
      >
        <Img
          src={screenshot}
          style={{
            width: '90%',
            height: 'auto',
            borderRadius: '12px',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
            transform: `scale(${scale}) translateX(${translateX}px)`,
            filter: blurBackground ? 'blur(0px)' : 'none',
          }}
        />
      </div>

      {/* Overlays */}
      {overlays.map((overlay, index) => {
        const isVisible = frame >= overlay.startFrame && frame < overlay.startFrame + overlay.duration;

        if (!isVisible) return null;

        const overlayStyle: React.CSSProperties = {
          position: 'absolute',
          left: `${overlay.position.x}%`,
          top: `${overlay.position.y}%`,
        };

        switch (overlay.type) {
          case 'glow-highlight':
            return (
              <div key={index} style={overlayStyle}>
                <GlowHighlight
                  width={overlay.props.width}
                  height={overlay.props.height}
                  borderColor={overlay.props.borderColor || 'green'}
                  pulseCount={overlay.props.pulseCount || 2}
                  borderRadius={overlay.props.borderRadius || 8}
                  startFrame={overlay.startFrame}
                />
              </div>
            );

          case 'metric':
            return (
              <div key={index} style={overlayStyle}>
                <AnimatedMetric
                  from={overlay.props.from}
                  to={overlay.props.to}
                  suffix={overlay.props.suffix}
                  prefix={overlay.props.prefix}
                  duration={overlay.props.duration || 45}
                  delay={overlay.startFrame - frame}
                  decimals={overlay.props.decimals || 0}
                  fontSize={overlay.props.fontSize || '3rem'}
                  glowOnComplete={overlay.props.glowOnComplete ?? true}
                />
              </div>
            );

          case 'text':
            const textOpacity = interpolate(
              frame,
              [overlay.startFrame, overlay.startFrame + 20],
              [0, 1],
              {
                extrapolateLeft: 'clamp',
                extrapolateRight: 'clamp',
              }
            );

            return (
              <div
                key={index}
                style={{
                  ...overlayStyle,
                  opacity: textOpacity,
                }}
              >
                <div
                  style={{
                    padding: '1rem 2rem',
                    backgroundColor: 'rgba(0, 0, 0, 0.7)',
                    backdropFilter: 'blur(10px)',
                    borderRadius: '8px',
                    border: '2px solid rgba(0, 204, 136, 0.5)',
                    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                  }}
                >
                  <p
                    style={{
                      fontSize: overlay.props.fontSize || '1.5rem',
                      fontWeight: overlay.props.fontWeight || 600,
                      color: 'white',
                      margin: 0,
                    }}
                  >
                    {overlay.props.text}
                  </p>
                </div>
              </div>
            );

          case 'badge':
            const badgeOpacity = interpolate(
              frame,
              [overlay.startFrame, overlay.startFrame + 15],
              [0, 1],
              {
                extrapolateLeft: 'clamp',
                extrapolateRight: 'clamp',
              }
            );

            const badgeScale = interpolate(
              frame,
              [overlay.startFrame, overlay.startFrame + 10, overlay.startFrame + 15],
              [0, 1.2, 1],
              {
                extrapolateLeft: 'clamp',
                extrapolateRight: 'clamp',
              }
            );

            return (
              <div
                key={index}
                style={{
                  ...overlayStyle,
                  opacity: badgeOpacity,
                  transform: `scale(${badgeScale})`,
                }}
              >
                <div
                  style={{
                    padding: '0.75rem 1.5rem',
                    backgroundColor: overlay.props.backgroundColor || 'rgba(251, 191, 36, 0.9)',
                    borderRadius: '24px',
                    border: '2px solid rgba(251, 191, 36, 1)',
                    boxShadow: '0 0 20px rgba(251, 191, 36, 0.6)',
                    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                  }}
                >
                  <p
                    style={{
                      fontSize: overlay.props.fontSize || '1.75rem',
                      fontWeight: 700,
                      color: overlay.props.color || '#78350F',
                      margin: 0,
                    }}
                  >
                    {overlay.props.text}
                  </p>
                </div>
              </div>
            );

          default:
            return null;
        }
      })}
    </AbsoluteFill>
  );
};

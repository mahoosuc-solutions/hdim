import React from 'react';
import { AbsoluteFill, Sequence, staticFile } from 'remotion';
import { ScreenshotWithOverlay } from './ScreenshotWithOverlay';
import { TitleSlide } from './TitleSlide';
import { ProblemSlide } from './ProblemSlide';
import { NarrativeText } from './NarrativeText';
import { CTASlide } from './CTASlide';
import {
  RoleStoryConfig,
  VideoVariant,
  calculateTimings,
} from '../types/role-story.types';

interface RoleStoryVideoProps {
  config: RoleStoryConfig;
  variant?: VideoVariant;
}

/**
 * Data-driven role story video composition.
 *
 * Each role video is defined by a config object — not custom components.
 * The composition renders:
 *   TitleSlide -> ProblemSlide -> N screenshot scenes (with NarrativeText) -> CTASlide
 *
 * Scene durations scale down for the 'short' variant (65% of default).
 */
export const RoleStoryVideo: React.FC<RoleStoryVideoProps> = ({
  config,
  variant = 'default',
}) => {
  const timings = calculateTimings(config, variant);

  return (
    <AbsoluteFill>
      {/* Title slide */}
      <Sequence
        from={timings.titleSlide.from}
        durationInFrames={timings.titleSlide.duration}
      >
        <TitleSlide
          roleTitle={config.role.title}
          roleSubtitle={config.role.subtitle}
          headline={config.titleSlide.headline}
          subheadline={config.titleSlide.subheadline}
          accentColor={config.role.accentColor}
        />
      </Sequence>

      {/* Problem slide */}
      <Sequence
        from={timings.problemSlide.from}
        durationInFrames={timings.problemSlide.duration}
      >
        <ProblemSlide
          statement={config.problemSlide.statement}
          metric={config.problemSlide.metric}
          accentColor={config.role.accentColor}
        />
      </Sequence>

      {/* Screenshot scenes with narrative captions */}
      {config.scenes.map((scene, index) => {
        const timing = timings.scenes[index];

        return (
          <Sequence
            key={index}
            from={timing.from}
            durationInFrames={timing.duration}
          >
            <ScreenshotWithOverlay
              screenshot={staticFile(scene.screenshot)}
              overlays={scene.overlays}
              zoomLevel={scene.zoomLevel ?? 1.04}
              panDirection={scene.panDirection ?? 'none'}
            />
            <NarrativeText
              text={scene.narrativeCaption}
              accentColor={config.role.accentColor}
            />
          </Sequence>
        );
      })}

      {/* CTA slide */}
      <Sequence
        from={timings.cta.from}
        durationInFrames={timings.cta.duration}
      >
        <CTASlide config={config.cta} accentColor={config.role.accentColor} />
      </Sequence>
    </AbsoluteFill>
  );
};

import React from 'react';
import { AbsoluteFill, Sequence } from 'remotion';
import { SetupScene } from './compositions/care-gap-closure/SetupScene';
import { IdentificationScene } from './compositions/care-gap-closure/IdentificationScene';
import { ActionScene } from './compositions/care-gap-closure/ActionScene';
import { ImpactScene } from './compositions/care-gap-closure/ImpactScene';
import { OutcomeScene } from './compositions/care-gap-closure/OutcomeScene';

interface CareGapClosureVideoProps {
  variant?: 'short' | 'default';
}

export const CareGapClosureVideo: React.FC<CareGapClosureVideoProps> = ({ variant = 'default' }) => {
  // Timing for 60s (short) or 80s (default) video
  // All times in frames at 30fps

  // Default variant timing (80s = 2400 frames)
  const timings = variant === 'short'
    ? {
        // Short variant (60s = 1800 frames)
        setup: { from: 0, duration: 240 },           // 0-8s
        identification: { from: 240, duration: 360 }, // 8-20s
        action: { from: 600, duration: 480 },        // 20-36s
        impact: { from: 1080, duration: 360 },       // 36-48s
        outcome: { from: 1440, duration: 360 },      // 48-60s
      }
    : {
        // Default variant (80s = 2400 frames)
        setup: { from: 0, duration: 300 },           // 0-10s
        identification: { from: 300, duration: 450 }, // 10-25s
        action: { from: 750, duration: 600 },        // 25-45s
        impact: { from: 1350, duration: 450 },       // 45-60s
        outcome: { from: 1800, duration: 600 },      // 60-80s
      };

  return (
    <AbsoluteFill>
      {/* Scene 1: Setup - Provider logs into Clinical Portal */}
      <Sequence from={timings.setup.from} durationInFrames={timings.setup.duration}>
        <SetupScene variant={variant} />
      </Sequence>

      {/* Scene 2: Identification - Eleanor's high-priority care gap */}
      <Sequence from={timings.identification.from} durationInFrames={timings.identification.duration}>
        <IdentificationScene variant={variant} />
      </Sequence>

      {/* Scene 3: Action - Quick action to schedule screening */}
      <Sequence from={timings.action.from} durationInFrames={timings.action.duration}>
        <ActionScene variant={variant} />
      </Sequence>

      {/* Scene 4: Impact - Gap closed, statistics update */}
      <Sequence from={timings.impact.from} durationInFrames={timings.impact.duration}>
        <ImpactScene variant={variant} />
      </Sequence>

      {/* Scene 5: Outcome - Call to action */}
      <Sequence from={timings.outcome.from} durationInFrames={timings.outcome.duration}>
        <OutcomeScene variant={variant} />
      </Sequence>
    </AbsoluteFill>
  );
};

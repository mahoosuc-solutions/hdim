import React from 'react';
import { AbsoluteFill, Sequence } from 'remotion';
import { ProblemScene } from './compositions/ProblemScene';
import { SolutionScene } from './compositions/SolutionScene';
import { DemoScene } from './compositions/DemoScene';

// 60 second version for social media
export const VideoShort: React.FC = () => {
  return (
    <AbsoluteFill>
      {/* Scene 1: Problem (0-15s = 0-450 frames) */}
      <Sequence from={0} durationInFrames={450}>
        <ProblemScene variant="short" />
      </Sequence>

      {/* Scene 2: Solution (15-30s = 450-900 frames) */}
      <Sequence from={450} durationInFrames={450}>
        <SolutionScene variant="short" />
      </Sequence>

      {/* Scene 3: Demo (30-60s = 900-1800 frames) */}
      <Sequence from={900} durationInFrames={900}>
        <DemoScene variant="short" />
      </Sequence>
    </AbsoluteFill>
  );
};

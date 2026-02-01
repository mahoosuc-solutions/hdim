import React from 'react';
import { AbsoluteFill, Sequence } from 'remotion';
import { ProblemScene } from './compositions/ProblemScene';
import { SolutionScene } from './compositions/SolutionScene';
import { DemoScene } from './compositions/DemoScene';

export const Video: React.FC = () => {
  return (
    <AbsoluteFill>
      {/* Scene 1: Problem (0-25s = 0-750 frames) */}
      <Sequence from={0} durationInFrames={750}>
        <ProblemScene variant="default" />
      </Sequence>

      {/* Scene 2: Solution (25-50s = 750-1500 frames) */}
      <Sequence from={750} durationInFrames={750}>
        <SolutionScene variant="default" />
      </Sequence>

      {/* Scene 3: Demo (50-90s = 1500-2700 frames) */}
      <Sequence from={1500} durationInFrames={1200}>
        <DemoScene variant="default" />
      </Sequence>
    </AbsoluteFill>
  );
};

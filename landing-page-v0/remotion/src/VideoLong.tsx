import React from 'react';
import { AbsoluteFill, Sequence } from 'remotion';
import { ProblemScene } from './compositions/ProblemScene';
import { SolutionScene } from './compositions/SolutionScene';
import { DemoScene } from './compositions/DemoScene';

// 120 second version for YouTube
export const VideoLong: React.FC = () => {
  return (
    <AbsoluteFill>
      {/* Scene 1: Problem (0-30s = 0-900 frames) */}
      <Sequence from={0} durationInFrames={900}>
        <ProblemScene variant="long" />
      </Sequence>

      {/* Scene 2: Solution (30-60s = 900-1800 frames) */}
      <Sequence from={900} durationInFrames={900}>
        <SolutionScene variant="long" />
      </Sequence>

      {/* Scene 3: Demo (60-120s = 1800-3600 frames) */}
      <Sequence from={1800} durationInFrames={1800}>
        <DemoScene variant="long" />
      </Sequence>
    </AbsoluteFill>
  );
};

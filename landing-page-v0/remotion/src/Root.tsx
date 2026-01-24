import React from 'react';
import { Composition } from 'remotion';
import { Video } from './Video';
import { VideoShort } from './VideoShort';
import { VideoLong } from './VideoLong';
import { TestSimple } from './TestSimple';
import { CareGapClosureVideo } from './CareGapClosureVideo';

export const RemotionRoot: React.FC = () => {
  return (
    <>
      {/* 90 second default version */}
      <Composition
        id="Main"
        component={Video}
        durationInFrames={2700} // 90 seconds at 30fps
        fps={30}
        width={1920}
        height={1080}
      />

      {/* 60 second social media cut */}
      <Composition
        id="Short"
        component={VideoShort}
        durationInFrames={1800} // 60 seconds at 30fps
        fps={30}
        width={1920}
        height={1080}
      />

      {/* 120 second YouTube cut */}
      <Composition
        id="Long"
        component={VideoLong}
        durationInFrames={3600} // 120 seconds at 30fps
        fps={30}
        width={1920}
        height={1080}
      />

      {/* Test composition */}
      <Composition
        id="Test"
        component={TestSimple}
        durationInFrames={150} // 5 seconds
        fps={30}
        width={1920}
        height={1080}
      />

      {/* Care Gap Closure Demo - Default (80 seconds) */}
      <Composition
        id="CareGapClosure"
        component={CareGapClosureVideo}
        durationInFrames={2400} // 80 seconds at 30fps
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ variant: 'default' }}
      />

      {/* Care Gap Closure Demo - Short (60 seconds) */}
      <Composition
        id="CareGapClosureShort"
        component={CareGapClosureVideo}
        durationInFrames={1800} // 60 seconds at 30fps
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ variant: 'short' }}
      />
    </>
  );
};

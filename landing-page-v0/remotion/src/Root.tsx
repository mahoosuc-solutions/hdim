import { Composition } from 'remotion';
import { ProblemScene } from './compositions/ProblemScene';

export const RemotionRoot: React.FC = () => {
  return (
    <>
      {/* 90 second default version */}
      <Composition
        id="Main"
        component={ProblemScene}
        durationInFrames={750} // 25 seconds at 30fps (just Problem scene for now)
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{
          variant: 'default',
        }}
      />

      {/* 60 second social media cut */}
      <Composition
        id="Short"
        component={ProblemScene}
        durationInFrames={450} // 15 seconds at 30fps
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{
          variant: 'short',
        }}
      />

      {/* 120 second YouTube cut */}
      <Composition
        id="Long"
        component={ProblemScene}
        durationInFrames={900} // 30 seconds at 30fps
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{
          variant: 'long',
        }}
      />
    </>
  );
};

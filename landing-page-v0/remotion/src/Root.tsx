import React from 'react';
import { Composition } from 'remotion';
import { Video } from './Video';
import { VideoShort } from './VideoShort';
import { VideoLong } from './VideoLong';
import { TestSimple } from './TestSimple';
import { CareGapClosureVideo } from './CareGapClosureVideo';
import { RoleStoryWrapper } from './compositions/role-videos/RoleStoryWrapper';

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

      {/* Care Gap Closure Demo - Ultra-Short (30 seconds) */}
      <Composition
        id="CareGapClosure30"
        component={CareGapClosureVideo}
        durationInFrames={900} // 30 seconds at 30fps
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ variant: 'ultra-short' }}
      />

      {/* ── Role Story Videos (7 roles × 2 variants = 14) ── */}

      {/* Care Manager */}
      <Composition
        id="RoleCareManager"
        component={RoleStoryWrapper}
        durationInFrames={2700}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'care-manager' as const, variant: 'default' as const }}
      />
      <Composition
        id="RoleCareManagerShort"
        component={RoleStoryWrapper}
        durationInFrames={1833}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'care-manager' as const, variant: 'short' as const }}
      />

      {/* CMO / VP Quality */}
      <Composition
        id="RoleCMO"
        component={RoleStoryWrapper}
        durationInFrames={2700}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'cmo' as const, variant: 'default' as const }}
      />
      <Composition
        id="RoleCMOShort"
        component={RoleStoryWrapper}
        durationInFrames={1833}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'cmo' as const, variant: 'short' as const }}
      />

      {/* Quality Analyst */}
      <Composition
        id="RoleQualityAnalyst"
        component={RoleStoryWrapper}
        durationInFrames={2700}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'quality-analyst' as const, variant: 'default' as const }}
      />
      <Composition
        id="RoleQualityAnalystShort"
        component={RoleStoryWrapper}
        durationInFrames={1833}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'quality-analyst' as const, variant: 'short' as const }}
      />

      {/* Provider / Physician */}
      <Composition
        id="RoleProvider"
        component={RoleStoryWrapper}
        durationInFrames={2700}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'provider' as const, variant: 'default' as const }}
      />
      <Composition
        id="RoleProviderShort"
        component={RoleStoryWrapper}
        durationInFrames={1833}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'provider' as const, variant: 'short' as const }}
      />

      {/* Data Analyst */}
      <Composition
        id="RoleDataAnalyst"
        component={RoleStoryWrapper}
        durationInFrames={2700}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'data-analyst' as const, variant: 'default' as const }}
      />
      <Composition
        id="RoleDataAnalystShort"
        component={RoleStoryWrapper}
        durationInFrames={1833}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'data-analyst' as const, variant: 'short' as const }}
      />

      {/* Administrator */}
      <Composition
        id="RoleAdmin"
        component={RoleStoryWrapper}
        durationInFrames={2700}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'admin' as const, variant: 'default' as const }}
      />
      <Composition
        id="RoleAdminShort"
        component={RoleStoryWrapper}
        durationInFrames={1833}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'admin' as const, variant: 'short' as const }}
      />

      {/* AI User */}
      <Composition
        id="RoleAIUser"
        component={RoleStoryWrapper}
        durationInFrames={2700}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'ai-user' as const, variant: 'default' as const }}
      />
      <Composition
        id="RoleAIUserShort"
        component={RoleStoryWrapper}
        durationInFrames={1833}
        fps={30}
        width={1920}
        height={1080}
        defaultProps={{ role: 'ai-user' as const, variant: 'short' as const }}
      />
    </>
  );
};

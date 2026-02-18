import { Overlay } from '../components/ScreenshotWithOverlay';

export type RoleType =
  | 'care-manager'
  | 'cmo'
  | 'quality-analyst'
  | 'provider'
  | 'data-analyst'
  | 'admin'
  | 'ai-user';

export type VideoVariant = 'short' | 'default';

export interface RoleInfo {
  title: string;
  subtitle: string;
  accentColor: string; // CSS color for role-specific highlights
}

export interface TitleSlideConfig {
  headline: string;
  subheadline: string;
  durationFrames: number;
}

export interface ProblemSlideConfig {
  statement: string;
  metric: string; // e.g., "73% of care gaps take >30 days to close"
  durationFrames: number;
}

export interface SceneConfig {
  screenshot: string; // staticFile path relative to public/
  overlays: Overlay[];
  zoomLevel?: number;
  panDirection?: 'left' | 'right' | 'none';
  durationFrames: number;
  narrativeCaption: string; // Clinical annotation subtitle
}

export interface CTASlideConfig {
  headline: string;
  highlightText: string; // Accent-colored text below headline
  stats: Array<{
    value: string;
    backgroundColor: string;
    borderColor: string;
    textColor: string;
    glowColor: string;
  }>;
  ctaText: string;
  ctaUrl: string;
  durationFrames: number;
}

export interface RoleStoryConfig {
  role: RoleInfo;
  titleSlide: TitleSlideConfig;
  problemSlide: ProblemSlideConfig;
  scenes: SceneConfig[];
  cta: CTASlideConfig;
}

/**
 * Calculates per-scene frame allocations for a given variant.
 *
 * Fixed slides (title, problem, CTA) keep their configured durations.
 * Short variant scales scene durations to 65% of default.
 */
export function calculateTimings(
  config: RoleStoryConfig,
  variant: VideoVariant
): {
  totalFrames: number;
  titleSlide: { from: number; duration: number };
  problemSlide: { from: number; duration: number };
  scenes: Array<{ from: number; duration: number }>;
  cta: { from: number; duration: number };
} {
  const sceneScale = variant === 'short' ? 0.65 : 1;
  const fixedScale = variant === 'short' ? 0.75 : 1;

  const titleDuration = Math.round(config.titleSlide.durationFrames * fixedScale);
  const problemDuration = Math.round(config.problemSlide.durationFrames * fixedScale);
  const ctaDuration = Math.round(config.cta.durationFrames * fixedScale);

  let cursor = 0;

  const titleSlide = { from: cursor, duration: titleDuration };
  cursor += titleDuration;

  const problemSlide = { from: cursor, duration: problemDuration };
  cursor += problemDuration;

  const scenes = config.scenes.map((scene) => {
    const duration = Math.round(scene.durationFrames * sceneScale);
    const entry = { from: cursor, duration };
    cursor += duration;
    return entry;
  });

  const cta = { from: cursor, duration: ctaDuration };
  cursor += ctaDuration;

  return { totalFrames: cursor, titleSlide, problemSlide, scenes, cta };
}

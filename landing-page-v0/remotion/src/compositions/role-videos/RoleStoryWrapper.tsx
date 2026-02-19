import React from 'react';
import { RoleStoryVideo } from '../../components/RoleStoryVideo';
import { RoleStoryConfig, RoleType, VideoVariant } from '../../types/role-story.types';

// Lazy config imports — each role defines its own config file
import { careManagerConfig } from './care-manager-video.config';
import { cmoConfig } from './cmo-video.config';
import { qualityAnalystConfig } from './quality-analyst-video.config';
import { providerConfig } from './provider-video.config';
import { dataAnalystConfig } from './data-analyst-video.config';
import { adminConfig } from './admin-video.config';
import { aiUserConfig } from './ai-user-video.config';

const CONFIG_MAP: Record<RoleType, RoleStoryConfig> = {
  'care-manager': careManagerConfig,
  cmo: cmoConfig,
  'quality-analyst': qualityAnalystConfig,
  provider: providerConfig,
  'data-analyst': dataAnalystConfig,
  admin: adminConfig,
  'ai-user': aiUserConfig,
};

// Props must all be optional for Remotion's Composition defaultProps to work
interface RoleStoryWrapperProps {
  role?: RoleType;
  variant?: VideoVariant;
}

export const RoleStoryWrapper: React.FC<RoleStoryWrapperProps> = ({
  role = 'care-manager',
  variant = 'default',
}) => {
  const config = CONFIG_MAP[role];
  return <RoleStoryVideo config={config} variant={variant} />;
};

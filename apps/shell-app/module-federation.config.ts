import { ModuleFederationConfig } from '@nx/module-federation';

const config: ModuleFederationConfig = {
  name: 'shell-app',
  remotes: ['mfePatients'],
};

export const sharedMappings = new Map([
  ['@health-platform/shared/data-access', { singleton: true, strictVersion: true, requiredVersion: 'auto' }],
  ['@health-platform/shared/util-auth', { singleton: true, strictVersion: true, requiredVersion: 'auto' }],
  ['@health-platform/shared/ui-common', { singleton: true, strictVersion: true, requiredVersion: 'auto' }],
]);

/**
 * Nx requires a default export of the config to allow correct resolution of the module federation graph.
 **/
export default config;

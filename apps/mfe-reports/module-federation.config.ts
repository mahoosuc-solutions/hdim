import { ModuleFederationConfig } from '@nx/module-federation';

const config: ModuleFederationConfig = {
  name: 'mfeReports',
  exposes: {
    './Routes': 'apps/mfe-reports/src/app/remote-entry/entry.routes.ts',
  },
};

export default config;

export default {
  displayName: 'clinical-portal',
  preset: '../../jest.preset.js',
  setupFilesAfterEnv: [
    '<rootDir>/src/test-setup.ts',
    '<rootDir>/src/testing/setup-accessibility-tests.ts',
  ],
  coverageDirectory: '../../coverage/apps/clinical-portal',
  transform: {
    '^.+\\.(ts|mjs|js|html)$': [
      'jest-preset-angular',
      {
        tsconfig: '<rootDir>/tsconfig.spec.json',
        stringifyContentPathRegex: '\\.(html|svg)$',
      },
    ],
  },
  transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$|d3-.*|internmap|delaunator|robust-predicates|lodash-es)'],
  snapshotSerializers: [
    'jest-preset-angular/build/serializers/no-ng-attributes',
    'jest-preset-angular/build/serializers/ng-snapshot',
    'jest-preset-angular/build/serializers/html-comment',
  ],
};

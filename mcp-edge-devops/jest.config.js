/** @type {import('jest').Config} */
module.exports = {
  testMatch: ['**/__tests__/**/*.test.js'],
  testPathIgnorePatterns: ['/node_modules/', '\\.live\\.test\\.js$'],
  collectCoverageFrom: ['lib/**/*.js', 'server.js'],
  coverageProvider: 'v8',
  coverageThreshold: {
    global: { statements: 95, branches: 95, functions: 95, lines: 95 }
  },
  forceExit: true
};

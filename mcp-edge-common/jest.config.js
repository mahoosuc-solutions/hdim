/** @type {import('jest').Config} */
module.exports = {
  testMatch: ['**/__tests__/**/*.test.js'],
  testPathIgnorePatterns: ['/node_modules/'],
  collectCoverageFrom: ['lib/**/*.js'],
  coverageProvider: 'v8',
  coverageThreshold: {
    global: { statements: 95, branches: 85, functions: 95, lines: 95 }
  }
};

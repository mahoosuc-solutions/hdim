const fs = require('node:fs');
const path = require('node:path');

function isDemoMode() {
  return ['1', 'true', 'yes'].includes(
    String(process.env.HDIM_DEMO_MODE || '').trim().toLowerCase()
  );
}

function loadFixture(fixturesDir, toolName) {
  const filePath = path.join(fixturesDir, `${toolName}.json`);
  try {
    const raw = fs.readFileSync(filePath, 'utf8');
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function createDemoInterceptor(fixturesDir) {
  return async function intercept(toolName, args, realHandler) {
    if (!isDemoMode()) {
      return realHandler(args);
    }

    const fixture = loadFixture(fixturesDir, toolName);
    if (fixture) {
      return { content: [{ type: 'text', text: JSON.stringify(fixture, null, 2) }] };
    }

    // No fixture — fall through to real handler
    return realHandler(args);
  };
}

module.exports = { isDemoMode, loadFixture, createDemoInterceptor };

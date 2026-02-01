import { bundle } from '@remotion/bundler';
import { renderMedia, selectComposition } from '@remotion/renderer';
import path from 'path';

const compositions = {
  '60s': { id: 'Short', output: 'hdim-demo-60s.mp4' },
  '90s': { id: 'Main', output: 'hdim-demo-90s.mp4' },
  '120s': { id: 'Long', output: 'hdim-demo-120s.mp4' },
};

async function renderVideo(compositionName) {
  console.log(`🎬 Rendering ${compositionName} composition...`);

  // Step 1: Bundle Remotion project
  console.log('📦 Bundling project...');
  const bundled = await bundle({
    entryPoint: path.join(process.cwd(), 'src/index.ts'),
    webpackOverride: (config) => config,
  });

  // Step 2: Get composition metadata
  const composition = await selectComposition({
    serveUrl: bundled,
    id: compositions[compositionName].id,
  });

  // Step 3: Render video
  console.log(`🎥 Rendering ${composition.durationInFrames} frames at ${composition.fps} fps...`);

  await renderMedia({
    composition,
    serveUrl: bundled,
    codec: 'h264',
    outputLocation: path.join(process.cwd(), 'out', compositions[compositionName].output),
    chromiumOptions: {
      // WSL2-friendly flags
      args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
    },
    concurrency: 50, // Use 50% of CPU cores
    onProgress: ({ renderedFrames, encodedFrames, totalFrames }) => {
      const progress = Math.round((renderedFrames / totalFrames) * 100);
      console.log(`Progress: ${progress}% (${renderedFrames}/${totalFrames} frames)`);
    },
  });

  console.log(`✅ Video saved: out/${compositions[compositionName].output}`);
}

// Parse CLI argument
const compositionArg = process.argv[2] || '90s';

if (!compositions[compositionArg]) {
  console.error(`❌ Invalid composition: ${compositionArg}`);
  console.error(`Valid options: ${Object.keys(compositions).join(', ')}`);
  process.exit(1);
}

renderVideo(compositionArg).catch((err) => {
  console.error('❌ Render failed:', err);
  process.exit(1);
});

import { Config } from '@remotion/cli/config';

Config.setCodec('h264');
Config.setVideoImageFormat('png');
Config.setPixelFormat('yuv420p');
Config.setChromeMode('chrome-for-testing');

// WSL2/Docker optimizations
Config.setEncodingTimeout(300000);         // 5 minutes (WSL2 can be slower)
Config.setConcurrency('50%');              // Use 50% of CPU cores (prevent overload)
Config.setChromiumDisableParallelRendering(false);  // Enable multi-process on Linux

Config.overrideWebpackConfig((config) => {
  return config;
});

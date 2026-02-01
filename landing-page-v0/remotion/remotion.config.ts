import { Config } from '@remotion/cli/config';

Config.setCodec('h264');
Config.setVideoImageFormat('png');
Config.setPixelFormat('yuv420p');

// WSL2/Docker optimizations
Config.setConcurrency('50%');              // Use 50% of CPU cores (prevent overload)
Config.setTimeoutInMilliseconds(300000);   // 5 minutes timeout (WSL2 can be slower)

Config.overrideWebpackConfig((config) => {
  return config;
});

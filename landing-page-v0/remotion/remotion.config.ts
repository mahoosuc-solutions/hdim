import { Config } from '@remotion/cli/config';

Config.setCodec('h264');
Config.setVideoImageFormat('png');
Config.setPixelFormat('yuv420p');
Config.setChromeMode('chrome-for-testing');

Config.overrideWebpackConfig((config) => {
  return config;
});

import { Config } from '@remotion/cli/config';

Config.setCodec('h264');
Config.setVideoImageFormat('png');
Config.setPixelFormat('yuv420p');

Config.overrideWebpackConfig((config) => {
  return config;
});

const path = require('path');
const pak = require('../package.json');

module.exports = {
  project: {
    android: {
      sourceDir: './android',
    },
    ios: {
      sourceDir: './ios',
    },
  },
  dependencies: {
    [pak.name]: {
      root: path.join(__dirname, '..'),
    },
  },
};
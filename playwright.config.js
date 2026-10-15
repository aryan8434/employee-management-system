const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: process.env.APP_URL || 'http://127.0.0.1:8080',
    browserName: 'chromium',
    channel: process.env.BROWSER_CHANNEL || undefined,
    screenshot: 'only-on-failure'
  }
});

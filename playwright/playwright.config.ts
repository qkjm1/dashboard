import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 30000,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  use: {
    headless: true,
    viewport: { width: 1280, height: 720 },
    actionTimeout: 10000,
    ignoreHTTPSErrors: true,
    baseURL: 'http://localhost:8080',
    screenshot: 'only-on-failure',   // 실패 시 스크린샷 자동 저장
    video: 'retain-on-failure'       // 실패 시 비디오 자동 저장
  },
});

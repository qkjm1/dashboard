import { test, expect } from '@playwright/test';

test('만료된 링크 접근 시 404', async ({ page }) => {
  const expiredSlug = 'expired123';
  await page.goto(`/r/${expiredSlug}`);
  await expect(page).toHaveTitle(/404/); // 404 페이지 존재 시
});

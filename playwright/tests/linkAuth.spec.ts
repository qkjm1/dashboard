import { test, expect } from '@playwright/test';

test('권한 없는 사용자 링크 접근', async ({ page }) => {
  const privateSlug = 'private123';
  await page.goto(`/r/${privateSlug}`);
  await expect(page).toHaveTitle(/403/); // 권한 없을 때
});

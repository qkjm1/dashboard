import { test, expect } from '@playwright/test';

test('링크 리다이렉트', async ({ page }) => {
  // 실제 slug 값으로 바꿔야 함
  const slug = 'abc123';
  await page.goto(`/r/${slug}`);

  // 이동 후 URL 검증 (실제 테스트 시 서버에서 리다이렉트)
  await expect(page).toHaveURL('https://www.google.com');
});

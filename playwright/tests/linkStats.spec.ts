import { test, expect } from '@playwright/test';

test('클릭 통계 반영', async ({ request }) => {
  const slug = 'abc123';
  // 클릭 API 호출
  await request.post(`/click/${slug}`);

  // 클릭 수 조회
  const res = await request.get(`/click/${slug}/count`);
  const count = await res.json();
  expect(count).toBeGreaterThan(0);
});

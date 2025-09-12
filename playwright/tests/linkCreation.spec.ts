import { test, expect } from '@playwright/test';

test('링크 생성', async ({ request }) => {
  const response = await request.post('/links', {
    data: { url: 'https://www.google.com' }
  });

  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.slug).toBeDefined();
  expect(body.originalUrl).toBe('https://www.google.com');
});

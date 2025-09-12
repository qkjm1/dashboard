import { request } from '@playwright/test';
import * as fs from 'fs';

const BASE_URL = 'http://localhost:8080/r/';
const SLUG = 'abc123';
const THREAD_COUNT = 10; 
const REQUESTS_PER_THREAD = 10;

type Result = { responseTimeMs: number; statusCode: number };

(async () => {
  const allResults: Result[] = [];

  for (let t = 0; t < THREAD_COUNT; t++) {
    for (let r = 0; r < REQUESTS_PER_THREAD; r++) {
      const start = Date.now();
      let status = 0;
      try {
        const response = await request.newContext().get(BASE_URL + SLUG);
        status = response.status();
      } catch {
        status = 500;
      }
      const end = Date.now();
      allResults.push({ responseTimeMs: end - start, statusCode: status });
    }
  }

  // CSV 기록
  const csvLines = ['RequestNo,ResponseTimeMs,StatusCode'];
  let i = 1;
  let totalTime = 0;
  let maxTime = 0;
  let minTime = Number.MAX_SAFE_INTEGER;
  let successCount = 0;

  allResults.forEach(r => {
    csvLines.push(`${i++},${r.responseTimeMs},${r.statusCode}`);
    totalTime += r.responseTimeMs;
    maxTime = Math.max(maxTime, r.responseTimeMs);
    minTime = Math.min(minTime, r.responseTimeMs);
    if (r.statusCode === 200) successCount++;
  });

  const avgTime = totalTime / allResults.length;
  const successRate = (successCount / allResults.length) * 100;

  csvLines.push('');
  csvLines.push(`TotalRequests,AvgResponseTimeMs,MaxResponseTimeMs,MinResponseTimeMs,SuccessRate%`);
  csvLines.push(`${allResults.length},${avgTime},${maxTime},${minTime},${successRate}`);

  fs.writeFileSync('performance_report.csv', csvLines.join('\n'));
  console.log('부하 테스트 완료. performance_report.csv 생성됨.');
})();

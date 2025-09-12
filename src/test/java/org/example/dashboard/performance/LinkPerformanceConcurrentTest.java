package org.example.dashboard.performance;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LinkPerformanceConcurrentTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:8080";

    // 요청을 지속적으로 보낸 후 응답 시간 및 실패 성공 여부 확인
    @Test
    void testConcurrentLinkPerformance() throws InterruptedException {
        int threadCount = 10;       // 동시에 요청을 보낼 스레드 수
        int requestsPerThread = 10; // 각 스레드가 보낼 요청 수

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * requestsPerThread);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        // 1️⃣ 링크 생성
                        ResponseEntity<String> createResponse = restTemplate.getForEntity(
                                baseUrl + "/links?url=https://www.google.com", String.class);

                        if (createResponse.getStatusCode().is2xxSuccessful()) {
                            successCount.incrementAndGet();
                            // JSON에서 slug 추출 (간단하게 substring 사용)
                            String body = createResponse.getBody();
                            String slug = body.split("\"slug\":\"")[1].split("\"")[0];

                            // 2️⃣ 리다이렉트 요청
                            ResponseEntity<String> redirectResponse = restTemplate.getForEntity(
                                    baseUrl + "/r/" + slug, String.class);

                            if (redirectResponse.getStatusCode().is3xxRedirection()) {
                                successCount.incrementAndGet();
                            } else {
                                failCount.incrementAndGet();
                            }
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        latch.await(); // 모든 요청이 끝날 때까지 대기
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        int totalRequests = threadCount * requestsPerThread * 2; // 생성+리다이렉트
        System.out.println("총 요청: " + totalRequests);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("총 시간(ms): " + (endTime - startTime));
        System.out.println("평균 요청 시간(ms): " + ((endTime - startTime) / (double) totalRequests));
    }
}

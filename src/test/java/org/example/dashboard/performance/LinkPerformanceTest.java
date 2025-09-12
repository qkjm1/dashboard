package org.example.dashboard.performance;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LinkPerformanceTest {

    private static final String BASE_URL = "http://localhost:8080/r/";
    private static final int THREAD_COUNT = 50;
    private static final int REQUESTS_PER_THREAD = 10;
    private static final String SLUG = "abc123";

    static class Result {
        long responseTimeMs;
        int statusCode;

        Result(long responseTimeMs, int statusCode) {
            this.responseTimeMs = responseTimeMs;
            this.statusCode = statusCode;
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        RestTemplate restTemplate = new RestTemplate();
        List<Future<List<Result>>> futures = new ArrayList<>();

        // 요청 생성
        for (int t = 0; t < THREAD_COUNT; t++) {
            futures.add(executor.submit(() -> {
                List<Result> threadResults = new ArrayList<>();
                for (int r = 0; r < REQUESTS_PER_THREAD; r++) {
                    Instant start = Instant.now();
                    int status;
                    try {
                        restTemplate.getForEntity(BASE_URL + SLUG, String.class);
                        status = 200;
                    } catch (Exception e) {
                        status = 500;
                    }
                    Instant end = Instant.now();
                    long durationMs = Duration.between(start, end).toMillis();
                    threadResults.add(new Result(durationMs, status));
                }
                return threadResults;
            }));
        }

        List<Result> allResults = new ArrayList<>();
        for (Future<List<Result>> f : futures) {
            try {
                allResults.addAll(f.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();

        // CSV 기록
        long totalTime = 0;
        long maxTime = Long.MIN_VALUE;
        long minTime = Long.MAX_VALUE;
        int successCount = 0;
        int failCount = 0;

        try (FileWriter csv = new FileWriter("performance_results.csv")) {
            csv.append("RequestNo,ResponseTimeMs,StatusCode\n");
            int requestNo = 1;
            for (Result r : allResults) {
                csv.append(requestNo++ + "," + r.responseTimeMs + "," + r.statusCode + "\n");
                totalTime += r.responseTimeMs;
                maxTime = Math.max(maxTime, r.responseTimeMs);
                minTime = Math.min(minTime, r.responseTimeMs);
                if (r.statusCode == 200) successCount++;
                else failCount++;
            }

            double avgTime = allResults.isEmpty() ? 0 : (double) totalTime / allResults.size();
            double successRate = allResults.isEmpty() ? 0 : (double) successCount / allResults.size() * 100;

            csv.append("\n");
            csv.append("TotalRequests,AvgResponseTimeMs,MaxResponseTimeMs,MinResponseTimeMs,SuccessRate%\n");
            csv.append(allResults.size() + "," + avgTime + "," + maxTime + "," + minTime + "," + successRate + "\n");
        }

        System.out.println("CSV 생성 완료: performance_results.csv");

        // 히스토그램 생성
        HistogramDataset histogramDataset = new HistogramDataset();
        double[] times = allResults.stream().mapToDouble(r -> r.responseTimeMs).toArray();
        histogramDataset.addSeries("Response Time", times, 20); // 20 bins
        JFreeChart histogram = ChartFactory.createHistogram(
                "Response Time Histogram",
                "Time (ms)",
                "Frequency",
                histogramDataset
        );
        ChartUtils.saveChartAsPNG(new File("response_time_histogram.png"), histogram, 800, 600);
        System.out.println("히스토그램 생성 완료: response_time_histogram.png");

        // 파이 차트 생성 (성공/실패)
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        pieDataset.setValue("Success", successCount);
        pieDataset.setValue("Fail", failCount);
        JFreeChart pieChart = ChartFactory.createPieChart(
                "Request Success/Fail Rate",
                pieDataset,
                true,
                true,
                false
        );
        ChartUtils.saveChartAsPNG(new File("success_fail_pie.png"), pieChart, 600, 400);
        System.out.println("파이 차트 생성 완료: success_fail_pie.png");
    }
}

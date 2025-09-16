package org.example.dashboard.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.contains;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class APIIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long linkId;

    @BeforeEach
    void setupData() {
        // 기존 데이터 삭제
        jdbcTemplate.update("DELETE FROM click_log WHERE link_id IN (SELECT id FROM link WHERE slug = ?)", "testSlug");
        jdbcTemplate.update("DELETE FROM link WHERE slug = ?", "testSlug");

        // 테스트 링크 생성
        jdbcTemplate.update("INSERT INTO link (slug, original_url, active) VALUES (?, ?, ?)",
                "testSlug", "https://example.com", true);

        linkId = jdbcTemplate.queryForObject("SELECT id FROM link WHERE slug = ?", Long.class, "testSlug");
    }

    private void addClick(Long linkId, String ip, String source, String device, String os, String browser, String country, int minutesOffset, LocalDateTime baseTime) {
        LocalDateTime clickedAt = baseTime.minusMinutes(minutesOffset);

        jdbcTemplate.update(
                "INSERT INTO click_log (link_id, clicked_at, ip_hash, channel, device_type, os, browser, user_agent, country_code) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                linkId, clickedAt, ip, source, device, os, browser, "Mozilla/5.0", country
        );
    }

    // -----------------------------
    // Unique Stats 테스트
    // -----------------------------
    @Test
    void testUniqueStatsIntegration() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now();

        addClick(linkId, "ip1", "qr", "DESKTOP", "Windows", "Chrome", "KR", 0, baseTime);
        addClick(linkId, "ip2", "qr", "MOBILE", "Android", "Chrome", "KR", 5, baseTime);
        addClick(linkId, "ip1", "qr", "DESKTOP", "Windows", "Chrome", "KR", 1, baseTime);

        mockMvc.perform(get("/api/links/testSlug/unique-stats")
                        .param("windowMinutes", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(3))
                .andExpect(jsonPath("$.uniqueApprox").value(2))
                .andExpect(jsonPath("$.duplicateRatio").value(1.0 / 3))
                .andExpect(jsonPath("$.uniqueWindowed").value(2))
                .andExpect(jsonPath("$.windowMinutes").value(10));
    }

    // -----------------------------
    // Scan Distribution 테스트
    // -----------------------------
    @Test
    void testScanDistributionIntegration() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        addClick(linkId, "ip1", "QR:test", "DESKTOP", "Windows", "Chrome", "KR", 5, baseTime);
        addClick(linkId, "ip2", "QR:test", "MOBILE", "Android", "Chrome", "KR", 2, baseTime);

        mockMvc.perform(get("/api/links/testSlug/scan-distribution")
                        .param("source", "qr")
                        .param("granularity", "hour")
                        .param("start", baseTime.minusMinutes(10).toString())
                        .param("end", baseTime.plusMinutes(59).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cnt").value(2)); // Mapper에서 cnt 컬럼명
    }

    // -----------------------------
    // Source 필터 테스트
    // -----------------------------
    @Test
    void testSourceFilterIntegration() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now();

        addClick(linkId, "ip1", "QR:test", "DESKTOP", "Windows", "Chrome", "KR", 0, baseTime);
        addClick(linkId, "ip2", "link", "DESKTOP", "Mac", "Safari", "US", 1, baseTime);

        // QR 소스만
        mockMvc.perform(get("/api/links/testSlug/scan-distribution")
                        .param("source", "qr")
                        .param("granularity", "hour"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cnt").value(1));

        // LINK 소스만
        mockMvc.perform(get("/api/links/testSlug/scan-distribution")
                        .param("source", "link")
                        .param("granularity", "hour"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cnt").value(1));
    }

    // -----------------------------
    // Country 필터 테스트
    // -----------------------------
    @Test
    void testCountryFilterIntegration() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now();

        // 클릭 데이터 추가 (KR 2회, US 1회)
        addClick(linkId, "ip1", "QR:test", "DESKTOP", "Windows", "Chrome", "KR", 0, baseTime);
        addClick(linkId, "ip2", "QR:test", "MOBILE", "Android", "Chrome", "KR", 1, baseTime);
        addClick(linkId, "ip3", "QR:test", "DESKTOP", "Mac", "Safari", "US", 1, baseTime);

        mockMvc.perform(get("/api/links/testSlug/geo/countries") // endpoint 수정
                        .param("start", baseTime.minusDays(1).toString())
                        .param("end", baseTime.plusDays(1).toString()))
                .andExpect(status().isOk())
                // JsonPath 결과가 리스트이므로 contains 사용
                .andExpect(jsonPath("$[?(@.countryCode=='KR')].cnt", contains(2)))
                .andExpect(jsonPath("$[?(@.countryCode=='US')].cnt", contains(1)));
    }

}

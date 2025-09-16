package org.example.dashboard.controller;

import org.example.dashboard.dto.UniqueStatsDTO;
import org.example.dashboard.service.ClickLogService;
import org.example.dashboard.service.LinkService;
import org.example.dashboard.vo.ClickLog;
import org.example.dashboard.vo.Link;
import org.example.dashboard.dto.LinkStatsDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.lessThanOrEqualTo;


import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(APIController.class)
class APIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LinkService linkService;

    @MockBean
    private ClickLogService clickLogService;

    @Test
    void testGetLink_NotFound() throws Exception {
        when(linkService.getLink("invalidSlug")).thenReturn(null);

        mockMvc.perform(get("/api/links/invalidSlug"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testTimeDistribution_InvalidGranularity() throws Exception {
        Link mockLink = new Link();
        mockLink.setSlug("abc123");
        mockLink.setActive(true);

        when(linkService.getLink("abc123")).thenReturn(mockLink);

        mockMvc.perform(get("/api/links/abc123/time-distribution")
                        .param("granularity", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStatsResponseStructure() throws Exception {
        Link mockLink = new Link();
        mockLink.setSlug("testSlug");
        mockLink.setActive(true);

        // dto 패키지 타입으로 생성
        LinkStatsDTO linkStatsDTO = new LinkStatsDTO();
        linkStatsDTO.setTotalClicks(100L);
        linkStatsDTO.setClicksLast24hByHour(List.of());
        linkStatsDTO.setClicksLast7dByDate(List.of());
        linkStatsDTO.setTopBrowsers(List.of());
        linkStatsDTO.setTopOS(List.of());
        linkStatsDTO.setTopReferrers(List.of());

        when(linkService.getLink("testSlug")).thenReturn(mockLink);
        when(clickLogService.buildStatsDTO(mockLink.getId())).thenReturn(linkStatsDTO);

        mockMvc.perform(get("/api/links/testSlug/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(100));
    }

    @Test
    void testRecentLogsLimit() throws Exception {
        Link mockLink = new Link();
        mockLink.setId(1L);
        mockLink.setSlug("testSlug");
        mockLink.setActive(true);

        List<ClickLog> mockLogs = List.of(new ClickLog(), new ClickLog(), new ClickLog());

        when(linkService.getLink("testSlug")).thenReturn(mockLink);
        when(clickLogService.findByLinkId(1L)).thenReturn(mockLogs);

        mockMvc.perform(get("/api/links/testSlug/logs")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", lessThanOrEqualTo(5)));
    }


    @Test
    void testCountryDistWithDateRange() throws Exception {
        Link mockLink = new Link();
        mockLink.setId(1L);
        mockLink.setSlug("testSlug");
        mockLink.setActive(true);

        when(linkService.getLink("testSlug")).thenReturn(mockLink);
        when(clickLogService.countryDistBySlug("testSlug",
                LocalDateTime.parse("2025-09-01T00:00:00"),
                LocalDateTime.parse("2025-09-12T23:59:59")))
                .thenReturn(List.of()); // 결과는 빈 리스트로 테스트

        mockMvc.perform(get("/api/links/testSlug/geo/countries")
                        .param("start", "2025-09-01T00:00:00")
                        .param("end", "2025-09-12T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testUniqueStats() throws Exception {
        Link mockLink = new Link();
        mockLink.setId(1L);
        mockLink.setSlug("testSlug");
        mockLink.setActive(true);

        when(linkService.getLink("testSlug")).thenReturn(mockLink);

        UniqueStatsDTO dto = new UniqueStatsDTO();
        dto.setTotalClicks(12L);
        dto.setUniqueApprox(5L);
        dto.setDuplicateRatio(0.5833);
        dto.setUniqueWindowed(3L);
        dto.setWindowMinutes(10);

        when(clickLogService.uniqueStatsBySlug("testSlug", null, null, 10)).thenReturn(dto);

        mockMvc.perform(get("/api/links/testSlug/unique-stats")
                        .param("windowMinutes", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(12))
                .andExpect(jsonPath("$.duplicateRatio").value(0.5833));
    }


}

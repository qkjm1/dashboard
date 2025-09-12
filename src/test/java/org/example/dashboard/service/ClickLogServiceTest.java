package org.example.dashboard.service;

import org.example.dashboard.vo.ClickLog;
import org.example.dashboard.vo.Link;
import org.example.dashboard.repository.ClickLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClickLogServiceTest {

    @Mock
    private ClickLogRepository clickLogRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ClickLogService clickLogService;

    private static final UserAgentAnalyzer UAA = UserAgentAnalyzer
            .newBuilder()
            .withField(UserAgent.AGENT_NAME)
            .withField(UserAgent.OPERATING_SYSTEM_NAME)
            .withField(UserAgent.DEVICE_CLASS)
            .withCache(10000)
            .build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveClickFromRequest_shouldSaveClickLog_dynamic() throws Exception {
        // given
        Link link = new Link();
        link.setId(1L);
        link.setSlug("pRrfLN");
        link.setOriginalUrl("https://www.google.com");

        String userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
        String referer = "https://www.google.com";

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("Referer")).thenReturn(referer);
        when(request.getHeader("User-Agent")).thenReturn(userAgentString);

        // when
        clickLogService.saveClickFromRequest(link, request);

        // then
        ArgumentCaptor<ClickLog> captor = ArgumentCaptor.forClass(ClickLog.class);
        verify(clickLogRepository, times(1)).insertClickLog(captor.capture());
        ClickLog savedLog = captor.getValue();

        assertEquals(link.getId(), savedLog.getLinkId());
        assertNotNull(savedLog.getIpHash());
        assertNotNull(savedLog.getUserAgent());

        // Referrer 기반 채널 검증
        String host = new URI(referer).getHost().toLowerCase();
        String expectedChannel;
        if (host.contains("google.")) expectedChannel = "GOOGLE";
        else if (host.contains("naver.")) expectedChannel = "NAVER";
        else if (host.contains("kakao.")) expectedChannel = "KAKAO";
        else if (host.contains("instagram.")) expectedChannel = "INSTAGRAM";
        else if (host.contains("facebook.") || host.contains("fb.")) expectedChannel = "FACEBOOK";
        else expectedChannel = "UNKNOWN";

        assertEquals(expectedChannel, savedLog.getChannel());

        // User-Agent 분석 검증
        UserAgent ua = UAA.parse(userAgentString);

        String expectedBrowser = ua.getValue(UserAgent.AGENT_NAME);
        String expectedOs = ua.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        String deviceClass = ua.getValue(UserAgent.DEVICE_CLASS);
        String expectedDeviceType = mapDeviceClass(deviceClass);

        assertEquals(expectedBrowser, savedLog.getBrowser());
        assertEquals(expectedOs, savedLog.getOs());
        assertEquals(expectedDeviceType, savedLog.getDeviceType());
    }

    // 테스트용 mapDeviceClass 복사
    private String mapDeviceClass(String deviceClass) {
        String d = (deviceClass == null ? "" : deviceClass).toLowerCase();
        if (d.contains("tablet")) return "TABLET";
        if (d.contains("mobile") || d.contains("phone")) return "MOBILE";
        if (d.contains("desktop") || d.contains("laptop") || d.contains("tv") || d.contains("game")) return "DESKTOP";
        return "OTHER";
    }
}

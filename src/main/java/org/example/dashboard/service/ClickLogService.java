package org.example.dashboard.service;

import org.example.dashboard.dto.BrowserCountDTO;
import org.example.dashboard.dto.LinkStatsDTO;
import org.example.dashboard.repository.ClickLogRepository;
import org.example.dashboard.vo.ClickLog;
import org.example.dashboard.vo.Link; // 네 프로젝트에 맞춰 import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;


@Service
public class ClickLogService {

    @Autowired
    private ClickLogRepository clickLogRepository;

    
    private static final UserAgentAnalyzer UAA = UserAgentAnalyzer
            .newBuilder()
            .withField(UserAgent.AGENT_NAME)            // 브라우저
            .withField(UserAgent.OPERATING_SYSTEM_NAME) // OS
            .withField(UserAgent.DEVICE_CLASS)          // 기기 분류
            .withCache(10000)                           // 선택: 캐시
            .build();

    // 클릭 로그 저장
    public void saveClick(ClickLog clickLog) {
        clickLogRepository.insertClickLog(clickLog);
    }

    // 특정 링크의 클릭 로그 리스트
    public List<ClickLog> getLogsByLinkId(Long linkId) {
        return clickLogRepository.selectByLinkId(linkId);
    }

    // 특정 링크의 클릭 수
    public int getClickCount(Long linkId) {
        return clickLogRepository.countByLinkId(linkId);
    }

    // ---- 브라우저/OS/채널 판별 로직 ----
    public String detectOS(String ua) {
        if (ua == null) return "Unknown";
        String s = ua.toLowerCase();
        if (s.contains("windows nt")) return "Windows";
        if (s.contains("iphone") || s.contains("ipad") || s.contains("ios")) return "iOS";
        if (s.contains("android")) return "Android";
        if (s.contains("mac os x") || s.contains("macintosh")) return "macOS";
        if (s.contains("linux")) return "Linux";
        return "Unknown";
    }

    public String detectBrowser(String ua) {
        if (ua == null) return "Other";
        String s = ua.toLowerCase();
        if (s.contains("edg/")) return "Edge";        // Edge 먼저
        if (s.contains("chrome/")) return "Chrome";
        if (s.contains("safari/") && !s.contains("chrome/")) return "Safari";
        if (s.contains("firefox/")) return "Firefox";
        return "Other";
    }

    public String detectChannel(String ref) {
        if (ref == null) return "DIRECT";
        String r = ref.toLowerCase();
        if (r.contains("instagram")) return "Instagram";
        if (r.contains("kakao")) return "Kakao";
        if (r.contains("naver")) return "Naver";
        return "Other";
    }
    
    /** 링크ID 기준 집계 한 번에 반환 */
    public Map<String, Object> buildStatsByLinkId(Long linkId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClicks", clickLogRepository.countByLinkId(linkId));
        stats.put("last24hByHour", clickLogRepository.last24hByHour(linkId));
        stats.put("last7dByDate", clickLogRepository.last7dByDate(linkId));
        stats.put("byChannel", clickLogRepository.byChannel(linkId));
        stats.put("byDevice", clickLogRepository.byDevice(linkId));
        stats.put("topBrowsers", clickLogRepository.topBrowsers(linkId, 5));
        stats.put("topOS", clickLogRepository.topOS(linkId, 5));
        stats.put("topReferrers", clickLogRepository.topReferrerHost(linkId, 10));
        return stats;
    }
    
    public void saveClickFromRequest(Link link, HttpServletRequest req) {
        ClickLog log = new ClickLog();
        log.setLinkId(link.getId());

        // IP 해시 (간단 버전 유지)
        String ip = req.getRemoteAddr();
        log.setIpHash(ip == null ? "UNKNOWN" : Integer.toHexString(ip.hashCode()));

        // 1) Referrer & Channel
        String ref = req.getHeader("Referer");
        log.setReferrer(ref);
        String host = null;
        try { host = (ref != null) ? new URI(ref).getHost() : null; } catch (Exception ignored) {}
        log.setChannel(toChannel(host, ref)); // DIRECT/UNKNOWN 포함

        // 2) UA → browser/os/deviceType (YAUAA)
        String uaString = req.getHeader("User-Agent");
        log.setUserAgent(uaString);

        if (uaString != null && !uaString.isBlank()) {
            UserAgent ua = UAA.parse(uaString);

            String browser = nz(ua.getValue(UserAgent.AGENT_NAME), "UNKNOWN");
            String os      = nz(ua.getValue(UserAgent.OPERATING_SYSTEM_NAME), "UNKNOWN");
            String deviceC = nz(ua.getValue(UserAgent.DEVICE_CLASS), "Unknown");

            log.setBrowser(browser);
            log.setOs(os);
            log.setDeviceType(mapDeviceClass(deviceC)); // "MOBILE" | "TABLET" | "DESKTOP" | "OTHER"
        } else {
            log.setBrowser("UNKNOWN");
            log.setOs("UNKNOWN");
            log.setDeviceType("OTHER");
        }

        clickLogRepository.insertClickLog(log);
        
        
        
    }

    private String toChannel(String host, String referrer) {
        if (referrer == null) return "DIRECT";
        if (host == null) return "UNKNOWN";
        host = host.toLowerCase();
        if (host.contains("google."))    return "GOOGLE";
        if (host.contains("naver."))     return "NAVER";
        if (host.contains("kakao."))     return "KAKAO";
        if (host.contains("instagram.")) return "INSTAGRAM";
        if (host.contains("facebook.") || host.contains("fb.")) return "FACEBOOK";
        return "UNKNOWN";
    }

    private String mapDeviceClass(String deviceClass) {
        // YAUAA DeviceClass 예: "Desktop", "Mobile", "Tablet", "Phone", "TV", "Game Console", "Watch", "Unknown" ...
        String d = deviceClass.toLowerCase();
        if (d.contains("tablet")) return "TABLET";
        if (d.contains("mobile") || d.contains("phone")) return "MOBILE";
        if (d.contains("desktop") || d.contains("laptop") || d.contains("tv") || d.contains("game")) return "DESKTOP";
        return "OTHER";
    }

    private String nz(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
    
    
    public LinkStatsDTO buildStatsDTO(Long linkId) {
        LinkStatsDTO dto = new LinkStatsDTO();
        dto.setTotalClicks(clickLogRepository.countByLinkId(linkId));

        // 필드 이름은 프론트에서 쓰기 좋은 alias로 XML에서 맞춰주었거나,
        // Map 키를 그대로 사용(예: hour/cnt, date/cnt 등)
        List<Map<String, Object>> h24 = clickLogRepository.last24hByHour(linkId);
        List<Map<String, Object>> d7  = clickLogRepository.last7dByDate(linkId);
        List<Map<String, Object>> topBrowsers = clickLogRepository.topBrowsers(linkId, 5);
        List<Map<String, Object>> topOS       = clickLogRepository.topOS(linkId, 5);
        List<Map<String, Object>> topRefs     = clickLogRepository.topReferrerHost(linkId, 10);

        dto.setClicksLast24hByHour(h24);
        dto.setClicksLast7dByDate(d7);
        dto.setTopBrowsers(topBrowsers);
        dto.setTopOS(topOS);

        // host로 집계했다면 DTO의 키와 의미를 명시적으로 맞추자.
        // (DTO 필드명이 topReferrers면 각 맵에 host 키가 들어있어도 OK)
        dto.setTopReferrers(topRefs);
        return dto;
    }

    // (선택) 최근 로그 조회 편의 메서드
    public List<ClickLog> findByLinkId(Long linkId) {
        return clickLogRepository.selectByLinkId(linkId);
    }
    
    public List<BrowserCountDTO> getBrowserCountsBySlug(String slug) {
        return clickLogRepository.browseCntBySlug(slug);
    }
    
}

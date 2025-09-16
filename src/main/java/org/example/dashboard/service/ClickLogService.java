package org.example.dashboard.service;

import org.example.dashboard.dto.BrowserCountDTO;
import org.example.dashboard.dto.BurstStatsDTO;
import org.example.dashboard.dto.CountryCountDTO;
import org.example.dashboard.dto.LinkStatsDTO;
import org.example.dashboard.dto.ReExposeStatsDTO;
import org.example.dashboard.dto.ReferrerCountDTO;
import org.example.dashboard.dto.TimeBucketCountDTO;
import org.example.dashboard.dto.UniqueStatsDTO;
import org.example.dashboard.repository.ClickLogRepository;
import org.example.dashboard.support.GeoIPResolver;
import org.example.dashboard.vo.ClickLog;
import org.example.dashboard.vo.Link; // 네 프로젝트에 맞춰 import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@Service
@RequiredArgsConstructor
public class ClickLogService {

	@Autowired
	private ClickLogRepository clickLogRepository;
	@Autowired
	private GeoIPResolver geoIPResolver;   // CF 헤더 없을 때 보조   
    private final ClickLogEnricher enricher;       // UA/OS/봇/해시/host 등 일괄 처리

	private static final UserAgentAnalyzer UAA = UserAgentAnalyzer.newBuilder().withField(UserAgent.AGENT_NAME) // 브라우저
			.withField(UserAgent.OPERATING_SYSTEM_NAME) // OS
			.withField(UserAgent.DEVICE_CLASS) // 기기 분류
			.withCache(10000) // 선택: 캐시
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
		if (ua == null)
			return "Unknown";
		String s = ua.toLowerCase();
		if (s.contains("windows nt"))
			return "Windows";
		if (s.contains("iphone") || s.contains("ipad") || s.contains("ios"))
			return "iOS";
		if (s.contains("android"))
			return "Android";
		if (s.contains("mac os x") || s.contains("macintosh"))
			return "macOS";
		if (s.contains("linux"))
			return "Linux";
		return "Unknown";
	}

	public String detectBrowser(String ua) {
		if (ua == null)
			return "Other";
		String s = ua.toLowerCase();
		if (s.contains("edg/"))
			return "Edge"; // Edge 먼저
		if (s.contains("chrome/"))
			return "Chrome";
		if (s.contains("safari/") && !s.contains("chrome/"))
			return "Safari";
		if (s.contains("firefox/"))
			return "Firefox";
		return "Other";
	}

	public String detectChannel(String ref) {
		if (ref == null)
			return "DIRECT";
		String r = ref.toLowerCase();
		if (r.contains("instagram"))
			return "Instagram";
		if (r.contains("kakao"))
			return "Kakao";
		if (r.contains("naver"))
			return "Naver";
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

    /* 공통: XFF 등에서 클라이언트 IP 추출 */
    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return req.getRemoteAddr();
    }

    /** QR 등 채널이 명시되는 케이스 */
    public void saveClickWithChannel(Link link, HttpServletRequest req, String channelOverride) {
        ClickLog log = ClickLog.builder()
                .linkId(link.getId())
                .clickedAt(LocalDateTime.now())
                .channel(channelOverride)  // 우선 명시된 채널 사용
                .build();

        // referrer는 디버깅/보존용으로만 세팅
        log.setReferrer(req.getHeader("Referer"));

        // Enricher로 나머지 필드 채우기
        enricher.enrichFromRequest(log, req);  // UA/봇/ipHash/referrerHost/OS/Browser/DeviceType 등

        if (log.getCountryCode() == null || log.getCountryCode().isBlank()) {
            String ip = extractClientIp(req);
            String cc = geoIPResolver.countryCode(ip);
            if (cc != null && !cc.isBlank()) log.setCountryCode(cc.toUpperCase());
        }

        clickLogRepository.insertClickLog(log);
    }

    /** 리다이렉트 일반 케이스 저장: 채널은 referrer로 산정 */
    public void saveClickFromRequest(Link link, HttpServletRequest req) {
        ClickLog log = ClickLog.builder()
                .linkId(link.getId())
                .clickedAt(LocalDateTime.now())
                .build();

        // 1) 우선 채널만 결정(나머지는 Enricher에서)
        String ref = req.getHeader("Referer");
        String host = null;
        try { host = (ref != null) ? new URI(ref).getHost() : null; } catch (Exception ignored) {}
        log.setReferrer(ref);
        log.setChannel(toChannel(host, ref));

        // 2) 요청 데이터 풍부화 (UA/OS/브라우저/디바이스/봇/ipHash/referrerHost/countryCode 등)
        enricher.enrichFromRequest(log, req);  // ← 일원화된 로직 사용  (:contentReference[oaicite:3]{index=3})

        // 3) 국가코드 보조 결정(Enricher가 못 채웠다면 GeoIP로)
        if (log.getCountryCode() == null || log.getCountryCode().isBlank()) {
            String ip = extractClientIp(req);
            String cc = geoIPResolver.countryCode(ip);
            if (cc != null && !cc.isBlank()) log.setCountryCode(cc.toUpperCase());
        }

        // 4) 저장
        clickLogRepository.insertClickLog(log);
    }

    /* referrer 기반 간단 채널 매핑 */
    private String toChannel(String host, String referrer) {
        if (referrer == null) return "DIRECT";
        if (host == null)     return "UNKNOWN";
        String h = host.toLowerCase();
        if (h.contains("google."))    return "GOOGLE";
        if (h.contains("naver."))     return "NAVER";
        if (h.contains("kakao."))     return "KAKAO";
        if (h.contains("instagram.")) return "INSTAGRAM";
        if (h.contains("facebook.") || h.contains("fb.")) return "FACEBOOK";
        return "UNKNOWN";
    }

	private String mapDeviceClass(String deviceClass) {
		// YAUAA DeviceClass 예: "Desktop", "Mobile", "Tablet", "Phone", "TV", "Game
		// Console", "Watch", "Unknown" ...
		String d = deviceClass.toLowerCase();
		if (d.contains("tablet"))
			return "TABLET";
		if (d.contains("mobile") || d.contains("phone"))
			return "MOBILE";
		if (d.contains("desktop") || d.contains("laptop") || d.contains("tv") || d.contains("game"))
			return "DESKTOP";
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
		List<Map<String, Object>> d7 = clickLogRepository.last7dByDate(linkId);
		List<Map<String, Object>> topBrowsers = clickLogRepository.topBrowsers(linkId, 5);
		List<Map<String, Object>> topOS = clickLogRepository.topOS(linkId, 5);
		List<Map<String, Object>> topRefs = clickLogRepository.topReferrerHost(linkId, 10);

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

	// 슬러그 기준으로 브라우저별 카운트
	public List<BrowserCountDTO> getBrowserCountsBySlug(String slug) {
		return clickLogRepository.browseCntBySlug(slug);
	}

	// 시간별로 분포도 보는
	public List<TimeBucketCountDTO> hourlyDistBySlug(String slug) {
		return mapList(clickLogRepository.hourlyDistBySlug(slug));
	}

	public List<TimeBucketCountDTO> dowDistBySlug(String slug) {
		return mapList(clickLogRepository.dowDistBySlug(slug));
	}

	public List<TimeBucketCountDTO> monthDistBySlug(String slug) {
		return mapList(clickLogRepository.monthDistBySlug(slug));
	}

	// --- 버스트/하프라이프 ---
	public BurstStatsDTO burstStats(String slug) {
		List<TimeBucketCountDTO> raw = mapList(clickLogRepository.hoursSinceCreateBySlug(slug));
		// 버킷 누락 채우기: 0..max
		int max = raw.stream().mapToInt(r -> Integer.parseInt(r.getBucket())).max().orElse(0);
		long[] byHour = new long[max + 1];
		for (TimeBucketCountDTO r : raw) {
			int h = Integer.parseInt(r.getBucket());
			byHour[h] = r.getCnt();
		}
		List<Long> cumulative = new ArrayList<>(max + 1);
		long running = 0, total = 0;
		for (long v : byHour) {
			running += v;
			cumulative.add(running);
			total += v;
		}

		Integer halflife = null;
		if (total > 0) {
			long half = (total + 1) / 2; // 반 이상 도달 첫 시점
			for (int i = 0; i < cumulative.size(); i++) {
				if (cumulative.get(i) >= half) {
					halflife = i;
					break;
				}
			}
		}

		// DTO 구성
		List<TimeBucketCountDTO> normalized = IntStream.range(0, byHour.length)
				.mapToObj(i -> new TimeBucketCountDTO(String.valueOf(i), byHour[i])).collect(Collectors.toList());

		BurstStatsDTO dto = new BurstStatsDTO();
		dto.setTotalClicks(total);
		dto.setHalflifeHours(halflife);
		dto.setByHourSinceCreate(normalized);
		dto.setCumulative(cumulative);
		return dto;
	}

	// --- 재노출 전/후 시계열 ---
	public ReExposeStatsDTO reExposeStats(String slug, LocalDateTime center, int windowHours) {
		Timestamp centerTs = Timestamp.valueOf(center);

		List<TimeBucketCountDTO> before = mapList(
				clickLogRepository.seriesHourlyAroundMoment(slug, centerTs, windowHours, "before"));
		List<TimeBucketCountDTO> after = mapList(
				clickLogRepository.seriesHourlyAroundMoment(slug, centerTs, windowHours, "after"));

		long beforeTotal = before.stream().mapToLong(TimeBucketCountDTO::getCnt).sum();
		long afterTotal = after.stream().mapToLong(TimeBucketCountDTO::getCnt).sum();

		ReExposeStatsDTO dto = new ReExposeStatsDTO();
		dto.setWindowHours(windowHours);
		dto.setBeforeTotal(beforeTotal);
		dto.setAfterTotal(afterTotal);
		dto.setBeforeSeries(before);
		dto.setAfterSeries(after);
		return dto;
	}

	// 공통 변환
	private List<TimeBucketCountDTO> mapList(List<Map<String, Object>> rows) {
		return rows.stream().map(m -> {
			String bucket = String.valueOf(m.get("bucket"));
			long cnt = ((Number) m.get("cnt")).longValue();
			return new TimeBucketCountDTO(bucket, cnt);
		}).collect(Collectors.toList());
	}

	public List<ReferrerCountDTO> topReferrersBySlug(String slug, int limit) {
		return clickLogRepository.topReferrersBySlug(slug, limit);
	}

	public List<ReferrerCountDTO> topChannelsBySlug(String slug, int limit) {
		return clickLogRepository.topChannelsBySlug(slug, limit);
	}

	public List<ReferrerCountDTO> topReferrersByTargetUrl(String targetUrl, int limit) {
		return clickLogRepository.topReferrersByTargetUrl(targetUrl, limit);
	}

	public List<CountryCountDTO> countryDistBySlug(String slug, LocalDateTime start, LocalDateTime end) {
		return clickLogRepository.countryDistBySlug(slug, start, end);
	}

	public UniqueStatsDTO uniqueStatsBySlug(String slug, LocalDateTime start, LocalDateTime end, int windowMinutes) {
		long total = clickLogRepository.totalClicksBySlug(slug, start, end);
		long uniq = clickLogRepository.uniqueApproxBySlug(slug, start, end);
		long win = clickLogRepository.uniqueWindowedBySlug(slug, start, end, windowMinutes);

		UniqueStatsDTO dto = new UniqueStatsDTO();
		dto.setTotalClicks(total);
		dto.setUniqueApprox(uniq);
		dto.setUniqueWindowed(win);
		dto.setWindowMinutes(windowMinutes);
		dto.setDuplicateRatio(total > 0 ? (double) (total - uniq) / total : 0.0);
		return dto;
	}

	// qr 조회용 메서드
	public List<ReferrerCountDTO> qrVsLinkBySlug(String slug, LocalDateTime start, LocalDateTime end) {
		return clickLogRepository.qrVsLinkBySlug(slug, start, end);
	}

	public List<ReferrerCountDTO> qrMediumTopBySlug(String slug, LocalDateTime start, LocalDateTime end, int limit) {
		return clickLogRepository.qrMediumBySlug(slug, start, end, limit);
	}

	public List<TimeBucketCountDTO> scanDistribution(String slug, String source, String granularity,
			LocalDateTime start, LocalDateTime end) {
		return clickLogRepository.timeDistributionFiltered(slug, source, granularity, start, end);
	}
	
	public Map<String, Object> botMetrics(String slug, LocalDateTime start, LocalDateTime end);
	
	public Map<String, Object> detectSpikes(String slug, String bucket, double z, LocalDateTime start, LocalDateTime end);


}

package org.example.dashboard.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.example.dashboard.dto.BrowserCountDTO;
import org.example.dashboard.dto.BurstStatsDTO;
import org.example.dashboard.dto.CountryCountDTO;
import org.example.dashboard.dto.LinkFullInfoDTO;
import org.example.dashboard.dto.LinkStatsDTO;
import org.example.dashboard.dto.ReExposeStatsDTO;
import org.example.dashboard.dto.ReferrerCountDTO;
import org.example.dashboard.dto.TimeBucketCountDTO;
import org.example.dashboard.dto.UniqueStatsDTO;
import org.example.dashboard.dto.UrlMetaDTO;
import org.example.dashboard.service.ClickLogService;
import org.example.dashboard.service.LinkService;
import org.example.dashboard.vo.ClickLog;
import org.example.dashboard.vo.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class APIController {

	@Autowired
	private LinkService linkService;

	@Autowired
	private ClickLogService clickLogService;

	/*
	 * 국가 api 보조용
	 */
	private Link requireActive(String slug) {
		Link link = linkService.getLink(slug);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 없거나 비활성");
		}
		return link;
	}

	/*
	 * slug 기준 lick 테이블 전부
	 */
	@GetMapping("/links/{slug}")
	public Link getLink(@PathVariable String slug) {
		Link link = linkService.getLink(slug);
		System.out.println(link);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 없거나 비활성");
		}
		return linkService.getLink(slug);
	}

	/*
	 * clickLick 테이블
	 */
	@GetMapping("/links/{slug}/stats")
	public LinkStatsDTO getStats(@PathVariable String slug) {
		Link link = linkService.getLink(slug);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 없거나 비활성");
		}
		return clickLogService.buildStatsDTO(link.getId());
	}

	/*
	 * lick + clickLick + 메타 한 번에
	 * 
	 * 25-09-12 UrlMetaDTO(메타;미리보기,요약 등) 아직 구현 안되어있음 (현재 null로 나옴)
	 * 
	 * 
	 */
	@GetMapping("/links/{slug}/full")
	public LinkFullInfoDTO getFull(@PathVariable String slug) {
		Link link = linkService.getLink(slug);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 없거나 비활성");
		}
		LinkStatsDTO stats = clickLogService.buildStatsDTO(link.getId());

		// 메타는 당장 필요 없으면 null
		UrlMetaDTO meta = new UrlMetaDTO();

		LinkFullInfoDTO dto = new LinkFullInfoDTO();
		dto.setLink(link);
		dto.setStats(stats);
		dto.setMeta(meta);
		return dto;
	}

	/*
	 * 해당 slug 최근 클릭로그 N건 현재 100건이 디폴트
	 */
	@GetMapping("/links/{slug}/logs")
	public List<ClickLog> getRecentLogs(@PathVariable String slug, @RequestParam(defaultValue = "100") int limit) {
		Link link = linkService.getLink(slug);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 없거나 비활성");
		}
		// 필요 시 리포지토리에 '최근 N건'용 쿼리를 추가하거나
		// selectByLinkId 후 상위 N개만 잘라 반환
		List<ClickLog> all = clickLogService.findByLinkId(link.getId());
		return all.size() > limit ? all.subList(0, limit) : all;
	}

	/*
	 * 해당 slug 유입 브라우저랑 카운트 수
	 */
	@GetMapping("/links/{slug}/browsers")
	public List<BrowserCountDTO> getBrowserCounts(@PathVariable String slug) {
		var link = linkService.getLink(slug);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 없거나 비활성");
		}
		return clickLogService.getBrowserCountsBySlug(slug);
	}

	/*
	 * 시간대/요일/월별 분포 /links/{slug}/time-distribution?granularity={hour|dow|month} <
	 * 세개중 하나 파라미터로 넘겨야함 dow (1=일요일, 2=월요일, 3=화요일 -- 7=토요) 요별 hour (1~24) 시별 month
	 * (yyyy-mm) 월별
	 */
	@GetMapping("/links/{slug}/time-distribution")
	public Object timeDistribution(@PathVariable String slug, @RequestParam(defaultValue = "hour") String granularity // hour|dow|month
	) {
		switch (granularity) {
		case "hour":
			return clickLogService.hourlyDistBySlug(slug);
		case "dow":
			return clickLogService.dowDistBySlug(slug);
		case "month":
			return clickLogService.monthDistBySlug(slug);
		default:
			throw new IllegalArgumentException("granularity must be hour|dow|month");
		}
	}

	/*
	 * 생성시점이후 클릭 누적 bucket이 생성시 1시간을 0부터 셈 (json 명) byHourSinceCreate: 생성 후 0시간,
	 * 1시간…의 클릭 수 cumulative: 같은 인덱스 기준 누적 클릭 수 halflifeHours: 누적이 전체의 50%를 넘어선 최초
	 * 시간
	 */
	@GetMapping("/links/{slug}/burst")
	public BurstStatsDTO burst(@PathVariable String slug) {
		return clickLogService.burstStats(slug);
	}

	/*
	 * 재노출 전/후 24시간 비교
	 * 
	 * todo 만기기간 정해야
	 */
	@GetMapping("/links/{slug}/reexpose")
	public ReExposeStatsDTO reexpose(@PathVariable String slug, @RequestParam String at, // ISO-8601, e.g.
																							// 2025-09-11T14:00:00
			@RequestParam(defaultValue = "24") int windowHours) {
		LocalDateTime center = LocalDateTime.parse(at);
		return clickLogService.reExposeStats(slug, center, windowHours);
	}

	/*
	 * slug 기준으로 링크가 게시된 사이트의 출저 limit 고정 = 5
	 */
	@GetMapping("/links/{slug}/referrers")
	public List<ReferrerCountDTO> topReferrers(@PathVariable String slug, @RequestParam(defaultValue = "5") int limit) {
		Link link = linkService.getLink(slug);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 없거나 비활성");
		}
		return clickLogService.topReferrersBySlug(slug, limit);
	}

	/*
	 * slug 기준으로 링크가 게시된 사이트의 채널 이름 (네이버/구글/카카오 등) limit 고정 = 5
	 */
	@GetMapping("/links/{slug}/channels")
	public List<ReferrerCountDTO> topChannels(@PathVariable String slug, @RequestParam(defaultValue = "5") int limit) {
		Link link = linkService.getLink(slug);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 없거나 비활성");
		}
		return clickLogService.topChannelsBySlug(slug, limit);
	}

	/*
	 * 동일 url 기준으로 게시된 referrers별로 가져오기 limit 고정 = 5
	 */
	@GetMapping("/targets/referrers")
	public List<ReferrerCountDTO> topReferrersForTarget(@RequestParam String url,
			@RequestParam(defaultValue = "5") int limit) {
		return clickLogService.topReferrersByTargetUrl(url, limit);
	}

	/*
	 * 국가분포
	 */
	@GetMapping("/links/{slug}/geo/countries")
	public List<CountryCountDTO> countryDist(@PathVariable String slug,
			@RequestParam(required = false) LocalDateTime start, @RequestParam(required = false) LocalDateTime end) {
		requireActive(slug);
		return clickLogService.countryDistBySlug(slug, start, end);
	}

	/* 회의록 5번부분임
	 * 유니크; 같은 ip 중복클릭확인 totalClicks : 선택한 기간동안 해당 slug의 총 클릭 수 uniqueApprox : 같은
	 * 사용자 여부 확인 / ip_hash + user_agent 조합으로 판단 / 정확도는 떨어지지만 가벼운 근사치 (ip_hash가 null이
	 * 있으면 1로 계) duplicateRatio : 중복 클릭 비율 ex)12회 중 유니크 1 → (12-1)/12 = 0.9167
	 * uniqueWindowed : 동일한 (ip_hash, user_agent) 사용자의 클릭들을 시간순으로 정렬하고, 연속 클릭 간격이
	 * windowMinutes를 초과할 때만 새로운 세션으로 카운트 windowMinutes : 계산에 적용된 세션 간격(분) 값
	 */
	@GetMapping("/links/{slug}/unique-stats")
	public UniqueStatsDTO uniqueStats(@PathVariable String slug, @RequestParam(defaultValue = "10") int windowMinutes,
			@RequestParam(required = false) LocalDateTime start, @RequestParam(required = false) LocalDateTime end) {
		requireActive(slug);
		return clickLogService.uniqueStatsBySlug(slug, start, end, windowMinutes);
	}

	
	/* 필요없으면 사용안해도 되는 부분
	 * qr경로로 진입했을때 집계 부분입니다
	 * 
	 * qrMediumTop : 내부매체 반환
	 * QR 내부 매체(포스터/명함/배너) 클릭 카운터
	 * 
	 * ex)http://localhost:8080/api/links/a0boWV/qr-vs-link?start=2025-09-01T00:00:00&end=2025-09-12T23:59:59
	 * start-end는 LocalDateTime 형태로 넣어야함
	 * 
	 */
	@GetMapping("/links/{slug}/qr-vs-link")
	public Map<String, Object> qrVsLink(@PathVariable String slug, @RequestParam(required = false) LocalDateTime start,
			@RequestParam(required = false) LocalDateTime end, @RequestParam(defaultValue = "5") int topMedium // QR 매체
																												// TOP N
	) {
		requireActive(slug);
		var vs = clickLogService.qrVsLinkBySlug(slug, start, end);
		var mediums = clickLogService.qrMediumTopBySlug(slug, start, end, topMedium);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("summary", vs); // [{key:'QR',cnt:...},{key:'LINK',cnt:...}]
		res.put("qrMediumTop", mediums); // [{key:'poster',cnt:...},...]
		return res;
	}
	
	/*
	 *  시간 분포별 qr진입
	 *  granularity 파라미터는 hour(시간)|dow(요일) 중 선택
	 */
	@GetMapping("/links/{slug}/scan-distribution")
	public List<TimeBucketCountDTO> scanDistribution(@PathVariable String slug,
			@RequestParam(defaultValue = "qr") String source, // qr|link
			@RequestParam(defaultValue = "hour") String granularity, // hour|dow
			@RequestParam(required = false) LocalDateTime start, @RequestParam(required = false) LocalDateTime end) {
		requireActive(slug);
		return clickLogService.scanDistribution(slug, source, granularity, start, end);
	}

	/*
	 * timeDistribution()에서 예외가 400으로 넘어가야 하는데 404로 넘어가버려서
	 * 400(badRequest)로 보낼 수 있도록 이끌어주는 컨트롤러
	 */
	@RestControllerAdvice
	public class GlobalExceptionHandler {
	    @ExceptionHandler(IllegalArgumentException.class)
	    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
	       return ResponseEntity.badRequest().body(e.getMessage());
	    }
	}	
	
	
	
	
	

}

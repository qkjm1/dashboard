package org.example.dashboard.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.example.dashboard.dto.BrowserCountDTO;
import org.example.dashboard.dto.LinkFullInfoDTO;
import org.example.dashboard.dto.LinkStatsDTO;
import org.example.dashboard.dto.UrlMetaDTO;
import org.example.dashboard.service.ClickLogService;
import org.example.dashboard.service.LinkService;
import org.example.dashboard.vo.ClickLog;
import org.example.dashboard.vo.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
	 * 해당 slug 최근 클릭로그 N건
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

}

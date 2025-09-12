package org.example.dashboard.controller;

import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/click")
public class ClickLogController {

	@Autowired
	private LinkService linkService;

	@Autowired
	private ClickLogService clickLogService;

	// 특정 링크 클릭 로그 가져오기
	@GetMapping("/{linkId}")
	public List<ClickLog> getLogs(@PathVariable Long linkId) {
		return clickLogService.getLogsByLinkId(linkId);
	}

	// 특정 링크 클릭 수
	@GetMapping("/{linkId}/count")
	public int getClickCount(@PathVariable Long linkId) {
		return clickLogService.getClickCount(linkId);
	}

	// 클릭 기록 저장 (RedirectController에서 호출)
	@PostMapping("/{linkId}/log")
	public String logClick(@PathVariable Long linkId, HttpServletRequest request) {
		Link link = new Link();
	    link.setId(linkId);
		
		clickLogService.saveClickFromRequest(link, request);
		return "Click logged!";
	}

	// 1) slug 기준
	@GetMapping("/links/{slug}/stats")
	public Map<String, Object> getStatsBySlug(@PathVariable String slug) {
		var link = linkService.getLink(slug);
		if (link == null || !Boolean.TRUE.equals(link.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "링크가 존재하지 않거나 비활성화");
		}
		return clickLogService.buildStatsByLinkId(link.getId());
	}

}

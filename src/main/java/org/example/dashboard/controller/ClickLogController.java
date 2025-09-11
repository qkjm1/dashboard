package org.example.dashboard.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.dashboard.service.ClickLogService;
import org.example.dashboard.vo.ClickLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/click")
public class ClickLogController {

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
        String ua = request.getHeader("User-Agent");
        String ref = request.getHeader("Referer");

        ClickLog log = new ClickLog();
        log.setLinkId(linkId);
        log.setIpHash(Integer.toHexString(request.getRemoteAddr().hashCode())); // 간단 hash
        log.setReferrer(ref);
        log.setUserAgent(ua);
        log.setDeviceType(ua != null && ua.contains("Mobile") ? "MOBILE" : "DESKTOP");
        log.setBrowser(clickLogService.detectBrowser(request.getHeader("User-Agent")));
        log.setOs(clickLogService.detectOS(request.getHeader("User-Agent")));
        log.setChannel(clickLogService.detectChannel(request.getHeader("Referer")));


        clickLogService.saveClick(log);
        return "Click logged!";
    }
}

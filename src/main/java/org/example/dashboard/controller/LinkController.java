package org.example.dashboard.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dashboard.service.ClickLogService;
import org.example.dashboard.service.LinkService;
import org.example.dashboard.vo.ClickLog;
import org.example.dashboard.vo.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class LinkController {

    @Autowired
    private LinkService linkService;

    @Autowired
    private ClickLogService clickLogService;

    // 링크 생성 (POST /links)
    @PostMapping("/links")
    public Link createLink(@RequestParam String url) {
        return linkService.createLink(url);
    }

    //링크 생성 테스트
    @GetMapping("/links")
    public Link createLinkTest(@RequestParam String url) {
        return linkService.createLink(url);
    }


    // 링크 리다이렉트 (GET /r/{slug})
    @GetMapping("/r/{slug}")
    public void redirect(@PathVariable String slug,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        Link link = linkService.getLink(slug);
        System.out.println("DEBUG: " + link);  // <- link 객체 내용 확인

        if (link == null || !link.getActive()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "링크가 존재하지 않거나 만료되었습니다.");
            return;
        }

        
        System.out.println("Redirecting to: " + link.getOriginalUrl());

        // 클릭 로그 기록
        ClickLog log = new ClickLog();
        log.setLinkId(link.getId());
        log.setIpHash(Integer.toHexString(request.getRemoteAddr().hashCode()));
        log.setReferrer(request.getHeader("Referer"));
        log.setUserAgent(request.getHeader("User-Agent"));
        System.out.println(log);
     
        // 클릭로그저장 풍부화 데이터 합치
        clickLogService.saveClickFromRequest(link, request);
        
        response.sendRedirect(link.getOriginalUrl());
    }
    
    
    /*
     * 프론트에서 qr연결
     */
    @GetMapping("/q/{slug}")
    public void redirectQr(@PathVariable String slug,
                           @RequestParam(required = false) String m,   // medium: poster/bizcard/banner...
                           @RequestParam(required = false) String loc, // 위치 식별자(선택)
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        Link link = linkService.getLink(slug);
        if (link == null || !Boolean.TRUE.equals(link.getActive())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // channel = "QR:<m>[:<loc>]" 규칙
        String medium = (m == null || m.isBlank()) ? "generic" : m.trim();
        String channel = (loc == null || loc.isBlank())
                ? "QR:" + medium
                : "QR:" + medium + ":" + loc.trim();

        clickLogService.saveClickWithChannel(link, request, channel);
        response.sendRedirect(link.getOriginalUrl());
    }
    
    
    

}

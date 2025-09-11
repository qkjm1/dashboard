package org.example.dashboard.controller;


import jakarta.servlet.http.HttpServletResponse;
import org.example.dashboard.service.LinkService;
import org.example.dashboard.vo.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class LinkController {
    @Autowired
    private LinkService linkService;


    // 링크 가져오기
    @RequestMapping("/links")
    public Link createLink(@RequestParam String url) {
        return linkService.createLink(url);
    }

    // 링크로 리다이렉트
    @RequestMapping("/r/{slug}")
    public void redirect(@PathVariable String slug, HttpServletResponse response) throws IOException {
        Link link = linkService.getLink(slug);
        if(link == null || !link.getActive()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "링크가 존재하지 않거나 만료되었습니다.");
            return;
        }
        response.sendRedirect(link.getOriginalUrl());
    }

}

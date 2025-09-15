package org.example.dashboard.service;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

import java.net.URI;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClickLogEnricher {

    // uap-java 파서 (UA→OS/브라우저 파싱용, 꼭 안써도 되지만 있으면 좋아요)
    private final Parser uaParser = new Parser();

    public void enrichFromRequest(org.example.dashboard.vo.ClickLog log, HttpServletRequest req) {
        // 1) 기본 헤더
        String ua = req.getHeader("User-Agent");
        String ref = req.getHeader("Referer");
        String country = firstNonBlank(
                req.getHeader("CF-IPCountry"),
                req.getHeader("X-Country-Code"),
                null
        );

        log.setUa(ua);
        log.setReferrer(ref);
        log.setReferrerHost(extractHost(ref));
        log.setCountry(safeUpper(country));

        // 2) IP (있다면 저장 – 해시/마스킹은 기존 정책 유지)
        String ip = firstNonBlank(
                req.getHeader("CF-Connecting-IP"),
                req.getHeader("X-Forwarded-For"),
                req.getRemoteAddr()
        );
        log.setIp(ip); // 컬럼명이 ip_hash라면, 서비스에서 해시 후 set

        // 3) 봇 라벨링 (간단 규칙)
        BotLabel label = classifyBot(ua, log.getReferrerHost());
        log.setIsBot(label.bot ? 1 : 0);
        log.setBotType(label.type);

        // 4) (선택) OS/브라우저 세부필드가 있다면 채우기
        if (ua != null) {
            Client c = uaParser.parse(ua);
            // 예시: log.setOs(c.os.family);
            // 예시: log.setBrowser(c.userAgent.family);
        }
    }

    private String extractHost(String url) {
        try {
            if (url == null || url.isBlank()) return null;
            return new URI(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return null;
    }

    private String safeUpper(String s) {
        return (s == null) ? null : s.toUpperCase(Locale.ROOT);
    }

    private BotLabel classifyBot(String ua, String refHost) {
        if (ua == null) return new BotLabel(false, null);
        String u = ua.toLowerCase(Locale.ROOT);

        // 미리보기(차단 대신 'preview' 라벨) – 통계에서 분리용
        if (u.contains("facebookexternalhit") || u.contains("twitterbot") ||
            u.contains("slackbot") || u.contains("whatsapp") || u.contains("kakaotalk"))
            return new BotLabel(true, "preview");

        // 대표 크롤러
        if (u.contains("googlebot") || u.contains("bingbot") ||
            u.contains("yeti") || u.contains("naverbot") ||
            u.contains("ahrefs") || u.contains("semrush"))
            return new BotLabel(true, "crawler");

        // 헬스체크/검증기
        if ("validator.w3.org".equalsIgnoreCase(refHost))
            return new BotLabel(true, "healthcheck");

        return new BotLabel(false, null);
    }

    private record BotLabel(boolean bot, String type) {}
}
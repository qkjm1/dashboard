package org.example.dashboard.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClickLogEnricher {

    private final Parser uaParser = new Parser(); // uap-java

    /**
     * HttpServletRequest에서 값을 추출/가공해 ClickLog VO에 채워넣는다.
     * - userAgent, referrer, referrerHost, countryCode
     * - ipHash(SHA-256), isBot/botType
     * - os/browser/deviceType
     * - clickedAt(없으면 여기서 세팅)
     */
    public void enrichFromRequest(org.example.dashboard.vo.ClickLog log, HttpServletRequest req) {
        // 1) 원시 헤더 추출
        final String ua = req.getHeader("User-Agent");
        final String ref = req.getHeader("Referer");
        final String countryCode = firstNonBlank(
                req.getHeader("CF-IPCountry"),
                req.getHeader("X-Country-Code"),
                null
        );

        // 2) VO 필드 세팅 (VO 명세와 일치)
        log.setUserAgent(ua);
        log.setReferrer(ref);
        log.setReferrerHost(extractHost(ref));
        log.setCountryCode(toUpperOrNull(countryCode));

        // clickedAt이 비어있으면 지금 시각으로 채움
        if (log.getClickedAt() == null) {
            log.setClickedAt(LocalDateTime.now());
        }

        // 3) IP → 해시 저장 (원문 저장 지양)
        final String ip = firstNonBlank(
                req.getHeader("CF-Connecting-IP"),
                req.getHeader("X-Forwarded-For"),
                req.getRemoteAddr()
        );
        if (ip != null && !ip.isBlank()) {
            log.setIpHash(sha256Hex(ip));
        }

        // 4) 봇 라벨링
        final BotLabel label = classifyBot(ua, log.getReferrerHost());
        log.setIsBot(label.bot ? 1 : 0);
        log.setBotType(label.type);

        // 5) UA 파싱 (os/browser/deviceType)
        if (ua != null && !ua.isBlank()) {
            Client c = uaParser.parse(ua);

            if (c.os != null) {
                log.setOs(nz(c.os.family, "UNKNOWN"));
            } else {
                log.setOs("UNKNOWN");
            }

            if (c.userAgent != null) {
                log.setBrowser(nz(c.userAgent.family, "UNKNOWN"));
            } else {
                log.setBrowser("UNKNOWN");
            }

            // deviceType 판정 (간단 룰)
            final String family = (c.device == null || c.device.family == null)
                    ? "" : c.device.family.toLowerCase(Locale.ROOT);

            final String devType =
                    (family.contains("tablet")) ? "TABLET" :
                    (family.contains("mobile") || family.contains("phone")) ? "MOBILE" :
                    (family.contains("desktop") || family.isEmpty()) ? "DESKTOP" : "OTHER";

            log.setDeviceType(devType);
        } else {
            // UA가 없을 때의 안전값
            if (log.getOs() == null)       log.setOs("UNKNOWN");
            if (log.getBrowser() == null)  log.setBrowser("UNKNOWN");
            if (log.getDeviceType() == null) log.setDeviceType("OTHER");
        }
    }

    /* ========== helpers ========== */

    private String extractHost(String url) {
        try { return (url == null || url.isBlank()) ? null : new URI(url).getHost(); }
        catch (Exception e) { return null; }
    }

    private String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return null;
    }

    private String toUpperOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.toUpperCase(Locale.ROOT);
    }

    private String nz(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return null; // 실패 시 null
        }
    }

    private BotLabel classifyBot(String ua, String refHost) {
        if (ua == null) return new BotLabel(false, null);
        final String u = ua.toLowerCase(Locale.ROOT);

        // (A) 미리보기 봇 (통계 분리용)
        if (u.contains("facebookexternalhit") || u.contains("twitterbot") ||
            u.contains("slackbot") || u.contains("whatsapp") || u.contains("kakaotalk")) {
            return new BotLabel(true, "preview");
        }

        // (B) 대표 크롤러
        if (u.contains("googlebot") || u.contains("bingbot") ||
            u.contains("yeti") || u.contains("naverbot") ||
            u.contains("ahrefs") || u.contains("semrush")) {
            return new BotLabel(true, "crawler");
        }

        // (C) 헬스체크/검증기
        if ("validator.w3.org".equalsIgnoreCase(refHost)) {
            return new BotLabel(true, "healthcheck");
        }

        return new BotLabel(false, null);
    }

    private record BotLabel(boolean bot, String type) {}
}

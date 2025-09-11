package org.example.dashboard.service;

import org.example.dashboard.repository.ClickLogRepository;
import org.example.dashboard.vo.ClickLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClickLogService {

    @Autowired
    private ClickLogRepository clickLogRepository;

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
    public String detectBrowser(String ua) {
        if (ua == null) return "UNKNOWN";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Safari")) return "Safari";
        if (ua.contains("Firefox")) return "Firefox";
        return "Other";
    }

    public String detectOS(String ua) {
        if (ua == null) return "UNKNOWN";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS")) return "Mac";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone")) return "iOS";
        return "Other";
    }

    public String detectChannel(String ref) {
        if (ref == null) return "DIRECT";
        if (ref.contains("instagram")) return "Instagram";
        if (ref.contains("kakao")) return "Kakao";
        if (ref.contains("naver")) return "Naver";
        return "Other";
    }
}

package org.example.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClickLog {
    private Long id;
    private Long linkId;
    
    
    private String ipHash;   
    private String referrer;   // 디버깅/원본 보존
    private String referrerHost;   // 통계 그룹핑 + 인덱스 최적화
    private String channel;
    private String deviceType; 
    private String os;
    private String browser;
    private String userAgent;     // 이하 ua
    private LocalDateTime clickedAt;   
    private String countryCode;     // 이하 country
    
    
    
    private Integer isBot;      // 0/1
    private String botType;     // crawler/preview/healthcheck/null
    
}

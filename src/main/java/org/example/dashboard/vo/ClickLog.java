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
    private String referrer;
    private String channel;
    private String deviceType;
    private String os;
    private String browser;
    private String userAgent;
    private LocalDateTime clickedAt;
}

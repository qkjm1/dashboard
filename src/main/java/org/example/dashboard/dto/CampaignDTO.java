// src/main/java/org/example/dashboard/dto/CampaignDTO.java
package org.example.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON에 안 내려가게
public class CampaignDTO {

    // [공유 필드: 캠페인 요약용]
    private String slug;
    private Long totalClicks;
    private List<ReferrerCountDTO> topReferrers; // host 기준
    private List<ReferrerCountDTO> topChannels;  // channel 기준

    // [공유 필드: 비교용]
    private String targetUrl;
    private List<CampaignDTO> campaigns; // slug별 요약들

    /* ---------- 분류/생성 메서드 ---------- */

    // slug 하나의 요약 객체 생성
    public static CampaignDTO summary(String slug,
                                      long totalClicks,
                                      List<ReferrerCountDTO> topReferrers,
                                      List<ReferrerCountDTO> topChannels) {
        return CampaignDTO.builder()
                .slug(slug)
                .totalClicks(totalClicks)
                .topReferrers(topReferrers)
                .topChannels(topChannels)
                .build();
    }

    // 동일 targetUrl에 대한 비교 객체(상위 컨테이너) 생성
    public static CampaignDTO compare(String targetUrl,
                                      List<CampaignDTO> campaigns) {
        return CampaignDTO.builder()
                .targetUrl(targetUrl)
                .campaigns(campaigns)
                .build();
    }

    /* ---------- 타입 판별 헬퍼 ---------- */
    public boolean isSummary() { return slug != null; }
    public boolean isCompare() { return targetUrl != null; }
}

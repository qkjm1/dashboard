
package org.example.dashboard.dto;
import lombok.Data;

/*
 *  유니크/중복 클릭 확인 모음 dto
 */
@Data
public class UniqueStatsDTO {
    private long totalClicks;         // 총 클릭
    private long uniqueApprox;        // ip_hash + UA 기준 유니크
    private double duplicateRatio;    // (total - unique)/total
    private long uniqueWindowed;      // 짧은 간격 필터 적용 유니크(세션 수)
    private int windowMinutes;        // 필터 간격(분)
}

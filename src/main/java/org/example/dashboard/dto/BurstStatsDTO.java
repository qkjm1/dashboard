package org.example.dashboard.dto;

import lombok.Data;
import org.example.dashboard.vo.Link;


import lombok.*;
import java.util.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class BurstStatsDTO {
    private long totalClicks;
    private Integer halflifeHours; // 총 클릭의 50% 도달 시간(없으면 null)
    private List<TimeBucketCountDTO> byHourSinceCreate; // hour 0,1,2,... 분포
    private List<Long> cumulative;  // 누적 합(위와 동일 인덱스)
}
package org.example.dashboard.dto;


import lombok.*;
import java.util.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class ReExposeStatsDTO {
    private int windowHours; // 기본 24
    private long beforeTotal;
    private long afterTotal;
    private List<TimeBucketCountDTO> beforeSeries; // 시간별
    private List<TimeBucketCountDTO> afterSeries;  // 시간별
}

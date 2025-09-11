package org.example.dashboard.dto;

import lombok.*;

import org.example.dashboard.vo.Link;



@Data
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class TimeBucketCountDTO {
    private String bucket; // "0"~"23", "1"~"7", "2025-09", "2025-09-12 14:00:00" 등
    private long cnt;
}
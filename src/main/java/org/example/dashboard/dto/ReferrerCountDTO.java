package org.example.dashboard.dto;

import lombok.Data;

@Data
public class ReferrerCountDTO {
    private String key;   // referrerHost 또는 channel
    private long cnt;
}
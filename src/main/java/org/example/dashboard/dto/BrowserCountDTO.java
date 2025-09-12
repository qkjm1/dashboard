package org.example.dashboard.dto;

import lombok.Data;
import org.example.dashboard.vo.Link;


/*
 * 브라우저별 유입클릭 카운트
 */
@Data
public class BrowserCountDTO {
    private String browser; // Chrome, Safari, ...
    private int cnt;        // 해당 브라우저 클릭 수
}

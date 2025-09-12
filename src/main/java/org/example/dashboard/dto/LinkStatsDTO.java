package org.example.dashboard.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;


/* 링크집계결과만 담음
 * json 예시는 변수는 오른쪽에
 * 
 */
@Data
public class LinkStatsDTO {
	
	// 총 클릭 수
    private long totalClicks;
    
    // 최근 24시간 시간대별
    private List<Map<String, Object>> clicksLast24hByHour; // [{hour: "2025-09-11 13", cnt: 10}, ...]
    
    //최근 7일 일자별
    private List<Map<String, Object>> clicksLast7dByDate;  // [{date: "2025-09-05", cnt: 120}, ...]
    
    // 상위 레퍼런스 / 브라우저 / os목록
    private List<Map<String, Object>> topReferrers;        // [{referrer: "...", cnt: 30}, ...]
    private List<Map<String, Object>> topBrowsers;         // [{browser: "Chrome", cnt: 50}, ...]
    private List<Map<String, Object>> topOS;               // [{os: "iOS", cnt: 40}, ...]
}

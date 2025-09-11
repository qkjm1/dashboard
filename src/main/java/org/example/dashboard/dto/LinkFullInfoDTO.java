package org.example.dashboard.dto;

import lombok.Data;
import org.example.dashboard.vo.Link;


/*
 * dto로 모은 정보 모음
 */
@Data
public class LinkFullInfoDTO {
	
	// 링크 자체 정보 (슬러그, 원본 URL, 활성 여부, 생성일 )
    private Link link; 
    
    // 클릭 로그 통계 (클릭 수, 시간대/요일 분포, 상위 리퍼러·브라우저·OS 등)
    private LinkStatsDTO stats;      
    
    // 원본 URL에서 스크랩한 메타 태그(title, description, og:image…)/미리보기 썸네일같은거
    private UrlMetaDTO meta;
}

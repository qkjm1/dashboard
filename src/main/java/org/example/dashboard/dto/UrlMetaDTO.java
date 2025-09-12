package org.example.dashboard.dto;

import lombok.Data;


/*
 * 원본페이지의 미리보기 정보 등 메타 데이터
 */

@Data
public class UrlMetaDTO {
	
	//  페이지 제목
    private String title;
    
    // 요약 문구
    private String description;
    
    // 공유 섬네일
    private String ogImage;
    
    // 사이트 이름
    private String ogSiteName;
}

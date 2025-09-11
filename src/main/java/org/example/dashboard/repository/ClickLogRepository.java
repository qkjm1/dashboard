package org.example.dashboard.repository;

import org.example.dashboard.vo.ClickLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClickLogRepository {
	
    void insertClickLog(ClickLog clickLog);
    
    List<ClickLog> selectByLinkId(Long linkId);
    
    int countByLinkId(Long linkId);
    
    
    // ↓ 아래 7개가 반드시 선언되어 있어야 서비스가 컴파일됩니다
    List<Map<String, Object>> last24hByHour(@Param("linkId") Long linkId);
    List<Map<String, Object>> last7dByDate(@Param("linkId") Long linkId);
    List<Map<String, Object>> byChannel(@Param("linkId") Long linkId);
    List<Map<String, Object>> byDevice(@Param("linkId") Long linkId);
    List<Map<String, Object>> topBrowsers(@Param("linkId") Long linkId, @Param("limit") int limit);
    List<Map<String, Object>> topOS(@Param("linkId") Long linkId, @Param("limit") int limit);
    List<Map<String, Object>> topReferrerHost(@Param("linkId") Long linkId, @Param("limit") int limit);
}

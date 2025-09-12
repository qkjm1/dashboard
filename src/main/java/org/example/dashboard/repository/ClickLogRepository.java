package org.example.dashboard.repository;

import org.example.dashboard.dto.BrowserCountDTO;
import org.example.dashboard.dto.ReferrerCountDTO;
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

	List<BrowserCountDTO> browseCntBySlug(@Param("slug") String slug);

	// ↓ 아래 7개가 반드시 선언되어 있어야 서비스가 컴파일됩니다
	List<Map<String, Object>> last24hByHour(@Param("linkId") Long linkId);

	List<Map<String, Object>> last7dByDate(@Param("linkId") Long linkId);

	List<Map<String, Object>> byChannel(@Param("linkId") Long linkId);

	List<Map<String, Object>> byDevice(@Param("linkId") Long linkId);

	List<Map<String, Object>> topBrowsers(@Param("linkId") Long linkId, @Param("limit") int limit);

	List<Map<String, Object>> topOS(@Param("linkId") Long linkId, @Param("limit") int limit);

	List<Map<String, Object>> topReferrerHost(@Param("linkId") Long linkId, @Param("limit") int limit);

	List<Map<String, Object>> hourlyDistBySlug(@Param("slug") String slug);

	List<Map<String, Object>> dowDistBySlug(@Param("slug") String slug);

	List<Map<String, Object>> monthDistBySlug(@Param("slug") String slug);

	List<Map<String, Object>> hoursSinceCreateBySlug(@Param("slug") String slug);

	List<Map<String, Object>> seriesHourlyAroundMoment(@Param("slug") String slug,
			@Param("centerTs") java.sql.Timestamp centerTs, @Param("windowHours") int windowHours,
			@Param("side") String side // "before" | "after"
	);
	
	// 리퍼러 호스트 TOP N (slug 기준)
    List<ReferrerCountDTO> topReferrersBySlug(@Param("slug") String slug,
                                              @Param("limit") int limit);

    // 채널 TOP N (slug 기준)
    List<ReferrerCountDTO> topChannelsBySlug(@Param("slug") String slug,
                                             @Param("limit") int limit);

    // 동일 타깃 URL 전체(모든 slug 합산) 기준 리퍼러 TOP N
    List<ReferrerCountDTO> topReferrersByTargetUrl(@Param("targetUrl") String targetUrl,
                                                   @Param("limit") int limit);
}

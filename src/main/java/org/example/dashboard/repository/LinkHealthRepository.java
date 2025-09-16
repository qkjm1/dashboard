package org.example.dashboard.repository;

import org.example.dashboard.dto.BrowserCountDTO;
import org.example.dashboard.dto.CountryCountDTO;
import org.example.dashboard.dto.ReferrerCountDTO;
import org.example.dashboard.dto.TimeBucketCountDTO;
import org.example.dashboard.vo.ClickLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LinkHealthRepository {

	Map<String, Object> findLatest(@Param("slug") String slug);
	List<Map<String, Object>> findRecent(@Param("slug") String slug, @Param("limit") int limit);


	Map<String, Object> summaryCounts(@Param("slug") String slug,
	@Param("start") LocalDateTime start,
	@Param("end") LocalDateTime end);


	List<Map<String, Object>> summaryByStatus(@Param("slug") String slug,
	@Param("start") LocalDateTime start,
	@Param("end") LocalDateTime end);
}

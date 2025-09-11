package org.example.dashboard.repository;

import org.example.dashboard.vo.ClickLog;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ClickLogRepository {
    void insertClickLog(ClickLog clickLog);
    List<ClickLog> selectByLinkId(Long linkId);
    int countByLinkId(Long linkId);
}

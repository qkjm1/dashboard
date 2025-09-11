package org.example.dashboard.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.dashboard.vo.Link;

@Mapper
public interface LinkRepository {
    void insertLink(Link link);

    Link selectBySlug(@Param("slug") String slug);

    int countBySlug(@Param("slug") String slug);
}

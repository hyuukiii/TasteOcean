package com.example.ocean.mapper;

import com.example.ocean.domain.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventMapper {
    List<Event> selectTodayEvents(@Param("userId") String userId, @Param("workspaceCd") String workspaceCd);

    int countCompletedEventsThisWeekByWorkspace(@Param("workspaceCd") String workspaceCd);

    int countUpcomingEventsThisWeekByWorkspace(String workspaceCd);

    int countCreatedEventsThisWeekByWorkspace(String workspaceCd);

}


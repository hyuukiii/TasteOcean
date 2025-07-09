package com.example.ocean.service;

import com.example.ocean.domain.Event;
import com.example.ocean.mapper.EventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    @Autowired
    private EventMapper eventMapper;

    public List<Event> getTodayEvents(String userId, String workspaceCd) {
        System.out.println("🛠 서비스 진입: userId = " + userId + ", workspaceCd = " + workspaceCd);
        return eventMapper.selectTodayEvents(userId, workspaceCd);
    }


    public int countCompletedEventsThisWeekByWorkspace(String workspaceCd) {
        return eventMapper.countCompletedEventsThisWeekByWorkspace(workspaceCd);
    }

    public int countUpcomingEventsThisWeekByWorkspace(String workspaceCd) {
        return eventMapper.countUpcomingEventsThisWeekByWorkspace(workspaceCd);
    }

    public int countCreatedEventsThisWeekByWorkspace(String workspaceCd) {
        return eventMapper.countCreatedEventsThisWeekByWorkspace(workspaceCd);
    }

}

package com.example.ocean.controller.event;

import com.example.ocean.domain.Event;
import com.example.ocean.mapper.MemberTransactionMapper;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.EventService;
import com.example.ocean.service.WorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private WorkspaceService workspaceService;

    @GetMapping("/today")
    public List<Event> getTodayEvents(@RequestParam String userId, @RequestParam String workspaceCd) {
        System.out.println("📥 오늘 일정 요청: userId = " + userId + ", workspaceCd = " + workspaceCd);
        return eventService.getTodayEvents(userId, workspaceCd);
    }

    @GetMapping("/this-week-completed-count")
    public int getThisWeekCompletedCount(@RequestParam("workspaceCd") String workspaceCd) {
        int count = eventService.countCompletedEventsThisWeekByWorkspace(workspaceCd);
        return count;
    }

    @GetMapping("/this-week-upcoming-count")
    public int getThisWeekUpcomingCount(@RequestParam("workspaceCd") String workspaceCd) {
        int count = eventService.countUpcomingEventsThisWeekByWorkspace(workspaceCd);
        return count;
    }

    @GetMapping("/this-week-created-count")
    public int getThisWeekCreatedCount(@RequestParam("workspaceCd") String workspaceCd) {
        int count = eventService.countCreatedEventsThisWeekByWorkspace(workspaceCd);
        return count;
    }

    @GetMapping("/{workspaceCd}/usage-time")
    @ResponseBody
    public ResponseEntity<Long> getUserUsageTime(
            @PathVariable String workspaceCd,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long seconds = workspaceService.getAccumulatedTime(workspaceCd, userPrincipal.getId());
        return ResponseEntity.ok(seconds);
    }
}

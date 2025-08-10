package com.example.ocean.controller.teamCalendar;

import com.example.ocean.domain.Notification;
import com.example.ocean.dto.request.EventCreateRequest;
import com.example.ocean.dto.request.EventUpdateRequest;
import com.example.ocean.dto.response.CalendarResponse;
import com.example.ocean.dto.response.EventDetailResponse;
import com.example.ocean.service.NotificationService;
import com.example.ocean.service.TeamCalendarService;
import com.example.ocean.service.UserService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/calendar/team")
@RequiredArgsConstructor
public class TeamCalendarAPIController {

    private final TeamCalendarService teamCalendarService;

    private final NotificationService notificationService;

    private final UserService userService;

    @GetMapping("")
    public ResponseEntity<List<CalendarResponse>> personalCalendar(
            @RequestParam(required = false) String workspaceCd
    ) {
        List<CalendarResponse> result = teamCalendarService.getTeamEvents(workspaceCd);
        //List <Workspace
        if (result == null || result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/events/{eventCd}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable String eventCd) {
        EventDetailResponse result = teamCalendarService.selectTeamEventDetail(eventCd);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createEvent(
            @RequestPart("request") EventCreateRequest request,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @AuthenticationPrincipal UserDetails userPrincipal
    ) {
        log.info("📌 [팀 일정] createEvent 진입 - 요청자: {}", userPrincipal.getUsername());

        int result = teamCalendarService.insertTeamEvent(request, files);
        log.info("🧾 팀 일정 저장 결과: {}", result);

        if (result == 1) {
            String userId = userPrincipal.getUsername(); // ✅ 이 값을 사용
            // String nickname = userService.getUserProfile(userId).getUserName(); // ❌ 필요 없음

            Notification notification = new Notification();
            notification.setNotiId(UUID.randomUUID().toString());
            notification.setWorkspaceCd(request.getWorkspaceCd());
            notification.setCreatedBy(userId); // ✅ 수정된 부분
            notification.setNotiState("NEW_EVENT");

            log.info("📨 [팀 일정] 알림 생성 요청: {}", notification);

            try {
                notificationService.createNotification(notification);
                log.info("✅ [팀 일정] 알림 저장 완료");
            } catch (Exception e) {
                log.error("❌ [팀 일정] 알림 저장 실패", e);
            }

            return ResponseEntity.ok("일정 등록 성공");
        }

        log.warn("❌ [팀 일정] 일정 등록 실패: result != 1");
        return ResponseEntity.badRequest().body("일정 등록 실패");
    }



    @PutMapping("/events/{eventCd}")
    public ResponseEntity<String> updateEventDetail(
            @PathVariable String eventCd,
            @RequestPart("request") EventUpdateRequest request,
            @RequestPart(required = false) MultipartFile[] files,
            @RequestPart(required = false) List<String> deletedFileIds
    ) {
        int result = teamCalendarService.updateTeamEvent(request, deletedFileIds, files);
        return result == 1
                ? ResponseEntity.ok("일정 수정 성공")
                : ResponseEntity.badRequest().body("일정 수정 실패");
    }

    @DeleteMapping("/events/{eventCd}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String eventCd,
            @RequestParam(required = false) String userId
    ) {
        int result = teamCalendarService.deleteTeamEvent(eventCd, userId);
        return result == 1
                ? ResponseEntity.ok("일정 삭제 성공")
                : ResponseEntity.badRequest().body("일정 삭제 실패");
    }

    @GetMapping("/events/{eventCd}/files")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileId") String fileId) throws IOException {
        return teamCalendarService.downloadFile(fileId);
    }

}
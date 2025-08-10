package com.example.ocean.controller.personalCalendar;

import com.example.ocean.domain.Notification;
import com.example.ocean.dto.request.EventCreateRequest;
import com.example.ocean.dto.request.EventUpdateRequest;
import com.example.ocean.dto.response.EventDetailResponse;
import com.example.ocean.dto.response.CalendarResponse;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.NotificationService;
import com.example.ocean.service.PersonalCalendarService;
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
@RequestMapping("/api/calendar/personal")
@RequiredArgsConstructor
public class PersonalCalendarAPIController {

    private final PersonalCalendarService personalCalendarService;
    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping("/{currentUserId}")
    public ResponseEntity<List<CalendarResponse>> personalCalendar(
            @PathVariable String currentUserId,
            @RequestParam(required = false) String workspaceCd

    ) {
        List<CalendarResponse> result = personalCalendarService.getPersonalEvents(currentUserId, workspaceCd);
        //List <Workspace
        if (result == null || result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/events/{eventCd}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable String eventCd) {
        EventDetailResponse result = personalCalendarService.getPersonalEventDetail(eventCd);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createEvent(
            @RequestPart("request") EventCreateRequest eventCreateRequest,
            @RequestPart(value = "attendenceIds", required = false) List<String> attendenceIds,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @AuthenticationPrincipal UserDetails userPrincipal
    ) {
        log.info("📌 [개인일정] createEvent 진입");

        int result = personalCalendarService.createPersonalEvent(eventCreateRequest, attendenceIds, files);
        log.info("🔍 personalCalendarService 결과: {}", result);

        if (result == 1) {
            String userId = userPrincipal.getUsername();
            log.info("👤 로그인 유저 ID: {}", userId);

            Notification notification = new Notification();
            notification.setNotiId(UUID.randomUUID().toString());
            notification.setWorkspaceCd(eventCreateRequest.getWorkspaceCd());
            notification.setCreatedBy(userId);
            notification.setNotiState("NEW_EVENT");

            log.info("📨 알림 생성 요청 객체: {}", notification);

            try {
                notificationService.createNotification(notification);
                log.info("✅ MAIN_NOTIFICATION 저장 완료");
            } catch (Exception e) {
                log.error("❌ MAIN_NOTIFICATION 저장 실패", e);
            }

            return ResponseEntity.ok("일정 등록 성공");
        } else {
            log.warn("❌ 일정 등록 실패: personalCalendarService 결과값이 {}", result);
            return ResponseEntity.badRequest().body("일정 등록 실패");
        }
    }


    @PutMapping("/events/{eventCd}")
    public ResponseEntity<String> updateEventDetail(
            @PathVariable String eventCd,
            @RequestPart("request") EventUpdateRequest request,
            @RequestPart(required = false) MultipartFile[] files,
            @RequestPart(required = false) List<String> deletedFileIds,
            @RequestPart(value = "attendenceIds", required = false) List<String> attendenceIds
    ) {

        int result = personalCalendarService.updatePersonalEvent(request, attendenceIds, deletedFileIds, files);

        return result == 1
                ? ResponseEntity.ok("일정 수정 성공")
                : ResponseEntity.badRequest().body("일정 수정 실패");
    }


    @DeleteMapping("/events/{eventCd}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String eventCd,
            @RequestParam(required = false) String userId
    ) {
        int result = personalCalendarService.deletePersonalEvent(eventCd, userId);

        return result == 1
                ? ResponseEntity.ok("일정 삭제 성공")
                : ResponseEntity.badRequest().body("일정 삭제 실패");
    }

    @GetMapping("/events/{eventCd}/files")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileId") String fileId) throws IOException {
        return personalCalendarService.downloadFile(fileId);
    }

}
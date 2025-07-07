package com.example.ocean.controller.meeting;

import com.example.ocean.dto.MeetingInfoDto;
import com.example.ocean.dto.MeetingRoomDto;
import com.example.ocean.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingApiController {

    private final MeetingService meetingService;

    /**
     * 사용자의 진행 중인 회의 조회
     */
    @GetMapping("/active")
    public ResponseEntity<MeetingRoomDto> getActiveMeeting(
            @RequestParam String workspaceCd,
            @RequestParam String userId) {

        try {
            MeetingRoomDto activeMeeting = meetingService.getActiveMeetingForUser(workspaceCd, userId);

            if (activeMeeting != null) {
                return ResponseEntity.ok(activeMeeting);
            } else {
                return ResponseEntity.noContent().build();
            }

        } catch (Exception e) {
            log.error("진행 중인 회의 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 회의 정보 조회 (미디어 서버용)
     */
    @GetMapping("/{roomId}/info")
    public ResponseEntity<MeetingInfoDto> getMeetingInfo(@PathVariable String roomId) {
        try {
            MeetingInfoDto meetingInfo = meetingService.getMeetingInfo(roomId);

            if (meetingInfo != null) {
                return ResponseEntity.ok(meetingInfo);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("회의 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 회의 상태 업데이트 (미디어 서버용)
     */
    @PutMapping("/{roomId}/status")
    public ResponseEntity<Void> updateMeetingStatus(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {

        try {
            String status = request.get("status");
            meetingService.updateMeetingStatus(roomId, status);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("회의 상태 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

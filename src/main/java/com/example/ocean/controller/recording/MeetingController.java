package com.example.ocean.controller.recording;

import com.example.ocean.service.MeetingService;
import com.example.ocean.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    /**
     * 진행 중인 회의 목록 조회
     */
    @GetMapping("/active")
    public ResponseEntity<List<ActiveMeetingDto>> getActiveMeetings(
            @RequestParam String workspaceId,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            List<ActiveMeetingDto> meetings = meetingService.getActiveMeetings(workspaceId);
            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            log.error("진행 중인 회의 목록 조회 실패: workspaceId={}", workspaceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 미팅 참가자 추가 (재접속 포함)
     */
    @PostMapping("/{roomId}/participants")
    public ResponseEntity<Void> joinMeeting(
            @PathVariable String roomId,
            @RequestParam(required = false) boolean rejoin,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            if (rejoin) {
                // 재접속 처리
                meetingService.rejoinParticipant(roomId, user.getId());
            } else {
                // 신규 참가
                meetingService.addParticipant(roomId, user.getId(), "PARTICIPANT");
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("미팅 참가 실패: roomId={}, userId={}, rejoin={}",
                    roomId, user.getId(), rejoin, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * 미팅 종료 (호스트 권한 확인)
     */
    @PostMapping("/{roomId}/end")
    public ResponseEntity<Void> endMeeting(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            // 호스트 권한 확인
            if (!meetingService.isHost(roomId, user.getId())) {
                return ResponseEntity.status(403).build(); // Forbidden
            }

            meetingService.endMeeting(roomId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("미팅 종료 실패: roomId={}", roomId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 미팅에서 나가기 (일시적)
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveMeeting(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            meetingService.leaveParticipant(roomId, user.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("미팅 나가기 실패: roomId={}, userId={}", roomId, user.getId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * 미팅 상태 확인
     */
    @GetMapping("/{roomId}/status")
    public ResponseEntity<MeetingStatusResponse> getMeetingStatus(@PathVariable String roomId) {
        try {
            boolean isActive = meetingService.isMeetingActive(roomId);
            String hostId = meetingService.getHostId(roomId);

            return ResponseEntity.ok(new MeetingStatusResponse(
                    roomId,
                    isActive,
                    hostId
            ));
        } catch (Exception e) {
            log.error("미팅 상태 조회 실패: roomId={}", roomId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 호스트 권한 이전
     */
    @PostMapping("/{roomId}/transfer-host")
    public ResponseEntity<Void> transferHost(
            @PathVariable String roomId,
            @RequestParam String newHostId,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            // 현재 호스트 권한 확인
            if (!meetingService.isHost(roomId, user.getId())) {
                return ResponseEntity.status(403).build();
            }

            meetingService.transferHost(roomId, newHostId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("호스트 권한 이전 실패: roomId={}, newHostId={}", roomId, newHostId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 미팅 상태 응답 DTO
     */
    record MeetingStatusResponse(String roomId, boolean isActive, String hostId) {}
}

package com.example.ocean.controller.recording;

import com.example.ocean.domain.WorkspaceMember;
import com.example.ocean.dto.ActiveMeetingDto;
import com.example.ocean.mapper.WorkspaceMapper;
import com.example.ocean.service.MeetingService;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final WorkspaceService workspaceService;  // final 추가!

    @GetMapping("/active")
    public ResponseEntity<List<ActiveMeetingDto>> getActiveMeetings(
            @RequestParam String workspaceId,
            @AuthenticationPrincipal UserPrincipal user) {

        log.info("진행 중인 회의 목록 조회 - workspaceId: {}, userId: {}",
                workspaceId, user.getId());

        try {
            // 워크스페이스 멤버 권한 확인
            WorkspaceMember member = workspaceService.findMemberByWorkspaceAndUser(
                    workspaceId,
                    user.getId()
            );

            if (member == null) {
                log.warn("워크스페이스 멤버가 아닙니다 - workspaceId: {}, userId: {}",
                        workspaceId, user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 진행 중인 회의 목록 조회
            List<ActiveMeetingDto> meetings = meetingService.getActiveMeetings(workspaceId);

            log.info("진행 중인 회의 {} 개 조회됨", meetings.size());

            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            log.error("진행 중인 회의 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
     * 현재 사용자가 특정 회의의 호스트인지 확인
     */
    @GetMapping("/{roomId}/is-host")
    public ResponseEntity<Map<String, Object>> checkIfHost(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            boolean isHost = meetingService.isHost(roomId, user.getId());
            String hostId = meetingService.getHostId(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("isHost", isHost);
            response.put("hostId", hostId);
            response.put("currentUserId", user.getId());

            log.info("호스트 확인 - roomId: {}, userId: {}, isHost: {}",
                    roomId, user.getId(), isHost);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("호스트 확인 실패: roomId={}, userId={}", roomId, user.getId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 회의 재참여 처리
     * 호스트 정보를 유지하면서 재참여를 처리합니다.
     */
    @PostMapping("/{roomId}/rejoin")
    public ResponseEntity<Map<String, Object>> rejoinMeeting(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            log.info("회의 재참여 요청 - roomId: {}, userId: {}", roomId, user.getId());

            // 회의 정보 조회
            MeetingRoom meeting = meetingService.getMeetingByRoomId(roomId);
            if (meeting == null) {
                return ResponseEntity.notFound().build();
            }

            // 호스트 여부 확인
            boolean isHost = meeting.getHostId().equals(user.getId());

            // 참가자 상태 업데이트 (재참여로 표시)
            meetingService.updateParticipantStatus(roomId, user.getId(), "REJOINED");

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("meetingTitle", meeting.getRoomNm());
            response.put("hostId", meeting.getHostId());
            response.put("isHost", isHost);
            response.put("userId", user.getId());
            response.put("userName", user.getUsername());
            response.put("status", meeting.getStatus());

            log.info("재참여 응답 - isHost: {}, hostId: {}", isHost, meeting.getHostId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("회의 재참여 실패: roomId={}, userId={}", roomId, user.getId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 회의 참가자 목록 조회 (호스트 정보 포함)
     */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<Map<String, Object>> getParticipants(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            // 회의 정보 조회
            MeetingRoom meeting = meetingService.getMeetingByRoomId(roomId);
            if (meeting == null) {
                return ResponseEntity.notFound().build();
            }

            // 참가자 목록 조회
            List<MeetingParticipant> participants = meetingService.getActiveParticipants(roomId);

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("hostId", meeting.getHostId());
            response.put("currentUserId", user.getId());
            response.put("isHost", meeting.getHostId().equals(user.getId()));
            response.put("participants", participants.stream()
                    .map(p -> {
                        Map<String, Object> pMap = new HashMap<>();
                        pMap.put("userId", p.getUserId());
                        pMap.put("userName", p.getUser().getUserNm());
                        pMap.put("role", p.getRole());
                        pMap.put("joinedAt", p.getJoinedDate());
                        pMap.put("isActive", "Y".equals(p.getActiveState()));
                        return pMap;
                    })
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("참가자 목록 조회 실패: roomId={}", roomId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 미팅 상태 응답 DTO
     */
    record MeetingStatusResponse(String roomId, boolean isActive, String hostId) {}
}

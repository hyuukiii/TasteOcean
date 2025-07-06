package com.example.ocean.controller.meeting;

import com.example.ocean.dto.request.MeetingCreateRequest;
import com.example.ocean.dto.response.MeetingCreateResponse;
import com.example.ocean.domain.WorkspaceMember;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.service.MeetingService;
import com.example.ocean.service.WorkspaceService;
import com.example.ocean.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 회의 준비 페이지 컨트롤러
 *
 * @author Ocean Team
 * @since 2024.01.15
 */
@Slf4j
@Controller
@RequestMapping("/meeting")
@RequiredArgsConstructor
public class MeetingSetupController {

    private final MeetingService meetingService;
    private final WorkspaceService workspaceService;
    private final EmailService emailService;

    @Value("${media.server.url:https://localhost:3001}")
    private String mediaServerUrl;

    /**
     * 회의 준비 페이지 표시
     *
     * @param workspaceCd 워크스페이스 코드
     * @param model       Spring MVC Model
     * @param user        인증된 사용자 정보
     * @return 회의 준비 페이지 뷰
     */
    @GetMapping("/setup")
    public String showSetupPage(
            @RequestParam String workspaceCd,
            Model model,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            // 워크스페이스 검증
            if (!workspaceService.hasAccess(workspaceCd, user.getId())) {
                log.warn("워크스페이스 접근 권한 없음: userId={}, workspaceCd={}",
                        user.getId(), workspaceCd);
                return "redirect:/error?message=access-denied";
            }

            // 워크스페이스 정보 조회
            String workspaceName = workspaceService.getWorkspaceName(workspaceCd);

            // 워크스페이스 멤버 목록 조회
            List<WorkspaceMember> members = workspaceService.getActiveMembers(workspaceCd);  // ✅ 타입 변경

            // 사용자 회의 설정 조회 (있으면)
            var preferences = meetingService.getUserPreferences(user.getId());

            // 모델에 데이터 추가
            model.addAttribute("workspaceCd", workspaceCd);
            model.addAttribute("workspaceName", workspaceName);
            model.addAttribute("members", members);
            model.addAttribute("currentUserId", user.getId());
            model.addAttribute("mediaServerUrl", mediaServerUrl);

            if (preferences != null) {
                model.addAttribute("preferences", preferences);
            }

            log.info("회의 준비 페이지 접속: userId={}, workspaceCd={}",
                    user.getId(), workspaceCd);

            return "meeting/meeting-setup";

        } catch (Exception e) {
            log.error("회의 준비 페이지 로드 실패", e);
            return "redirect:/error";
        }
    }

    /**
     * 회의 생성 API
     *
     * @param request 회의 생성 요청 데이터
     * @param user    인증된 사용자 정보
     * @return 생성된 회의 정보
     */
    @PostMapping("/create")
    @ResponseBody
    public MeetingCreateResponse createMeeting(
            @RequestBody MeetingCreateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            // 워크스페이스 접근 권한 확인
            if (!workspaceService.hasAccess(request.getWorkspaceCd(), user.getId())) {
                throw new SecurityException("워크스페이스 접근 권한이 없습니다.");
            }

            // 룸 ID 생성
            String roomId = generateRoomId(request.getMeetingType());

            // DB에 미팅룸 정보 저장
            meetingService.createMeetingRoom(
                    roomId,
                    request.getTitle(),
                    request.getWorkspaceCd(),
                    user.getId(),
                    request.getMeetingType()
            );

            // 회의 옵션 저장
            meetingService.saveMeetingOptions(roomId, request);

            // 사용자 설정 저장 (다음 회의에서 재사용)
            meetingService.saveUserPreferences(user.getId(), request);

            // 멤버 초대 처리
            if (request.getInvitedMembers() != null && !request.getInvitedMembers().isEmpty()) {
                for (String memberId : request.getInvitedMembers()) {
                    meetingService.inviteMember(roomId, memberId, user.getId());
                }
            }

            // 이메일 초대 처리
            if (request.getInvitedEmails() != null && !request.getInvitedEmails().isEmpty()) {
                emailService.sendMeetingInvitations(
                        roomId,
                        request.getTitle(),
                        request.getInvitedEmails(),
                        user.getName()
                );
            }

            // 캘린더 일정 생성 (예약된 경우)
            if (request.getScheduledTime() != null) {
                meetingService.createCalendarEvent(
                        roomId,
                        request.getTitle(),
                        request.getScheduledTime(),
                        request.getDuration(),
                        user.getId()
                );
            }

            log.info("회의 생성 완료: roomId={}, title={}, creator={}",
                    roomId, request.getTitle(), user.getId());

            // 응답 생성
            return MeetingCreateResponse.builder()
                    .roomId(roomId)
                    .joinUrl(buildJoinUrl(roomId, request.getTitle()))
                    .displayName(user.getName())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("회의 생성 실패", e);

            return MeetingCreateResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 빠른 회의 시작 (설정 페이지 건너뛰기)
     */
    @PostMapping("/quick-start")
    @ResponseBody
    public MeetingCreateResponse quickStart(
            @RequestParam String workspaceCd,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            // 기본 설정으로 회의 생성
            MeetingCreateRequest request = MeetingCreateRequest.builder()
                    .title("빠른 회의 - " + user.getName())
                    .workspaceCd(workspaceCd)
                    .meetingType("sketch")
                    .duration(60)
                    .muteOnJoin(true)
                    .build();

            return createMeeting(request, user);  // createMeeting이 이미 title을 처리함

        } catch (Exception e) {
            log.error("빠른 회의 시작 실패", e);

            return MeetingCreateResponse.builder()
                    .success(false)
                    .errorMessage("회의를 시작할 수 없습니다.")
                    .build();
        }
    }

    /**
     * 회의 룸 ID 생성
     */
    private String generateRoomId(String meetingType) {
        String prefix = meetingType != null ? meetingType : "meeting";
        String timestamp = LocalDateTime.now().toString()
                .replace(":", "-")
                .replace(".", "-");
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s-%s-%s", prefix, timestamp, uuid);
    }

    /**
     * 회의 참가 URL 생성
     */
    private String buildJoinUrl(String roomId, String title) {
        try {
            String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
            String encodedTitle = URLEncoder.encode(title != null ? title : "회의", StandardCharsets.UTF_8);

            return String.format("%s/ocean-video-chat-complete.html?roomId=%s&meetingTitle=%s",
                    mediaServerUrl, encodedRoomId, encodedTitle);
        } catch (Exception e) {
            log.error("URL 인코딩 실패", e);
            // 인코딩 실패 시 원본 값 사용
            return String.format("%s/ocean-video-chat-complete.html?roomId=%s&meetingTitle=%s",
                    mediaServerUrl, roomId, title != null ? title : "회의");
        }
    }
}
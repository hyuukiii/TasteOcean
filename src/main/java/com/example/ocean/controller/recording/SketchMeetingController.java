package com.example.ocean.controller.recording;

import com.example.ocean.service.MeetingService;
import com.example.ocean.security.oauth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SketchMeetingController {

    @Value("${media.server.url:https://localhost:3001}")
    private String mediaServerUrl;

    private final MeetingService meetingService;

    /**
     * 스케치 회의 시작 (워크스페이스 파라미터 포함)
     */
    @GetMapping("/sketch")
    public RedirectView startSketchMeeting(
            @RequestParam(required = false) String workspaceCd,
            @AuthenticationPrincipal UserPrincipal user) {

        try {
            // 사용자 인증 확인
            if (user == null) {
                log.warn("인증되지 않은 사용자의 스케치 회의 접근 시도");
                return new RedirectView("/login");
            }

            // 워크스페이스 CD가 없으면 기본값 사용
            if (workspaceCd == null || workspaceCd.isEmpty()) {
                workspaceCd = "sketch-default";
                log.info("워크스페이스 CD가 없어 기본값 사용: {}", workspaceCd);
            }

            // 고유한 룸 ID 생성 (스케치 회의용)
            String roomId = "sketch-" + LocalDateTime.now().toString().replace(":", "-")
                    + "-" + UUID.randomUUID().toString().substring(0, 8);

            // ⭐ 미팅룸 생성 (DB에 저장)
            try {
                meetingService.createMeetingRoom(
                        roomId,
                        "스케치 회의 - " + user.getName(),
                        workspaceCd,
                        user.getId(),
                        "sketch"
                );
                log.info("스케치 미팅룸 생성 완료: roomId={}", roomId);
            } catch (Exception e) {
                log.error("스케치 미팅룸 생성 실패", e);
                return new RedirectView("/error?message=meeting-creation-failed");
            }

            // 미디어 서버로 리다이렉트할 URL 생성
            String redirectUrl = String.format(
                    "%s/ocean-video-chat-complete.html?roomId=%s&workspaceId=%s&peerId=%s&displayName=%s&meetingType=sketch",
                    mediaServerUrl,
                    URLEncoder.encode(roomId, StandardCharsets.UTF_8),
                    URLEncoder.encode(workspaceCd, StandardCharsets.UTF_8),
                    URLEncoder.encode(user.getId(), StandardCharsets.UTF_8),
                    URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)
            );

            log.info("스케치 회의 시작 - 사용자: {}, 룸ID: {}, 워크스페이스: {}",
                    user.getId(), roomId, workspaceCd);

            return new RedirectView(redirectUrl);

        } catch (Exception e) {
            log.error("스케치 회의 시작 실패", e);
            return new RedirectView("/error");
        }
    }
}
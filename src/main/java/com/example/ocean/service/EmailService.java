// ===== /service/EmailService.java =====

package com.example.ocean.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 이메일 발송 서비스
 *
 * @author Ocean Team
 * @since 2024.01.15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // TODO: JavaMailSender 주입 필요
    // private final JavaMailSender mailSender;

    /**
     * 회의 초대 이메일 발송
     *
     * @param roomId 회의실 ID
     * @param meetingTitle 회의 제목
     * @param emailList 수신자 이메일 목록
     * @param hostName 호스트 이름
     */
    public void sendMeetingInvitations(
            String roomId,
            String meetingTitle,
            List<String> emailList,
            String hostName) {

        log.info("회의 초대 이메일 발송: roomId={}, recipients={}",
                roomId, emailList.size());

        for (String email : emailList) {
            try {
                sendInvitationEmail(email, roomId, meetingTitle, hostName);
            } catch (Exception e) {
                log.error("이메일 발송 실패: {}", email, e);
            }
        }
    }

    /**
     * 개별 초대 이메일 발송
     */
    private void sendInvitationEmail(
            String to,
            String roomId,
            String meetingTitle,
            String hostName) {

        // TODO: 실제 이메일 발송 구현
        // 개발 중에는 로그로 대체
        log.info("이메일 발송 (개발모드): to={}, roomId={}, title={}",
                to, roomId, meetingTitle);

        /*
        // 실제 구현 예시:
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Ocean] " + meetingTitle + " 회의 초대");
        message.setText(buildEmailContent(roomId, meetingTitle, hostName));

        mailSender.send(message);
        */
    }

    /**
     * 이메일 내용 생성
     */
    private String buildEmailContent(
            String roomId,
            String meetingTitle,
            String hostName) {

        return String.format("""
            안녕하세요,
            
            %s님이 '%s' 회의에 초대했습니다.
            
            회의 참가 링크:
            http://localhost:8080/meeting/join?roomId=%s
            
            Ocean Team
            """, hostName, meetingTitle, roomId);
    }
}

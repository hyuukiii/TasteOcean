package com.example.ocean.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 회의 생성 요청 DTO
 *
 * @author Ocean Team
 * @since 2025.01.15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingCreateRequest {

    /**
     * 회의 제목
     */
    private String title;

    /**
     * 회의 설명
     */
    private String description;

    /**
     * 워크스페이스 코드
     */
    private String workspaceCd;

    /**
     * 회의 타입 (sketch, formal, etc.)
     */
    @Builder.Default
    private String meetingType = "sketch";

    /**
     * 예상 시간 (분 단위)
     */
    private Integer duration;

    // ===== 회의 옵션 =====

    /**
     * 자동 녹화 여부
     */
    @Builder.Default
    private boolean autoRecord = false;

    /**
     * 참가자 입장 시 음소거
     */
    @Builder.Default
    private boolean muteOnJoin = true;

    /**
     * 대기실 사용 여부
     */
    @Builder.Default
    private boolean waitingRoom = false;

    /**
     * 비디오 품질 (auto, 720, 480, 360)
     */
    @Builder.Default
    private String videoQuality = "auto";

    // ===== 초대 정보 =====

    /**
     * 초대할 멤버 ID 목록
     */
    private List<String> invitedMembers;

    /**
     * 초대할 이메일 목록
     */
    private List<String> invitedEmails;

    // ===== 예약 정보 =====

    /**
     * 예약 시간 (null이면 즉시 시작)
     */
    private LocalDateTime scheduledTime;
}

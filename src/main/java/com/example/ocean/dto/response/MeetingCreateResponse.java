package com.example.ocean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회의 생성 응답 DTO
 *
 * @author Ocean Team
 * @since 2024.01.15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingCreateResponse {

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 생성된 룸 ID
     */
    private String roomId;

    /**
     * 회의 참가 URL
     */
    private String joinUrl;

    /**
     * 사용자 표시 이름
     */
    private String displayName;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 에러 코드 (실패 시)
     */
    private String errorCode;

    /**
     * 사용자 프로필 이미지 URL
     */
    private String userProfileImg;
}

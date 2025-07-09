// ===== /dto/UserMeetingPreferences.java =====

package com.example.ocean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 회의 설정 DTO
 *
 * @author Ocean Team
 * @since 2024.01.15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMeetingPreferences {

    private String userId;

    /**
     * 기본 자동 녹화 설정
     */
    private boolean defaultAutoRecord;

    /**
     * 기본 음소거 입장 설정
     */
    private boolean defaultMuteOnJoin;

    /**
     * 기본 비디오 품질
     */
    private String defaultVideoQuality;

    /**
     * 기본 회의 시간
     */
    private Integer defaultDuration;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime updatedDate;
}

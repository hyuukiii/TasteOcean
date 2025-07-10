package com.example.ocean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveMeetingDto {

    private String roomId;
    private String title;
    private String hostId;
    private String hostName;
    private LocalDateTime startTime;
    private Integer participantCount;

    @Builder.Default
    private List<ParticipantDto> participants = new ArrayList<>();

    /**
     * 참가자 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDto {
        private String userId;
        private String displayName;
        private String profileImg;
        private Boolean isHost;
        private Boolean isVideoOn;
        private Boolean isAudioOn;
    }

    /**
     * 생성자 - 기본 정보만으로 생성
     */
    public ActiveMeetingDto(String roomId, String title, String hostId,
                            String hostName, LocalDateTime startTime, Integer participantCount) {
        this.roomId = roomId;
        this.title = title;
        this.hostId = hostId;
        this.hostName = hostName;
        this.startTime = startTime;
        this.participantCount = participantCount;
        this.participants = new ArrayList<>();
    }

    /**
     * 생성자 - 참가자 목록 포함
     */
    public ActiveMeetingDto(String roomId, String title, String hostId,
                            String hostName, LocalDateTime startTime,
                            List<ParticipantDto> participants, Integer participantCount) {
        this.roomId = roomId;
        this.title = title;
        this.hostId = hostId;
        this.hostName = hostName;
        this.startTime = startTime;
        this.participants = participants != null ? participants : new ArrayList<>();
        this.participantCount = participantCount;
    }
}

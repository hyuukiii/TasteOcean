package com.example.ocean.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveMeetingDto {
    private String roomId;
    private String title;
    private String hostId;
    private String hostName;
    private LocalDateTime startTime;
    private List<ParticipantDto> participants;
    private int participantCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDto {
        private String userId;
        private String displayName;
        private String profileImage;
        private boolean isActive;
        private LocalDateTime joinedAt;
    }
}

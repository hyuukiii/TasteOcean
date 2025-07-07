package com.example.ocean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRoomDto {
    private String roomId;
    private String title;
    private String workspaceCd;
    private String hostId;
    private String status;
    private Timestamp startTime;
    private int participantCount;
}

package com.example.ocean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingInfoDto {
    private String roomId;
    private String title;
    private String hostId;
    private String status;
    private String workspaceCd;
}

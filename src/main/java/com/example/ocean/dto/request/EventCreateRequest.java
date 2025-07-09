package com.example.ocean.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventCreateRequest {
    private String          userId;
    private String          workspaceCd;
    private String          title;
    private String          description;
    private LocalDateTime   startDatetime;
    private LocalDateTime   endDatetime;
    private String          color;
    private String          isShared;
    private String          progressStatus;
    private String          priority;
    private LocalDateTime   createdDate;
    private int             notifyTime;
    // 장소 정보 필드 추가
    private String          placeName;
    private String          address;
    private String          placeId;
    private Double          lat;
    private Double          lng;
}

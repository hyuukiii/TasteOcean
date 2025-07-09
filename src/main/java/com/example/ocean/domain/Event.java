package com.example.ocean.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import com.example.ocean.domain.Place;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder // 빌더 패턴 사용을 위해 추가
public class Event {
    private String          eventCd;            // EVENT_CD (PK)
    private String          workspaceCd;        // WORKSPACE_CD (FK)
    private String          userId;             // USER_ID (FK)
    private String          title;              // TITLE
    private String          description;        // DESCRIPTION
    private LocalDateTime   startDatetime;      // START_DATETIME
    private LocalDateTime   endDatetime;        // END_DATETIME
    private String          color;              // COLOR (ENUM: RED ORANGE YELLOW GREEN BLUE GRAY)
    private String          isShared;           // IS_SHARED ('0' or '1')
    private String          progressStatus;     // PROGRESS_STATUS (BEFORE / ING / DONE)
    private String          priority;           // PRIORITY (LOW / NORMAL / HIGH)
    private LocalDateTime   createdDate;        // CREATED_DATE
    private int             notifyTime;
    private String          userName;
    // 장소 정보 필드 추가
    private Place           place;
    private String          placeName;          // 장소 명
    private String          address;            // 장소 주소
    private String          placeId;            // 장소 ID
    private Double          lat;                // 위도
    private Double          lng;                // 경도
}
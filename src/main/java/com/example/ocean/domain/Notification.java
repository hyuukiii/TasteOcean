package com.example.ocean.domain;

import lombok.Data;

import java.util.Date;

@Data
public class Notification {

    private String notiId;        // 알림 ID (UUID 등)
    private String workspaceCd;   // 워크스페이스 코드
    private String createdBy;     // 생성자 닉네임 (JOIN 결과 포함)
    private Date createdDate;     // 생성일시
    private String notiState;     // 알림 타입 (NEW_EVENT, NEW_MEETING 등)

}

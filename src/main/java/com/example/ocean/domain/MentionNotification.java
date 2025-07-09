package com.example.ocean.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MentionNotification {
    private String notiCd;
    private String eventCd;
    private String userId;
    private String notiState;
    private String readState;
}

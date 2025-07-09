package com.example.ocean.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class MailInfo {
    private String eventCd;
    private String workspaceCd;
    private String title;
    private String description;
    private String workspaceName;
    private List<String> attendencdEmails;
}

package com.example.ocean.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class File {
    private String          fileId;
    private String          eventCd;
    private String          fileNm;
    private String          fileType;
    private String          filePath;
    private long            fileSize;
    private String          uploadedBy;
    private LocalDateTime uploadedDate;
    private String          activeState;
}

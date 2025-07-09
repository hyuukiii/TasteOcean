package com.example.ocean.dto.response;

import com.example.ocean.domain.Event;
import com.example.ocean.domain.File;
import com.example.ocean.domain.Place;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class EventDetailResponse {
    private Event event;
    private List<AttendeesInfo> attendences;
    private List<File> fileList;
    private Place place; // 장소 정보 필드 추가
}

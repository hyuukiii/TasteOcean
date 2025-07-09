package com.example.ocean.mapper;

import com.example.ocean.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    // 알림 등록
    int insertNotification(Notification dto);

    // 워크스페이스 알림 목록 조회
    List<Notification> selectNotificationsByWorkspace(@Param("workspaceCd") String workspaceCd);
}

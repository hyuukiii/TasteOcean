package com.example.ocean.service;

import com.example.ocean.domain.Notification;
import com.example.ocean.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMapper notificationMapper;

    @Autowired
    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    // 알림 생성
    public void createNotification(Notification notification) {
        log.info("📥 [NotificationService] 알림 생성 시도: {}", notification);
        int result = notificationMapper.insertNotification(notification);
        log.info("📤 [NotificationService] DB 삽입 결과: {}", result);
    }

    // 워크스페이스별 알림 목록 조회
    public List<Notification> getNotificationsByWorkspace(String workspaceCd) {
        return notificationMapper.selectNotificationsByWorkspace(workspaceCd);
    }
}
